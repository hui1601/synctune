module synctune.ui.main {
    // JavaFX modules
    requires javafx.controls;

    // SyncTune SDK for EventPublisher, annotations, logging
    requires transitive synctune.sdk.main;

    // Export UI packages
    exports ac.cwnu.synctune.ui;
    exports ac.cwnu.synctune.ui.view;
    exports ac.cwnu.synctune.ui.controller;
}
