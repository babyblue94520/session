package pers.clare.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class LocaleUtil {
    static List<Locale> locales = new ArrayList<>();

    static {
        locales.add(Locale.ENGLISH);
        locales.add(Locale.TRADITIONAL_CHINESE);
        locales.add(Locale.SIMPLIFIED_CHINESE);
    }

    private LocaleUtil() {
    }

    public static Locale get(String lang) {
        return Locale.lookup(Locale.LanguageRange.parse(lang), locales);
    }
}
