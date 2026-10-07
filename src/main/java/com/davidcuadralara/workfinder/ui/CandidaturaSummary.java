package com.davidcuadralara.workfinder.ui;

import com.davidcuadralara.workfinder.model.CategoriaProfesional;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.ResumenCandidaturas;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.scene.chart.PieChart;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Representa recuentos, sectores y barras. Los cálculos y la selección del ámbito pertenecen al servicio. */
final class CandidaturaSummary {
    private final ScrollPane root = new ScrollPane();
    private final Label total = label("—", "metric-value");
    private final Label note = label("Cargando el resumen…", "pending-text");
    private final Label scale = label("Esperando los datos…", "pending-text");
    private final PieChart stateChart = new PieChart();
    private final Label statePlaceholder = label("Esperando\ndatos", "state-pie-placeholder");
    private final Map<EstadoCandidatura, Label> states = new EnumMap<>(EstadoCandidatura.class);
    private final Map<EstadoCandidatura, Tooltip> stateTips = new EnumMap<>(EstadoCandidatura.class);
    private final Map<CategoriaProfesional, Label> counts = new EnumMap<>(CategoriaProfesional.class);
    private final Map<CategoriaProfesional, DoubleProperty> proportions = new EnumMap<>(CategoriaProfesional.class);
    private final Map<CategoriaProfesional, Region> bars = new EnumMap<>(CategoriaProfesional.class);
    private final Map<CategoriaProfesional, Region> highlights = new EnumMap<>(CategoriaProfesional.class);
    private final DoubleProperty flowProgress = new SimpleDoubleProperty(0);
    private final Timeline categoryFlow = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(flowProgress, 0)),
            new KeyFrame(Duration.seconds(3.2), new KeyValue(flowProgress, 1, Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(4.4), new KeyValue(flowProgress, 1)));
    private final boolean animationsEnabled = Boolean.parseBoolean(System.getProperty("workfinder.animations", "true"));
    private final ChangeListener<Boolean> windowVisibility = (property, previous, current) -> updateFlow();
    private final ChangeListener<Window> sceneWindow = (property, previous, current) -> watchWindow(previous, current);
    private boolean hasLeadingCategory;
    private boolean disposed;
    private final Map<CategoriaProfesional, Tooltip> tips = new EnumMap<>(CategoriaProfesional.class);

    CandidaturaSummary() {
        categoryFlow.setCycleCount(Animation.INDEFINITE);
        total.setId("resumen-total");
        total.setAccessibleText("Total de candidaturas: esperando datos");
        note.setId("resumen-nota");
        scale.setId("resumen-escala");
        VBox totalBox = new VBox(2, label("Total de candidaturas", "muted-text"), total, note);
        totalBox.getStyleClass().add("summary-total");
        Tooltip.install(totalBox, new Tooltip("El total y las gráficas incluyen todas las candidaturas.\nLos filtros solo afectan a la tabla."));

        stateChart.setId("resumen-estados-grafica");
        stateChart.getStyleClass().add("state-pie");
        stateChart.setLegendVisible(false);
        stateChart.setLabelsVisible(false);
        stateChart.setAnimated(false);
        stateChart.setStartAngle(90);
        stateChart.setClockwise(true);
        stateChart.setMinSize(0, 0);
        stateChart.setVisible(false);
        statePlaceholder.setId("resumen-estados-vacio");
        statePlaceholder.setAlignment(Pos.CENTER);
        statePlaceholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        statePlaceholder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane pie = new StackPane(stateChart, statePlaceholder);
        pie.setMinSize(112, 132);
        pie.setPrefSize(132, 132);
        pie.setMaxSize(132, 132);
        VBox legend = new VBox(6);
        legend.setMinWidth(0);
        HBox.setHgrow(legend, Priority.ALWAYS);
        HBox distribution = new HBox(10, pie, legend);
        distribution.setAlignment(Pos.CENTER_LEFT);
        VBox stateBox = new VBox(6, label("Por estado", "card-title"), distribution);
        for (EstadoCandidatura state : EstadoCandidatura.values()) {
            Label name = label(state.toString(), "state-name");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            Label count = label("—", "summary-count");
            count.setMinWidth(Region.USE_PREF_SIZE);
            count.setId("resumen-estado-" + key(state));
            HBox row = new HBox(8, name, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("summary-state");
            row.pseudoClassStateChanged(PseudoClass.getPseudoClass(key(state)), true);
            Tooltip tip = new Tooltip(state + ": esperando datos");
            tip.setShowDelay(Duration.millis(150));
            Tooltip.install(row, tip);
            row.setAccessibleText(state + ": esperando datos");
            legend.getChildren().add(row);
            stateTips.put(state, tip);
            states.put(state, count);
        }
        VBox categoryBox = new VBox(6, label("Por categoría", "card-title"), scale);
        categoryBox.setId("resumen-categorias");
        for (CategoriaProfesional category : CategoriaProfesional.values()) {
            Label name = label(category.toString(), "category-label");
            name.setMinWidth(0);
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            Label count = label("—", "summary-count");
            count.setMinWidth(Region.USE_PREF_SIZE);
            count.setId("resumen-categoria-" + key(category));
            HBox heading = new HBox(8, name, count);
            heading.setAlignment(Pos.CENTER_LEFT);
            StackPane fill = new StackPane();
            Region highlight = new Region();
            highlight.setId("flujo-categoria-" + key(category));
            highlight.getStyleClass().add("category-flow");
            highlight.setManaged(false);
            highlight.setMouseTransparent(true);
            highlight.setVisible(false);
            highlight.resize(40, 7);
            highlight.translateXProperty().bind(fill.widthProperty().add(40).multiply(flowProgress).subtract(40));
            fill.getChildren().add(highlight);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(fill.widthProperty());
            clip.heightProperty().bind(fill.heightProperty());
            clip.setArcWidth(4);
            clip.setArcHeight(4);
            fill.setClip(clip);
            highlights.put(category, highlight);
            fill.setMinWidth(0);
            fill.getStyleClass().add("category-fill");
            fill.setId("barra-categoria-" + key(category));
            DoubleProperty proportion = new SimpleDoubleProperty(0);
            StackPane track = new StackPane(fill);
            track.setAlignment(Pos.CENTER_LEFT);
            track.getStyleClass().add("category-track");
            fill.maxWidthProperty().bind(track.widthProperty().multiply(proportion));
            fill.setVisible(false);
            Tooltip tip = new Tooltip(category.toString() + ": esperando datos");
            VBox row = new VBox(4, heading, track);
            row.getStyleClass().add("summary-category");
            row.pseudoClassStateChanged(PseudoClass.getPseudoClass(key(category)), true);
            row.setAccessibleText(category.toString() + ": esperando datos");
            Tooltip.install(row, tip);
            categoryBox.getChildren().add(row);
            counts.put(category, count);
            proportions.put(category, proportion);
            bars.put(category, fill);
            tips.put(category, tip);
        }

        VBox panel = new VBox(6, totalBox, new Separator(), stateBox, new Separator(), categoryBox);
        panel.getStyleClass().addAll("panel", "summary-panel");
        root.setId("resumen-scroll");
        root.setAccessibleText("Estadísticas de todas las candidaturas; los filtros solo afectan a la tabla.");
        root.setContent(panel);
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setMinWidth(250);
        root.getStyleClass().add("summary-scroll");
        SplitPane.setResizableWithParent(root, false);
        root.sceneProperty().addListener((property, previous, current) -> {
            if (previous != null) previous.windowProperty().removeListener(sceneWindow);
            if (current != null) current.windowProperty().addListener(sceneWindow);
            watchWindow(previous == null ? null : previous.getWindow(), current == null ? null : current.getWindow());
        });
    }

    ScrollPane getRoot() { return root; }

    void show(ResumenCandidaturas summary) {
        total.setText(Long.toString(summary.total()));
        total.setAccessibleText("Total de candidaturas: " + summary.total());
        note.setText(summary.total() == 0 ? "Añade tu primera candidatura." : "");
        note.setVisible(summary.total() == 0);
        note.setManaged(summary.total() == 0);
        var slices = javafx.collections.FXCollections.<PieChart.Data>observableArrayList();
        for (EstadoCandidatura state : EstadoCandidatura.values()) {
            long count = summary.porEstado().get(state);
            states.get(state).setText(Long.toString(count));
            double percentage = summary.total() == 0 ? 0 : 100.0 * count / summary.total();
            String description = state + ": " + count + (count == 1 ? " candidatura" : " candidaturas")
                    + String.format(Locale.forLanguageTag("es-ES"), " · %.1f %%", percentage);
            states.get(state).setAccessibleText(description);
            states.get(state).getParent().setAccessibleText(description);
            stateTips.get(state).setText(description);
            if (count > 0) {
                PieChart.Data slice = new PieChart.Data(state.toString(), count);
                slice.nodeProperty().addListener((property, previous, node) -> {
                    if (node != null) prepareSlice(node, state, description);
                });
                slices.add(slice);
            }
        }
        stateChart.getData().setAll(slices);
        stateChart.setVisible(summary.total() > 0);
        statePlaceholder.setVisible(summary.total() == 0);
        statePlaceholder.setText("Sin\ncandidaturas");
        stateChart.setAccessibleText("Distribución por estado de " + summary.total() + " candidaturas");
        hasLeadingCategory = summary.maxCategoria() > 0;
        scale.setText("Escala común · 0–" + summary.maxCategoria());
        for (CategoriaProfesional category : CategoriaProfesional.values()) {
            long count = summary.porCategoria().get(category);
            counts.get(category).setText(Long.toString(count));
            counts.get(category).setAccessibleText(category + ": " + count);
            // Proporción geométrica para dibujar; los recuentos y el máximo llegan calculados.
            proportions.get(category).set(summary.maxCategoria() == 0 ? 0 : (double) count / summary.maxCategoria());
            bars.get(category).setVisible(count > 0);
            highlights.get(category).setVisible(!disposed && animationsEnabled && count > 0 && count == summary.maxCategoria());
            String description = category + ": " + count + (count == 1 ? " candidatura" : " candidaturas");
            tips.get(category).setText(description);
            bars.get(category).getParent().getParent().setAccessibleText(description);
        }
        updateFlow();
    }

    void failedLoad(boolean hasConfirmedData) {
        note.setText(hasConfirmedData ? "Datos conservados de la última carga." : "No se pudo cargar el resumen.");
        note.setVisible(true);
        note.setManaged(true);
        if (!hasConfirmedData) {
            scale.setText("Sin datos disponibles.");
            statePlaceholder.setText("Sin datos\ndisponibles");
        }
    }

    void close() {
        disposed = true;
        categoryFlow.stop();
        highlights.values().forEach(highlight -> highlight.setVisible(false));
        if (root.getScene() != null) {
            root.getScene().windowProperty().removeListener(sceneWindow);
            if (root.getScene().getWindow() != null) {
                root.getScene().getWindow().showingProperty().removeListener(windowVisibility);
            }
        }
    }

    private void watchWindow(Window previous, Window current) {
        if (previous != null) previous.showingProperty().removeListener(windowVisibility);
        if (current != null && !disposed) current.showingProperty().addListener(windowVisibility);
        updateFlow();
    }

    private void updateFlow() {
        if (disposed || !animationsEnabled || !hasLeadingCategory) {
            categoryFlow.stop();
        } else if (root.getScene() != null && root.getScene().getWindow() != null
                && root.getScene().getWindow().isShowing()) {
            categoryFlow.play();
        } else {
            categoryFlow.pause();
        }
    }

    private static void prepareSlice(Node node, EstadoCandidatura state, String description) {
        node.setId("sector-estado-" + key(state));
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass(key(state)), true);
        node.setAccessibleText(description);
        node.setFocusTraversable(true);
        Tooltip tip = new Tooltip(description);
        tip.setShowDelay(Duration.millis(150));
        Tooltip.install(node, tip);
        node.focusedProperty().addListener((property, previous, focused) -> {
            if (focused && node.getScene() != null && node.getScene().getWindow().isShowing()) {
                var bounds = node.localToScreen(node.getBoundsInLocal());
                if (bounds != null) tip.show(node, bounds.getMinX(), bounds.getMaxY() + 6);
            } else {
                tip.hide();
            }
        });
    }

    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static Label label(String text, String cssClass) {
        Label label = new Label(text);
        label.getStyleClass().add(cssClass);
        label.setWrapText(true);
        return label;
    }
}
