pluginManagement {
    repositories {
        // 华为云镜像（优先）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        // 阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 官方仓库（通过代理）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 华为云镜像（优先）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        // 阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 官方仓库（通过代理）
        google()
        mavenCentral()
        // JitPack 仓库（用于 paddleocr4android）
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SpeedCalendar"
include(":app")
 