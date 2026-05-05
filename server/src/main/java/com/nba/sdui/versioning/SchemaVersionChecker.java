package com.nba.sdui.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Checks whether a client's declared schema version is still supported,
 * and produces the upgrade-required response when it is not.
 *
 * <p>When {@code clientVersion < minSupportedVersion}, the server returns an
 * {@code X-Schema-Version-Mismatch: upgrade-required} header and an ErrorState
 * section prompting the user to update. Per AGENTS.md §8.0, error states are
 * first-class server-composed sections.
 */
@Component
public class SchemaVersionChecker {

    private static final Logger log = LoggerFactory.getLogger(SchemaVersionChecker.class);

    public static final String MISMATCH_HEADER = "X-Schema-Version-Mismatch";
    public static final String UPGRADE_REQUIRED = "upgrade-required";

    private final SchemaVersionConfig config;
    private final ObjectMapper objectMapper;

    public SchemaVersionChecker(SchemaVersionConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if the client version is below the minimum supported version.
     *
     * @return true if the client must upgrade
     */
    public boolean isUpgradeRequired(String clientVersionStr) {
        try {
            SchemaVersion clientVersion = SchemaVersion.parse(clientVersionStr);
            return clientVersion.isOlderThan(config.minSupportedVersion());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid client schema version '{}', treating as upgrade-required", clientVersionStr);
            return true;
        }
    }

    /**
     * Compose an ErrorState response for clients that are too far behind.
     * This is a server-composed AtomicComposite (per §8.0 and §6).
     */
    public JsonNode composeUpgradeRequiredResponse(String clientVersionStr, String traceId) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "upgrade-required");
        response.put("traceId", traceId != null ? traceId : "");
        response.put("schemaVersion", config.getCurrentVersion());

        ArrayNode sections = objectMapper.createArrayNode();

        // ErrorState section — server-composed, no client invention
        ObjectNode errorSection = objectMapper.createObjectNode();
        errorSection.put("id", "error-schema-version-mismatch");
        errorSection.put("type", "AtomicComposite");
        errorSection.put("analyticsId", "error_upgrade_required");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("type", "Box");

        ObjectNode style = objectMapper.createObjectNode();
        style.put("padding", "token:nba.spacing.lg");
        style.put("alignItems", "center");
        style.put("justifyContent", "center");
        data.set("style", style);

        ArrayNode children = objectMapper.createArrayNode();

        // Title
        ObjectNode title = objectMapper.createObjectNode();
        title.put("type", "Text");
        ObjectNode titleProps = objectMapper.createObjectNode();
        titleProps.put("content", "Update Required");
        titleProps.put("typography", "title2");
        titleProps.put("align", "center");
        title.set("props", titleProps);
        children.add(title);

        // Body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "Text");
        ObjectNode bodyProps = objectMapper.createObjectNode();
        bodyProps.put("content", "This version of the app is no longer supported. Please update to continue.");
        bodyProps.put("typography", "body");
        bodyProps.put("align", "center");
        body.set("props", bodyProps);
        children.add(body);

        // Version info (diagnostic)
        ObjectNode versionInfo = objectMapper.createObjectNode();
        versionInfo.put("type", "Text");
        ObjectNode versionProps = objectMapper.createObjectNode();
        versionProps.put("content", "Your version: " + clientVersionStr + " • Required: " + config.getMinSupportedVersion() + "+");
        versionProps.put("typography", "caption");
        versionProps.put("align", "center");
        versionInfo.set("props", versionProps);
        children.add(versionInfo);

        data.set("children", children);
        errorSection.set("data", data);
        sections.add(errorSection);

        response.set("sections", sections);
        return response;
    }
}
