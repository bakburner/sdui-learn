.PHONY: dev dev-server dev-web codegen

codegen:
	@echo "=== Running codegen ==="
	@cd codegen && bash generate.sh
	@cd codegen && ./gradlew generateJsonSchema2Pojo
	@echo "=== Codegen complete ==="

dev:
	@echo "Opening Terminal sessions for server and web..."
	@$(MAKE) dev-server
	@$(MAKE) dev-web

dev-server:
	@osascript -e 'tell application "Terminal" to do script "cd \"$(PWD)/server\" && ./gradlew bootRun"' >/dev/null

dev-web:
	@osascript -e 'tell application "Terminal" to do script "cd \"$(PWD)/web\" && npm run dev"' >/dev/null
