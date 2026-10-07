package com.davidcuadralara.workfinder.service;

import com.davidcuadralara.workfinder.model.*;
import com.davidcuadralara.workfinder.repository.CandidaturaRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CandidaturaFilterTest {
    @TempDir Path directory;
    private Path database;
    private CandidaturaService service;
    private List<Candidatura> source;

    @BeforeEach void prepare() {
        database = directory.resolve("filters.db");
        service = new CandidaturaService(new CandidaturaRepository(database),
                Clock.fixed(Instant.parse("2026-10-07T09:00:00Z"), ZoneId.of("Europe/Madrid")));
        source = List.of(
                sample(1, "Órbita Ingeniería", "Java", EstadoCandidatura.ENVIADA, CategoriaProfesional.DESARROLLO_SOFTWARE, Modalidad.REMOTA),
                sample(2, "ÓRBITA Datos", "Analista", EstadoCandidatura.EN_PROCESO, CategoriaProfesional.DATOS, Modalidad.HIBRIDA),
                sample(3, "Otra empresa", "Órbita Ingeniería", EstadoCandidatura.RECHAZADA, CategoriaProfesional.DESARROLLO_SOFTWARE, Modalidad.PRESENCIAL));
    }

    @Test void noCriteriaPreservesOrderAndReturnsAnImmutableResult() {
        List<Candidatura> result = service.filtrar(source, FiltroCandidaturas.todos());
        assertEquals(source, result);
        assertSame(source.get(0), result.get(0));
        assertThrows(UnsupportedOperationException.class, result::clear);
        assertFalse(Files.exists(database), "Filtrar la lista no abre SQLite");
    }

    @Test void companySearchIgnoresCaseAccentsAndOuterWhitespace() {
        var filter = new FiltroCandidaturas("  orbita ingenieria  ", null, null, null);
        assertEquals("orbita ingenieria", filter.empresa());
        assertEquals(List.of(source.get(0)), service.filtrar(source, filter));
    }

    @Test void partialCompanySearchNeverMatchesPositionLocationOrNotes() {
        assertEquals(source.subList(0, 2), service.filtrar(source, new FiltroCandidaturas("orbita", null, null, null)));
    }

    @Test void percentageUnderscoreAndQuotesAreLiteralCharacters() {
        Candidatura literal = sample(4, "100%_ O'Brien", "Java", EstadoCandidatura.ENVIADA, CategoriaProfesional.OTRAS, Modalidad.REMOTA);
        List<Candidatura> data = List.of(literal, source.get(0));
        for (String query : List.of("%_", "O'Brien", "100%"))
            assertEquals(List.of(literal), service.filtrar(data, new FiltroCandidaturas(query, null, null, null)));
        assertFalse(Files.exists(database));
    }

    @Test void allCriteriaCombineWithAndRatherThanOr() {
        var filter = new FiltroCandidaturas("orbita", EstadoCandidatura.ENVIADA, CategoriaProfesional.DESARROLLO_SOFTWARE, Modalidad.REMOTA);
        assertEquals(List.of(source.get(0)), service.filtrar(source, filter));
        assertTrue(service.filtrar(source, new FiltroCandidaturas("orbita", EstadoCandidatura.ENVIADA, CategoriaProfesional.DATOS, Modalidad.REMOTA)).isEmpty());
    }

    @Test void eachEnumFilterCanBeUsedIndependently() {
        assertEquals(List.of(source.get(1)), service.filtrar(source, new FiltroCandidaturas("", EstadoCandidatura.EN_PROCESO, null, null)));
        assertEquals(List.of(source.get(1)), service.filtrar(source, new FiltroCandidaturas("", null, CategoriaProfesional.DATOS, null)));
        assertEquals(List.of(source.get(2)), service.filtrar(source, new FiltroCandidaturas("", null, null, Modalidad.PRESENCIAL)));
    }

    @Test void clearingFiltersRestoresAllRowsWithoutMutatingTheSource() {
        List<Candidatura> none = service.filtrar(source, new FiltroCandidaturas("ausente", null, null, null));
        assertTrue(none.isEmpty());
        assertEquals(3, source.size());
        assertEquals(source, service.filtrar(source, FiltroCandidaturas.todos()));
    }

    @Test void emptyDataAndNullOrBlankCompanyAreSupported() {
        assertTrue(service.filtrar(List.of(), new FiltroCandidaturas("empresa", EstadoCandidatura.OFERTA, null, null)).isEmpty());
        assertEquals(source, service.filtrar(source, new FiltroCandidaturas(null, null, null, null)));
        assertFalse(new FiltroCandidaturas("  ", null, null, null).activo());
        assertTrue(new FiltroCandidaturas("", null, null, Modalidad.REMOTA).activo());
    }

    @Test void unicodeCompanyNamesCanBeSearchedWithoutDiacritics() {
        Candidatura company = sample(4, "Núñez", "Java", EstadoCandidatura.ENVIADA, CategoriaProfesional.OTRAS, Modalidad.REMOTA);
        assertEquals(List.of(company), service.filtrar(List.of(company), new FiltroCandidaturas("NUNEZ", null, null, null)));
    }

    @Test void confirmedStateChangeMovesApplicationBetweenFilteredResults() throws Exception {
        service.inicializarYListar();
        Candidatura original = service.anadir(source.get(0).conId(0));
        var sent = new FiltroCandidaturas("", EstadoCandidatura.ENVIADA, null, null);
        var rejected = new FiltroCandidaturas("", EstadoCandidatura.RECHAZADA, null, null);
        assertEquals(1, service.filtrar(service.listar(), sent).size());
        service.cambiarEstado(original, EstadoCandidatura.RECHAZADA);
        assertTrue(service.filtrar(service.listar(), sent).isEmpty());
        assertEquals(original.getId(), service.filtrar(service.listar(), rejected).get(0).getId());
        assertEquals(EstadoCandidatura.ENVIADA, original.getEstado());
    }

    private Candidatura sample(long id, String company, String position, EstadoCandidatura state, CategoriaProfesional category, Modalidad mode) {
        return new Candidatura(id, company, position, category, state, mode, "Madrid", "",
                LocalDate.of(2026, 10, 7), false, false, "Condiciones y notas");
    }
}

