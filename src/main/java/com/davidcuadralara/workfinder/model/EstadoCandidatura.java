package com.davidcuadralara.workfinder.model;

public enum EstadoCandidatura {
    ENVIADA("Enviada"),
    EN_PROCESO("En revisión"),
    ENTREVISTA("Entrevista"),
    OFERTA("Oferta"),
    RECHAZADA("Rechazada"),
    RETIRADA("Retirada");

    private final String nombre;

    EstadoCandidatura(String nombre) { this.nombre = nombre; }

    @Override public String toString() { return nombre; }
}
