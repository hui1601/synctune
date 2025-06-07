module synctune.player.main {
    requires transitive synctune.sdk.main;
    requires org.slf4j;
    requires java.desktop;  // AudioEngine에서 javax.sound.sampled 사용
    
    exports ac.cwnu.synctune.player;
    exports ac.cwnu.synctune.player.playback;
}