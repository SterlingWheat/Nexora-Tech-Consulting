package com.example.backend.repository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface UpcomingTurnoAssignment {

    Integer getIdTurno();

    Integer getIdVoluntario();

    String getNombre();

    String getApellido();

    String getEmail();

    String getActividad();

    LocalDate getFecha();

    LocalTime getHoraInicio();
}
