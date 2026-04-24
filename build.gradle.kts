plugins {
    id("java")
    id("org.springframework.boot") version "4.0.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

group = "com.kingdom.yami"
version = "1.0.0-SNAPSHOT"
description = "yami-parent"

val springBootVersion = "4.0.5"
val springCloudVersion = "2025.1.1"
val springCloudAlibabaVersion = "2025.1.0.0"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    // 修复点：在 Kotlin DSL 的 subprojects 中，必须显式指定类型才能调用插件扩展
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
            mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:$springCloudAlibabaVersion")
        }
    }

    dependencies {
        add("testImplementation", "org.assertj:assertj-core")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}

configure(listOf(project(":yami-backend"), project(":yami-gateway"))) {
    apply(plugin = "org.springframework.boot")
    dependencies {
        add("implementation", "org.springframework.cloud:spring-cloud-starter")
        add("implementation", "org.springframework.boot:spring-boot-starter-logging")
        add("compileOnly", "org.projectlombok:lombok")
        add("annotationProcessor", "org.projectlombok:lombok")
        add("implementation", "org.springframework.cloud:spring-cloud-starter-loadbalancer")

        add("implementation", "org.springframework.boot:spring-boot-starter-data-jdbc")
        add("implementation", "com.mysql:mysql-connector-j")
        add("implementation", "org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1")

        add("implementation", "org.springframework.boot:spring-boot-starter-web")
        add("implementation", "org.springframework.cloud:spring-cloud-starter-openfeign")
        add("implementation", "com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")
        add("implementation", "com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config")
        add("implementation", "org.springframework.boot:spring-boot-starter-data-redis")
        add("implementation", "com.fasterxml.jackson.core:jackson-databind")

        add("implementation", "org.aspectj:aspectjweaver:1.9.24")
        add("implementation", "io.jsonwebtoken:jjwt-api:0.12.6")
        add("runtimeOnly", "io.jsonwebtoken:jjwt-impl:0.12.6")
        add("runtimeOnly", "io.jsonwebtoken:jjwt-jackson:0.12.6")

        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-webmvc-test")

        add("implementation", "cn.hutool:hutool-all:5.8.44")
    }

    tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
        enabled = true
    }

    tasks.named<Jar>("jar") {
        enabled = false
    }
}

project(":yami-gateway") {
    dependencies {
        "implementation"("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    }
}


configure(listOf(project(":yami-common"))) {
    tasks.named<Jar>("jar") {
        enabled = true
    }
}