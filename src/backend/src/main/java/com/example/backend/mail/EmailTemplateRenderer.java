package com.example.backend.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class EmailTemplateRenderer {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
    private final Clock clock;
    private final String systemName;

    public EmailTemplateRenderer(
            Clock clock,
            @Value("${app.mail.system-name:Nexora Tech Consulting}") String systemName) {
        this.clock = clock;
        this.systemName = systemName;
    }

    public RenderedEmail render(EmailTemplateType type, EmailTemplateData data) {
        Objects.requireNonNull(type, "El tipo de plantilla es obligatorio.");
        Objects.requireNonNull(data, "Los datos de la plantilla son obligatorios.");

        String name = escape(hasText(data.recipientName()) ? data.recipientName().trim() : "equipo");
        String plainName = hasText(data.recipientName()) ? data.recipientName().trim() : "equipo";
        String email = escape(hasText(data.recipientEmail()) ? data.recipientEmail().trim() : "");
        String details = hasText(data.details()) ? data.details().trim() : "";
        String date = LocalDateTime.now(clock).format(DATE_FORMAT);
        String safeSystemName = escape(systemName);

        TemplateContent content = contentFor(type, data, name, plainName, email, details);
        String html = wrapHtml(safeSystemName, content.heading(), content.htmlBody(), date);
        String text = wrapText(safeSystemName, content.heading(), content.textBody(), date);
        return new RenderedEmail(content.subject(), html, text);
    }

    private TemplateContent contentFor(
            EmailTemplateType type,
            EmailTemplateData data,
            String name,
            String plainName,
            String email,
            String details) {
        return switch (type) {
            case WELCOME -> new TemplateContent(
                    "Bienvenido a " + systemName,
                    "Te damos la bienvenida",
                    "<p>Hola " + name + ",</p><p>Tu cuenta ya está lista para que accedas a "
                            + escape(systemName) + ".</p>" + accountEmail(email),
                    "Hola " + plainName + ",\nTu cuenta ya está lista para que accedas a "
                            + systemName + "." + plainAccountEmail(data.recipientEmail()));
            case PASSWORD_RECOVERY -> passwordRecovery(data, name, plainName);
            case PASSWORD_RESET -> new TemplateContent(
                    "Contraseña actualizada - " + systemName,
                    "Tu contraseña fue actualizada",
                    "<p>Hola " + name + ",</p><p>La contraseña de tu cuenta fue restablecida.</p>"
                            + "<p>Si no reconoces este cambio, contacta al equipo de TI.</p>" + accountEmail(email),
                    "Hola " + plainName + ",\nLa contraseña de tu cuenta fue restablecida."
                            + "\nSi no reconoces este cambio, contacta al equipo de TI."
                            + plainAccountEmail(data.recipientEmail()));
            case ADMIN_NOTIFICATION -> detailsTemplate(
                    "Notificación administrativa - " + systemName,
                    "Notificación administrativa",
                    name,
                    details,
                    "Se ha generado una notificación administrativa.");
            case ACCOUNT_CHANGE -> detailsTemplate(
                    "Cambios en tu cuenta - " + systemName,
                    "Se actualizaron los datos de tu cuenta",
                    name,
                    details,
                    "Se realizaron cambios en tu cuenta.");
            case OPERATION_CONFIRMATION -> detailsTemplate(
                    "Operación confirmada - " + systemName,
                    "Operación completada",
                    name,
                    details,
                    "La operación solicitada se completó correctamente.");
        };
    }

    private TemplateContent passwordRecovery(EmailTemplateData data, String name, String plainName) {
        if (!hasText(data.actionUrl()) || !hasText(data.actionLabel()) || data.expiresAt() == null) {
            throw new IllegalArgumentException("La recuperación requiere enlace, acción y vencimiento.");
        }
        if (!data.expiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("El enlace de recuperación debe tener un vencimiento futuro.");
        }

        String actionUrl = validateActionUrl(data.actionUrl());
        String actionLabel = escape(data.actionLabel().trim());
        String expiry = data.expiresAt().format(DATE_FORMAT);
        String details = escape(hasText(data.details())
                ? data.details().trim()
                : "Solicitaste recuperar el acceso a tu cuenta.");
        return new TemplateContent(
                "Recuperación de contraseña - " + systemName,
                "Recupera el acceso a tu cuenta",
                "<p>Hola " + name + ",</p><p>" + details + "</p>"
                        + actionButton(actionUrl, actionLabel)
                        + "<p>Este enlace vence el " + escape(expiry)
                        + ". Si no solicitaste este cambio, ignora este correo.</p>",
                "Hola " + plainName + ",\n" + plain(data.details(),
                        "Solicitaste recuperar el acceso a tu cuenta.")
                        + "\n" + data.actionLabel() + ": " + actionUrl
                        + "\nEste enlace vence el " + expiry
                        + ". Si no solicitaste este cambio, ignora este correo.");
    }

    private TemplateContent detailsTemplate(
            String subject,
            String heading,
            String name,
            String details,
            String defaultDetails) {
        String safeDetails = hasText(details) ? escape(details).replace("\n", "<br>") : escape(defaultDetails);
        String plainDetails = hasText(details) ? details : defaultDetails;
        return new TemplateContent(
                subject,
                heading,
                "<p>Hola " + name + ",</p><p>" + safeDetails + "</p>",
                "Hola " + name + ",\n" + plainDetails);
    }

    private String wrapHtml(String safeSystemName, String heading, String body, String date) {
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    @media only screen and (max-width: 600px) {
                      .email-card { width: 100%% !important; }
                      .email-content { padding: 24px 18px !important; }
                    }
                  </style>
                </head>
                <body style="margin:0;background:#f3f6fb;font-family:Arial,Helvetica,sans-serif;color:#243247;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6fb;padding:28px 12px;">
                    <tr><td align="center">
                      <table role="presentation" class="email-card" width="600" cellspacing="0" cellpadding="0"
                        style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;">
                        <tr><td style="background:#123b63;padding:24px 30px;color:#ffffff;font-size:20px;font-weight:bold;">%s</td></tr>
                        <tr><td class="email-content" style="padding:32px 30px;">
                          <h1 style="margin:0 0 20px;color:#123b63;font-size:24px;">%s</h1>
                          <div style="font-size:16px;line-height:1.6;">%s</div>
                        </td></tr>
                        <tr><td style="padding:20px 30px;background:#f7f9fc;color:#627187;font-size:12px;line-height:1.5;">
                          Mensaje automático de %s.<br>Enviado el %s.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeSystemName, safeSystemName, escape(heading), body, safeSystemName, escape(date));
    }

    private String wrapText(String system, String heading, String body, String date) {
        return system + "\n" + heading + "\n\n" + body + "\n\nMensaje automático de "
                + system + ".\nEnviado el " + date + ".";
    }

    private String accountEmail(String email) {
        return email.isEmpty() ? "" : "<p>Correo de la cuenta: <strong>" + email + "</strong></p>";
    }

    private String plainAccountEmail(String email) {
        return hasText(email) ? "\nCorreo de la cuenta: " + email.trim() : "";
    }

    private String actionButton(String url, String label) {
        return "<p style=\"margin:26px 0;\"><a href=\"" + escape(url)
                + "\" style=\"display:inline-block;background:#1769aa;color:#ffffff;text-decoration:none;"
                + "padding:13px 22px;border-radius:6px;font-weight:bold;\">" + label + "</a></p>";
    }

    private String validateActionUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            boolean secureScheme = "https".equalsIgnoreCase(scheme);
            boolean localDevelopment = "http".equalsIgnoreCase(scheme)
                    && ("localhost".equalsIgnoreCase(uri.getHost())
                    || "127.0.0.1".equals(uri.getHost()));
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getRawUserInfo() != null
                    || (!secureScheme && !localDevelopment)) {
                throw new IllegalArgumentException("El enlace de recuperación debe usar HTTPS.");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El enlace de recuperación no es válido.");
        }
    }

    private String plain(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record TemplateContent(String subject, String heading, String htmlBody, String textBody) {
    }
}
