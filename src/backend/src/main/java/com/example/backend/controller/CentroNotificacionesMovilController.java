package com.example.backend.controller;

import com.example.backend.dto.NotificacionMovilDTO;
import com.example.backend.service.CentroNotificacionesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movil/notificaciones")
public class CentroNotificacionesMovilController {

    private final CentroNotificacionesService service;

    public CentroNotificacionesMovilController(CentroNotificacionesService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionMovilDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(service.listar(authentication.getName()));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(
            @PathVariable Long id,
            Authentication authentication) {
        service.marcarLeida(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
