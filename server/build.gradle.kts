plugins {
    java
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.nba.sdui"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // SAF — orchestration, resilience, caching, correlation, metrics.
    // Published to Maven Local from /Users/arobinson/Projects/service-aggregation-framework
    // via `make publish-saf` / `make sync-saf`.
    implementation("com.nba:service-aggregation-framework:1.0.0-SNAPSHOT")

    // Micrometer Prometheus registry — emits /actuator/prometheus.
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Jackson JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Environment variables (.env support)
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Ably for token generation
    implementation("io.ably:ably-java:1.2.33")
    
    // Lombok (optional - for reducing boilerplate)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    // JSON Schema (Draft-07) validator — A3 schema-conformance test asserts
    // every composed screen/section parses cleanly against schema/sdui-schema.json.
    testImplementation("com.networknt:json-schema-validator:1.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ── Generated SDUI models ───────────────────────────────────
// The `:codegen` composite build generates `com.nba.sdui.models.generated.*`
// POJOs from `schema/sdui-schema.json`. We add the generator's output
// directory directly to the server's main source set and make compileJava
// depend on the generator task. AGENTS.md §1.2 makes the schema the wire
// contract and bans hand-edits to generated files; this wiring lets the
// server compile-time-bind to the schema without copying generated sources
// into `src/main/java`.
val generatedModelsDir = file("${rootDir}/../codegen/build/generated-sources/jsonschema2pojo")

sourceSets {
    main {
        java {
            srcDir(generatedModelsDir)
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(gradle.includedBuild("codegen").task(":generateJsonSchema2Pojo"))
}

// Bundle the schema/ token registries into server resources so the runtime
// can load them from the classpath. Rebuild when the schema/ files change.
tasks.named<ProcessResources>("processResources") {
    from("${projectDir}/../schema") {
        include("sdui-schema.json")
        include("color-tokens.json")
        include("style-tokens.json")
        include("icon-tokens.json")
        include("spacing-tokens.json")
        include("corner-radius-tokens.json")
        include("motion-tokens.json")
        include("shadow-tokens.json")
        include("font-tokens.json")
        include("typography-tokens.json")
        into("schema")
    }
}
