package ac.cwnu.synctune;

import ac.cwnu.synctune.core.CoreModule;
import ac.cwnu.synctune.sdk.log.LogManager;
import org.slf4j.Logger;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("SyncTune Application starting...");
        CoreModule.bootstrap("ac.cwnu.synctune");
        log.info("Main thread finished. Application will continue running if non-daemon threads exist.");
    }
}