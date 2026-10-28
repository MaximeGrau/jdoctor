dependencies {
    "api"("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    "api"("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
}

// Runbooks live at the repository root so they are easy to find and edit;
// they ship inside the core jar so every module can read them.
tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("runbooks")) {
        into("runbooks")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
