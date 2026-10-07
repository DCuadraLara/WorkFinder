package com.davidcuadralara.workfinder.service;

import com.davidcuadralara.workfinder.model.*;
import com.davidcuadralara.workfinder.repository.CandidaturaRepository;
import com.davidcuadralara.workfinder.repository.PersistenciaException;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CandidaturaSummaryTest {
    @TempDir Path directory;
    private Path database;
    private CandidaturaService service;

    @BeforeEach void prepare() {
        database = directory.resolve("summary.db");
        service = new CandidaturaService(new CandidaturaRepository(database),
                Clock.fixed(Instant.parse("2026-10-07T09:00:00Z"), ZoneId.of("Europe/Madrid")));
    }

    @Test void emptyDataIncludesEveryStateAndCategoryWithZero() {
        ResumenCandidaturas summary = service.calcularResumen(List.of());
        assertEquals(0, summary.total());
        assertEquals(0, summary.maxCategoria());
        assertEquals(Set.of(EstadoCandidatura.values()), summary.porEstado().keySet());
        assertEquals(Set.of(CategoriaProfesional.values()), summary.porCategoria().keySet());
        assertTrue(summary.porEstado().values().stream().allMatch(n -> n == 0));
        assertTrue(summary.porCategoria().values().stream().allMatch(n -> n == 0));
        assertFalse(Files.exists(database), "Calcular no debe abrir SQLite");
    }

    @Test void countsApplicationsInsteadOfDistinctCompaniesOrPositions() {
        Candidatura one = sample(EstadoCandidatura.ENVIADA, CategoriaProfesional.DESARROLLO_SOFTWARE);
        Candidatura two = sample(EstadoCandidatura.RECHAZADA, CategoriaProfesional.DESARROLLO_SOFTWARE);
        ResumenCandidaturas summary = service.calcularResumen(List.of(one, two));
        assertEquals(2, summary.total());
        assertEquals(1L, summary.porEstado().get(EstadoCandidatura.ENVIADA));
        assertEquals(1L, summary.porEstado().get(EstadoCandidatura.RECHAZADA));
        assertEquals(2L, summary.porCategoria().get(CategoriaProfesional.DESARROLLO_SOFTWARE));
        assertEquals(2, summary.maxCategoria());
    }

    @Test void allStatesAndCategoriesAreCountedAndSumsEqualTotal() {
        List<Candidatura> data = new ArrayList<>();
        for (EstadoCandidatura state : EstadoCandidatura.values())
            data.add(sample(state, CategoriaProfesional.DATOS));
        for (CategoriaProfesional category : CategoriaProfesional.values())
            data.add(sample(EstadoCandidatura.OFERTA, category));
        ResumenCandidaturas summary = service.calcularResumen(data);
        assertEquals(data.size(), summary.total());
        assertEquals(summary.total(), summary.porEstado().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(summary.total(), summary.porCategoria().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(7L, summary.porCategoria().get(CategoriaProfesional.DATOS));
        assertEquals(8L, summary.porEstado().get(EstadoCandidatura.OFERTA));
        assertEquals(7, summary.maxCategoria());
    }

    @Test void returnedSummaryIsAnImmutableSnapshot() {
        List<Candidatura> data = new ArrayList<>(List.of(sample(EstadoCandidatura.ENVIADA, CategoriaProfesional.DATOS)));
        ResumenCandidaturas summary = service.calcularResumen(data);
        data.clear();
        assertEquals(1, summary.total());
        assertThrows(UnsupportedOperationException.class, () -> summary.porEstado().put(EstadoCandidatura.ENVIADA, 50L));
        assertThrows(UnsupportedOperationException.class, () -> summary.porCategoria().clear());
    }

    @Test void filteringDoesNotMutateGlobalSourceOrItsSummary() {
        List<Candidatura> data = List.of(sample(EstadoCandidatura.ENVIADA, CategoriaProfesional.DATOS),
                sample(EstadoCandidatura.RECHAZADA, CategoriaProfesional.OTRAS));
        var before = service.calcularResumen(data);
        assertEquals(1, service.filtrar(data, new FiltroCandidaturas("", EstadoCandidatura.ENVIADA, null, null)).size());
        assertEquals(before, service.calcularResumen(data));
    }

    @Test void summaryReflectsConfirmedCreateEditStateChangeDeleteAndReopening() throws Exception {
        service.inicializarYListar();
        Candidatura first = service.anadir(sample(EstadoCandidatura.ENVIADA, CategoriaProfesional.DATOS));
        assertEquals(1, service.calcularResumen(service.listar()).total());
        Candidatura edited = new Candidatura(first.getId(), first.getEmpresa(), first.getPuesto(), CategoriaProfesional.DISENO,
                EstadoCandidatura.ENTREVISTA, first.getModalidad(), first.getUbicacion(), "", first.getFechaEnvio(), false, false, "");
        service.editar(edited);
        var summary = service.calcularResumen(service.listar());
        assertEquals(0L, summary.porCategoria().get(CategoriaProfesional.DATOS));
        assertEquals(1L, summary.porCategoria().get(CategoriaProfesional.DISENO));
        assertEquals(1L, summary.porEstado().get(EstadoCandidatura.ENTREVISTA));
        service.cambiarEstado(edited, EstadoCandidatura.OFERTA);
        assertEquals(1L, service.calcularResumen(new CandidaturaRepository(database).listar()).porEstado().get(EstadoCandidatura.OFERTA));
        service.eliminar(first.getId());
        assertEquals(0, service.calcularResumen(service.listar()).total());
        assertEquals(0, service.calcularResumen(service.listar()).maxCategoria());
    }

    @Test void sqlFailureCannotChangeConfirmedSummary() throws Exception {
        service.inicializarYListar();
        Candidatura saved = service.anadir(sample(EstadoCandidatura.ENVIADA, CategoriaProfesional.DATOS));
        var before = service.calcularResumen(service.listar());
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + database);
                PreparedStatement s = c.prepareStatement("CREATE TRIGGER reject_state BEFORE UPDATE OF estado ON candidaturas BEGIN SELECT RAISE(ABORT, 'qa'); END")) {
            s.executeUpdate();
        }
        assertThrows(PersistenciaException.class, () -> service.cambiarEstado(saved, EstadoCandidatura.RECHAZADA));
        assertEquals(before, service.calcularResumen(service.listar()));
    }

    @Test void largeTotalsDoNotUsePercentagesOrRoundAwayApplications() {
        List<Candidatura> data = new ArrayList<>();
        for (int i = 0; i < 1500; i++) data.add(sample(EstadoCandidatura.ENVIADA, CategoriaProfesional.DATOS));
        data.add(sample(EstadoCandidatura.EN_PROCESO, CategoriaProfesional.OTRAS));
        ResumenCandidaturas summary = service.calcularResumen(data);
        assertEquals(1501, summary.total());
        assertEquals(1500L, summary.porCategoria().get(CategoriaProfesional.DATOS));
        assertEquals(1L, summary.porCategoria().get(CategoriaProfesional.OTRAS));
        assertEquals(1500, summary.maxCategoria());
    }

    private Candidatura sample(EstadoCandidatura state, CategoriaProfesional category) {
        return new Candidatura(0, "Misma empresa", "Mismo puesto", category, state, Modalidad.REMOTA,
                "", "", LocalDate.of(2026, 10, 7), false, false, "");
    }
}
