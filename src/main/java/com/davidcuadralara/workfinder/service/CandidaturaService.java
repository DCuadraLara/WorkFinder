package com.davidcuadralara.workfinder.service;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.FiltroCandidaturas;
import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.ResumenCandidaturas;
import java.util.EnumMap;
import java.text.Normalizer;
import java.util.Locale;
import com.davidcuadralara.workfinder.model.Modalidad;
import com.davidcuadralara.workfinder.repository.CandidaturaRepository;
import com.davidcuadralara.workfinder.repository.PersistenciaException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reglas de negocio. No depende de JavaFX ni muestra mensajes en pantalla. */
public final class CandidaturaService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private final CandidaturaRepository repository;
    private final Clock reloj;

    public CandidaturaService(CandidaturaRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public CandidaturaService(CandidaturaRepository repository, Clock reloj) {
        this.repository = Objects.requireNonNull(repository);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public List<Candidatura> inicializarYListar() throws PersistenciaException {
        repository.inicializar();
        return repository.listar();
    }

    public List<Candidatura> listar() throws PersistenciaException { return repository.listar(); }

    public Candidatura anadir(Candidatura candidatura) throws ValidacionException, PersistenciaException {
        if (candidatura.getId() != 0) throw new ValidacionException(Map.of("general", "Una candidatura nueva no debe tener identificador."));
        return repository.anadir(validarYNormalizar(candidatura));
    }

    public Candidatura editar(Candidatura candidatura) throws ValidacionException, PersistenciaException {
        validarId(candidatura.getId());
        return repository.editar(validarYNormalizar(candidatura));
    }

    public void eliminar(long id) throws ValidacionException, PersistenciaException {
        validarId(id);
        repository.eliminar(id);
    }

    public Candidatura cambiarEstado(Candidatura candidatura, EstadoCandidatura nuevoEstado)
            throws ValidacionException, PersistenciaException {
        validarId(candidatura.getId());
        if (nuevoEstado == null) throw new ValidacionException(Map.of("estado", "Selecciona un estado."));
        if (candidatura.getEstado() == nuevoEstado) return candidatura;
        repository.cambiarEstado(candidatura.getId(), candidatura.getEstado(), nuevoEstado);
        return candidatura.conEstado(nuevoEstado);
    }

    /** Filtra la lista confirmada, sin JDBC, sin mutar modelos ni alterar el orden de presentación. */
    public List<Candidatura> filtrar(List<Candidatura> candidaturas, FiltroCandidaturas filtro) {
        Objects.requireNonNull(candidaturas);
        Objects.requireNonNull(filtro);
        String empresa = normalizarBusqueda(filtro.empresa());
        return candidaturas.stream().filter(c ->
                (empresa.isEmpty() || normalizarBusqueda(c.getEmpresa()).contains(empresa))
                && (filtro.estado() == null || c.getEstado() == filtro.estado())
                && (filtro.categoria() == null || c.getCategoria() == filtro.categoria())
                && (filtro.modalidad() == null || c.getModalidad() == filtro.modalidad()))
                .toList();
    }

    /** Cuenta candidaturas, no empresas. Se calcula sobre la lista completa confirmada. */
    public ResumenCandidaturas calcularResumen(List<Candidatura> candidaturas) {
        Objects.requireNonNull(candidaturas);
        Map<EstadoCandidatura, Long> estados = new EnumMap<>(EstadoCandidatura.class);
        Map<CategoriaProfesional, Long> categorias = new EnumMap<>(CategoriaProfesional.class);
        for (EstadoCandidatura estado : EstadoCandidatura.values()) estados.put(estado, 0L);
        for (CategoriaProfesional categoria : CategoriaProfesional.values()) categorias.put(categoria, 0L);
        for (Candidatura candidatura : candidaturas) {
            estados.merge(candidatura.getEstado(), 1L, Long::sum);
            categorias.merge(candidatura.getCategoria(), 1L, Long::sum);
        }
        long maximo = categorias.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return new ResumenCandidaturas(candidaturas.size(), estados, categorias, maximo);
    }

    private String normalizarBusqueda(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public LocalDate interpretarFecha(String texto) throws ValidacionException {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.strip(), FECHA);
        } catch (DateTimeParseException error) {
            throw new ValidacionException(Map.of("fecha", "Introduce una fecha válida con formato dd/mm/aaaa."));
        }
    }

    private Candidatura validarYNormalizar(Candidatura c) throws ValidacionException {
        Map<String, String> errores = new LinkedHashMap<>();
        String empresa = texto(c.getEmpresa());
        String puesto = texto(c.getPuesto());
        String ubicacion = texto(c.getUbicacion());
        String enlace = texto(c.getEnlace());
        String notas = c.getNotas() == null ? "" : c.getNotas();
        obligatorio(errores, "empresa", empresa, "Escribe el nombre de la empresa.");
        obligatorio(errores, "puesto", puesto, "Escribe el puesto al que te presentas.");
        limite(errores, "empresa", empresa, 200);
        limite(errores, "puesto", puesto, 200);
        limite(errores, "ubicacion", ubicacion, 200);
        limite(errores, "enlace", enlace, 2000);
        limite(errores, "notas", notas, 10000);
        if (c.getCategoria() == null) errores.put("categoria", "Selecciona una categoría profesional.");
        if (c.getEstado() == null) errores.put("estado", "Selecciona un estado.");
        if (c.getModalidad() == null) errores.put("modalidad", "Selecciona una modalidad.");
        if (c.getModalidad() != null && c.getModalidad() != Modalidad.REMOTA && ubicacion.isEmpty())
            errores.put("ubicacion", "Indica la ubicación para una candidatura presencial o híbrida.");
        if (c.getFechaEnvio() == null) errores.put("fecha", "Indica la fecha de envío.");
        else if (c.getFechaEnvio().isAfter(LocalDate.now(reloj)))
            errores.put("fecha", "La fecha de envío no puede ser posterior a hoy.");
        if (!enlace.isEmpty() && !esEnlaceValido(enlace))
            errores.put("enlace", "Introduce una URL completa con http:// o https:// y un dominio, o deja el campo vacío.");
        if (!errores.isEmpty()) throw new ValidacionException(errores);
        return new Candidatura(c.getId(), empresa, puesto, c.getCategoria(), c.getEstado(), c.getModalidad(),
                ubicacion, enlace, c.getFechaEnvio(), c.isCvEnviado(), c.isCartaEnviada(), notas);
    }

    private boolean esEnlaceValido(String enlace) {
        try {
            URI uri = new URI(enlace);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || (uri.getPort() > 0 && uri.getPort() <= 65535));
        } catch (URISyntaxException error) {
            return false;
        }
    }

    private String texto(String valor) { return valor == null ? "" : valor.strip(); }

    private void obligatorio(Map<String, String> errores, String campo, String valor, String mensaje) {
        if (valor.isEmpty()) errores.put(campo, mensaje);
    }

    private void limite(Map<String, String> errores, String campo, String valor, int maximo) {
        if (valor.length() > maximo) errores.put(campo, "Utiliza como máximo " + maximo + " caracteres.");
    }

    private void validarId(long id) throws ValidacionException {
        if (id <= 0) throw new ValidacionException(Map.of("general", "Selecciona una candidatura guardada."));
    }
}
