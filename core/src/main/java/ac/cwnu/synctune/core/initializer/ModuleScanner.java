package ac.cwnu.synctune.core.initializer;

import ac.cwnu.synctune.sdk.annotation.ModuleStart;
import ac.cwnu.synctune.sdk.log.LogManager;
import ac.cwnu.synctune.sdk.module.SyncTuneModule;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 클래스패스에서 {@link ModuleStart} 어노테이션이 붙은 {@link SyncTuneModule} 구현 클래스를 스캔합니다.
 */
public class ModuleScanner {
    private static final Logger log = LogManager.getLogger(ModuleScanner.class);
    private final String basePackageToScan;

    public ModuleScanner(String basePackageToScan) {
        this.basePackageToScan = (basePackageToScan != null && !basePackageToScan.isEmpty())
                ? basePackageToScan
                : "ac.cwnu.synctune"; // 기본 스캔 패키지
        log.debug("ModuleScanner initialized. Base package for scan: '{}'", this.basePackageToScan);
    }

    /**
     * 지정된 기본 패키지에서 {@link ModuleStart} 어노테이션이 붙은 모듈 클래스들을 찾습니다.
     * CoreModule 자체는 이 스캔 결과에서 제외됩니다.
     *
     * @return 스캔된 {@link SyncTuneModule}의 자식 클래스들의 집합.
     */
    public Set<Class<? extends SyncTuneModule>> scanForModules() {
        log.info("Scanning for modules annotated with @ModuleStart in package: '{}'", basePackageToScan);

        ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(basePackageToScan))
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes.filterResultsBy(s -> true)); // 모든 하위타입 스캔

        Reflections reflections = new Reflections(configBuilder);

        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(ModuleStart.class);
        if (annotatedClasses.isEmpty()) {
            log.info("No classes found annotated with @ModuleStart in package '{}'.", basePackageToScan);
            return new HashSet<>();
        }

        // 실제로 상속여부를 확인하지만 IDE의 경고 발생을 방지하기 위해 @SuppressWarnings 사용
        @SuppressWarnings("unchecked") Set<Class<? extends SyncTuneModule>> moduleClasses = annotatedClasses.stream()
                .filter(SyncTuneModule.class::isAssignableFrom) // SyncTuneModule을 상속하는지 확인
                .map(clazz -> (Class<? extends SyncTuneModule>) clazz) // 캐스팅
                .filter(clazz -> !ac.cwnu.synctune.core.CoreModule.class.isAssignableFrom(clazz)) // CoreModule 자체 제외
                .collect(Collectors.toSet());

        if (moduleClasses.isEmpty()) {
            log.info("Found classes annotated with @ModuleStart, but none extend SyncTuneModule (or they are CoreModule itself).");
        } else {
            log.info("Module scan completed. Found {} SyncTuneModule(s) (excluding CoreModule): {}",
                    moduleClasses.size(),
                    moduleClasses.stream().map(Class::getName).collect(Collectors.joining(", ")));
        }

        annotatedClasses.stream()
                .filter(clazz -> !SyncTuneModule.class.isAssignableFrom(clazz))
                .forEach(clazz -> log.warn("Class {} is annotated with @ModuleStart but does not extend SyncTuneModule. It will be ignored.", clazz.getName()));

        return moduleClasses;
    }
}