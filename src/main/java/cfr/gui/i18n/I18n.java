package cfr.gui.i18n;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/** 界面文案的运行时语言切换。 */
public final class I18n {
    public enum Language {
        ZH("中文", Locale.SIMPLIFIED_CHINESE, "zh"),
        EN("English", Locale.ENGLISH, "en");

        private final String displayName;
        private final Locale locale;
        private final String code;

        Language(String displayName, Locale locale, String code) {
            this.displayName = displayName;
            this.locale = locale;
            this.code = code;
        }

        public String displayName() {
            return displayName;
        }

        public Locale locale() {
            return locale;
        }

        @Override public String toString() {
            return displayName;
        }
    }

    private static final String BUNDLE_NAME = "cfr.gui.i18n.messages";
    private static final String PREFERENCE_KEY = "language";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(I18n.class);
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Language language = loadLanguage();
    private static volatile ResourceBundle bundle = loadBundle(language);

    private I18n() { }

    public static String t(String key, Object... arguments) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException exception) {
            pattern = key;
        }
        return arguments.length == 0 ? pattern : format(pattern, arguments);
    }

    public static Language language() {
        return language;
    }

    public static void setLanguage(Language value) {
        if (value == null || value == language) {
            return;
        }
        language = value;
        bundle = loadBundle(value);
        try {
            PREFERENCES.put(PREFERENCE_KEY, value.code);
        } catch (RuntimeException ignored) {
            // 语言仅保存在当前会话中。
        }
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    private static Language loadLanguage() {
        try {
            String stored = PREFERENCES.get(PREFERENCE_KEY, "zh");
            return "en".equalsIgnoreCase(stored) ? Language.EN : Language.ZH;
        } catch (RuntimeException exception) {
            return Language.ZH;
        }
    }

    private static ResourceBundle loadBundle(Language value) {
        return ResourceBundle.getBundle(BUNDLE_NAME, value.locale);
    }

    private static String format(String pattern, Object[] arguments) {
        StringBuilder result = new StringBuilder(pattern.length());
        int index = 0;
        while (index < pattern.length()) {
            int open = pattern.indexOf('{', index);
            int close = open < 0 ? -1 : pattern.indexOf('}', open + 1);
            if (open < 0 || close < 0) {
                result.append(pattern, index, pattern.length());
                break;
            }
            result.append(pattern, index, open);
            String token = pattern.substring(open + 1, close);
            try {
                int argument = Integer.parseInt(token);
                result.append(argument >= 0 && argument < arguments.length
                        ? String.valueOf(arguments[argument]) : pattern.substring(open, close + 1));
            } catch (NumberFormatException exception) {
                result.append(pattern, open, close + 1);
            }
            index = close + 1;
        }
        return result.toString();
    }
}
