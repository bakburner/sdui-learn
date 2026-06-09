package com.nba.sdui.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates cross-element refreshPolicy invariants that Draft-07 cannot
 * conveniently express.
 */
public final class RefreshPolicyInvariantValidator {

    private RefreshPolicyInvariantValidator() {}

    public static List<String> validateSection(JsonNode sectionNode) {
        List<String> violations = new ArrayList<>();
        JsonNode refreshPolicies = sectionNode.path("refreshPolicy");
        if (refreshPolicies.isMissingNode() || refreshPolicies.isNull()) {
            return violations;
        }
        if (!refreshPolicies.isArray()) {
            violations.add("refreshPolicy must be an array");
            return violations;
        }

        int opaqueCount = 0;
        int sectionEndpointCount = 0;
        int staticCount = 0;
        for (int i = 0; i < refreshPolicies.size(); i++) {
            JsonNode policy = refreshPolicies.get(i);
            if (!policy.isObject()) {
                violations.add("refreshPolicy[" + i + "] must be an object");
                continue;
            }

            boolean hasOpaqueSource = hasNonBlankText(policy, "channel")
                    || hasNonBlankText(policy, "url");
            if (hasOpaqueSource) {
                opaqueCount++;
            }
            if (hasNonBlankText(policy, "sectionEndpoint")) {
                sectionEndpointCount++;
            }
            if ("static".equals(policy.path("type").asText())) {
                staticCount++;
            }
        }

        if (opaqueCount > 1) {
            violations.add("refreshPolicy has more than one opaque element (channel/url)");
        }
        if (sectionEndpointCount > 1) {
            violations.add("refreshPolicy has more than one sectionEndpoint element");
        }
        if (staticCount > 0 && refreshPolicies.size() > 1) {
            violations.add("refreshPolicy static element must be solo");
        }

        return violations;
    }

    private static boolean hasNonBlankText(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return false;
        }
        return !node.path(field).asText("").isBlank();
    }
}
