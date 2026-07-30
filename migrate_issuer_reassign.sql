-- 발급처 재배치. 문구가 실제 상황과 어긋나던 두 갈래를 고친다.

-- 1) 복지카드·의료급여증은 "발급이 불필요"한 게 아니라 이미 갖고 있어 그대로 내는 서류다.
--    잃어버린 경우가 실제로 문제인데 지금 문구는 그때 어디로 가야 하는지 안 알려준다.
--    둘 다 주민센터에서 재발급하므로 발급처 하나로 묶는다.
insert into document_issuers (issuer_name, issuer_site, issue_guide)
select '본인 소지(분실 시 주민센터)', null, '소지 중인 것을 제출 · 분실 시 주민센터에서 재발급'
where not exists (
    select 1 from document_issuers where issuer_name = '본인 소지(분실 시 주민센터)'
);

update cb_document_issuers
set document_issuer_id = (select document_issuer_id from document_issuers where issuer_name = '본인 소지(분실 시 주민센터)')
where document_name in ('복지카드', '의료급여증');

-- 2) 위임장·신고서는 "본인 작성"보다 "신청 공고 확인"이 맞다.
--    양식을 신청 기관이 주고, 사용자가 먼저 할 일도 공고에서 서식을 받는 것이기 때문이다.
--    ("본인 작성"은 마스터 서류 2건이 그대로 쓰고 있어 발급처 자체는 남겨둔다)
update cb_document_issuers
set document_issuer_id = (select document_issuer_id from document_issuers where issuer_name = '신청 공고 확인')
where document_name in ('위임장', '타 의료비 지원금 수령내역 신고서');

-- 3) '따로 발급 불필요'에는 신분증 사본만 남는다. 사본은 원본을 갖고 있으면 복사로 끝나므로 문구가 그대로 맞다.
