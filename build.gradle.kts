plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.htmlcellviewer"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        datagrip("2024.1")
        bundledPlugins("com.intellij.database")
        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "HTML Cell Viewer"
        version = "1.0.0"

        ideaVersion {
            sinceBuild = "232"
        }
    }
}

tasks.patchPluginXml {
    untilBuild.set(provider { null })
}

tasks.buildSearchableOptions {
    enabled = false
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.encoding = "UTF-8"
}
