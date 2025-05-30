package ac.cwnu.synctune.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이벤트 리스너 메서드를 식별하기 위한 어노테이션입니다.
 * 이 어노테이션이 붙은 메서드는 Core 모듈에 의해 이벤트 핸들러로 등록됩니다.
 * 메서드는 반드시 하나의 파라미터를 가져야 하며, 이 파라미터의 타입이 수신할 이벤트의 타입이 됩니다.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener {

}