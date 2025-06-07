// player/src/main/java/module-info.java

module synctune.player.main {
    // SDK 의존성
    requires synctune.sdk.main;
    
    // Java Desktop API (javax.sound.sampled 사용을 위해 필요)
    requires java.desktop;
    
    // 로깅
    requires org.slf4j;
    
    // 패키지 내보내기 (다른 모듈에서 접근해야 하는 경우)
    exports ac.cwnu.synctune.player;
}