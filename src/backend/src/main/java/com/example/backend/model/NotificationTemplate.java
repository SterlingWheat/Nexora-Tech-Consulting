package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_template_type", columnNames = "template_type"))
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "template_type", nullable = false, length = 40)
    private String templateType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "text_body", nullable = false, columnDefinition = "TEXT")
    private String textBody;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NotificationTemplate() {
    }

    public NotificationTemplate(String templateType, String name, String subject, String htmlBody, String textBody) {
        this.templateType = templateType;
        this.name = name;
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
        this.active = true;
    }

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public String getTemplateType() {
        return templateType;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String subject, String htmlBody, String textBody, boolean active) {
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
        this.active = active;
    }
}
