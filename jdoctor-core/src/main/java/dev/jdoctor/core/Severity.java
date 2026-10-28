package dev.jdoctor.core;

/** How urgent a finding is. Declared from least to most severe. */
public enum Severity {
    INFO,
    WARNING,
    CRITICAL;

    public boolean isAtLeast(Severity other) {
        return compareTo(other) >= 0;
    }
}
