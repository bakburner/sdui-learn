package com.nba.sdui.versioning;

import java.util.Objects;

/**
 * Semantic version (major.minor) for SDUI schema compatibility.
 *
 * <p>Major increments indicate breaking changes (new required fields, new enum
 * values for closed shapes, removed fields). Minor increments indicate additive
 * changes (new optional fields, new section types).
 */
public final class SchemaVersion implements Comparable<SchemaVersion> {

    private final int major;
    private final int minor;

    private SchemaVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Parse a version string like "1.0" or "2.1".
     *
     * @throws IllegalArgumentException if format is invalid
     */
    public static SchemaVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Schema version must not be null or blank");
        }
        String[] parts = version.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Schema version must be major.minor, got: " + version);
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (major < 0 || minor < 0) {
                throw new IllegalArgumentException("Schema version components must be non-negative: " + version);
            }
            return new SchemaVersion(major, minor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Schema version must contain integers: " + version, e);
        }
    }

    public static SchemaVersion of(int major, int minor) {
        return new SchemaVersion(major, minor);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    /**
     * Returns true if this version is older than (less than) the other.
     */
    public boolean isOlderThan(SchemaVersion other) {
        return this.compareTo(other) < 0;
    }

    /**
     * Returns true if this version supports a feature introduced in the given version.
     * A client supports a feature if its version is >= the feature's introduction version.
     */
    public boolean supports(SchemaVersion introducedIn) {
        return this.compareTo(introducedIn) >= 0;
    }

    @Override
    public int compareTo(SchemaVersion other) {
        int majorCmp = Integer.compare(this.major, other.major);
        if (majorCmp != 0) return majorCmp;
        return Integer.compare(this.minor, other.minor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaVersion that)) return false;
        return major == that.major && minor == that.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
