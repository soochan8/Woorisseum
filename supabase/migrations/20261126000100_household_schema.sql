-- 우리씀 함께 쓰기 1차: 테이블·제약·조회 RLS.
-- Supabase SQL Editor에서 Role=postgres로 파일 전체를 한 번 실행한다.
-- 원격 DB에 아직 적용하지 않은 파일이다. 이름·키·URL을 바꿔 넣을 필요가 없다.
-- 기존 테이블·사용자를 삭제하지 않는다. 같은 이름의 객체가 있으면 중단한다.
-- 프로필 저장·초대 발급·확인·수락 RPC는 후속 마이그레이션에서 추가한다.
-- 앱의 직접 쓰기와 초대 테이블 조회는 이번 단계부터 차단한다.
-- SQL Editor 수동 실행은 Supabase CLI의 마이그레이션 이력을 자동 기록하지 않는다.
-- 이후 CLI를 연결할 때는 실제 적용 여부 확인 후 이 버전의 이력을 정합화한다.

begin;

-- 플랫폼 스키마를 만들거나 고치지 않고 실행 전제만 확인한다.
do $preflight$
begin
    if current_user <> 'postgres' then
        raise exception 'Run this migration as postgres in Supabase SQL Editor';
    end if;
    if current_setting('server_version_num')::integer < 150000 then
        raise exception 'PostgreSQL 15 or later is required';
    end if;
    if current_setting('server_encoding') <> 'UTF8' then
        raise exception 'UTF8 server encoding is required';
    end if;
    if to_regclass('auth.users') is null then
        raise exception 'Supabase auth.users must already exist';
    end if;
end;
$preflight$;

-- 이 스키마는 Supabase API의 Exposed schemas에 추가하지 않는다.
create schema woorisseum_private authorization postgres;
revoke all on schema woorisseum_private from public, anon, authenticated;
grant usage on schema woorisseum_private to authenticated;
alter default privileges for role postgres in schema woorisseum_private
    revoke execute on functions from public;

create table public.profiles (
    user_id uuid primary key references auth.users(id) on delete restrict,
    display_name text not null,
    emoji_code text not null,
    created_at timestamptz not null default transaction_timestamp(),
    updated_at timestamptz not null default transaction_timestamp(),
    revision bigint not null default 1,
    -- API 계약의 White_Space 집합. DB와 앱에 같은 코드포인트 기준을 적용한다.
    constraint profiles_display_name_valid check (
        char_length(display_name) between 1 and 30
        and display_name = btrim(display_name,
            U&'\0009\000A\000B\000C\000D\0020\0085\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000')
    ),
    constraint profiles_emoji_code_valid check (
        emoji_code in ('CAT', 'DOG', 'RABBIT', 'BEAR', 'FOX', 'PANDA', 'FROG', 'PENGUIN')
    ),
    constraint profiles_revision_positive check (revision >= 1),
    constraint profiles_timestamps_valid check (
        isfinite(created_at) and isfinite(updated_at) and updated_at >= created_at
    )
);

create table public.households (
    id uuid primary key default gen_random_uuid(),
    created_by uuid not null references public.profiles(user_id) on delete restrict,
    created_at timestamptz not null default transaction_timestamp(),
    closed_at timestamptz,
    constraint households_timestamps_valid check (
        isfinite(created_at)
        and (closed_at is null or (isfinite(closed_at) and closed_at >= created_at))
    )
);

create table public.household_members (
    user_id uuid primary key references public.profiles(user_id) on delete restrict,
    household_id uuid not null references public.households(id) on delete restrict,
    slot smallint not null,
    joined_at timestamptz not null default transaction_timestamp(),
    constraint household_members_slot_valid check (slot in (1, 2)),
    constraint household_members_household_slot_unique unique (household_id, slot),
    constraint household_members_joined_at_finite check (isfinite(joined_at))
);

create table public.household_invitations (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete restrict,
    created_by uuid not null references public.profiles(user_id) on delete restrict,
    create_request_id uuid not null,
    token_hash bytea not null,
    created_at timestamptz not null default transaction_timestamp(),
    expires_at timestamptz not null,
    accepted_by uuid references public.profiles(user_id) on delete restrict,
    accepted_at timestamptz,
    revoked_at timestamptz,
    constraint household_invitations_request_unique unique (created_by, create_request_id),
    constraint household_invitations_token_unique unique (token_hash),
    constraint household_invitations_token_length check (octet_length(token_hash) = 32),
    constraint household_invitations_expiry_valid check (
        isfinite(created_at) and isfinite(expires_at)
        and expires_at = created_at + interval '24 hours'
    ),
    constraint household_invitations_acceptance_pair check (
        (accepted_by is null) = (accepted_at is null)
    ),
    constraint household_invitations_not_self check (
        accepted_by is null or accepted_by <> created_by
    ),
    constraint household_invitations_single_terminal_state check (
        accepted_at is null or revoked_at is null
    ),
    constraint household_invitations_accepted_at_valid check (
        accepted_at is null or (
            isfinite(accepted_at) and accepted_at >= created_at and accepted_at < expires_at
        )
    ),
    constraint household_invitations_revoked_at_valid check (
        revoked_at is null or (isfinite(revoked_at) and revoked_at >= created_at)
    )
);

-- now()를 인덱스 조건에 넣지 않는다. 만료된 미처리 초대도 재발급 시 무효화한다.
create unique index household_invitations_one_pending_per_household
    on public.household_invitations (household_id)
    where accepted_at is null and revoked_at is null;

alter table public.profiles enable row level security;
alter table public.households enable row level security;
alter table public.household_members enable row level security;
alter table public.household_invitations enable row level security;

-- Supabase 기본 권한과 무관하게 앱·미인증 사용자의 직접 접근을 명시적으로 제한한다.
revoke all on table public.profiles, public.households,
    public.household_members, public.household_invitations
    from public, anon, authenticated;

-- 화면에 필요한 열만 조회 허용. 쓰기는 검증·잠금을 수행하는 후속 RPC로만 제공한다.
grant select (user_id, display_name, emoji_code, revision) on public.profiles to authenticated;
grant select (id) on public.households to authenticated;
grant select (user_id, household_id) on public.household_members to authenticated;

-- SECURITY DEFINER의 소유자는 위에서 확인한 postgres다.
-- RLS가 자기 구성원 테이블을 다시 조회하며 재귀하지 않도록 제한된 헬퍼에서만 우회한다.
-- 호출자 ID 인자를 받지 않고 검증된 세션의 auth.uid()만 사용한다.
create function woorisseum_private.current_household_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $function$
    select m.household_id
    from public.household_members as m
    join public.households as h on h.id = m.household_id
    where m.user_id = (select auth.uid())
      and h.closed_at is null;
$function$;

revoke all on function woorisseum_private.current_household_id() from public, anon, authenticated;
grant execute on function woorisseum_private.current_household_id() to authenticated;

create policy profiles_read_self_or_household_peer
on public.profiles for select to authenticated
using (
    user_id = (select auth.uid())
    or exists (
        select 1 from public.household_members as peer
        where peer.user_id = profiles.user_id
          and peer.household_id = (select woorisseum_private.current_household_id())
    )
);

create policy households_read_current
on public.households for select to authenticated
using (id = (select woorisseum_private.current_household_id()));

create policy household_members_read_current
on public.household_members for select to authenticated
using (household_id = (select woorisseum_private.current_household_id()));

-- household_invitations에는 앱용 정책·권한을 부여하지 않는다.
-- 토큰 확인과 발급자 표시 정보는 후속 RPC에서 검증한 뒤 제한적으로 반환한다.
-- 이 단계만 적용한 상태에서는 앱이 프로필·공유 공간·초대를 생성할 수 없다.

commit;

-- 실행 결과: 네 테이블 각각 rls_enabled=true가 표시되어야 한다.
select c.relname as table_name, c.relrowsecurity as rls_enabled
from pg_catalog.pg_class as c
join pg_catalog.pg_namespace as n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relkind = 'r'
  and c.relname in ('profiles', 'households', 'household_members', 'household_invitations')
order by c.relname;
