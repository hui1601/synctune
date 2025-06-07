module ac.cwnu.synctune.ui {
    // JavaFX modules
    requires javafx.controls;

    // SyncTune SDK for EventPublisher, annotations, logging
    requires ac.cwnu.synctune.sdk;

    // Export UI packages
    exports ac.cwnu.synctune.ui;
    exports ac.cwnu.synctune.ui.view;
    exports ac.cwnu.synctune.ui.controller;
}
