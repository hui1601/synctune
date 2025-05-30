package ac.cwnu.synctune.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SyncTuneModule의 구현체를 식별하기 위한 어노테이션입니다.
 * Core 모듈은 이 어노테이션이 붙은 클래스를 찾아 모듈을 초기화합니다.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleStart {
}