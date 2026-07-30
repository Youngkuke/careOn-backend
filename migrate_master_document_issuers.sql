-- 발급처가 비어 있던 마스터 서류 4건을 채운다.
-- 이 4건은 앱에서 "발급처 확인 필요"로 표시되고 있었다.

-- 1) "제출할 서류가 없다"는 뜻의 행을 위한 발급처.
--    기존 "따로 발급 불필요"(신분증 사본 등)와는 다르다. 그쪽은 낼 서류는 있고 발급만 안 받아도 되는
--    경우이고, 이건 아예 낼 서류가 없는 경우다.
insert into document_issuers (issuer_name, issuer_site, issue_guide)
select '별도 서류 제출 불필요', null, '별도 서류 제출 불필요'
where not exists (
    select 1 from document_issuers where issuer_name = '별도 서류 제출 불필요'
);

-- 2) 서류가 아니라 안내 문구인 행 → 제출할 게 없다고 알린다.
insert into document_issues (document_id, document_issuer_id)
select d.document_id, i.document_issuer_id
from documents d
cross join document_issuers i
where d.document_name in ('별도 서류 제출 불필요 (공공마이데이터 동의 처리)', '제출서류 없음')
  and i.issuer_name = '별도 서류 제출 불필요'
  and not exists (
      select 1 from document_issues e where e.document_id = d.document_id
  );

-- 3) 발급처가 상황마다 달라 하나로 못 정하는 행 → 공고를 보라고 안내한다.
--    "공고문 참조"는 이름 자체가 그 뜻이고, "성적표"는 학교냐 시험기관이냐가 제도마다 다르다.
insert into document_issues (document_id, document_issuer_id)
select d.document_id, i.document_issuer_id
from documents d
cross join document_issuers i
where d.document_name in ('공고문 참조', '성적표')
  and i.issuer_name = '신청 공고 확인'
  and not exists (
      select 1 from document_issues e where e.document_id = d.document_id
  );
