package dev.jdoctor.analyze;

import java.util.List;

/** Tells application code apart from frameworks and the JDK in stack frames. */
public final class Frames {

    private static final List<String> FRAMEWORK_PREFIXES = List.of(
            "com.zaxxer.", "org.springframework.", "org.hibernate.", "org.apache.", "jakarta.", "javax.",
            "java.", "jdk.", "sun.", "com.sun.", "org.jooq.", "io.micrometer.", "io.netty.", "reactor.",
            "kotlin.", "kotlinx.", "com.fasterxml.", "org.eclipse.jetty.", "io.undertow.", "org.junit.",
            "net.bytebuddy.");

    private Frames() {
    }

    public static boolean isApplication(String frame) {
        return FRAMEWORK_PREFIXES.stream().noneMatch(frame::startsWith);
    }

    /** {@code com.example.Foo.bar(Foo.java:12)} becomes {@code com.example.Foo.bar}. */
    public static String method(String frame) {
        return withoutProxy(frame.replaceAll("\\(.*\\)$", ""));
    }

    /** Spring proxies ({@code @Transactional}, {@code @Async}) show up as {@code Service$$SpringCGLIB$$0}. */
    public static String withoutProxy(String frame) {
        return frame.replaceAll("\\$\\$SpringCGLIB\\$\\$\\d+", "");
    }

    /** {@code com.example.Foo.bar(Foo.java:12)} becomes {@code Foo.bar}, short enough for a title. */
    public static String shortMethod(String frame) {
        String method = method(frame);
        String[] parts = method.split("\\.");
        return parts.length >= 2 ? parts[parts.length - 2] + "." + parts[parts.length - 1] : method;
    }
}
