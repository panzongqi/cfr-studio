package cfr.gui.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 与界面无关的源码搜索，避免大小写处理改变字符下标。 */
public final class SourceSearch {
    private SourceSearch() { }

    public static List<Integer> findAll(String source, String query, boolean caseSensitive) {
        if (query.isEmpty()) {
            return List.of();
        }
        List<Integer> matches = new ArrayList<>();
        for (int start = 0; start + query.length() <= source.length(); start++) {
            if (regionMatches(source, start, query, caseSensitive)) {
                matches.add(start);
            }
        }
        return List.copyOf(matches);
    }

    private static boolean regionMatches(String source, int start, String query, boolean caseSensitive) {
        for (int i = 0; i < query.length(); i++) {
            char left = source.charAt(start + i);
            char right = query.charAt(i);
            if (left != right && (caseSensitive
                    || Character.toLowerCase(Character.toUpperCase(left))
                    != Character.toLowerCase(Character.toUpperCase(right)))) {
                return false;
            }
        }
        return true;
    }

    public static int next(List<Integer> matches, int caret, boolean forward) {
        if (matches.isEmpty()) {
            return -1;
        }
        if (forward) {
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i) > caret) {
                    return i;
                }
            }
            return 0;
        }
        for (int i = matches.size() - 1; i >= 0; i--) {
            if (matches.get(i) < caret) {
                return i;
            }
        }
        return matches.size() - 1;
    }
}
