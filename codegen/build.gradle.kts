plugins {
    id("java")
    id("org.jsonschema2pojo") version "1.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    // For @Generated annotation (javax.annotation.processing.Generated)
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

jsonSchema2Pojo {
    // Source schema location - includes wrapper schema for complete type generation
    sourceFiles = files(
        "${projectDir}/../schema/sdui-schema.json",
        "${projectDir}/../schema/sdui-all-types.json"
    )
    
    // Target directory for generated sources
    targetDirectory = layout.buildDirectory.dir("generated-sources/jsonschema2pojo").get().asFile
    
    // Package name for generated classes
    targetPackage = "com.nba.sdui.models.generated"
    
    // Use Jackson annotations
    setAnnotationStyle("jackson2")
    
    // Include Jackson @JsonInclude annotation
    setInclusionLevel("NON_NULL")
    
    // Generate builder pattern methods
    generateBuilders = true
    
    // Use Java 21 features
    targetVersion = "21"
    
    // Include getters/setters
    includeGetters = true
    includeSetters = true
    
    // Include toString
    includeToString = true
    
    // Include additional properties map for extensibility
    includeAdditionalProperties = true
    
    // Use title as class name when available
    useTitleAsClassname = true
    
    // Initialize collections
    initializeCollections = true
    
    // Include Jakarta Bean Validation annotations (JSR-303/380)
    includeJsr303Annotations = true
    useJakartaValidation = true 
    
    // Date/time configuration
    dateTimeType = "java.time.OffsetDateTime"
    dateType = "java.time.LocalDate"
    timeType = "java.time.LocalTime"
}

// Make compileJava depend on schema generation
tasks.compileJava {
    dependsOn("generateJsonSchema2Pojo")
}

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/jsonschema2pojo"))
        }
    }
}