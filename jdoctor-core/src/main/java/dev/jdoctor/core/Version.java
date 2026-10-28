package dev.jdoctor.core;

/** The running jdoctor version, read from the jar manifest. */
public final class Version {

    private Version() {
    }

    public static String current() {
        String version = Version.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }
}
