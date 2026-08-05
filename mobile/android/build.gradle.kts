allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

// Workaround: o modulo camera_android_camerax (camera-core 1.5.x) precisa de
// androidx.concurrent:concurrent-futures (CallbackToFutureAdapter) no classpath de compilacao.
subprojects {
    val sub = this
    if (sub.name == "camera_android_camerax") {
        sub.plugins.withId("com.android.library") {
            sub.dependencies.add("implementation", "androidx.concurrent:concurrent-futures:1.2.0")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
