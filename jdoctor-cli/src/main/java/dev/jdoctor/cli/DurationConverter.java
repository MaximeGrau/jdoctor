package dev.jdoctor.cli;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine;

/** Accepts {@code 90}, {@code 90s}, {@code 2m}. A bare number is seconds. */
final class DurationConverter implements CommandLine.ITypeConverter<Duration> {

    private static final Pattern FORMAT = Pattern.compile("(\\d+)\\s*(s|m)?");

    @Override
    public Duration convert(String value) {
        Matcher m = FORMAT.matcher(value.strip());
        if (!m.matches()) {
            throw new CommandLine.TypeConversionException("Invalid duration '" + value + "'. Use 30s or 2m.");
        }
        long amount = Long.parseLong(m.group(1));
        return "m".equals(m.group(2)) ? Duration.ofMinutes(amount) : Duration.ofSeconds(amount);
    }
}
