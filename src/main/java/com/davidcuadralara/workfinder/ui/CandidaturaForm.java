package com.davidcuadralara.workfinder.ui;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.Modalidad;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Formulario dentro de la ventana. Recoge valores y representa errores del servicio. */
final class CandidaturaForm {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final BorderPane root = new BorderPane();
    private final GridPane fields = new GridPane();
    private final ScrollPane scroll = new ScrollPane(fields);
    private final Map<String, Control> controls = new LinkedHashMap<>();
    private final Map<String, Label> errors = new LinkedHashMap<>();
    private final TextField empresa = new TextField();
    private final TextField puesto = new TextField();
    private final ComboBox<CategoriaProfesional> categoria = new ComboBox<>();
    private final ComboBox<EstadoCandidatura> estado = new ComboBox<>();
    private final ComboBox<Modalidad> modalidad = new ComboBox<>();
    private final TextField ubicacion = new TextField();
    private final TextField enlace = new TextField();
    private final TextField fecha = new TextField();
    private final CheckBox cv = new CheckBox("CV enviado");
    private final CheckBox carta = new CheckBox("Carta enviada");
    private final TextArea notas = new TextArea();
    private final Button save = new Button("Guardar candidatura");
    private final Button cancel = new Button("Cancelar");
    private final long id;

    CandidaturaForm(Candidatura source, Runnable onSave, Runnable onCancel) {
        id = source == null ? 0 : source.getId();
        root.setId("candidatura-form");
        root.getStyleClass().add("candidatura-form");
        fields.setHgap(20);
        fields.setVgap(14);
        for (int index = 0; index < 2; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            fields.getColumnConstraints().add(column);
        }
        categoria.getItems().setAll(CategoriaProfesional.values());
        estado.getItems().setAll(EstadoCandidatura.values());
        modalidad.getItems().setAll(Modalidad.values());
        categoria.setPromptText("Selecciona una categoría");
        estado.setPromptText("Selecciona un estado");
        modalidad.setPromptText("Selecciona una modalidad");
        ubicacion.setPromptText("Ciudad o zona; opcional en remoto");
        enlace.setPromptText("https://… (opcional)");
        fecha.setPromptText("dd/mm/aaaa");
        notas.setPromptText("Condiciones, contacto, salario o próximos pasos…");
        notas.setWrapText(true);
        notas.setPrefRowCount(3);

        fields.add(field("empresa", "Empresa *", empresa), 0, 0);
        fields.add(field("puesto", "Puesto *", puesto), 1, 0);
        fields.add(field("categoria", "Categoría profesional *", categoria), 0, 1);
        fields.add(field("estado", "Estado *", estado), 1, 1);
        fields.add(field("modalidad", "Modalidad *", modalidad), 0, 2);
        fields.add(field("ubicacion", "Ubicación", ubicacion), 1, 2);
        fields.add(field("fecha", "Fecha de envío * · dd/mm/aaaa", fecha), 0, 3);
        cv.setId("form-cv");
        carta.setId("form-carta");
        VBox documents = new VBox(10, cv, carta);
        documents.setAlignment(Pos.CENTER_LEFT);
        fields.add(documents, 1, 3);
        fields.add(field("enlace", "Enlace de la oferta", enlace), 0, 4, 2, 1);
        fields.add(field("notas", "Notas y condiciones", notas), 0, 5, 2, 1);

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("form-scroll");
        root.setCenter(scroll);
        save.setId("guardar-candidatura-button");
        cancel.setId("cancelar-form-button");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> onSave.run());
        cancel.setOnAction(event -> onCancel.run());
        Label required = new Label("* Obligatorio. Ubicación necesaria en presencial o híbrido.");
        required.getStyleClass().add("pending-text");
        required.setWrapText(true);
        VBox bottom = new VBox(10, required, new HBox(10, save, cancel));
        bottom.getStyleClass().add("form-actions");
        root.setBottom(bottom);
        load(source);
    }

    BorderPane getRoot() { return root; }
    String getFechaText() { return fecha.getText(); }

    Candidatura read(LocalDate date) {
        return new Candidatura(id, empresa.getText(), puesto.getText(), categoria.getValue(), estado.getValue(),
                modalidad.getValue(), ubicacion.getText(), enlace.getText(), date, cv.isSelected(), carta.isSelected(), notas.getText());
    }

    void focusFirst() { empresa.requestFocus(); }

    void setBusy(boolean busy) {
        fields.setDisable(busy);
        save.setDisable(busy);
        cancel.setDisable(busy);
    }

    void clearErrors() {
        controls.forEach((name, control) -> {
            control.pseudoClassStateChanged(INVALID, false);
            control.setAccessibleHelp(null);
        });
        errors.values().forEach(error -> {
            error.setText("");
            error.setVisible(false);
            error.setManaged(false);
        });
    }

    void showErrors(Map<String, String> messages) {
        clearErrors();
        Control first = null;
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            Control control = controls.get(entry.getKey());
            if (control == null) continue;
            control.pseudoClassStateChanged(INVALID, true);
            control.setAccessibleHelp(entry.getValue());
            Label error = errors.get(entry.getKey());
            error.setText(entry.getValue());
            error.setVisible(true);
            error.setManaged(true);
            if (first == null) first = control;
        }
        if (first != null) {
            fields.applyCss();
            fields.layout();
            double y = first.getParent().getBoundsInParent().getMinY();
            double available = fields.getHeight() - scroll.getViewportBounds().getHeight();
            scroll.setVvalue(available > 0 ? Math.min(1, y / available) : 0);
            first.requestFocus();
        }
    }

    private VBox field(String name, String title, Control control) {
        control.setId("form-" + name);
        control.setMaxWidth(Double.MAX_VALUE);
        Label label = new Label(title);
        label.setWrapText(true);
        label.setLabelFor(control);
        label.getStyleClass().add("field-label");
        Label error = new Label();
        error.setId("error-" + name);
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        error.getStyleClass().add("field-error");
        controls.put(name, control);
        errors.put(name, error);
        return new VBox(6, label, control, error);
    }

    private void load(Candidatura c) {
        if (c == null) {
            estado.setValue(EstadoCandidatura.ENVIADA);
            fecha.setText(LocalDate.now().format(FECHA));
            return;
        }
        empresa.setText(c.getEmpresa());
        puesto.setText(c.getPuesto());
        categoria.setValue(c.getCategoria());
        estado.setValue(c.getEstado());
        modalidad.setValue(c.getModalidad());
        ubicacion.setText(c.getUbicacion());
        enlace.setText(c.getEnlace());
        fecha.setText(c.getFechaEnvio().format(FECHA));
        cv.setSelected(c.isCvEnviado());
        carta.setSelected(c.isCartaEnviada());
        notas.setText(c.getNotas());
        save.setText("Guardar cambios");
    }
}
