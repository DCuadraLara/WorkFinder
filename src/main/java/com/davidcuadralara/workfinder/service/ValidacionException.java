package com.davidcuadralara.workfinder.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Errores por nombre de campo para que la interfaz los marque sin repetir las reglas. */
public final class ValidacionException extends Exception {
    private final Map<String, String> errores;

    public ValidacionException(Map<String, String> errores) {
        super("Revisa los campos indicados antes de guardar.");
        this.errores = Collections.unmodifiableMap(new LinkedHashMap<>(errores));
    }

    public Map<String, String> getErrores() { return errores; }
}
