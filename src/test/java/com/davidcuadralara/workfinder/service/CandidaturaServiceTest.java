package com.davidcuadralara.workfinder.service;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.Modalidad;
import com.davidcuadralara.workfinder.repository.CandidaturaRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CandidaturaServiceTest {
    @TempDir Path directory;
    private Path database;
    private CandidaturaService service;

    @BeforeEach void prepare() {
        database = directory.resolve("validation.db");
        service = new CandidaturaService(new CandidaturaRepository(database),
                Clock.fixed(Instant.parse("2026-10-07T09:00:00Z"), ZoneId.of("Europe/Madrid")));
    }

    @Test void mandatoryFieldsAreReportedBeforeOpeningSqlite() {
        Candidatura c = new Candidatura(0, "  ", null, null, null, null, "", "", null, false, false, "");
        ValidacionException error = assertThrows(ValidacionException.class, () -> service.anadir(c));
        assertTrue(error.getErrores().keySet().containsAll(Set.of("empresa", "puesto", "categoria", "estado", "modalidad", "fecha")));
        assertFalse(Files.exists(database), "Validar no debe abrir ni crear la base");
    }

    @ParameterizedTest @ValueSource(strings = {"31/02/2026", "29/02/2025", "2026-10-07", "7/10/2026", "texto", "07/13/2026"})
    void invalidDatesAreNotSilentlyAdjusted(String value) {
        ValidacionException error = assertThrows(ValidacionException.class, () -> service.interpretarFecha(value));
        assertTrue(error.getErrores().containsKey("fecha"));
    }

    @Test void validLeapDayAndEmptyDate() throws Exception {
        assertEquals(LocalDate.of(2024, 2, 29), service.interpretarFecha("29/02/2024"));
        assertNull(service.interpretarFecha(" "));
    }

    @ParameterizedTest @ValueSource(strings = {"example.com", "ftp://example.com/job", "javascript:alert(1)",
            "https://", "https://example.com/a b", "https://user:pass@example.com", "https://example.com:99999"})
    void invalidOfferLinksAreRejectedBeforePersistence(String url) {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.anadir(sample(Modalidad.REMOTA, "", url, LocalDate.of(2026, 10, 7))));
        assertTrue(error.getErrores().containsKey("enlace"));
        assertFalse(Files.exists(database));
    }

    @Test void futureSendDateIsRejected() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.anadir(sample(Modalidad.REMOTA, "", "", LocalDate.of(2026, 10, 8))));
        assertTrue(error.getErrores().containsKey("fecha"));
    }

    @ParameterizedTest @ValueSource(strings = {"PRESENCIAL", "HIBRIDA"})
    void locationIsRequiredWhenAttendanceIsNeeded(String value) {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> service.anadir(sample(Modalidad.valueOf(value), " ", "", LocalDate.of(2026, 10, 7))));
        assertTrue(error.getErrores().containsKey("ubicacion"));
    }

    @Test void remoteLocationCanBeEmptyAndOptionalTextIsNormalized() throws Exception {
        service.inicializarYListar();
        Candidatura saved = service.anadir(sample(Modalidad.REMOTA, "  ", " https://example.com/job?q=1 ", LocalDate.of(2026, 10, 7)));
        assertEquals("Empresa", saved.getEmpresa());
        assertEquals("Puesto", saved.getPuesto());
        assertEquals("", saved.getUbicacion());
        assertEquals("https://example.com/job?q=1", saved.getEnlace());
        assertEquals("Condiciones\n  Conservar formato", saved.getNotas());
    }

    @Test void textLimitsAreErrorsNotTruncation() {
        String longCompany = "E".repeat(201);
        Candidatura c = new Candidatura(0, longCompany, "Puesto", CategoriaProfesional.OTRAS,
                EstadoCandidatura.ENVIADA, Modalidad.REMOTA, "", "", LocalDate.of(2026, 10, 7),
                false, false, "N".repeat(10001));
        ValidacionException error = assertThrows(ValidacionException.class, () -> service.anadir(c));
        assertTrue(error.getErrores().keySet().containsAll(Set.of("empresa", "notas")));
        assertEquals(longCompany, c.getEmpresa());
        assertFalse(Files.exists(database));
    }

    @Test void invalidIdsCannotEditOrDelete() {
        assertThrows(ValidacionException.class, () -> service.eliminar(0));
        assertThrows(ValidacionException.class, () -> service.eliminar(-1));
        assertThrows(ValidacionException.class, () -> service.editar(sample(Modalidad.REMOTA, "", "", LocalDate.of(2026, 10, 7))));
        assertThrows(ValidacionException.class, () -> service.anadir(sample(Modalidad.REMOTA, "", "", LocalDate.of(2026, 10, 7)).conId(10)));
        assertFalse(Files.exists(database));
    }

    private Candidatura sample(Modalidad mode, String location, String url, LocalDate date) {
        return new Candidatura(0, " Empresa ", " Puesto ", CategoriaProfesional.DESARROLLO_SOFTWARE,
                EstadoCandidatura.ENVIADA, mode, location, url, date, true, false, "Condiciones\n  Conservar formato");
    }

    @Test void stateChangeValidatesBeforeAccessingDatabase() {
        Candidatura saved = sample(Modalidad.REMOTA, "", "", LocalDate.of(2026, 10, 7)).conId(1);
        ValidacionException error = assertThrows(ValidacionException.class, () -> service.cambiarEstado(saved, null));
        assertTrue(error.getErrores().containsKey("estado"));
        assertThrows(ValidacionException.class, () -> service.cambiarEstado(saved.conId(0), EstadoCandidatura.RECHAZADA));
        assertFalse(Files.exists(database));
    }

    @Test void stateChangeReturnsConfirmedCopyAndLeavesOriginalIntact() throws Exception {
        service.inicializarYListar();
        Candidatura original = service.anadir(sample(Modalidad.REMOTA, "", "", LocalDate.of(2026, 10, 7)));
        Candidatura updated = service.cambiarEstado(original, EstadoCandidatura.EN_PROCESO);
        assertEquals(original.getId(), updated.getId());
        assertEquals(EstadoCandidatura.ENVIADA, original.getEstado());
        assertEquals(EstadoCandidatura.EN_PROCESO, updated.getEstado());
        assertEquals("En revisión", updated.getEstado().toString());
        assertEquals(updated.getEstado(), service.listar().get(0).getEstado());
        assertSame(updated, service.cambiarEstado(updated, EstadoCandidatura.EN_PROCESO));
    }
}
