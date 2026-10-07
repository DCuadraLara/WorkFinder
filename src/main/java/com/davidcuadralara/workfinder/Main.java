package com.davidcuadralara.workfinder;

import com.davidcuadralara.workfinder.ui.WorkFinderApplication;
import javafx.application.Application;

/** Entrada para IDE y Maven, independiente de la clase que hereda de Application. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Application.launch(WorkFinderApplication.class, args);
    }
}
