package com.davidcuadralara.workfinder.ui;

import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.FiltroCandidaturas;
import com.davidcuadralara.workfinder.model.Modalidad;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Controles de búsqueda. Entrega criterios a la vista, sin reglas de filtrado ni consultas. */
final class CandidaturaSearch {
    private final VBox root = new VBox(10);
    private final TextField empresa = new TextField();
    private final ComboBox<EstadoCandidatura> estado = selector("Todos los estados", EstadoCandidatura.values(), "filtro-estado");
    private final ComboBox<CategoriaProfesional> categoria = selector("Todas las categorías", CategoriaProfesional.values(), "filtro-categoria");
    private final ComboBox<Modalidad> modalidad = selector("Cualquier modalidad", Modalidad.values(), "filtro-modalidad");
    private final PauseTransition delay = new PauseTransition(Duration.millis(200));
    private final Consumer<FiltroCandidaturas> onChange;
    private boolean resetting;

    CandidaturaSearch(Consumer<FiltroCandidaturas> onChange, Runnable onClose) {
        this.onChange = Objects.requireNonNull(onChange);
        Objects.requireNonNull(onClose);
        root.setId("busqueda-panel");
        root.getStyleClass().add("search-panel");
        empresa.setId("buscar-empresa-field");
        empresa.setPromptText("Nombre de la empresa");
        empresa.setAccessibleText("Buscar solo por empresa");
        empresa.setTooltip(new Tooltip("Coincidencia parcial, sin distinguir mayúsculas ni tildes."));
        Button clear = new Button("Limpiar");
        clear.setId("limpiar-filtros-button");
        clear.setTooltip(new Tooltip("Quitar la búsqueda y todos los filtros."));
        Button close = new Button("×");
        close.setId("cerrar-filtros-button");
        close.setAccessibleText("Ocultar búsqueda y filtros");
        close.setTooltip(new Tooltip("Ocultar el panel. Los criterios se conservan."));
        close.getStyleClass().add("search-button");
        HBox companyRow = new HBox(10, empresa, clear, close);
        HBox.setHgrow(empresa, Priority.ALWAYS);
        HBox filters = new HBox(10, estado, categoria, modalidad);
        for (ComboBox<?> combo : new ComboBox<?>[]{estado, categoria, modalidad}) {
            HBox.setHgrow(combo, Priority.ALWAYS);
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setMinWidth(140);
        }
        categoria.setPrefWidth(250);
        estado.setPrefWidth(180);
        modalidad.setPrefWidth(180);
        root.getChildren().addAll(companyRow, filters);
        delay.setOnFinished(event -> publish());
        empresa.textProperty().addListener((observable, before, after) -> {
            if (!resetting) delay.playFromStart();
        });
        estado.valueProperty().addListener((observable, before, after) -> publish());
        categoria.valueProperty().addListener((observable, before, after) -> publish());
        modalidad.valueProperty().addListener((observable, before, after) -> publish());
        empresa.setOnAction(event -> publish());
        clear.setOnAction(event -> clear());
        close.setOnAction(event -> onClose.run());
    }

    VBox getRoot() { return root; }
    void focus() { empresa.requestFocus(); }
    void setBusy(boolean busy) { root.setDisable(busy); }
    void stop() { delay.stop(); }

    private void clear() {
        resetting = true;
        delay.stop();
        try {
            empresa.clear();
            estado.setValue(null);
            categoria.setValue(null);
            modalidad.setValue(null);
        } finally {
            resetting = false;
        }
        publish();
        empresa.requestFocus();
    }

    private void publish() {
        if (resetting) return;
        delay.stop();
        onChange.accept(new FiltroCandidaturas(empresa.getText(), estado.getValue(), categoria.getValue(), modalidad.getValue()));
    }

    private static <T extends Enum<T>> ComboBox<T> selector(String any, T[] values, String id) {
        ComboBox<T> combo = new ComboBox<>();
        combo.setId(id);
        combo.setEditable(false);
        combo.setAccessibleText(any);
        combo.setPromptText(any);
        combo.getItems().add(null);
        combo.getItems().addAll(values);
        combo.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? any : item.toString());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? any : item.toString());
            }
        });
        return combo;
    }
}
