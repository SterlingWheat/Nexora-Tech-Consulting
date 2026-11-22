package com.example.backend.service;

import com.example.backend.dto.NotificacionMovilDTO;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.NotificationOutboxMessage;
import com.example.backend.repository.NotificationOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CentroNotificacionesService {

    private static final String SENT = "SENT";

    private final NotificationOutboxRepository repository;
    private final Clock clock;

    public CentroNotificacionesService(NotificationOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NotificacionMovilDTO> listar(String email) {
        return repository.findAllByRecipientIgnoreCaseAndStatusOrderByCreatedAtDesc(email, SENT)
                .stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public void marcarLeida(Long id, String email) {
        NotificationOutboxMessage notificacion =
                repository.findByIdAndRecipientIgnoreCaseAndStatus(id, email, SENT)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No se encontró el aviso solicitado."));

        if (notificacion.getReadAt() == null
                && repository.markAsRead(id, email, LocalDateTime.now(clock)) != 1) {
            throw new ConflictException("El aviso cambió y no se pudo marcar como leído.");
        }
    }

    private NotificacionMovilDTO aDto(NotificationOutboxMessage message) {
        String tipo = NotificationTemplateType.fromPath(message.getTemplateType()).displayName();
        return new NotificacionMovilDTO(
                message.getId(),
                tipo,
                message.getActivity(),
                message.getActivityDate(),
                message.getSentAt() == null ? message.getCreatedAt() : message.getSentAt(),
                message.getReadAt() != null);
    }
}
