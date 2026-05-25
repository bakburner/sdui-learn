package com.nba.sdui.service;

/**
 * Wire-form motion token constants matched against {@code schema/motion-tokens.json}.
 */
public final class MotionTokens {

    private MotionTokens() {}

    public static final String EASING_DEFAULT = "token:nba.motion.easing.default";
    public static final String EASING_LINEAR = "token:nba.motion.easing.linear";

    public static final String DURATION_FAST = "token:nba.motion.duration.fast";
    public static final String DURATION_DEFAULT = "token:nba.motion.duration.default";
    public static final String DURATION_SLOW = "token:nba.motion.duration.slow";
    public static final String DURATION_HERO = "token:nba.motion.duration.hero";
}
