package kz.iqadam.esyllabus.syllabus.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class CourseMetadataSupport {

    private CourseMetadataSupport() {
    }

    public static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public static String toCsv(List<String> values) {
        return values == null
                ? ""
                : values.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .collect(java.util.stream.Collectors.joining("|"));
    }

    public static List<String> defaultTags(String title, String program, String code) {
        Set<String> tags = new LinkedHashSet<>();
        addTokens(tags, title);
        addTokens(tags, program);
        addTokens(tags, code);
        return tags.stream().limit(8).toList();
    }

    private static void addTokens(Set<String> tags, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Arrays.stream(text.replace('/', ' ').replace('-', ' ').split("\\s+"))
                .map(token -> token.replaceAll("[^\\p{L}\\p{Nd}]", ""))
                .map(token -> token.toLowerCase(Locale.ROOT))
                .filter(token -> token.length() >= 3)
                .forEach(tags::add);
    }
}
