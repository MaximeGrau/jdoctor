plugins {
    application
}

dependencies {
    "implementation"(project(":jdoctor-collectors"))
    "implementation"(project(":jdoctor-analyzers"))
    "implementation"(project(":jdoctor-llm"))
    "implementation"("info.picocli:picocli:4.7.7")
    "annotationProcessor"("info.picocli:picocli-codegen:4.7.7")
}

application {
    mainClass.set("dev.jdoctor.cli.JDoctor")
    applicationName = "jdoctor"
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
