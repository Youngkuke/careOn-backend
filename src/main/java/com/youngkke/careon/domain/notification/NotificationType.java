package com.youngkke.careon.domain.notification;

/** ERD의 notification.notification_type ENUM('DEADLINE_D7','DEADLINE_D3','DEADLINE_D1','RESULT_DDAY')에 대응. */
public enum NotificationType {
    DEADLINE_D7("신청 마감이 얼마 남지 않았어요", "%s 신청 마감이 7일 남았어요."),
    DEADLINE_D3("신청 마감이 얼마 남지 않았어요", "%s 신청 마감이 3일 남았어요."),
    DEADLINE_D1("신청 마감이 얼마 남지 않았어요", "%s 신청 마감이 내일까지예요."),
    RESULT_DDAY("오늘 결과가 발표돼요", "%s 결과 발표일이에요.");

    private final String pushTitle;
    private final String pushBodyFormat;

    NotificationType(String pushTitle, String pushBodyFormat) {
        this.pushTitle = pushTitle;
        this.pushBodyFormat = pushBodyFormat;
    }

    public String pushTitle() {
        return pushTitle;
    }

    public String pushBody(String policyName) {
        return String.format(pushBodyFormat, policyName);
    }
}
