-- cb 제도 서류의 발급처를 이름으로 잇는다.
--
-- cb 서류는 AI 생성 자유 텍스트라 documents 테이블에 행이 없다. documents에 넣으면 표기만 다른
-- 중복 행이 쌓여 마스터가 무너지므로(개인정보동의서 4종이 이미 그런 상태), 이름-발급처 매핑만 따로 둔다.
-- 발급처 자체는 document_issuers를 그대로 쓴다. 발급처 목록이 두 벌로 갈라지면 안 된다.

-- 1) 기존 발급처 이름 정리.
--    "해당 기관 양식"은 신청서·동의서 19건이 물고 있는데, 사용자가 알아야 할 건 "양식"이 아니라
--    "제도 공고를 보라"는 행동이다. 물린 19건 전부 신청서·동의서라 뜻이 어긋나지 않는다.
update document_issuers set issuer_name = '신청 공고 확인' where document_issuer_id = 5;

-- 2) 새 발급처.
--    "따로 발급 불필요"는 소지 중인 카드(복지카드·의료급여증)와 복사만 하면 되는 신분증 사본을 함께 덮는다.
--    "본인 소지"로 하지 않은 이유는 신분증 사본을 이미 갖고 있는 것처럼 읽히기 때문이다.
insert into document_issuers (issuer_name, issuer_site)
select v.issuer_name, v.issuer_site
from (values
    ('따로 발급 불필요', null),
    ('국민연금공단', 'https://www.nps.or.kr'),
    ('가입한 보험사', null)
) as v(issuer_name, issuer_site)
where not exists (
    select 1 from document_issuers e where e.issuer_name = v.issuer_name
);

-- 3) 매핑 표.
create table if not exists cb_document_issuers (
    cb_document_issuer_id serial primary key,
    document_name         varchar(200) not null,
    document_issuer_id    integer      not null references document_issuers (document_issuer_id),
    constraint uq_cb_document_issuers unique (document_name, document_issuer_id)
);

-- 4) 지금 발급처가 비어 있는 cb 서류 23종.
--    이름을 정확히 일치시킬 때만 걸린다. "~확인서면 병원" 같은 패턴 규칙을 쓰지 않는 이유는 틀려도
--    조용히 틀리기 때문이다. ("국민연금 수급자 확인서"는 확인서지만 병원이 아니다)
insert into cb_document_issuers (document_name, document_issuer_id)
select v.document_name, i.document_issuer_id
from (values
    -- 병원에서 발급
    ('진단서 또는 소견서',                          '병·의원(의료기관)'),
    ('일상생활동작검사서가 첨부된 진단서',          '병·의원(의료기관)'),
    ('일상생활동작검사서가 첨부된 소견서',          '병·의원(의료기관)'),
    ('요실금 진단서',                                '병·의원(의료기관)'),
    ('입(퇴)원확인서',                               '병·의원(의료기관)'),
    ('진료비 상세내역서',                            '병·의원(의료기관)'),
    ('처방전',                                       '병·의원(의료기관)'),
    -- 신청할 때 받는 양식
    ('재난적의료비 지원신청서',                      '신청 공고 확인'),
    ('무릎관절증 의료지원 신청서',                   '신청 공고 확인'),
    ('일상돌봄 서비스 신청서',                       '신청 공고 확인'),
    ('개인정보제공 동의서',                          '신청 공고 확인'),
    ('개인정보 수집·이용 및 제공 동의서',            '신청 공고 확인'),
    ('개인정보수집 및 이용·제공 동의서',             '신청 공고 확인'),
    ('개인정보동의서',                               '신청 공고 확인'),
    ('행정정보 공동이용 사전동의서',                 '신청 공고 확인'),
    -- 기관에 갈 필요 없음
    ('복지카드',                                     '따로 발급 불필요'),
    ('의료급여증',                                   '따로 발급 불필요'),
    ('신분증 사본',                                  '따로 발급 불필요'),
    -- 본인이 내용을 채워야 하는 것
    ('위임장',                                       '본인 작성'),
    ('타 의료비 지원금 수령내역 신고서',             '본인 작성'),
    -- 발급처가 분명한 것
    ('국민연금 수급자 확인서',                       '국민연금공단'),
    ('재직증명서',                                   '회사 인사부서'),
    ('민간보험 가입(계약)서류 및 지급내역 확인서',   '가입한 보험사')
) as v(document_name, issuer_name)
join document_issuers i on i.issuer_name = v.issuer_name
on conflict (document_name, document_issuer_id) do nothing;
