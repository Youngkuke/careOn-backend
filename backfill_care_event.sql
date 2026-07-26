-- Care 타임라인(care_event) 백필.
--
-- 타임라인은 사건이 일어나는 자리에서 기록하므로, 배포 이전에 쌓인 SOS·이탈은 그대로 두면 화면에서 사라진
-- 것처럼 보인다. 이미 원본 테이블에 시각이 남아 있는 것들만 여기서 옮겨 담는다.
--
-- 워치 연결 해제와 위치 공유 시작·종료는 애초에 이력이 없어(wear_device의 컬럼을 덮어쓰는 구조) 복원할 수 없다.
-- 연결(WEAR_PAIRED)만 현재 남아 있는 connected_at 기준으로 1건씩 넣는다.
--
-- 실행 조건: 애플리케이션 배포로 care_event 테이블이 만들어진 뒤 1회 실행.
-- NOT EXISTS 가드가 있어 여러 번 실행해도 중복 생성되지 않는다.
-- summary 문구는 CareEventType의 값과 일치시켜야 한다.

-- 1) SOS 발생
INSERT INTO care_event (cared_id, type, occurred_at, ref_id, summary)
SELECT e.cared_id, 'EMERGENCY_CREATED', e.requested_at, e.emergency_event_id, '도움 요청이 왔어요.'
FROM emergency_event e
WHERE NOT EXISTS (
    SELECT 1 FROM care_event c
    WHERE c.type = 'EMERGENCY_CREATED' AND c.ref_id = e.emergency_event_id
);

-- 2) SOS 확인
INSERT INTO care_event (cared_id, type, occurred_at, ref_id, summary)
SELECT e.cared_id, 'EMERGENCY_ACKNOWLEDGED', e.acknowledged_at, e.emergency_event_id, '보호자가 SOS를 확인했어요.'
FROM emergency_event e
WHERE e.acknowledged_at IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM care_event c
    WHERE c.type = 'EMERGENCY_ACKNOWLEDGED' AND c.ref_id = e.emergency_event_id
);

-- 3) 안심 구역 이탈 감지
INSERT INTO care_event (cared_id, type, occurred_at, ref_id, summary)
SELECT s.cared_id, 'SAFE_ZONE_EXIT_DETECTED', s.detected_at, s.safe_zone_event_id, '안심 구역을 벗어났어요.'
FROM safe_zone_event s
WHERE NOT EXISTS (
    SELECT 1 FROM care_event c
    WHERE c.type = 'SAFE_ZONE_EXIT_DETECTED' AND c.ref_id = s.safe_zone_event_id
);

-- 4) 이탈에 대한 응답 (응답 시각이 없는 옛 기록은 감지 시각으로 대체한다)
INSERT INTO care_event (cared_id, type, occurred_at, ref_id, summary)
SELECT
    s.cared_id,
    CASE s.response
        WHEN 'USER_OKAY' THEN 'SAFE_ZONE_USER_OKAY'
        WHEN 'NEED_HELP' THEN 'SAFE_ZONE_NEED_HELP'
        WHEN 'NO_RESPONSE' THEN 'SAFE_ZONE_NO_RESPONSE'
    END,
    COALESCE(s.responded_at, s.detected_at),
    s.safe_zone_event_id,
    CASE s.response
        WHEN 'USER_OKAY' THEN '괜찮다고 응답했어요.'
        WHEN 'NEED_HELP' THEN '도움이 필요하다고 응답했어요.'
        WHEN 'NO_RESPONSE' THEN '이탈 알림에 응답이 없었어요.'
    END
FROM safe_zone_event s
WHERE s.response IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM care_event c
    WHERE c.ref_id = s.safe_zone_event_id
      AND c.type IN ('SAFE_ZONE_USER_OKAY', 'SAFE_ZONE_NEED_HELP', 'SAFE_ZONE_NO_RESPONSE')
);

-- 5) 워치 연결 (재페어링으로 덮어써진 과거 연결은 복원할 수 없어 현재 값 1건만 넣는다)
INSERT INTO care_event (cared_id, type, occurred_at, ref_id, summary)
SELECT w.cared_id, 'WEAR_PAIRED', w.connected_at, NULL, '워치를 연결했어요.'
FROM wear_device w
WHERE NOT EXISTS (
    SELECT 1 FROM care_event c
    WHERE c.type = 'WEAR_PAIRED' AND c.cared_id = w.cared_id
);
