-- 우리씀 함께 쓰기 2차: 내 프로필 저장·수정과 현재 상태 조회 RPC.
-- 1차 household_schema.sql 적용 후 SQL Editor의 새 Query에서 전체를 한 번 실행한다.
-- Role=postgres. 키·URL·사용자 ID를 넣거나 1차 파일을 다시 실행할 필요가 없다.
-- 기존 테이블·사용자·기록은 삭제하지 않는다. 초대 발급·확인·수락은 다음 단계다.

begin;

do $preflight$
begin
    if current_user <> 'postgres' then
        raise exception 'Run this migration as postgres in Supabase SQL Editor';
    end if;
    if to_regclass('public.profiles') is null
       or to_regclass('public.households') is null
       or to_regclass('public.household_members') is null
       or to_regclass('public.household_invitations') is null
       or to_regprocedure('woorisseum_private.current_household_id()') is null then
        raise exception 'Apply household_schema migration first';
    end if;
end;
$preflight$;

-- 후속 초대 함수도 이 잠금 규칙을 공유한다.
-- 행이 없는 최초 프로필 생성도 직렬화하고, 잠금 키 자체를 정렬해 교착 순서를 통일한다.
create function woorisseum_private.lock_users(p_user_ids uuid[])
returns void
language plpgsql
volatile
set search_path = ''
as $function$
declare
    v_key bigint;
begin
    if p_user_ids is null or cardinality(p_user_ids) = 0
       or array_position(p_user_ids, null) is not null then
        raise exception 'User lock requires non-null user IDs';
    end if;
    for v_key in
        select distinct pg_catalog.hashtextextended('woorisseum:user:' || ids.user_id::text, 0)
        from unnest(p_user_ids) as ids(user_id)
        order by 1
    loop
        perform pg_catalog.pg_advisory_xact_lock(v_key);
    end loop;
end;
$function$;

create function woorisseum_private.profile_json(p_profile public.profiles)
returns jsonb
language sql
immutable
strict
set search_path = ''
as $function$
    select jsonb_build_object(
        'user_id', p_profile.user_id,
        'display_name', p_profile.display_name,
        'emoji_code', p_profile.emoji_code,
        'revision', p_profile.revision
    );
$function$;

revoke all on function woorisseum_private.lock_users(uuid[]) from public, anon, authenticated;
revoke all on function woorisseum_private.profile_json(public.profiles) from public, anon, authenticated;

create function public.save_my_profile_v1(
    p_display_name text,
    p_emoji_code text,
    p_expected_revision bigint
)
returns jsonb
language plpgsql
volatile
security definer
set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_name text;
    v_profile public.profiles%rowtype;
begin
    -- SECURITY DEFINER여도 호출자의 검증된 사용자 ID만 사용한다.
    if v_user_id is null or not exists (select 1 from auth.users where id = v_user_id) then
        raise exception using errcode = '42501', message = 'Authentication required';
    end if;

    v_name := btrim(p_display_name,
        U&'\0009\000A\000B\000C\000D\0020\0085\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000');
    if v_name is null or char_length(v_name) not between 1 and 30 then
        return jsonb_build_object('ok', false, 'data', null,
            'error', jsonb_build_object('code', 'INVALID_ARGUMENT', 'field', 'display_name'));
    end if;
    if p_emoji_code is null or p_emoji_code not in
        ('CAT', 'DOG', 'RABBIT', 'BEAR', 'FOX', 'PANDA', 'FROG', 'PENGUIN') then
        return jsonb_build_object('ok', false, 'data', null,
            'error', jsonb_build_object('code', 'INVALID_ARGUMENT', 'field', 'emoji_code'));
    end if;
    if p_expected_revision is not null and p_expected_revision < 1 then
        return jsonb_build_object('ok', false, 'data', null,
            'error', jsonb_build_object('code', 'INVALID_ARGUMENT', 'field', 'expected_revision'));
    end if;

    perform woorisseum_private.lock_users(array[v_user_id]);
    select * into v_profile from public.profiles where user_id = v_user_id for update;

    if not found then
        if p_expected_revision is not null then
            return jsonb_build_object('ok', false, 'data', null,
                'error', jsonb_build_object('code', 'PROFILE_CONFLICT', 'field', null));
        end if;
        insert into public.profiles(user_id, display_name, emoji_code)
        values (v_user_id, v_name, p_emoji_code)
        returning * into v_profile;
    elsif v_profile.display_name = v_name and v_profile.emoji_code = p_emoji_code then
        -- 같은 입력의 재전송은 기존 결과를 반환하고 revision을 올리지 않는다.
        null;
    elsif p_expected_revision is null or p_expected_revision <> v_profile.revision
          or v_profile.revision = 9223372036854775807 then
        return jsonb_build_object('ok', false, 'data', null,
            'error', jsonb_build_object('code', 'PROFILE_CONFLICT', 'field', null));
    else
        update public.profiles
        set display_name = v_name,
            emoji_code = p_emoji_code,
            revision = revision + 1,
            updated_at = greatest(clock_timestamp(), updated_at)
        where user_id = v_user_id
        returning * into v_profile;
    end if;

    return jsonb_build_object('ok', true,
        'data', jsonb_build_object('profile', woorisseum_private.profile_json(v_profile)),
        'error', null);
end;
$function$;

create function public.get_together_state_v1()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $function$
declare
    v_user_id uuid := auth.uid();
    v_profile public.profiles%rowtype;
    v_household_id uuid;
    v_household public.households%rowtype;
    v_members jsonb;
    v_member_count bigint;
    v_profile_count bigint;
    v_pending public.household_invitations%rowtype;
    v_invitation jsonb := null;
    v_state text;
begin
    -- STABLE 함수 안의 조회는 호출 문장에 대한 동일 스냅샷을 사용한다.
    if v_user_id is null or not exists (select 1 from auth.users where id = v_user_id) then
        raise exception using errcode = '42501', message = 'Authentication required';
    end if;
    select * into v_profile from public.profiles where user_id = v_user_id;
    select household_id into v_household_id from public.household_members where user_id = v_user_id;

    if v_household_id is null then
        return jsonb_build_object('ok', true, 'error', null, 'data', jsonb_build_object(
            'profile', case when v_profile.user_id is null then null
                            else woorisseum_private.profile_json(v_profile) end,
            'connection_state', 'NOT_CONNECTED', 'household', null, 'invitation', null
        ));
    end if;

    select * into v_household from public.households where id = v_household_id;
    if v_profile.user_id is null or v_household.id is null or v_household.closed_at is not null then
        raise exception using errcode = 'XX000', message = 'Household state is inconsistent';
    end if;

    select count(*) into v_member_count
    from public.household_members where household_id = v_household_id;
    select count(p.user_id), jsonb_agg(woorisseum_private.profile_json(p) order by p.user_id)
    into v_profile_count, v_members
    from public.household_members as m
    join public.profiles as p on p.user_id = m.user_id
    where m.household_id = v_household_id;
    if v_member_count not between 1 and 2 or v_profile_count <> v_member_count then
        raise exception using errcode = 'XX000', message = 'Household state is inconsistent';
    end if;

    select * into v_pending from public.household_invitations
    where household_id = v_household_id and accepted_at is null and revoked_at is null;

    if v_member_count = 2 then
        if v_pending.id is not null then
            raise exception using errcode = 'XX000', message = 'Household state is inconsistent';
        end if;
        v_state := 'CONNECTED';
    else
        v_state := 'INVITING';
        if v_pending.id is not null then
            if v_pending.created_by <> v_user_id then
                raise exception using errcode = 'XX000', message = 'Household state is inconsistent';
            end if;
            v_invitation := jsonb_build_object(
                'invitation_id', v_pending.id,
                'status', case when statement_timestamp() >= v_pending.expires_at
                               then 'EXPIRED' else 'AVAILABLE' end,
                'expires_at', to_char(v_pending.expires_at at time zone 'UTC',
                    'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
            );
        end if;
    end if;

    return jsonb_build_object('ok', true, 'error', null, 'data', jsonb_build_object(
        'profile', woorisseum_private.profile_json(v_profile),
        'connection_state', v_state,
        'household', jsonb_build_object('household_id', v_household_id, 'members', v_members),
        'invitation', v_invitation
    ));
end;
$function$;

-- 함수 생성 직후 PUBLIC 기본 실행 권한을 회수하고 인증 사용자만 허용한다.
revoke all on function public.save_my_profile_v1(text, text, bigint) from public, anon, authenticated;
revoke all on function public.get_together_state_v1() from public, anon, authenticated;
grant execute on function public.save_my_profile_v1(text, text, bigint) to authenticated;
grant execute on function public.get_together_state_v1() to authenticated;

-- 앱의 직접 테이블 쓰기 차단은 1차 SQL 그대로 유지한다.
notify pgrst, 'reload schema';
commit;

-- 성공 시 두 행, authenticated_can_execute=true / anon_can_execute=false.
select p.proname as function_name,
       has_function_privilege('authenticated', p.oid, 'EXECUTE') as authenticated_can_execute,
       has_function_privilege('anon', p.oid, 'EXECUTE') as anon_can_execute
from pg_catalog.pg_proc as p
join pg_catalog.pg_namespace as n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname in ('save_my_profile_v1', 'get_together_state_v1')
order by p.proname;
