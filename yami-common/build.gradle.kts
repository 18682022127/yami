plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.kingdom.yami"
version = "1.0.0-SNAPSHOT"
description = "yami-common"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
