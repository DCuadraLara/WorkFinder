package com.davidcuadralara.workfinder.repository;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.Modalidad;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CandidaturaRepositoryTest {
    @TempDir Path directory;
    private Path database;
    private CandidaturaRepository repository;

    @BeforeEach void prepare() throws Exception {
        database = directory.resolve("data").resolve("workfinder.db");
        repository = new CandidaturaRepository(database);
        repository.inicializar();
    }

    @Test void schemaInitializationPreservesDataAcrossRepositoryInstances() throws Exception {
        assertTrue(repository.listar().isEmpty());
        Candidatura first = repository.anadir(sample("Compañía", LocalDate.of(2026, 10, 7)));
        CandidaturaRepository reopened = new CandidaturaRepository(database);
        reopened.inicializar();
        reopened.inicializar();
        Candidatura result = reopened.listar().get(0);
        assertEquals(first.getId(), result.getId());
        assertEquals("Compañía", result.getEmpresa());
        assertEquals(first.getPuesto(), result.getPuesto());
        assertEquals(first.getCategoria(), result.getCategoria());
        assertEquals(first.getEstado(), result.getEstado());
        assertEquals(first.getModalidad(), result.getModalidad());
        assertEquals(first.getUbicacion(), result.getUbicacion());
        assertEquals(first.getEnlace(), result.getEnlace());
        assertEquals(first.getFechaEnvio(), result.getFechaEnvio());
        assertTrue(result.isCvEnviado());
        assertFalse(result.isCartaEnviada());
        assertEquals(first.getNotas(), result.getNotas());
    }

    @Test void sameCompanyAndPositionAreDifferentApplicationsWithStableUniqueIds() throws Exception {
        Candidatura first = repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7)));
        Candidatura second = repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7)));
        assertTrue(first.getId() > 0);
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, repository.listar().size());
        repository.eliminar(second.getId());
        Candidatura third = repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7)));
        assertTrue(third.getId() > second.getId());
    }

    @Test void editAndDeleteAffectOnlySelectedId() throws Exception {
        Candidatura original = repository.anadir(sample("Una", LocalDate.of(2026, 10, 7)));
        Candidatura other = repository.anadir(sample("Otra", LocalDate.of(2026, 10, 6)));
        Candidatura edited = new Candidatura(original.getId(), "Empresa editada", "Nuevo puesto", CategoriaProfesional.DATOS,
                EstadoCandidatura.ENTREVISTA, Modalidad.HIBRIDA, "Madrid", "", LocalDate.of(2026, 10, 5), false, true, "Nuevas notas");
        repository.editar(edited);
        Candidatura result = repository.listar().stream().filter(c -> c.getId() == original.getId()).findFirst().orElseThrow();
        assertEquals("Empresa editada", result.getEmpresa());
        assertEquals("Nuevo puesto", result.getPuesto());
        assertEquals(CategoriaProfesional.DATOS, result.getCategoria());
        assertEquals(EstadoCandidatura.ENTREVISTA, result.getEstado());
        assertEquals(Modalidad.HIBRIDA, result.getModalidad());
        assertEquals("Madrid", result.getUbicacion());
        assertEquals("", result.getEnlace());
        assertEquals(LocalDate.of(2026, 10, 5), result.getFechaEnvio());
        assertFalse(result.isCvEnviado());
        assertTrue(result.isCartaEnviada());
        assertEquals("Nuevas notas", result.getNotas());
        repository.eliminar(original.getId());
        assertEquals(List.of(other.getId()), repository.listar().stream().map(Candidatura::getId).toList());
    }

    @Test void datesSortChronologicallyThenByNewestId() throws Exception {
        Candidatura older = repository.anadir(sample("Antigua", LocalDate.of(2025, 12, 31)));
        Candidatura recent = repository.anadir(sample("Nueva", LocalDate.of(2026, 1, 1)));
        Candidatura newest = repository.anadir(sample("Otra", LocalDate.of(2026, 1, 1)));
        assertEquals(List.of(newest.getId(), recent.getId(), older.getId()), repository.listar().stream().map(Candidatura::getId).toList());
    }

    @Test void quotesAndSqlLikeTextRemainLiteralData() throws Exception {
        String company = "O'Brien'); DROP TABLE candidaturas; --";
        repository.anadir(sample(company, LocalDate.of(2026, 10, 7)));
        repository.anadir(sample("Otra", LocalDate.of(2026, 10, 7)));
        assertEquals(2, repository.listar().size());
        assertTrue(repository.listar().stream().anyMatch(c -> c.getEmpresa().equals(company)));
    }

    @Test void missingRowsAreFailuresNotSuccessfulEditsOrDeletes() {
        assertThrows(PersistenciaException.class, () -> repository.editar(sample("Ausente", LocalDate.of(2026, 10, 7)).conId(999)));
        assertThrows(PersistenciaException.class, () -> repository.eliminar(999));
    }

    @Test void sqlFailuresPreserveOldRowAndCanBeRetried() throws Exception {
        Candidatura original = repository.anadir(sample("Original", LocalDate.of(2026, 10, 7)));
        sql("CREATE TRIGGER reject_update BEFORE UPDATE ON candidaturas BEGIN SELECT RAISE(ABORT, 'qa failure'); END");
        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> repository.editar(sample("Cambio", LocalDate.of(2026, 10, 7)).conId(original.getId())));
        assertNotNull(error.getCause());
        assertEquals("Original", repository.listar().get(0).getEmpresa());
        sql("DROP TRIGGER reject_update");
        repository.editar(sample("Cambio", LocalDate.of(2026, 10, 7)).conId(original.getId()));
        assertEquals("Cambio", repository.listar().get(0).getEmpresa());
        sql("CREATE TRIGGER reject_delete BEFORE DELETE ON candidaturas BEGIN SELECT RAISE(ABORT, 'qa failure'); END");
        assertThrows(PersistenciaException.class, () -> repository.eliminar(original.getId()));
        assertEquals(1, repository.listar().size());
    }

    @Test void failedInsertDoesNotCreateAnApplication() throws Exception {
        sql("CREATE TRIGGER reject_insert BEFORE INSERT ON candidaturas BEGIN SELECT RAISE(ABORT, 'qa failure'); END");
        assertThrows(PersistenciaException.class, () -> repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7))));
        assertTrue(repository.listar().isEmpty());
        sql("DROP TRIGGER reject_insert");
        assertTrue(repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7))).getId() > 0);
    }

    @Test void corruptDatabaseAndUnusablePathProduceControlledErrors() throws Exception {
        Path corrupt = directory.resolve("corrupt.db");
        Files.writeString(corrupt, "not sqlite");
        assertThrows(PersistenciaException.class, () -> new CandidaturaRepository(corrupt).inicializar());
        Path blockedParent = directory.resolve("a-file");
        Files.writeString(blockedParent, "file");
        assertThrows(PersistenciaException.class, () -> new CandidaturaRepository(blockedParent.resolve("data.db")).inicializar());
    }

    @Test void incompatibleStoredEnumIsControlledAndDoesNotEraseData() throws Exception {
        Candidatura saved = repository.anadir(sample("Original", LocalDate.of(2026, 10, 7)));
        sql("UPDATE candidaturas SET estado = 'UNKNOWN'");
        assertThrows(PersistenciaException.class, repository::listar);
        sql("UPDATE candidaturas SET estado = 'ENVIADA'");
        assertEquals(saved.getId(), repository.listar().get(0).getId());
    }

    private void sql(String text) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                PreparedStatement statement = connection.prepareStatement(text)) {
            statement.executeUpdate();
        }
    }

    @Test void changingStatePreservesOtherFieldsAndOtherApplicationsAcrossReopening() throws Exception {
        Candidatura original = repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7)));
        Candidatura other = repository.anadir(sample("Empresa", LocalDate.of(2026, 10, 7)));
        // Simula una edición de otro campo posterior a la carga de la fila en pantalla.
        sql("UPDATE candidaturas SET notas = 'Notas más recientes' WHERE id = " + original.getId());
        repository.cambiarEstado(original.getId(), original.getEstado(), EstadoCandidatura.EN_PROCESO);
        List<Candidatura> loaded = new CandidaturaRepository(database).listar();
        Candidatura changed = loaded.stream().filter(c -> c.getId() == original.getId()).findFirst().orElseThrow();
        assertEquals(EstadoCandidatura.EN_PROCESO, changed.getEstado());
        assertEquals("En revisión", changed.getEstado().toString());
        assertEquals("Notas más recientes", changed.getNotas());
        assertEquals(original.getEmpresa(), changed.getEmpresa());
        assertEquals(original.getPuesto(), changed.getPuesto());
        assertEquals(original.getCategoria(), changed.getCategoria());
        assertEquals(original.getModalidad(), changed.getModalidad());
        assertEquals(original.getUbicacion(), changed.getUbicacion());
        assertEquals(original.getEnlace(), changed.getEnlace());
        assertEquals(original.getFechaEnvio(), changed.getFechaEnvio());
        assertEquals(original.isCvEnviado(), changed.isCvEnviado());
        assertEquals(original.isCartaEnviada(), changed.isCartaEnviada());
        assertEquals(other.getEstado(), loaded.stream().filter(c -> c.getId() == other.getId()).findFirst().orElseThrow().getEstado());
    }

    @Test void failedStateChangePreservesOldStateAndAllowsRetry() throws Exception {
        Candidatura original = repository.anadir(sample("Original", LocalDate.of(2026, 10, 7)));
        sql("CREATE TRIGGER reject_state BEFORE UPDATE OF estado ON candidaturas BEGIN SELECT RAISE(ABORT, 'qa state failure'); END");
        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> repository.cambiarEstado(original.getId(), EstadoCandidatura.ENVIADA, EstadoCandidatura.RECHAZADA));
        assertNotNull(error.getCause());
        assertEquals(EstadoCandidatura.ENVIADA, repository.listar().get(0).getEstado());
        sql("DROP TRIGGER reject_state");
        repository.cambiarEstado(original.getId(), EstadoCandidatura.ENVIADA, EstadoCandidatura.RECHAZADA);
        assertEquals(EstadoCandidatura.RECHAZADA, repository.listar().get(0).getEstado());
    }

    @Test void staleStateOrMissingApplicationCannotBeOverwritten() throws Exception {
        Candidatura original = repository.anadir(sample("Original", LocalDate.of(2026, 10, 7)));
        repository.cambiarEstado(original.getId(), EstadoCandidatura.ENVIADA, EstadoCandidatura.ENTREVISTA);
        assertThrows(PersistenciaException.class,
                () -> repository.cambiarEstado(original.getId(), EstadoCandidatura.ENVIADA, EstadoCandidatura.RECHAZADA));
        assertEquals(EstadoCandidatura.ENTREVISTA, repository.listar().get(0).getEstado());
        repository.eliminar(original.getId());
        assertThrows(PersistenciaException.class,
                () -> repository.cambiarEstado(original.getId(), EstadoCandidatura.ENTREVISTA, EstadoCandidatura.RECHAZADA));
    }

    private Candidatura sample(String company, LocalDate date) {
        return new Candidatura(0, company, "Prácticas de Java", CategoriaProfesional.DESARROLLO_SOFTWARE,
                EstadoCandidatura.ENVIADA, Modalidad.REMOTA, "", "https://example.com/oferta?q=java", date,
                true, false, "Primera línea\nSegunda línea: 'comillas' y ñ");
    }
}
