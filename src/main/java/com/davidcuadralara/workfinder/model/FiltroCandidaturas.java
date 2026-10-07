package com.davidcuadralara.workfinder.model;

/** Criterios combinables. Un enum null significa cualquiera; empresa vacía no limita la búsqueda. */
public record FiltroCandidaturas(String empresa, EstadoCandidatura estado,
        CategoriaProfesional categoria, Modalidad modalidad) {
    public FiltroCandidaturas {
        empresa = empresa == null ? "" : empresa.strip();
    }

    public static FiltroCandidaturas todos() {
        return new FiltroCandidaturas("", null, null, null);
    }

    public boolean activo() {
        return !empresa.isEmpty() || estado != null || categoria != null || modalidad != null;
    }
}
