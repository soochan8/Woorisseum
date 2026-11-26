-- 우리씀 함께 쓰기 3차: 초대 발급·확인·수락 RPC.
-- 1·2차 적용 후 SQL Editor의 새 Query에서 전체를 한 번 실행한다. Role=postgres.
-- 키·URL·사용자 ID를 넣지 않는다. 기존 기록을 삭제하거나 앱 로컬 DB를 변경하지 않는다.
-- pgcrypto는 PostgreSQL 암호 기능 확장이며 유료 서비스 가입을 수행하지 않는다.

begin;
do $preflight$
begin
    if current_user <> 'postgres' then
        raise exception 'Run this migration as postgres in Supabase SQL Editor';
    end if;
    if to_regprocedure('public.save_my_profile_v1(text,text,bigint)') is null
       or to_regprocedure('public.get_together_state_v1()') is null
       or to_regprocedure('woorisseum_private.lock_users(uuid[])') is null then
        raise exception 'Apply household_schema and household_profile_state first';
    end if;
end;
$preflight$;

create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;

-- 기존 pgcrypto의 설치 스키마를 이동하지 않고 실제 확장 함수의 스키마를 사용한다.
do $crypto$
declare
    v_schema text;
begin
    select n.nspname into strict v_schema
    from pg_catalog.pg_extension as e
    join pg_catalog.pg_namespace as n on n.oid = e.extnamespace
    where e.extname = 'pgcrypto';
    execute format($definition$
        create function woorisseum_private.invitation_random_bytes()
        returns bytea language sql volatile set search_path = ''
        as %L
    $definition$, format('select %I.gen_random_bytes(32)', v_schema));
end;
$crypto$;

create function woorisseum_private.invitation_failure(p_code text, p_field text default null)
returns jsonb language sql immutable set search_path = ''
as $function$
    select jsonb_build_object('ok', false, 'data', null,
        'error', jsonb_build_object('code', p_code, 'field', p_field));
$function$;

-- 정규형 43자만 32바이트로 복원한다. 해시 대상은 URL 문자열이 아닌 복원된 32바이트다.
create function woorisseum_private.invitation_token_bytes(p_token text)
returns bytea language plpgsql immutable set search_path = ''
as $function$
declare
    v_bytes bytea;
begin
    if p_token is null or char_length(p_token) <> 43 or p_token !~ '^[A-Za-z0-9_-]{43}$' then
        return null;
    end if;
    v_bytes := decode(translate(p_token, '-_', '+/') || '=', 'base64');
    if octet_length(v_bytes) <> 32
       or rtrim(translate(encode(v_bytes, 'base64'), '+/', '-_'), '=') <> p_token then
        return null;
    end if;
    return v_bytes;
end;
$function$;

-- 서버 업무 테이블을 추가한 뒤에도 데이터가 있는 준비 공간을 자동 폐쇄하지 않는다.
-- households를 참조하는 새 FK 테이블의 실제 행을 확인한다. 현재 관계·초대는 제외한다.
-- 향후 서버 쓰기도 동일 공간 행을 잠그고 열린 공간인지 확인하는 규칙을 공유해야 한다.
create function woorisseum_private.household_has_business_data(p_household_id uuid)
returns boolean language plpgsql stable set search_path = ''
as $function$
declare
    v_ref record;
    v_has_data boolean;
begin
    for v_ref in
        select n.nspname, c.relname, a.attname, cardinality(fk.conkey) as key_count
        from pg_catalog.pg_constraint as fk
        join pg_catalog.pg_class as c on c.oid = fk.conrelid
        join pg_catalog.pg_namespace as n on n.oid = c.relnamespace
        join pg_catalog.pg_attribute as a on a.attrelid = c.oid and a.attnum = fk.conkey[1]
        where fk.contype = 'f' and fk.confrelid = 'public.households'::regclass
          and fk.conrelid not in ('public.household_members'::regclass, 'public.household_invitations'::regclass)
    loop
        if v_ref.key_count <> 1 then
            return true; -- 지원하지 않는 복합 참조를 비어 있다고 판단하지 않는다.
        end if;
        execute format('select exists(select 1 from %I.%I where %I = $1)',
                       v_ref.nspname, v_ref.relname, v_ref.attname)
        into v_has_data using p_household_id;
        if v_has_data then return true; end if;
    end loop;
    return false;
end;
$function$;

create function public.create_household_invitation_v1(p_request_id uuid)
returns jsonb language plpgsql volatile security definer set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_household_id uuid;
    v_household public.households%rowtype;
    v_count bigint;
    v_bytes bytea;
    v_token text;
    v_invitation public.household_invitations%rowtype;
    v_now timestamptz;
begin
    if v_user_id is null or not exists(select 1 from auth.users where id = v_user_id) then
        raise exception using errcode = '42501', message = 'Authentication required';
    end if;
    if p_request_id is null then
        return woorisseum_private.invitation_failure('INVALID_ARGUMENT', 'request_id');
    end if;
    perform woorisseum_private.lock_users(array[v_user_id]);
    if not exists(select 1 from public.profiles where user_id = v_user_id) then
        return woorisseum_private.invitation_failure('PROFILE_REQUIRED');
    end if;
    if exists(select 1 from public.household_invitations
              where created_by = v_user_id and create_request_id = p_request_id) then
        return woorisseum_private.invitation_failure('ISSUANCE_RESULT_UNAVAILABLE');
    end if;

    select household_id into v_household_id from public.household_members where user_id = v_user_id;
    if v_household_id is not null then
        select * into v_household from public.households where id = v_household_id for update;
        if v_household.id is null or v_household.closed_at is not null then
            raise exception using errcode = 'XX000', message = 'Household state is inconsistent';
        end if;
        select count(*) into v_count from public.household_members where household_id = v_household_id;
        if v_count = 2 then return woorisseum_private.invitation_failure('ALREADY_CONNECTED'); end if;
        if v_count <> 1 then
            raise exception using errcode = 'XX000', message = 'Household state is inconsistent';
        end if;
    end if;

    -- 실패 가능성이 있는 난수 생성부터 수행한다. 이후 예외는 트랜잭션 전체를 중단한다.
    v_bytes := woorisseum_private.invitation_random_bytes();
    v_token := rtrim(translate(encode(v_bytes, 'base64'), '+/', '-_'), '=');
    v_now := clock_timestamp();
    if v_household_id is null then
        insert into public.households(created_by, created_at)
        values (v_user_id, v_now) returning id into v_household_id;
        insert into public.household_members(user_id, household_id, slot, joined_at)
        values (v_user_id, v_household_id, 1, v_now);
    end if;
    perform id from public.household_invitations
    where household_id = v_household_id and accepted_at is null and revoked_at is null
    order by id for update;
    update public.household_invitations set revoked_at = v_now
    where household_id = v_household_id and accepted_at is null and revoked_at is null;
    insert into public.household_invitations(
        household_id, created_by, create_request_id, token_hash, created_at, expires_at)
    values (v_household_id, v_user_id, p_request_id, sha256(v_bytes), v_now, v_now + interval '24 hours')
    returning * into v_invitation;
    return jsonb_build_object('ok', true, 'error', null, 'data', jsonb_build_object(
        'invitation_id', v_invitation.id, 'household_id', v_household_id, 'token', v_token,
        'expires_at', to_char(v_invitation.expires_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
    ));
end;
$function$;

create function public.preview_household_invitation_v1(p_token text)
returns jsonb language plpgsql stable security definer set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_bytes bytea;
    v_invitation public.household_invitations%rowtype;
    v_own_state jsonb;
    v_inviter_household_id uuid;
    v_inviter public.profiles%rowtype;
    v_target public.households%rowtype;
    v_count bigint;
begin
    if v_user_id is null or not exists(select 1 from auth.users where id = v_user_id) then
        raise exception using errcode = '42501', message = 'Authentication required';
    end if;
    v_bytes := woorisseum_private.invitation_token_bytes(p_token);
    if v_bytes is null then return woorisseum_private.invitation_failure('INVALID_ARGUMENT', 'token'); end if;
    select * into v_invitation from public.household_invitations where token_hash = sha256(v_bytes);
    if not found then return woorisseum_private.invitation_failure('INVITATION_NOT_FOUND'); end if;
    if v_invitation.created_by = v_user_id then return woorisseum_private.invitation_failure('SELF_INVITATION'); end if;

    v_own_state := public.get_together_state_v1()->'data';
    select household_id into v_inviter_household_id from public.household_members
    where user_id = v_invitation.created_by;
    if v_own_state->>'connection_state' = 'CONNECTED'
       and (v_own_state->'household'->>'household_id')::uuid = v_inviter_household_id then
        return woorisseum_private.invitation_failure('ALREADY_CONNECTED');
    end if;
    if v_invitation.accepted_at is not null then return woorisseum_private.invitation_failure('INVITATION_USED'); end if;
    if v_invitation.revoked_at is not null then return woorisseum_private.invitation_failure('INVITATION_REVOKED'); end if;
    if statement_timestamp() >= v_invitation.expires_at then return woorisseum_private.invitation_failure('INVITATION_EXPIRED'); end if;
    if v_own_state->>'connection_state' = 'CONNECTED' then return woorisseum_private.invitation_failure('ALREADY_CONNECTED'); end if;
    select * into v_target from public.households where id = v_invitation.household_id;
    if v_target.id is null or v_target.closed_at is not null
       or v_inviter_household_id is distinct from v_target.id then
        return woorisseum_private.invitation_failure('INVITATION_REVOKED');
    end if;
    select count(*) into v_count from public.household_members where household_id = v_target.id;
    if v_count >= 2 then return woorisseum_private.invitation_failure('HOUSEHOLD_FULL'); end if;
    if v_count <> 1 then raise exception using errcode = 'XX000', message = 'Household state is inconsistent'; end if;
    if v_own_state->>'connection_state' = 'INVITING'
       and woorisseum_private.household_has_business_data((v_own_state->'household'->>'household_id')::uuid) then
        return woorisseum_private.invitation_failure('CURRENT_HOUSEHOLD_HAS_DATA');
    end if;
    select * into strict v_inviter from public.profiles where user_id = v_invitation.created_by;
    return jsonb_build_object('ok', true, 'error', null, 'data', jsonb_build_object(
        'invitation_id', v_invitation.id, 'household_id', v_target.id,
        'inviter', jsonb_build_object('display_name', v_inviter.display_name, 'emoji_code', v_inviter.emoji_code),
        'expires_at', to_char(v_invitation.expires_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
    ));
end;
$function$;

create function public.accept_household_invitation_v1(p_token text)
returns jsonb language plpgsql volatile security definer set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_bytes bytea;
    v_invitation public.household_invitations%rowtype;
    v_own_state jsonb;
    v_old_household_id uuid;
    v_inviter_household_id uuid;
    v_target public.households%rowtype;
    v_count bigint;
    v_slot smallint;
    v_now timestamptz;
    v_rows bigint;
    v_outcome text;
begin
    if v_user_id is null or not exists(select 1 from auth.users where id = v_user_id) then
        raise exception using errcode = '42501', message = 'Authentication required';
    end if;
    v_bytes := woorisseum_private.invitation_token_bytes(p_token);
    if v_bytes is null then return woorisseum_private.invitation_failure('INVALID_ARGUMENT', 'token'); end if;
    -- 잠금 전에는 불변인 발급자·대상 ID만 찾는다. 이때 초대 행부터 잠그지 않는다.
    select * into v_invitation from public.household_invitations where token_hash = sha256(v_bytes);
    if not found then return woorisseum_private.invitation_failure('INVITATION_NOT_FOUND'); end if;
    if v_invitation.created_by = v_user_id then return woorisseum_private.invitation_failure('SELF_INVITATION'); end if;

    perform woorisseum_private.lock_users(array[v_user_id, v_invitation.created_by]);
    select household_id into v_old_household_id from public.household_members where user_id = v_user_id;
    -- 사용자 잠금 → 공간 ID 오름차순 → 초대 ID 오름차순. 발급도 같은 선행 순서를 지킨다.
    perform id from public.households
    where id = any(array_remove(array[v_old_household_id, v_invitation.household_id], null))
    order by id for update;
    perform id from public.household_invitations
    where id = v_invitation.id
       or (household_id = any(array_remove(array[v_old_household_id, v_invitation.household_id], null))
           and accepted_at is null and revoked_at is null)
    order by id for update;
    select * into strict v_invitation from public.household_invitations where id = v_invitation.id;
    v_own_state := public.get_together_state_v1()->'data';
    select household_id into v_inviter_household_id from public.household_members
    where user_id = v_invitation.created_by;

    -- 이미 확정된 본인 관계만 반환한다. 옛 토큰으로 새 권한을 부여하지 않는다.
    if v_own_state->>'connection_state' = 'CONNECTED'
       and (v_own_state->'household'->>'household_id')::uuid = v_inviter_household_id then
        if v_invitation.accepted_by = v_user_id and v_invitation.household_id = v_inviter_household_id then
            v_outcome := 'ALREADY_JOINED';
        else
            v_outcome := 'ALREADY_CONNECTED';
        end if;
        return jsonb_build_object('ok', true, 'error', null,
            'data', jsonb_build_object('outcome', v_outcome, 'household', v_own_state->'household'));
    end if;
    if v_invitation.accepted_at is not null then return woorisseum_private.invitation_failure('INVITATION_USED'); end if;
    if v_invitation.revoked_at is not null then return woorisseum_private.invitation_failure('INVITATION_REVOKED'); end if;
    if clock_timestamp() >= v_invitation.expires_at then return woorisseum_private.invitation_failure('INVITATION_EXPIRED'); end if;
    if v_own_state->>'connection_state' = 'CONNECTED' then return woorisseum_private.invitation_failure('ALREADY_CONNECTED'); end if;
    select * into v_target from public.households where id = v_invitation.household_id;
    if v_target.id is null or v_target.closed_at is not null
       or v_inviter_household_id is distinct from v_target.id then
        return woorisseum_private.invitation_failure('INVITATION_REVOKED');
    end if;
    select count(*) into v_count from public.household_members where household_id = v_target.id;
    if v_count >= 2 then return woorisseum_private.invitation_failure('HOUSEHOLD_FULL'); end if;
    if v_count <> 1 then raise exception using errcode = 'XX000', message = 'Household state is inconsistent'; end if;
    if v_old_household_id is not null
       and woorisseum_private.household_has_business_data(v_old_household_id) then
        return woorisseum_private.invitation_failure('CURRENT_HOUSEHOLD_HAS_DATA');
    end if;
    if not exists(select 1 from public.profiles where user_id = v_user_id) then
        return woorisseum_private.invitation_failure('PROFILE_REQUIRED');
    end if;
    select s.slot::smallint into v_slot from generate_series(1,2) as s(slot)
    where not exists(select 1 from public.household_members as m
                     where m.household_id = v_target.id and m.slot = s.slot)
    order by s.slot limit 1;
    if v_slot is null then return woorisseum_private.invitation_failure('HOUSEHOLD_FULL'); end if;
    -- 모든 잠금·조회 후, 첫 변경 직전에 만료를 다시 판단한다.
    v_now := clock_timestamp();
    if v_now >= v_invitation.expires_at then return woorisseum_private.invitation_failure('INVITATION_EXPIRED'); end if;

    -- 이 아래에는 정상 실패 반환이 없다. 예외는 부분 변경을 포함해 전체를 롤백한다.
    if v_old_household_id is not null then
        update public.household_invitations set revoked_at = v_now
        where household_id = v_old_household_id and accepted_at is null and revoked_at is null;
        delete from public.household_members where user_id = v_user_id and household_id = v_old_household_id;
        get diagnostics v_rows = row_count;
        if v_rows <> 1 then raise exception using errcode = 'XX000', message = 'Household state is inconsistent'; end if;
        update public.households set closed_at = v_now where id = v_old_household_id;
    end if;
    insert into public.household_members(user_id, household_id, slot, joined_at)
    values (v_user_id, v_target.id, v_slot, v_now);
    update public.household_invitations set accepted_by = v_user_id, accepted_at = v_now
    where id = v_invitation.id and accepted_at is null and revoked_at is null;
    get diagnostics v_rows = row_count;
    if v_rows <> 1 then raise exception using errcode = 'XX000', message = 'Household state is inconsistent'; end if;
    update public.household_invitations set revoked_at = v_now
    where household_id = v_target.id and accepted_at is null and revoked_at is null;

    v_own_state := public.get_together_state_v1()->'data';
    return jsonb_build_object('ok', true, 'error', null,
        'data', jsonb_build_object('outcome', 'JOINED', 'household', v_own_state->'household'));
end;
$function$;

revoke all on function woorisseum_private.invitation_random_bytes() from public, anon, authenticated;
revoke all on function woorisseum_private.invitation_failure(text,text) from public, anon, authenticated;
revoke all on function woorisseum_private.invitation_token_bytes(text) from public, anon, authenticated;
revoke all on function woorisseum_private.household_has_business_data(uuid) from public, anon, authenticated;
revoke all on function public.create_household_invitation_v1(uuid) from public, anon, authenticated;
revoke all on function public.preview_household_invitation_v1(text) from public, anon, authenticated;
revoke all on function public.accept_household_invitation_v1(text) from public, anon, authenticated;
grant execute on function public.create_household_invitation_v1(uuid) to authenticated;
grant execute on function public.preview_household_invitation_v1(text) to authenticated;
grant execute on function public.accept_household_invitation_v1(text) to authenticated;
notify pgrst, 'reload schema';
commit;

-- 성공 시 세 행: authenticated_can_execute=true, anon_can_execute=false.
select p.proname as function_name,
       has_function_privilege('authenticated', p.oid, 'EXECUTE') as authenticated_can_execute,
       has_function_privilege('anon', p.oid, 'EXECUTE') as anon_can_execute
from pg_catalog.pg_proc as p join pg_catalog.pg_namespace as n on n.oid = p.pronamespace
where n.nspname = 'public' and p.proname in (
    'create_household_invitation_v1', 'preview_household_invitation_v1', 'accept_household_invitation_v1')
order by p.proname;
