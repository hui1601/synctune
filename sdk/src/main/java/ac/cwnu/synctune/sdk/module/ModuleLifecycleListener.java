package ac.cwnu.synctune.sdk.module;

import ac.cwnu.synctune.sdk.model.ModuleInfo;

/**
 * 모듈의 생명주기 이벤트 (예: 로드, 시작, 중지, 언로드)를 수신하기 위한 리스너 인터페이스입니다.
 * Core 모듈이나 다른 관심 있는 컴포넌트가 이 리스너를 구현하여 모듈 상태 변화를 감지할 수 있습니다.
 */
public interface ModuleLifecycleListener {

    /**
     * 모듈이 로드되기 직전에 호출됩니다.
     *
     * @param moduleInfo 로드될 모듈의 정보
     */
    default void beforeModuleLoad(ModuleInfo moduleInfo) {
    }

    /**
     * 모듈이 성공적으로 로드되고 인스턴스화된 후에 호출됩니다.
     *
     * @param moduleInfo     로드된 모듈의 정보
     * @param moduleInstance 로드된 모듈의 인스턴스
     */
    default void afterModuleLoad(ModuleInfo moduleInfo, SyncTuneModule moduleInstance) {
    }

    /**
     * 모듈의 start() 메소드가 호출되기 직전에 호출됩니다.
     *
     * @param moduleInstance 시작될 모듈의 인스턴스
     */
    default void beforeModuleStart(SyncTuneModule moduleInstance) {
    }

    /**
     * 모듈의 start() 메소드가 성공적으로 완료된 후에 호출됩니다.
     *
     * @param moduleInstance 시작된 모듈의 인스턴스
     */
    default void afterModuleStart(SyncTuneModule moduleInstance) {
    }

    /**
     * 모듈의 stop() 메소드가 호출되기 직전에 호출됩니다.
     *
     * @param moduleInstance 중지될 모듈의 인스턴스
     */
    default void beforeModuleStop(SyncTuneModule moduleInstance) {
    }

    /**
     * 모듈의 stop() 메소드가 성공적으로 완료된 후에 호출됩니다.
     *
     * @param moduleInstance 중지된 모듈의 인스턴스
     */
    default void afterModuleStop(SyncTuneModule moduleInstance) {
    }

    /**
     * 모듈이 언로드되기 직전에 호출됩니다. (예: 플러그인 시스템에서)
     *
     * @param moduleInstance 언로드될 모듈의 인스턴스
     */
    default void beforeModuleUnload(SyncTuneModule moduleInstance) {
    }

    /**
     * 모듈이 성공적으로 언로드된 후에 호출됩니다.
     *
     * @param moduleInfo 언로드된 모듈의 정보
     */
    default void afterModuleUnload(ModuleInfo moduleInfo) {
    }
}