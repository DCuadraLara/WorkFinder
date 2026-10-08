package com.davidcuadralara.workfinder.ui;

import java.net.URL;
import java.util.Objects;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.davidcuadralara.workfinder.repository.CandidaturaRepository;
import com.davidcuadralara.workfinder.service.CandidaturaService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Ciclo de vida de JavaFX: conecta la vista, la hoja de estilos y la ventana. */
public final class WorkFinderApplication extends Application {
    private ExecutorService executor;
    private MainView view;

    @Override
    public void start(Stage stage) {
        Path database = Path.of(System.getProperty("workfinder.db.path",
                Path.of(System.getProperty("user.home"), ".workfinder", "workfinder.db").toString()));
        CandidaturaRepository repository = new CandidaturaRepository(database);
        CandidaturaService service = new CandidaturaService(repository);
        executor = Executors.newSingleThreadExecutor(operation -> {
            Thread thread = new Thread(operation, "workfinder-sqlite");
            thread.setDaemon(true);
            return thread;
        });
        view = new MainView(service, executor);
        Scene scene = new Scene(view.getRoot(), 1360, 820);
        URL stylesheet = Objects.requireNonNull(
                WorkFinderApplication.class.getResource("/com/davidcuadralara/workfinder/css/workfinder.css"),
                "No se ha encontrado la hoja de estilos de WorkFinder.");
        scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle("WorkFinder");
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.setOnShown(event -> view.playEntrance());
        stage.setOnHidden(event -> view.finishEntrance());
        stage.setOnCloseRequest(event -> {
            if (!view.requestClose()) {
                event.consume();
            }
        });
        stage.show();
        view.loadCandidates();
    }

    @Override public void stop() {
        if (view != null) view.close();
        if (executor != null) executor.shutdown();
    }

}
