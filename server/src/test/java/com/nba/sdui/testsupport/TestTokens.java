package com.nba.sdui.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.sdui.domain.tokens.TokenRegistry;
import com.nba.sdui.domain.tokens.Tokens;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Shared {@link Tokens} instance for unit tests that wire composers
 * and shared infrastructure by hand. Loads the same bundled JSON
 * registries the runtime bean uses, invoking the {@code @PostConstruct}
 * loader directly since these tests skip the Spring lifecycle.
 */
public final class TestTokens {

    public static final Tokens INSTANCE = build();

    private TestTokens() {}

    private static Tokens build() {
        ObjectMapper om = new ObjectMapper();
        TokenRegistry registry = new TokenRegistry(om);
        ReflectionTestUtils.invokeMethod(registry, "load");
        return new Tokens(om, registry);
    }
}
