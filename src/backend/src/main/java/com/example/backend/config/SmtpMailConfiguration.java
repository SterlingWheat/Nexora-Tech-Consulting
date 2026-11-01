package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties
public class SmtpMailConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.smtp")
    public SmtpProperties smtpProperties() {
        return new SmtpProperties();
    }

    @Bean
    public JavaMailSender javaMailSender(SmtpProperties smtpProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpProperties.getHost());
        mailSender.setPort(smtpProperties.getPort());
        mailSender.setUsername(smtpProperties.getUsername());
        mailSender.setPassword(smtpProperties.getPassword());
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.ssl.enable", Boolean.toString(smtpProperties.isSecure()));
        mailProperties.put("mail.smtp.starttls.enable", Boolean.toString(!smtpProperties.isSecure()));
        mailProperties.put("mail.smtp.starttls.required", Boolean.toString(!smtpProperties.isSecure()));
        mailProperties.put("mail.smtp.connectiontimeout", "10000");
        mailProperties.put("mail.smtp.timeout", "10000");
        mailProperties.put("mail.smtp.writetimeout", "10000");

        return mailSender;
    }
}
