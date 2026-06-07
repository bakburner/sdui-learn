rootProject.name = "sdui-server"

// Include the codegen build so the server can depend on its
// `generateJsonSchema2Pojo` task and consume its generated POJOs as
// regular compile sources. The codegen project lives at ../codegen.
includeBuild("../codegen")

// SAF (`com.nba:service-aggregation-framework`) resolves from GitHub
// Packages (https://github.com/NBA/saf). `mavenLocal()` is kept first
// so a developer iterating on SAF source can shortcircuit via
// `./gradlew publishToMavenLocal` in the sibling SAF repo. Credentials
// are read from `gpr.user`/`gpr.key` (in `~/.gradle/gradle.properties`)
// with a fallback to `GITHUB_ACTOR`/`GITHUB_TOKEN` env vars (CI).
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "GitHubPackages-SAF"
            url = uri("https://maven.pkg.github.com/NBA/saf")
            credentials {
                username = (settings.providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR"))
                password = (settings.providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN"))
            }
            content { includeModule("com.nba", "service-aggregation-framework") }
        }
    }
}

