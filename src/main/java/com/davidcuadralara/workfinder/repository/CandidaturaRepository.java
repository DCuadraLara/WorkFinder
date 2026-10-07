package com.davidcuadralara.workfinder.repository;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.Modalidad;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** JDBC síncrono. El llamante ejecuta este objeto fuera del hilo de JavaFX. */
public final class CandidaturaRepository {
    private static final String COLUMNAS = "id, empresa, puesto, categoria, estado, modalidad, "
            + "ubicacion, enlace, fecha_envio, cv_enviado, carta_enviada, notas";
    private final Path archivo;

    public CandidaturaRepository(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo).toAbsolutePath().normalize();
    }

    public void inicializar() throws PersistenciaException {
        try {
            Files.createDirectories(archivo.getParent());
            try (Connection conexion = conectar();
                    PreparedStatement consulta = conexion.prepareStatement("""
                            CREATE TABLE IF NOT EXISTS candidaturas (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                empresa TEXT NOT NULL CHECK (length(trim(empresa)) > 0),
                                puesto TEXT NOT NULL CHECK (length(trim(puesto)) > 0),
                                categoria TEXT NOT NULL,
                                estado TEXT NOT NULL,
                                modalidad TEXT NOT NULL,
                                ubicacion TEXT NOT NULL DEFAULT '',
                                enlace TEXT NOT NULL DEFAULT '',
                                fecha_envio TEXT NOT NULL,
                                cv_enviado INTEGER NOT NULL CHECK (cv_enviado IN (0, 1)),
                                carta_enviada INTEGER NOT NULL CHECK (carta_enviada IN (0, 1)),
                                notas TEXT NOT NULL DEFAULT ''
                            )
                            """)) {
                consulta.executeUpdate();
            }
        } catch (SQLException | IOException error) {
            throw fallo("No se ha podido abrir o preparar la base de datos.", error);
        }
    }

    public List<Candidatura> listar() throws PersistenciaException {
        List<Candidatura> resultado = new ArrayList<>();
        try (Connection conexion = conectar();
                PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT " + COLUMNAS + " FROM candidaturas ORDER BY fecha_envio DESC, id DESC");
                ResultSet filas = consulta.executeQuery()) {
            while (filas.next()) resultado.add(leer(filas));
            return List.copyOf(resultado);
        } catch (SQLException error) {
            throw fallo("No se han podido cargar las candidaturas.", error);
        }
    }

    public Candidatura anadir(Candidatura candidatura) throws PersistenciaException {
        try (Connection conexion = conectar()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement consulta = conexion.prepareStatement("""
                        INSERT INTO candidaturas (empresa, puesto, categoria, estado, modalidad,
                            ubicacion, enlace, fecha_envio, cv_enviado, carta_enviada, notas)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                completar(consulta, candidatura);
                if (consulta.executeUpdate() != 1) throw new SQLException("No se ha insertado la fila esperada.");
                long id;
                try (ResultSet claves = consulta.getGeneratedKeys()) {
                    if (!claves.next()) throw new SQLException("No se ha recibido el identificador generado.");
                    id = claves.getLong(1);
                }
                conexion.commit();
                return candidatura.conId(id);
            } catch (SQLException error) {
                try { conexion.rollback(); }
                catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        } catch (SQLException error) {
            throw fallo("No se ha podido guardar la candidatura.", error);
        }
    }

    public Candidatura editar(Candidatura candidatura) throws PersistenciaException {
        try (Connection conexion = conectar();
                PreparedStatement consulta = conexion.prepareStatement("""
                        UPDATE candidaturas SET empresa = ?, puesto = ?, categoria = ?, estado = ?,
                            modalidad = ?, ubicacion = ?, enlace = ?, fecha_envio = ?,
                            cv_enviado = ?, carta_enviada = ?, notas = ? WHERE id = ?
                        """)) {
            completar(consulta, candidatura);
            consulta.setLong(12, candidatura.getId());
            comprobarFila(consulta.executeUpdate());
            return candidatura;
        } catch (SQLException error) {
            throw fallo("No se han podido guardar los cambios.", error);
        }
    }

    public void eliminar(long id) throws PersistenciaException {
        try (Connection conexion = conectar();
                PreparedStatement consulta = conexion.prepareStatement("DELETE FROM candidaturas WHERE id = ?")) {
            consulta.setLong(1, id);
            comprobarFila(consulta.executeUpdate());
        } catch (SQLException error) {
            throw fallo("No se ha podido eliminar la candidatura.", error);
        }
    }

    /** Actualiza solo el estado y evita sobrescribir un cambio realizado desde otra ventana. */
    public void cambiarEstado(long id, EstadoCandidatura anterior, EstadoCandidatura nuevo)
            throws PersistenciaException {
        try (Connection conexion = conectar();
                PreparedStatement consulta = conexion.prepareStatement(
                        "UPDATE candidaturas SET estado = ? WHERE id = ? AND estado = ?")) {
            consulta.setString(1, nuevo.name());
            consulta.setLong(2, id);
            consulta.setString(3, anterior.name());
            if (consulta.executeUpdate() != 1) throw new PersistenciaException(
                    "La candidatura ya no existe o su estado ha cambiado. Recarga la lista antes de continuar.");
        } catch (SQLException error) {
            throw fallo("No se ha podido guardar el estado. Se conserva el estado anterior en pantalla.", error);
        }
    }

    private Connection conectar() throws SQLException {
        Properties opciones = new Properties();
        opciones.setProperty("busy_timeout", "4000");
        return DriverManager.getConnection("jdbc:sqlite:" + archivo, opciones);
    }

    private void completar(PreparedStatement consulta, Candidatura c) throws SQLException {
        consulta.setString(1, c.getEmpresa());
        consulta.setString(2, c.getPuesto());
        consulta.setString(3, c.getCategoria().name());
        consulta.setString(4, c.getEstado().name());
        consulta.setString(5, c.getModalidad().name());
        consulta.setString(6, c.getUbicacion());
        consulta.setString(7, c.getEnlace());
        consulta.setString(8, c.getFechaEnvio().toString());
        consulta.setInt(9, c.isCvEnviado() ? 1 : 0);
        consulta.setInt(10, c.isCartaEnviada() ? 1 : 0);
        consulta.setString(11, c.getNotas());
    }

    private Candidatura leer(ResultSet fila) throws SQLException, PersistenciaException {
        try {
            return new Candidatura(fila.getLong("id"), fila.getString("empresa"), fila.getString("puesto"),
                    CategoriaProfesional.valueOf(fila.getString("categoria")),
                    EstadoCandidatura.valueOf(fila.getString("estado")),
                    Modalidad.valueOf(fila.getString("modalidad")), fila.getString("ubicacion"),
                    fila.getString("enlace"), LocalDate.parse(fila.getString("fecha_envio")),
                    fila.getInt("cv_enviado") == 1, fila.getInt("carta_enviada") == 1, fila.getString("notas"));
        } catch (IllegalArgumentException | java.time.DateTimeException error) {
            throw new PersistenciaException("Una candidatura contiene una fecha o un valor de enum incompatible. "
                    + "No se han modificado los datos; revisa el archivo o restaura una copia válida.", error);
        }
    }

    private void comprobarFila(int filas) throws PersistenciaException {
        if (filas != 1) throw new PersistenciaException(
                "La candidatura ya no existe. Recarga la lista antes de continuar.");
    }

    private PersistenciaException fallo(String mensaje, Exception causa) {
        return new PersistenciaException(mensaje
                + " Comprueba la ubicación y los permisos del archivo, o si está bloqueado por otro programa.", causa);
    }
}
