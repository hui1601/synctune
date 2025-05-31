package ac.cwnu.synctune.sdk.model;

import java.util.Objects;

/**
 * 스캔된 모듈의 정보를 담는 DTO입니다.
 * 모듈 이름, 버전, 클래스 정보 등을 포함합니다.
 */
public final class ModuleInfo {
    private final String name;
    private final String version;
    private final Class<?> moduleClass; // 모듈의 메인 클래스

    public ModuleInfo(String name, String version, Class<?> moduleClass) {
        this.name = Objects.requireNonNull(name, "Module name cannot be null");
        this.version = Objects.requireNonNull(version, "Module version cannot be null");
        this.moduleClass = Objects.requireNonNull(moduleClass, "Module class cannot be null");
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Class<?> getModuleClass() {
        return moduleClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleInfo that = (ModuleInfo) o;
        return name.equals(that.name) &&
                version.equals(that.version) &&
                moduleClass.equals(that.moduleClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, moduleClass);
    }

    @Override
    public String toString() {
        return "ModuleInfo{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", moduleClass=" + moduleClass.getName() +
                '}';
    }
}