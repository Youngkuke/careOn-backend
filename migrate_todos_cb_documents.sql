-- 투두(todos)가 cb(복지로) 제도의 필요 서류도 담을 수 있게 하는 스키마 변경 + 기존 찜 백필.
--
-- 기존 제도의 서류는 우리가 정제해 관리하는 documents 테이블의 행을 document_id로 가리킨다.
-- cb 제도의 서류는 AI 서버가 cb.cb_institutions.required_documents_ai(jsonb)에 제도마다 생성해 둔
-- 문자열이라 가리킬 행이 없다. 그래서 이름/링크를 그대로 들고 있는 칸을 추가한다.
--
-- cb 서류를 documents에 만들어 넣지 않는 이유는, 그 테이블이 발급처(document_issuers)까지 연결된
-- 마스터 데이터이기 때문이다. AI 생성 자유 텍스트를 섞으면 표기가 조금씩 다른 중복 행이 계속 쌓이고,
-- 기존 제도 쪽 서류 품질까지 같이 나빠진다.
--
-- ★ ddl-auto=update로는 안 되는 부분이 있어 이 파일이 필요하다.
--   Hibernate의 update 모드는 컬럼을 추가하기는 해도, 이미 있는 컬럼의 NOT NULL 제약을 풀어주지는 않는다.
--   document_id가 NOT NULL로 남아 있으면 cb 제도의 투두 생성이 전부 실패한다.
--
-- 실행 시점: 새 코드 배포 "전"에 실행한다. 실행 전까지는 cb 제도 찜이 저장 단계에서 실패한다.
--   (찜과 투두 생성이 한 트랜잭션이라 투두가 막히면 찜도 함께 롤백된다)
-- 여러 번 실행해도 안전하다.

-- 1) cb 서류를 담을 칸 추가
ALTER TABLE todos
    ADD COLUMN IF NOT EXISTS document_name text;

ALTER TABLE todos
    ADD COLUMN IF NOT EXISTS document_url text;

-- url의 성격. certificate_issuance(발급처) 또는 form_download(서식 내려받기).
ALTER TABLE todos
    ADD COLUMN IF NOT EXISTS document_url_type varchar(40);

-- 2) document_id의 NOT NULL 해제 (cb 제도의 서류는 이 칸이 비어 있다)
ALTER TABLE todos
    ALTER COLUMN document_id DROP NOT NULL;

-- 3) 이미 저장돼 있는 cb 제도들의 투두 백필.
--
--    새 코드는 "찜하는 시점"에 투두를 만든다. 그 코드가 나가기 전에 이미 찜해둔 제도들은 투두가 없는
--    상태로 남아 있어, 앱 투두리스트에서 서류가 하나도 안 보인다. 이 구문이 그 몫을 채운다.
--
--    이미 투두가 있는 찜은 건드리지 않는다. 사용자가 체크해 둔 상태를 덮어쓰지 않기 위해서다.
--    (그래서 서류 단위가 아니라 찜 단위로 NOT EXISTS를 본다)
--
--    jsonb_array_elements는 배열이 아닌 값을 만나면 에러를 내고, 이 함수는 WHERE보다 먼저 평가되므로
--    조건절로는 막을 수 없다. 그래서 행마다 CASE로 먼저 걸러 빈 배열로 바꾼다.
INSERT INTO todos (saved_policy_id, document_name, document_url, document_url_type, is_checked)
SELECT sp.saved_policy_id,
       btrim(doc ->> 'name'),
       doc ->> 'url',
       doc ->> 'url_type',
       false
FROM saved_policies sp
         JOIN cb.cb_institutions i ON i.serv_id = sp.serv_id
         CROSS JOIN LATERAL jsonb_array_elements(
        CASE WHEN jsonb_typeof(i.required_documents_ai) = 'array'
                 THEN i.required_documents_ai
             ELSE '[]'::jsonb END) AS doc
WHERE sp.serv_id IS NOT NULL
  AND nullif(btrim(coalesce(doc ->> 'name', '')), '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM todos t WHERE t.saved_policy_id = sp.saved_policy_id);
