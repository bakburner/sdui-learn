package com.nba.sdui.versioning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SchemaVersion} value object.
 */
class SchemaVersionTest {

    @Test
    void parsesValidVersionStrings() {
        SchemaVersion v = SchemaVersion.parse("1.0");
        assertEquals(1, v.getMajor());
        assertEquals(0, v.getMinor());

        SchemaVersion v2 = SchemaVersion.parse("2.3");
        assertEquals(2, v2.getMajor());
        assertEquals(3, v2.getMinor());
    }

    @Test
    void rejectsNullAndBlank() {
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse(null));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse(""));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse("   "));
    }

    @Test
    void rejectsInvalidFormats() {
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse("1"));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse("1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse("abc.def"));
        assertThrows(IllegalArgumentException.class, () -> SchemaVersion.parse("-1.0"));
    }

    @Test
    void comparesCorrectly() {
        SchemaVersion v10 = SchemaVersion.of(1, 0);
        SchemaVersion v11 = SchemaVersion.of(1, 1);
        SchemaVersion v20 = SchemaVersion.of(2, 0);

        assertTrue(v10.isOlderThan(v11));
        assertTrue(v10.isOlderThan(v20));
        assertTrue(v11.isOlderThan(v20));

        assertFalse(v20.isOlderThan(v10));
        assertFalse(v10.isOlderThan(v10));
    }

    @Test
    void supportsChecksVersionCompatibility() {
        SchemaVersion client = SchemaVersion.of(1, 1);

        assertTrue(client.supports(SchemaVersion.of(1, 0)));
        assertTrue(client.supports(SchemaVersion.of(1, 1)));
        assertFalse(client.supports(SchemaVersion.of(1, 2)));
        assertFalse(client.supports(SchemaVersion.of(2, 0)));
    }

    @Test
    void equalsAndHashCode() {
        SchemaVersion a = SchemaVersion.parse("1.0");
        SchemaVersion b = SchemaVersion.of(1, 0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toStringFormat() {
        assertEquals("1.0", SchemaVersion.of(1, 0).toString());
        assertEquals("2.3", SchemaVersion.of(2, 3).toString());
    }
}
