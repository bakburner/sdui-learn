rootProject.name = "sdui-server"

// Include the codegen build so the server can depend on its
// `generateJsonSchema2Pojo` task and consume its generated POJOs as
// regular compile sources. The codegen project lives at ../codegen.
includeBuild("../codegen")

