package com.example.backend.repository;

/** Proyección: cuántas filas hay para cada valor de estado (RF-44, dashboard). */
public interface ConteoPorEstado {
    String getEstado();

    Long getCantidad();
}
