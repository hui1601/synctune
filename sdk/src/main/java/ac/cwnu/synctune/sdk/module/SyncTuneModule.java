package ac.cwnu.synctune.sdk.module;

/**
 * 각 모듈(Player, Lyrics 등)이 구현해야 할 추상 클래스입니다.
 * Core 모듈은 @ModuleStart 어노테이션이 붙은 이 클래스의 구현체를 찾아
 * start() 메서드를 호출하여 모듈을 초기화합니다.
 */
public abstract class SyncTuneModule {

    /**
     * 모듈이 초기화될 때 호출됩니다.
     * 이벤트 리스너 등록 등의 초기 설정을 수행합니다.
     */
    public abstract void start();

    /**
     * 모듈이 종료될 때 호출됩니다. (선택적)
     * 리소스 정리 등의 작업을 수행할 수 있습니다.
     */
    public void stop() {
    }

    /**
     * 모듈의 이름을 반환합니다. 로깅이나 디버깅에 사용될 수 있습니다.
     * @return 모듈 이름
     */
    public String getModuleName() {
        return this.getClass().getSimpleName();
    }
}