package com.davidcuadralara.workfinder.ui;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

/** Selector de presentación. El evento se delega a la vista; aquí no se guarda ningún dato. */
final class EstadoTableCell extends TableCell<Candidatura, EstadoCandidatura> {
    private final ComboBox<EstadoCandidatura> picker = new ComboBox<>();
    private boolean synchronizing;

    EstadoTableCell(BiConsumer<Candidatura, EstadoCandidatura> onChange) {
        Objects.requireNonNull(onChange);
        getStyleClass().add("state-cell");
        picker.getItems().setAll(EstadoCandidatura.values());
        picker.setEditable(false);
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.getStyleClass().add("state-picker");
        picker.setAccessibleText("Cambiar estado de candidatura");
        picker.setTooltip(new Tooltip("Selecciona un estado para guardarlo."));
        picker.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(EstadoCandidatura item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        picker.setCellFactory(ignored -> new ListCell<>() {
            { getStyleClass().add("state-option"); }
            @Override protected void updateItem(EstadoCandidatura item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                color(this, empty ? null : item);
            }
        });
        picker.valueProperty().addListener((observable, previous, selected) -> {
            if (synchronizing || isEmpty() || selected == null || getTableRow() == null) return;
            Candidatura candidatura = getTableRow().getItem();
            if (candidatura != null) onChange.accept(candidatura, selected);
        });
    }

    @Override protected void updateItem(EstadoCandidatura item, boolean empty) {
        super.updateItem(item, empty);
        synchronizing = true;
        try {
            picker.setValue(empty ? null : item);
            color(picker, empty ? null : item);
            setText(null);
            setGraphic(empty || item == null ? null : picker);
        } finally {
            synchronizing = false;
        }
    }

    private static void color(Node node, EstadoCandidatura selected) {
        for (EstadoCandidatura state : EstadoCandidatura.values()) {
            String name = state.name().toLowerCase(Locale.ROOT).replace('_', '-');
            node.pseudoClassStateChanged(PseudoClass.getPseudoClass(name), state == selected);
        }
    }
}
