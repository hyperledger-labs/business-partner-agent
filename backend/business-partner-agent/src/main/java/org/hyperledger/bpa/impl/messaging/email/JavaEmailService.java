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
import io.micronaut.email.javamail.sender.MailPropertiesProvider;
import io.micronaut.email.javamail.sender.SessionProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.validation.Valid;
import java.util.Properties;

/**
 * Basic mail service implementation based on JavaMail.
 * It can be activated by setting the following environment variables:
 * MAIL_USERNAME and MAIL_PASSWORD. The default smtp server is gmail,
 * if this is not an option it can be overwritten by setting the MAIL_SMTP_HOST variable
 */
@Slf4j
@Requires(notEnv = Environment.TEST)
@Requires(property = "mail.username")
@Requires(property = "mail.password")
@Singleton
public class JavaEmailService implements EmailService {

    @Value("${mail.username}")
    String mailUser;

    @Value("${mail.password}")
    String mailPassword;

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

    @Singleton
    @Requires(notEnv = Environment.TEST)
    @Requires(property = "mail.username")
    @Requires(property = "mail.password")
    public static class JavaMailSessionProvider implements SessionProvider {

        @Value("${mail.username}")
        String mailUser;

        @Value("${mail.password}")
        String mailPassword;

        private final Properties properties;

        JavaMailSessionProvider(MailPropertiesProvider mailPropertiesProvider) {
            this.properties = mailPropertiesProvider.mailProperties();
        }

        @Override
        @NonNull
        public Session session() {
            return Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUser, mailPassword);
                }
            });
        }
    }
}
