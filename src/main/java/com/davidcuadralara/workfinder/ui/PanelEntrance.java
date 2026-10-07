package com.davidcuadralara.workfinder.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/** Entrada de una sola vez: trazo de la marca y apertura lateral del cuaderno. */
final class PanelEntrance {

    private static final Interpolator EASE = Interpolator.SPLINE(0.22, 1, 0.36, 1);

    private final Region header;
    private final Region candidates;
    private final Region summary;
    private final Region footer;
    private final SVGPath mark;
    private final Rectangle revealClip = new Rectangle();
    private final DoubleProperty reveal = new SimpleDoubleProperty(0);
    private final Timeline animation = new Timeline();
    private final boolean enabled = Boolean.parseBoolean(System.getProperty("workfinder.animations", "true"));
    private final EventHandler<KeyEvent> skipHandler = event -> {
        if (event.getCode() == KeyCode.ESCAPE) {
            finish();
            event.consume();
        }
    };
    private Scene scene;
    private boolean finished;
    private boolean started;

    PanelEntrance(Region header, Region candidates, Region summary, Region footer, SVGPath mark) {
        this.header = header;
        this.candidates = candidates;
        this.summary = summary;
        this.footer = footer;
        this.mark = mark;
    }

    void prepare() {
        if (!enabled) {
            finish();
            return;
        }
        // Se preparan los nodos antes de mostrar la ventana para evitar un destello inicial.
        header.setOpacity(0);
        header.setTranslateY(5);
        candidates.setOpacity(0);
        candidates.setTranslateX(-8);
        summary.setOpacity(0);
        summary.setTranslateX(10);
        footer.setOpacity(0);
        mark.getStrokeDashArray().setAll(120.0, 120.0);
        mark.setStrokeDashOffset(120);

        // El recorte sigue el tamaño real del panel, incluso si se redimensiona al entrar.
        revealClip.widthProperty().bind(candidates.widthProperty().multiply(reveal));
        revealClip.heightProperty().bind(candidates.heightProperty());
        candidates.setClip(revealClip);

        animation.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(header.opacityProperty(), 0),
                        new KeyValue(header.translateYProperty(), 5),
                        new KeyValue(mark.strokeDashOffsetProperty(), 120),
                        new KeyValue(candidates.opacityProperty(), 0),
                        new KeyValue(candidates.translateXProperty(), -8),
                        new KeyValue(reveal, 0),
                        new KeyValue(summary.opacityProperty(), 0),
                        new KeyValue(summary.translateXProperty(), 10),
                        new KeyValue(footer.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(header.opacityProperty(), 1, EASE),
                        new KeyValue(header.translateYProperty(), 0, EASE),
                        new KeyValue(reveal, 0),
                        new KeyValue(candidates.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(380),
                        new KeyValue(mark.strokeDashOffsetProperty(), 0, EASE),
                        new KeyValue(candidates.opacityProperty(), 1, EASE)),
                new KeyFrame(Duration.millis(480),
                        new KeyValue(summary.opacityProperty(), 0),
                        new KeyValue(summary.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(780),
                        new KeyValue(reveal, 1, EASE),
                        new KeyValue(candidates.translateXProperty(), 0, EASE),
                        new KeyValue(footer.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(940),
                        new KeyValue(summary.opacityProperty(), 1, EASE),
                        new KeyValue(summary.translateXProperty(), 0, EASE),
                        new KeyValue(footer.opacityProperty(), 1, EASE)));
        animation.setOnFinished(event -> finish());
    }

    void play() {
        if (finished || started) {
            return;
        }
        started = true;
        scene = header.getScene();
        scene.addEventFilter(KeyEvent.KEY_PRESSED, skipHandler);
        animation.playFromStart();
    }

    /** Restaura siempre un panel completo, también al omitir la entrada o cerrar pronto. */
    void finish() {
        animation.stop();
        for (Region region : new Region[]{header, candidates, summary, footer}) {
            region.setOpacity(1);
            region.setTranslateX(0);
            region.setTranslateY(0);
        }
        candidates.setClip(null);
        revealClip.widthProperty().unbind();
        revealClip.heightProperty().unbind();
        mark.getStrokeDashArray().clear();
        mark.setStrokeDashOffset(0);
        if (scene != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, skipHandler);
            scene = null;
        }
        finished = true;
    }
}
