package cfr.gui.ui;

import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.Set;

/** 简单的 Java 词法着色，不改动源码，也不尝试解析语义。 */
public final class JavaSyntax {
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "true", "false", "null", "var", "record",
            "sealed", "permits", "yield");

    private JavaSyntax() { }

    public static void highlight(JTextPane pane) {
        String text = pane.getText();
        StyledDocument document = pane.getStyledDocument();
        SimpleAttributeSet plain = new SimpleAttributeSet();
        StyleConstants.setForeground(plain, new Color(36, 41, 47));
        SimpleAttributeSet keyword = new SimpleAttributeSet(plain);
        StyleConstants.setForeground(keyword, new Color(207, 34, 46));
        SimpleAttributeSet literal = new SimpleAttributeSet(plain);
        StyleConstants.setForeground(literal, new Color(9, 105, 218));
        SimpleAttributeSet comment = new SimpleAttributeSet(plain);
        StyleConstants.setForeground(comment, new Color(89, 99, 110));
        document.setCharacterAttributes(0, text.length(), plain, true);
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            int start = index;
            SimpleAttributeSet style = null;
            if (current == '/' && index + 1 < text.length() && text.charAt(index + 1) == '/') {
                index = text.indexOf('\n', index + 2);
                if (index < 0) index = text.length();
                style = comment;
            } else if (current == '/' && index + 1 < text.length() && text.charAt(index + 1) == '*') {
                int end = text.indexOf("*/", index + 2);
                index = end < 0 ? text.length() : end + 2;
                style = comment;
            } else if (current == '"' || current == '\'') {
                char quote = current;
                index++;
                while (index < text.length()) {
                    if (text.charAt(index) == '\\') {
                        index = Math.min(text.length(), index + 2);
                    } else if (text.charAt(index++) == quote) {
                        break;
                    }
                }
                style = literal;
            } else if (Character.isJavaIdentifierStart(current)) {
                index++;
                while (index < text.length() && Character.isJavaIdentifierPart(text.charAt(index))) {
                    index++;
                }
                if (KEYWORDS.contains(text.substring(start, index))) {
                    style = keyword;
                }
            } else {
                index++;
            }
            if (style != null) {
                document.setCharacterAttributes(start, index - start, style, true);
            }
        }
    }
}
