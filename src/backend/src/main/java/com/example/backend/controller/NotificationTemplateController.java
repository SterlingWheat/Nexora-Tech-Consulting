package com.example.backend.controller;

import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.dto.NotificationTemplateResponse;
import com.example.backend.dto.NotificationTemplateUpdateRequest;
import com.example.backend.exception.SmtpEmailException;
import com.example.backend.mail.NotificationTemplateService;
import com.example.backend.mail.NotificationTemplateType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/email-templates")
public class NotificationTemplateController {

    private final NotificationTemplateService service;

    public NotificationTemplateController(NotificationTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationTemplateResponse> list() {
        return service.list();
    }

    @GetMapping("/{type}")
    public NotificationTemplateResponse get(@PathVariable String type) {
        return service.get(NotificationTemplateType.fromPath(type));
    }

    @PutMapping("/{type}")
    public NotificationTemplateResponse update(
            @PathVariable String type,
            @Valid @RequestBody NotificationTemplateUpdateRequest request) {
        return service.update(NotificationTemplateType.fromPath(type), request);
    }

    @PostMapping("/{type}/send")
    public ResponseEntity<Void> send(
            @PathVariable String type,
            @Valid @RequestBody NotificationEmailRequest request) {
        try {
            service.send(NotificationTemplateType.fromPath(type), request);
            return ResponseEntity.noContent().build();
        } catch (SmtpEmailException exception) {
            if (exception.getReason() == SmtpEmailException.Reason.CONFIGURATION) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .build();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
