package com.example.backend.mail;

import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.dto.NotificationTemplateResponse;
import com.example.backend.dto.NotificationTemplateUpdateRequest;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.NotificationTemplate;
import com.example.backend.repository.NotificationTemplateRepository;
import com.example.backend.service.SmtpEmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}");
    private static final Pattern ANY_VARIABLE_PATTERN = Pattern.compile("\\{\\{[^{}]*}}");
    private static final List<String> REQUIRED_VARIABLES = List.of("nombre", "actividad", "fecha");

    private final NotificationTemplateRepository repository;
    private final SmtpEmailService smtpEmailService;

    public NotificationTemplateService(
            NotificationTemplateRepository repository,
            SmtpEmailService smtpEmailService) {
        this.repository = repository;
        this.smtpEmailService = smtpEmailService;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> list() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse get(NotificationTemplateType type) {
        return toResponse(findTemplate(type));
    }

    @Transactional
    public NotificationTemplateResponse update(
            NotificationTemplateType type,
            NotificationTemplateUpdateRequest request) {
        validateTemplate(request.subject(), request.htmlBody(), request.textBody());

        NotificationTemplate template = findTemplate(type);
        template.update(
                request.subject().trim(),
                request.htmlBody(),
                request.textBody(),
                request.active());

        return toResponse(repository.saveAndFlush(template));
    }

    public void send(NotificationTemplateType type, NotificationEmailRequest request) {
        NotificationTemplate template = findTemplate(type);

        if (!template.isActive()) {
            throw new BusinessValidationException("La plantilla seleccionada est  inactiva.");
        }

        if (request == null
                || !hasText(request.recipient())
                || !hasText(request.name())
                || !hasText(request.activity())
                || !hasText(request.date())) {
            throw new BusinessValidationException("El destinatario, nombre, actividad y fecha son obligatorios.");
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("nombre", normalizeValue(request.name()));
        values.put("actividad", normalizeValue(request.activity()));
        values.put("fecha", normalizeValue(request.date()));

        String subject = replaceVariables(template.getSubject(), values, false);
        String html = replaceVariables(template.getHtmlBody(), values, true);
        String text = replaceVariables(template.getTextBody(), values, false);

        ensureNoUnresolvedVariables(subject, html, text);

        smtpEmailService.sendEmail(request.recipient().trim(), subject, html, text);
    }

    private NotificationTemplate findTemplate(NotificationTemplateType type) {
        return repository.findByTemplateType(type.name())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontr  la plantilla solicitada."));
    }

    private void validateTemplate(String subject, String htmlBody, String textBody) {
        String content = subject + "\n" + htmlBody + "\n" + textBody;
        Matcher matcher = ANY_VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            Matcher validVariable = VARIABLE_PATTERN.matcher(matcher.group());
            if (!validVariable.matches() || !REQUIRED_VARIABLES.contains(validVariable.group(1))) {
                throw new BusinessValidationException("La plantilla contiene una variable no admitida.");
            }
        }

        for (String requiredVariable : REQUIRED_VARIABLES) {
            if (!content.contains("{{" + requiredVariable + "}}")
                    && !content.matches("(?s).*\\{\\{\\s*" + requiredVariable + "\\s*}}.*")) {
                throw new BusinessValidationException(
                        "La plantilla debe incluir las variables {{nombre}}, {{actividad}} y {{fecha}}.");
            }
        }

        String contentWithoutVariables = VARIABLE_PATTERN.matcher(content).replaceAll("");
        if (contentWithoutVariables.contains("{{") || contentWithoutVariables.contains("}}")) {
            throw new BusinessValidationException("La plantilla contiene una variable incompleta.");
        }
    }

    private String replaceVariables(String content, Map<String, String> values, boolean html) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) {
                throw new BusinessValidationException("La plantilla contiene una variable sin valor.");
            }
            String replacement = html ? HtmlUtils.htmlEscape(value) : value;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void ensureNoUnresolvedVariables(String subject, String html, String text) {
        if (subject.contains("{{") || subject.contains("}}")
                || html.contains("{{") || html.contains("}}")
                || text.contains("{{") || text.contains("}}")) {
            throw new BusinessValidationException("No se puede enviar una plantilla con variables sin reemplazar.");
        }
    }

    private String normalizeValue(String value) {
        return value.trim().replaceAll("[\\r\\n]+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getTemplateType(),
                template.getName(),
                template.getSubject(),
                template.getHtmlBody(),
                template.getTextBody(),
                template.isActive(),
                template.getUpdatedAt());
    }
}