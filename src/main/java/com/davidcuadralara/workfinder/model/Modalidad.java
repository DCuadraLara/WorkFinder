package com.davidcuadralara.workfinder.model;

public enum Modalidad {
    PRESENCIAL("Presencial"), HIBRIDA("Híbrida"), REMOTA("Remota");

    private final String nombre;

    Modalidad(String nombre) { this.nombre = nombre; }

    @Override public String toString() { return nombre; }
}
