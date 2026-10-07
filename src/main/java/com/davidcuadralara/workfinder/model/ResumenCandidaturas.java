package com.davidcuadralara.workfinder.model;

import java.util.Map;

/** Resumen inmutable, independiente de JavaFX. Los recuentos incluyen todos los valores de los enums. */
public record ResumenCandidaturas(long total, Map<EstadoCandidatura, Long> porEstado,
        Map<CategoriaProfesional, Long> porCategoria, long maxCategoria) {
    public ResumenCandidaturas {
        porEstado = Map.copyOf(porEstado);
        porCategoria = Map.copyOf(porCategoria);
    }
}
