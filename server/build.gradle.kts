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

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // SAF — orchestration, resilience, caching, correlation, metrics.
    // Resolved from GitHub Packages (https://github.com/NBA/saf); see
    // repository setup in settings.gradle.kts. `mavenLocal()` still wins
    // when a developer publishes a SAF SNAPSHOT locally.
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
// `com.nba.sdui.models.generated.*` POJOs are generated from
// `schema/sdui-schema.json` by the sibling `:codegen` project (run
// via `make codegen`). The output is written directly into
// `server/src/generated/java/` and committed alongside hand-written
// sources, matching the iOS/web/Android pattern: each client commits
// its own generated view of the schema. AGENTS.md §1.2 still bans
// hand-edits to those files; the schema remains the wire contract.
sourceSets {
    main {
        java {
            srcDir("${projectDir}/src/generated/java")
        }
    }
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
