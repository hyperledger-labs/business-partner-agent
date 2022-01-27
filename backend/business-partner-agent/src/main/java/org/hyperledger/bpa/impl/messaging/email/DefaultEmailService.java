/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.impl.messaging.email;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import io.micronaut.email.StringBody;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;

/**
 * Basic mail service implementation. It can be activated by setting the
 * following environment variables: MAIL_USERNAME, MAILJET_API_KEY,
 * MAILJET_API_SECRET.
 */
@Slf4j
@Requires(notEnv = Environment.TEST)
@Requires(property = "mail.username")
@Requires(property = "mailjet.api-key")
@Requires(property = "mailjet.api-secret")
@Singleton
public class DefaultEmailService implements EmailService {

    @Value("${mail.username}")
    String mailUser;

    @Inject
    EmailSender<?, ?> emailSender;

    @Override
    public void send(@NonNull @Valid EmailCmd cmd) {
        emailSender.send(Email.builder()
                .from(mailUser)
                .to(cmd.getTo())
                .subject(cmd.getSubject())
                .body(new StringBody(cmd.getTextBody())));
    }

    /*
     * Same as above, but using JavaMail features javamail: properties:
     * mail.smtp.host: ${MAIL_SMTP_HOST:`smtp.gmail.com`} mail.smtp.port: 587
     * mail.smtp.auth: true mail.smtp.starttls.enable: true
     * mail.smtp.connectiontimeout: 5000 mail.smtp.timeout: 5000
     * mail.smtp.writetimeout: 5000
     * 
     * 
     * @Singleton
     * 
     * @Requires(notEnv = Environment.TEST)
     * 
     * @Requires(property = "mail.username")
     * 
     * @Requires(property = "mail.password") public static class
     * JavaMailSessionProvider implements SessionProvider {
     * 
     * @Value("${mail.username}") String mailUser;
     * 
     * @Value("${mail.password}") String mailPassword;
     * 
     * private final Properties properties;
     * 
     * JavaMailSessionProvider(MailPropertiesProvider mailPropertiesProvider) {
     * this.properties = mailPropertiesProvider.mailProperties(); }
     * 
     * @Override
     * 
     * @NonNull public Session session() { return Session.getInstance(properties,
     * new Authenticator() {
     * 
     * @Override protected PasswordAuthentication getPasswordAuthentication() {
     * return new PasswordAuthentication(mailUser, mailPassword); } }); } }
     */

}
