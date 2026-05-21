target "docker-metadata-action" {}

group "default" {
  targets = ["sdui-prototype"]
}

target "sdui-prototype" {
  inherits   = ["docker-metadata-action"]
  context    = "."
  dockerfile = "server/Dockerfile"
  platforms  = ["linux/amd64"]
}
