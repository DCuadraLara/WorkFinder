package com.davidcuadralara.workfinder.ui;

import com.davidcuadralara.workfinder.model.Candidatura;
import com.davidcuadralara.workfinder.model.EstadoCandidatura;
import com.davidcuadralara.workfinder.model.FiltroCandidaturas;
import java.util.ArrayList;
import java.util.List;
import javafx.css.PseudoClass;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.davidcuadralara.workfinder.repository.PersistenciaException;
import com.davidcuadralara.workfinder.service.CandidaturaService;
import com.davidcuadralara.workfinder.service.ValidacionException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;

/** Vista y eventos. Las reglas y la persistencia se delegan al servicio. */
public final class MainView {

    private final BorderPane root = new BorderPane();
    private final TableView<Candidatura> table = new TableView<>();
    private final Button searchButton = new Button();
    private final Button addButton = new Button("Añadir candidatura");
    private final Button editButton = new Button("Editar");
    private final Button deleteButton = new Button("Eliminar");
    private final Button reloadButton = new Button("Recargar");
    private final StackPane contentHost = new StackPane();
    private final Label formTitle = label("", "section-title");
    private final Label message = label("", "operation-message");
    private final Label status = label("Abriendo WorkFinder…", "status-text");
    private final Label emptyTitle = label("Cargando candidaturas…", "empty-title");
    private final Label emptyExplanation = label("Preparando la base de datos local.", "muted-text");
    private final CandidaturaService service;
    private final ExecutorService executor;
    private VBox actions;
    private StackPane searchCorner;
    private CandidaturaForm form;
    private CandidaturaSearch search;
    private final CandidaturaSummary summaryPanel = new CandidaturaSummary();
    private final List<Candidatura> allCandidates = new ArrayList<>();
    private FiltroCandidaturas activeFilter = FiltroCandidaturas.todos();
    private final Label results = label("", "pending-text");
    private boolean searchWasVisible;
    private boolean busy;
    private boolean ready;
    private boolean closed;
    private final SVGPath brandMark = new SVGPath();
    private final PanelEntrance entrance;

    public MainView(CandidaturaService service, ExecutorService executor) {
        this.service = Objects.requireNonNull(service);
        this.executor = Objects.requireNonNull(executor);
        root.getStyleClass().add("workfinder-root");
        HBox header = createHeader();
        root.setTop(header);

        VBox candidates = createCandidatesPanel();
        ScrollPane summary = summaryPanel.getRoot();
        SplitPane workspace = new SplitPane(candidates, summary);
        workspace.setOrientation(Orientation.HORIZONTAL);
        workspace.setDividerPositions(0.78);
        workspace.getStyleClass().add("workspace");
        root.setCenter(workspace);

        status.setWrapText(true);
        status.setId("status-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(16, spacer, status);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("footer");
        root.setBottom(footer);
        entrance = new PanelEntrance(header, candidates, summary, footer, brandMark);
        entrance.prepare();
        table.getSelectionModel().selectedItemProperty().addListener((observable, before, after) -> updateActions());
        addButton.setOnAction(event -> showForm(null));
        editButton.setOnAction(event -> showForm(table.getSelectionModel().getSelectedItem()));
        deleteButton.setOnAction(event -> confirmDelete());
        reloadButton.setOnAction(event -> loadCandidates());
        searchButton.setOnAction(event -> showSearch(!search.getRoot().isVisible()));
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F && ready && !busy && form == null) {
                showSearch(true);
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.N && ready && !busy && form == null) {
                showForm(null);
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.S && form != null && !busy) {
                saveForm();
                event.consume();
            } else if (event.getCode() == KeyCode.F5 && !busy && form == null) {
                loadCandidates();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && search.getRoot().isVisible() && form == null) {
                showSearch(false);
                event.consume();
            }
        });
        updateActions();
    }

    public Parent getRoot() {
        return root;
    }

    public void playEntrance() {
        entrance.play();
    }

    public void finishEntrance() {
        entrance.finish();
    }

    public boolean isBusy() { return busy; }

    public void closingWhileBusy() {
        showMessage("Espera a que termine la operación antes de cerrar la ventana.", false);
    }

    /** Evita perder un formulario abierto o interrumpir una operación de guardado. */
    public boolean requestClose() {
        if (busy) {
            closingWhileBusy();
            return false;
        }
        if (form == null) return true;
        ButtonType discard = new ButtonType("Cerrar sin guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType back = new ButtonType("Volver al formulario", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                "Hay un formulario abierto. Si cierras WorkFinder, se perderán los cambios sin guardar.", discard, back);
        dialog.setTitle("Cerrar WorkFinder");
        dialog.setHeaderText("¿Cerrar sin guardar?");
        dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getStyleClass().add("workfinder-root");
        dialog.getDialogPane().getStylesheets().addAll(root.getScene().getStylesheets());
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().lookupButton(discard).getStyleClass().add("danger-button");
        ((Button) dialog.getDialogPane().lookupButton(discard)).setDefaultButton(false);
        ((Button) dialog.getDialogPane().lookupButton(back)).setDefaultButton(true);
        return dialog.showAndWait().orElse(back) == discard;
    }

    public void close() {
        closed = true;
        entrance.finish();
        search.stop();
        summaryPanel.close();
    }

    public void loadCandidates() {
        clearMessage();
        execute(service::inicializarYListar, loaded -> {
            ready = true;
            allCandidates.clear();
            allCandidates.addAll(loaded);
            refreshCandidates(null);
            status.setText("Lista actualizada.");
            updateActions();
        }, error -> {
            summaryPanel.failedLoad(ready);
            if (!ready) {
                emptyTitle.setText("No se pudieron cargar las candidaturas.");
                emptyExplanation.setText("Consulta el mensaje y pulsa Recargar para intentarlo de nuevo.");
            }
            showFailure(error);
        }, "Cargando candidaturas…");
    }

    private HBox createHeader() {
        brandMark.setContent("M2 27 L23 6 M9 6 H23 V20 M2 6 V27 H23");
        brandMark.getStyleClass().add("brand-mark");
        VBox title = new VBox(2,
                label("WorkFinder", "app-title"),
                label("Cuaderno de candidaturas", "brand-subtitle"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label caption = label("Prácticas y empleo", "header-caption");
        HBox header = new HBox(16, brandMark, title, spacer, caption);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    private VBox createCandidatesPanel() {
        formTitle.setVisible(false);
        formTitle.setManaged(false);

        configureTable();
        contentHost.getChildren().add(table);
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        search = new CandidaturaSearch(filter -> {
            if (!ready || closed) return;
            activeFilter = filter;
            refreshCandidates(null);
        }, () -> showSearch(false));
        search.getRoot().setVisible(false);
        search.getRoot().setManaged(false);
        actions = createActions();
        message.setId("operation-message");
        message.setWrapText(true);
        message.setVisible(false);
        message.setManaged(false);
        VBox panel = new VBox(12, formTitle, message, contentHost, search.getRoot(), actions);
        panel.setMinWidth(660);
        panel.getStyleClass().addAll("panel", "candidates-panel");
        return panel;
    }

    private StackPane createSearchButton() {
        SVGPath magnifier = new SVGPath();
        magnifier.setContent("M8 1 A7 7 0 1 0 8 15 A7 7 0 1 0 8 1 M13 13 L19 19");
        magnifier.getStyleClass().add("search-icon");
        searchButton.setId("buscar-empresa-button");
        searchButton.setGraphic(magnifier);
        searchButton.setAccessibleText("Buscar por empresa");
        searchButton.setAccessibleHelp("Busca por empresa y combina filtros de estado, categoría y modalidad. Ctrl+F abre el panel.");
        searchButton.getStyleClass().add("search-button");
        searchButton.setDisable(true);

        // El contenedor permite mostrar la ayuda aunque el botón esté desactivado.
        StackPane corner = new StackPane(searchButton);
        Tooltip.install(corner, new Tooltip("Búsqueda por empresa y filtros · Ctrl+F"));
        return corner;
    }

    private void configureTable() {
        table.setId("candidaturas-table");
        table.setAccessibleText("Tabla de candidaturas. Cambia el estado con su selector o selecciona una fila para editar o eliminar.");
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(column("Empresa", 165, Candidatura::getEmpresa));
        table.getColumns().add(column("Puesto", 190, Candidatura::getPuesto));
        table.getColumns().add(column("Categoría", 150, c -> c.getCategoria().toString()));
        TableColumn<Candidatura, EstadoCandidatura> stateColumn = new TableColumn<>("Estado");
        stateColumn.setPrefWidth(150);
        stateColumn.setMinWidth(130);
        stateColumn.setSortable(false);
        stateColumn.setReorderable(false);
        stateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getEstado()));
        stateColumn.setCellFactory(ignored -> new EstadoTableCell(this::changeState));
        table.getColumns().add(stateColumn);
        table.getColumns().add(column("Modalidad", 125, c -> c.getModalidad().toString()));
        table.getColumns().add(column("Ubicación", 150, Candidatura::getUbicacion));
        table.getColumns().add(column("Fecha envío", 135,
                c -> c.getFechaEnvio().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))));

        emptyExplanation.setWrapText(true);
        emptyTitle.setWrapText(true);
        VBox emptyMessage = new VBox(8, emptyTitle, emptyExplanation);
        HBox content = new HBox(20, createEmptyIllustration(), emptyMessage);
        content.setAlignment(Pos.CENTER_LEFT);
        BorderPane emptyState = new BorderPane();
        emptyState.setTop(content);
        emptyState.getStyleClass().add("empty-state");
        table.setPlaceholder(emptyState);
    }

    /** Dibujo vectorial propio: una hoja abierta, sin representar datos de una candidatura. */
    private Pane createEmptyIllustration() {
        SVGPath sheet = new SVGPath();
        sheet.setContent("M20 3 H53 L65 15 V53 H20 Z M53 3 V15 H65 M29 25 H55 M29 33 H49 M29 41 H43");
        sheet.getStyleClass().add("notebook-sheet");
        SVGPath corner = new SVGPath();
        corner.setContent("M9 29 V61 H44 M59 43 L74 28 M64 28 H74 V38");
        corner.getStyleClass().add("notebook-accent");
        Pane illustration = new Pane(sheet, corner);
        illustration.setMinSize(84, 66);
        illustration.setPrefSize(84, 66);
        illustration.setMaxSize(84, 66);
        illustration.setMouseTransparent(true);
        return illustration;
    }

    private TableColumn<Candidatura, String> column(String title, double width, Function<Candidatura, String> value) {
        TableColumn<Candidatura, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setMinWidth(80);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTooltip(empty || item == null || item.isBlank() ? null : new Tooltip(item));
            }
        });
        return column;
    }

    private VBox createActions() {
        addButton.setText("+  Añadir candidatura");
        addButton.getStyleClass().add("primary-button");
        deleteButton.getStyleClass().add("danger-button");
        addButton.setId("anadir-candidatura-button");
        editButton.setId("editar-candidatura-button");
        deleteButton.setId("eliminar-candidatura-button");
        reloadButton.setId("recargar-button");
        reloadButton.setTooltip(new Tooltip("Actualizar la lista · F5."));
        addButton.setTooltip(new Tooltip("Añadir una candidatura · Ctrl+N."));
        editButton.setTooltip(new Tooltip("Consultar o editar la candidatura seleccionada."));
        deleteButton.setTooltip(new Tooltip("Eliminar la candidatura seleccionada con confirmación."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        searchCorner = createSearchButton();
        HBox buttons = new HBox(10, addButton, editButton, deleteButton, spacer, searchCorner, reloadButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        results.setId("resultados-text");
        results.setText("Abriendo la lista…");
        results.setWrapText(true);
        VBox actions = new VBox(10, buttons, results);
        actions.getStyleClass().add("actions");
        return actions;
    }

    private void updateActions() {
        boolean unavailable = busy || !ready || form != null;
        boolean selected = table.getSelectionModel().getSelectedItem() != null;
        addButton.setDisable(unavailable);
        editButton.setDisable(unavailable || !selected);
        deleteButton.setDisable(unavailable || !selected);
        reloadButton.setDisable(busy || form != null);
        table.setDisable(busy);
        searchButton.setDisable(unavailable);
        search.setBusy(busy || !ready || form != null);
        if (form != null) form.setBusy(busy);
    }

    private void showForm(Candidatura source) {
        if (busy || !ready || form != null) return;
        entrance.finish();
        clearMessage();
        form = new CandidaturaForm(source, this::saveForm, () -> {
            closeForm();
            clearMessage();
            status.setText("Formulario cerrado sin guardar cambios.");
        });
        searchWasVisible = search.getRoot().isVisible();
        search.getRoot().setVisible(false);
        search.getRoot().setManaged(false);
        formTitle.setText(source == null ? "Nueva candidatura" : "Editar candidatura");
        formTitle.setVisible(true);
        formTitle.setManaged(true);
        contentHost.getChildren().setAll(form.getRoot());
        actions.setVisible(false);
        actions.setManaged(false);
        updateActions();
        Platform.runLater(() -> {
            if (form != null) {
                form.getRoot().applyCss();
                form.focusFirst();
            }
        });
    }

    private void closeForm() {
        form = null;
        formTitle.setText("");
        formTitle.setVisible(false);
        formTitle.setManaged(false);
        contentHost.getChildren().setAll(table);
        actions.setVisible(true);
        actions.setManaged(true);
        search.getRoot().setVisible(searchWasVisible);
        search.getRoot().setManaged(searchWasVisible);
        updateActions();
    }

    private void saveForm() {
        if (form == null || busy) return;
        clearMessage();
        form.clearErrors();
        Candidatura input;
        try {
            input = form.read(service.interpretarFecha(form.getFechaText()));
        } catch (ValidacionException error) {
            form.showErrors(error.getErrores());
            showMessage(error.getMessage(), true);
            status.setText("Formulario sin guardar. Revisa los campos indicados.");
            return;
        }
        execute(() -> input.getId() == 0 ? service.anadir(input) : service.editar(input), saved -> {
            allCandidates.removeIf(c -> c.getId() == saved.getId());
            allCandidates.add(saved);
            // Es orden de presentación; no contiene reglas del dominio ni consultas.
            allCandidates.sort(Comparator.comparing(Candidatura::getFechaEnvio).reversed()
                    .thenComparing(Comparator.comparingLong(Candidatura::getId).reversed()));
            closeForm();
            refreshCandidates(saved.getId());
            status.setText(input.getId() == 0 ? "Candidatura guardada." : "Cambios guardados.");
            if (table.getItems().stream().noneMatch(c -> c.getId() == saved.getId())) {
                showMessage("Guardada correctamente. Esta candidatura no coincide con la búsqueda o los filtros activos; pulsa la lupa y Limpiar para verla.", false);
            }
        }, error -> {
            if (error instanceof ValidacionException validation) {
                form.showErrors(validation.getErrores());
                showMessage(validation.getMessage(), true);
                status.setText("Formulario sin guardar. Revisa los campos indicados.");
            } else showFailure(error);
        }, "Guardando candidatura…");
    }

    private void showSearch(boolean visible) {
        if (!ready || busy || form != null || closed) return;
        entrance.finish();
        search.getRoot().setVisible(visible);
        search.getRoot().setManaged(visible);
        searchButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("expanded"), visible);
        if (visible) Platform.runLater(search::focus);
    }

    private void refreshCandidates(Long preferredId) {
        Candidatura selected = table.getSelectionModel().getSelectedItem();
        Long selectionId = preferredId != null ? preferredId : selected == null ? null : selected.getId();
        table.getItems().setAll(service.filtrar(allCandidates, activeFilter));
        table.getSelectionModel().clearSelection();
        if (selectionId != null) {
            for (Candidatura candidate : table.getItems()) {
                if (candidate.getId() == selectionId) {
                    table.getSelectionModel().select(candidate);
                    if (preferredId != null) table.scrollTo(candidate);
                    break;
                }
            }
        }
        boolean filtered = activeFilter.activo();
        results.setText(filtered ? table.getItems().size() + " de " + allCandidates.size() + " · Búsqueda y filtros activos"
                : allCandidates.size() + (allCandidates.size() == 1 ? " candidatura" : " candidaturas") + " · Lista completa");
        summaryPanel.show(service.calcularResumen(allCandidates));
        searchButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("filters-active"), filtered);
        if (allCandidates.isEmpty()) {
            emptyTitle.setText("Sin candidaturas todavía.");
            emptyExplanation.setText("Añade tu primera candidatura con el botón de abajo.");
        } else {
            emptyTitle.setText("Sin coincidencias.");
            emptyExplanation.setText("Prueba otra empresa o pulsa Limpiar en el panel de búsqueda y filtros.");
        }
        updateActions();
    }

    private void confirmDelete() {
        Candidatura selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || busy || form != null || !ready) return;
        ButtonType confirm = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar «" + selected.getPuesto() + "» en «" + selected.getEmpresa()
                        + "»?\n\nEsta acción no puede deshacerse.", confirm, ButtonType.CANCEL);
        dialog.setTitle("Eliminar candidatura");
        dialog.setHeaderText("Confirma la eliminación");
        dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getStyleClass().add("workfinder-root");
        dialog.getDialogPane().getStylesheets().addAll(root.getScene().getStylesheets());
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().lookupButton(confirm).getStyleClass().add("danger-button");
        ((Button) dialog.getDialogPane().lookupButton(confirm)).setDefaultButton(false);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != confirm) return;
        clearMessage();
        execute(() -> { service.eliminar(selected.getId()); return selected.getId(); }, id -> {
            allCandidates.removeIf(c -> c.getId() == id);
            refreshCandidates(null);
            status.setText("Candidatura eliminada.");
        }, this::showFailure, "Eliminando candidatura…");
    }

    private void changeState(Candidatura original, EstadoCandidatura proposed) {
        if (busy || closed || !ready || form != null || proposed == null) {
            table.refresh();
            return;
        }
        if (original.getEstado() == proposed) return;
        clearMessage();
        execute(() -> service.cambiarEstado(original, proposed), saved -> {
            for (int index = 0; index < allCandidates.size(); index++) {
                if (allCandidates.get(index).getId() == saved.getId()) {
                    allCandidates.set(index, saved);
                    break;
                }
            }
            refreshCandidates(null);
            status.setText("Estado guardado: " + saved.getEstado() + " · " + saved.getEmpresa() + ".");
        }, error -> {
            table.refresh();
            showFailure(error);
            status.setText("Estado sin guardar. Se ha restaurado el valor anterior en la tabla.");
        }, "Guardando estado…");
    }

    private <T> void execute(Callable<T> operation, Consumer<T> onSuccess, Consumer<Throwable> onFailure, String text) {
        if (busy || closed) return;
        busy = true;
        status.setText(text);
        updateActions();
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return operation.call(); }
        };
        task.setOnSucceeded(event -> {
            busy = false;
            if (closed) return;
            updateActions();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busy = false;
            if (closed) return;
            updateActions();
            onFailure.accept(task.getException());
        });
        executor.execute(task);
    }

    private void showFailure(Throwable error) {
        String text = error instanceof PersistenciaException ? error.getMessage()
                : "No se ha podido completar la operación. Tus datos en pantalla se conservan; vuelve a intentarlo.";
        showMessage(text, true);
        status.setText("La operación no se ha completado.");
        System.getLogger(MainView.class.getName()).log(System.Logger.Level.ERROR, "Error al acceder a las candidaturas", error);
    }

    private void showMessage(String text, boolean error) {
        message.setText(text);
        message.getStyleClass().remove("error-message");
        if (error) message.getStyleClass().add("error-message");
        message.setVisible(true);
        message.setManaged(true);
    }

    private void clearMessage() {
        message.setText("");
        message.setVisible(false);
        message.setManaged(false);
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
