package com.library.util;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

public final class EmailService {
    public static final String EMAIL_ADDRESS = "lungolushomo21@gmail.com";

    private EmailService() {}

    public static void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            Gmail gmail = GmailOAuthService.getGmailService();

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("lungolushomo21@gmail.com", "Library Management System"));
            message.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            message.writeTo(buffer);

            String encodedEmail = Base64.getUrlEncoder()
                    .encodeToString(buffer.toByteArray());

            Message gmailMessage = new Message();
            gmailMessage.setRaw(encodedEmail);

            gmail.users().messages().send("me", gmailMessage).execute();

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
