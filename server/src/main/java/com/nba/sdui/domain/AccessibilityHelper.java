package com.nba.sdui.domain;

import com.nba.sdui.models.generated.AccessibilityProperties;
import com.nba.sdui.models.generated.AtomicElement;

/**
 * Server-side accessibility annotation helpers for atomic elements.
 *
 * Per AGENTS.md §1.1, the server owns content semantics including
 * accessibility labels for information-bearing elements. Clients
 * realize these as platform-native accessibility attributes.
 *
 * Usage: call the static helpers after building an {@link AtomicElement}
 * to attach the {@code accessibility} block defined in the schema's
 * {@code AccessibilityProperties}.
 */
public final class AccessibilityHelper {

    private AccessibilityHelper() {} // utility class

    /**
     * Add an accessibility label and role to an atomic element.
     */
    public static void addLabel(AtomicElement element, String label, String role) {
        if (element == null || label == null || label.isBlank()) return;
        AccessibilityProperties a11y = new AccessibilityProperties();
        a11y.setLabel(label);
        if (role != null) a11y.setRole(AccessibilityProperties.Role.fromValue(role));
        element.setAccessibility(a11y);
    }

    /**
     * Mark an element as decorative (hidden from the accessibility tree).
     * Use for images that convey no information (backgrounds, gradients, decorative art).
     */
    public static void addHidden(AtomicElement element) {
        if (element == null) return;
        AccessibilityProperties a11y = new AccessibilityProperties();
        a11y.setHidden(true);
        element.setAccessibility(a11y);
    }

    /**
     * Add heading semantics with the given level (1–6).
     */
    public static void addHeading(AtomicElement element, String label, int level) {
        if (element == null || label == null || label.isBlank()) return;
        AccessibilityProperties a11y = new AccessibilityProperties();
        a11y.setLabel(label);
        a11y.setRole(AccessibilityProperties.Role.HEADING);
        a11y.setHeadingLevel(Math.max(1, Math.min(6, level)));
        element.setAccessibility(a11y);
    }

    /**
     * Add a label with button role for tappable containers.
     */
    public static void addButton(AtomicElement element, String label) {
        addLabel(element, label, "button");
    }

    /**
     * Add image role with a descriptive label.
     */
    public static void addImage(AtomicElement element, String label) {
        addLabel(element, label, "image");
    }
}
