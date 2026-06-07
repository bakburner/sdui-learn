package com.nba.sdui.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static regression guard: scans all {@code *Composer.java} source files for
 * the literal substring {@code screen/refresh/} and fails if any match.
 *
 * <p>This is a belt-and-suspenders backstop: the
 * {@link ScreenChannelContractTest} catches legacy endpoints in composed
 * payloads at runtime; this test catches them in source code at compile time
 * so a stale literal can never sneak back in through a branch merge.
 */
class ComposerEndpointRegressionGuardTest {

    private static final String FORBIDDEN_LITERAL = "screen/refresh/";

    @Test
    void composerSourcesMustNotContainLegacyRefreshEndpoint() throws IOException {
        Path composersDir = resolveComposersPath();
        List<String> violations = new ArrayList<>();

        try (var paths = Files.walk(composersDir)) {
            paths.filter(p -> p.getFileName().toString().endsWith("Composer.java"))
                    .forEach(p -> {
                        try {
                            String source = Files.readString(p);
                            if (source.contains(FORBIDDEN_LITERAL)) {
                                violations.add(p.getFileName().toString());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        assertTrue(violations.isEmpty(),
                "Composer sources contain the legacy 'screen/refresh/' literal — "
                + "use '/v1/sdui/screen/{id}' instead: " + violations);
    }

    private static Path resolveComposersPath() {
        Path[] candidates = {
                Path.of("src", "main", "java", "com", "nba", "sdui", "domain", "composer"),
                Path.of("server", "src", "main", "java", "com", "nba", "sdui", "domain", "composer"),
        };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not locate composer sources relative to cwd=" + Path.of("").toAbsolutePath());
    }
}
