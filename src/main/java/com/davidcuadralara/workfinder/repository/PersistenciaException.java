package com.davidcuadralara.workfinder.repository;

/** Error de persistencia con un mensaje útil y la causa original disponible para diagnóstico. */
public final class PersistenciaException extends Exception {
    public PersistenciaException(String mensaje, Throwable causa) { super(mensaje, causa); }
    public PersistenciaException(String mensaje) { super(mensaje); }
}
