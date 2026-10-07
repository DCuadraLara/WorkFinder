package com.davidcuadralara.workfinder.model;

import java.time.LocalDate;

/** Datos inmutables del dominio. Un id 0 corresponde a una candidatura aún no guardada. */
public final class Candidatura {
    private final long id;
    private final String empresa;
    private final String puesto;
    private final CategoriaProfesional categoria;
    private final EstadoCandidatura estado;
    private final Modalidad modalidad;
    private final String ubicacion;
    private final String enlace;
    private final LocalDate fechaEnvio;
    private final boolean cvEnviado;
    private final boolean cartaEnviada;
    private final String notas;

    public Candidatura(long id, String empresa, String puesto, CategoriaProfesional categoria,
            EstadoCandidatura estado, Modalidad modalidad, String ubicacion, String enlace,
            LocalDate fechaEnvio, boolean cvEnviado, boolean cartaEnviada, String notas) {
        this.id = id;
        this.empresa = empresa;
        this.puesto = puesto;
        this.categoria = categoria;
        this.estado = estado;
        this.modalidad = modalidad;
        this.ubicacion = ubicacion;
        this.enlace = enlace;
        this.fechaEnvio = fechaEnvio;
        this.cvEnviado = cvEnviado;
        this.cartaEnviada = cartaEnviada;
        this.notas = notas;
    }

    public long getId() { return id; }
    public String getEmpresa() { return empresa; }
    public String getPuesto() { return puesto; }
    public CategoriaProfesional getCategoria() { return categoria; }
    public EstadoCandidatura getEstado() { return estado; }
    public Modalidad getModalidad() { return modalidad; }
    public String getUbicacion() { return ubicacion; }
    public String getEnlace() { return enlace; }
    public LocalDate getFechaEnvio() { return fechaEnvio; }
    public boolean isCvEnviado() { return cvEnviado; }
    public boolean isCartaEnviada() { return cartaEnviada; }
    public String getNotas() { return notas; }

    public Candidatura conId(long nuevoId) {
        return new Candidatura(nuevoId, empresa, puesto, categoria, estado, modalidad,
                ubicacion, enlace, fechaEnvio, cvEnviado, cartaEnviada, notas);
    }

    public Candidatura conEstado(EstadoCandidatura nuevoEstado) {
        return new Candidatura(id, empresa, puesto, categoria, nuevoEstado, modalidad,
                ubicacion, enlace, fechaEnvio, cvEnviado, cartaEnviada, notas);
    }
}
