package com.example.backend.repository;

import com.example.backend.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Integer> {

    Optional<NotificationTemplate> findByTemplateType(String templateType);

    List<NotificationTemplate> findAllByOrderByNameAsc();
}
