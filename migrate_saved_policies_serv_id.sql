-- 찜(saved_policies)이 cb(복지로) 제도도 가리킬 수 있게 하는 스키마 변경.
--
-- 기존 제도는 policy_id(숫자)로, cb 제도는 serv_id(문자열)로 가리킨다. 둘 중 하나만 채워진다.
--
-- ★ ddl-auto=update로는 안 되는 부분이 있어 이 파일이 필요하다.
--   Hibernate의 update 모드는 컬럼을 추가하기는 해도, 이미 있는 컬럼의 NOT NULL 제약을 풀어주지는 않는다.
--   policy_id가 NOT NULL로 남아 있으면 cb 제도 찜이 전부 저장 실패한다.
--
-- 실행 시점: 새 코드 배포 "전"에 실행해도 되고 직후에 해도 되지만, 실행 전까지는 cb 제도 찜이 실패한다.
-- 여러 번 실행해도 안전하다.

-- 1) cb 제도 식별자 컬럼 추가
ALTER TABLE saved_policies
    ADD COLUMN IF NOT EXISTS serv_id varchar(100);

-- 2) policy_id의 NOT NULL 해제 (cb 제도를 찜하면 이 칸이 비어 있다)
ALTER TABLE saved_policies
    ALTER COLUMN policy_id DROP NOT NULL;

-- 3) 같은 사람이 같은 cb 제도를 두 번 찜하지 못하게 막는다.
--    Postgres는 유니크 제약에서 NULL을 서로 다른 값으로 취급하므로,
--    기존 제도 찜(serv_id가 NULL)끼리는 이 제약에 걸리지 않는다.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_saved_policies_carer_serv'
    ) THEN
        ALTER TABLE saved_policies
            ADD CONSTRAINT uk_saved_policies_carer_serv UNIQUE (carer_id, serv_id);
    END IF;
END $$;

-- 4) 둘 중 정확히 하나만 채워지도록 DB에서도 막는다.
--    애플리케이션에서 검증하지만, 잘못된 데이터가 들어가면 목록 조회가 통째로 깨지는 자리라 한 겹 더 둔다.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_saved_policies_target'
    ) THEN
        ALTER TABLE saved_policies
            ADD CONSTRAINT ck_saved_policies_target
            CHECK ((policy_id IS NULL) <> (serv_id IS NULL));
    END IF;
END $$;

-- 확인용
-- SELECT COUNT(*) FILTER (WHERE policy_id IS NOT NULL) AS 기존제도찜,
--        COUNT(*) FILTER (WHERE serv_id IS NOT NULL)   AS cb제도찜
-- FROM saved_policies;
