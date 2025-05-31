module synctune.core.main {
    requires transitive synctune.sdk.main;
    requires org.reflections;
    exports ac.cwnu.synctune.core;
    exports ac.cwnu.synctune.core.error;
    exports ac.cwnu.synctune.core.initializer;
    exports ac.cwnu.synctune.core.logging;
}