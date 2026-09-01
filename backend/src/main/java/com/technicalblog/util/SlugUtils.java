package com.technicalblog.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** URL slug creation and uniqueness handling. */
public final class SlugUtils {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s_]+");
    private static final Pattern ILLEGAL = Pattern.compile("[^a-zA-Z0-9-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");
    private static final Pattern EDGE_DASH = Pattern.compile("^-+|-+$");
    private static final int MAX_LENGTH = 200;

    private SlugUtils() {
    }

    /** Converts free text into a lowercase hyphenated slug, or an empty string when nothing usable remains. */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String slug = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        slug = DIACRITICS.matcher(slug).replaceAll("");
        slug = SEPARATORS.matcher(slug).replaceAll("-");
        slug = ILLEGAL.matcher(slug).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        slug = EDGE_DASH.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH);
        return slug.length() > MAX_LENGTH ? EDGE_DASH.matcher(slug.substring(0, MAX_LENGTH)).replaceAll("") : slug;
    }

    /**
     * Returns a slug accepted by the given availability test, appending -2, -3 and so on when needed.
     * Falls back to the second argument when the desired text produces an empty slug.
     */
    public static String uniqueSlug(String desired, String fallback, Predicate<String> isTaken) {
        String base = toSlug(desired);
        if (base.isEmpty()) {
            base = toSlug(fallback);
        }
        if (base.isEmpty()) {
            base = "item";
        }
        String candidate = base;
        int suffix = 2;
        while (isTaken.test(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
