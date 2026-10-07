package com.davidcuadralara.workfinder.model;

public enum CategoriaProfesional {
    DESARROLLO_SOFTWARE("Desarrollo de software"),
    DATOS("Datos"),
    SISTEMAS_REDES("Sistemas y redes"),
    DISENO("Diseño"),
    MARKETING_COMUNICACION("Marketing y comunicación"),
    ADMINISTRACION("Administración"),
    OTRAS("Otras");

    private final String nombre;

    CategoriaProfesional(String nombre) { this.nombre = nombre; }

    @Override public String toString() { return nombre; }
}
