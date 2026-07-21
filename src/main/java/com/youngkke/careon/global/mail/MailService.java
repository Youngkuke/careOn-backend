package com.youngkke.careon.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Gmail SMTP를 통해 이메일을 발송한다. */
@Component
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset-url}")
    private String passwordResetUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendPasswordResetEmail(String to, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[Careon] 비밀번호 재설정 안내");
        message.setText(
                "비밀번호 재설정을 요청하셨습니다.\n\n"
                + "아래 링크는 발송 후 30분간 유효합니다.\n"
                + passwordResetUrl + "?token=" + resetToken + "\n\n"
                + "본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.");
        mailSender.send(message);
    }
}
