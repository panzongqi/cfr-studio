package cfr.gui.ui;

import cfr.gui.i18n.I18n;
import cfr.gui.service.DecompiledSource;
import cfr.gui.service.SourceSearch;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.function.Consumer;

public final class SourcePanel extends JPanel {
    private static final Object HIGHLIGHT = new Object();
    private final JTextPane source = new JTextPane();
    private final JTextField query = new JTextField(16);
    private final JCheckBox caseSensitive = new JCheckBox(I18n.t("source.caseSensitive"));
    private final JToggleButton wrap = new JToggleButton(I18n.t("source.wrap"));
    private final JLabel sourceTitle = new JLabel(I18n.t("source.title"));
    private final JLabel status = Ui.muted(I18n.t("source.selectClass"));
    private final JLabel matches = Ui.muted("");
    private final JLabel findLabel = new JLabel(I18n.t("action.find"));
    private final JButton previousButton = Ui.button(I18n.t("action.previous"), null);
    private final JButton nextButton = Ui.button(I18n.t("action.next"), null);
    private final JButton copyButton = Ui.button(I18n.t("action.copy"), Ui.icon("copy"));
    private final JButton saveButton = Ui.button(I18n.t("action.save"), Ui.icon("save"));
    private final Timer highlightTimer;
    private DecompiledSource current;
    private List<Integer> occurrences = List.of();
    private int matchIndex = -1;
    private String messageKey;
    private Object[] messageArguments = new Object[0];
    private String statusKey = "source.selectClass";
    private Object[] statusArguments = new Object[0];
    private String matchesKey;
    private Object[] matchesArguments = new Object[0];

    public SourcePanel(Runnable saveAction) {
        super(new BorderLayout());
        source.setEditable(false);
        source.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        source.setBackground(new Color(255, 255, 255));
        source.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        source.getAccessibleContext().setAccessibleName(I18n.t("source.accessibility"));
        source.getDocument().addDocumentListener(new SimpleDocumentListener(this::refreshLineNumbers));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        sourceTitle.setFont(sourceTitle.getFont().deriveFont(Font.BOLD, 14f));
        toolbar.add(sourceTitle, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        wrap.setToolTipText(I18n.t("source.wrapTooltip"));
        wrap.addActionListener(event -> rewrap());
        copyButton.addActionListener(event -> copySource());
        saveButton.addActionListener(event -> saveAction.run());
        actions.add(wrap);
        actions.add(copyButton);
        actions.add(saveButton);
        toolbar.add(actions, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        JPanel editor = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(new NoWrapPane(source));
        scroll.getViewport().setBackground(source.getBackground());
        scroll.setRowHeaderView(new LineNumbers(source));
        scroll.getRowHeader().setBackground(Ui.PANEL);
        editor.add(scroll);
        editor.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Ui.BORDER));
        add(editor, BorderLayout.CENTER);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        query.putClientProperty("JTextField.placeholderText", I18n.t("source.searchPlaceholder"));
        query.getAccessibleContext().setAccessibleName(I18n.t("source.searchAccessibility"));
        previousButton.addActionListener(event -> selectMatch(false));
        nextButton.addActionListener(event -> selectMatch(true));
        search.add(findLabel);
        search.add(query);
        search.add(caseSensitive);
        search.add(previousButton);
        search.add(nextButton);
        search.add(matches);
        search.add(status);
        add(search, BorderLayout.SOUTH);
        query.addActionListener(event -> selectMatch(true));
        caseSensitive.addActionListener(event -> search());
        highlightTimer = new Timer(180, event -> search());
        highlightTimer.setRepeats(false);
        query.getDocument().addDocumentListener(new SimpleDocumentListener(highlightTimer::restart));
    }

    public void showMessage(String messageKey, Object... messageArguments) {
        current = null;
        this.messageKey = messageKey;
        this.messageArguments = messageArguments;
        sourceTitle.setText(I18n.t("source.title"));
        setText(I18n.t(messageKey, messageArguments));
        setStatus("source.selectClass");
        occurrences = List.of();
        setMatches(null);
    }

    public void showSource(ClassEntrySelection selection, DecompiledSource decompiled) {
        current = decompiled;
        messageKey = null;
        sourceTitle.setText(selection.displayName());
        setText(decompiled.java());
        if (decompiled.diagnostics().isEmpty()) {
            setStatus("source.completed");
        } else {
            setStatus("source.diagnostics", decompiled.diagnostics().size());
        }
        source.setCaretPosition(0);
        search();
    }

    public void retranslate() {
        caseSensitive.setText(I18n.t("source.caseSensitive"));
        wrap.setText(I18n.t("source.wrap"));
        wrap.setToolTipText(I18n.t("source.wrapTooltip"));
        copyButton.setText(I18n.t("action.copy"));
        saveButton.setText(I18n.t("action.save"));
        findLabel.setText(I18n.t("action.find"));
        previousButton.setText(I18n.t("action.previous"));
        nextButton.setText(I18n.t("action.next"));
        query.putClientProperty("JTextField.placeholderText", I18n.t("source.searchPlaceholder"));
        query.getAccessibleContext().setAccessibleName(I18n.t("source.searchAccessibility"));
        query.revalidate();
        query.repaint();
        source.getAccessibleContext().setAccessibleName(I18n.t("source.accessibility"));
        if (current == null) {
            sourceTitle.setText(I18n.t("source.title"));
            if (messageKey != null) {
                setText(I18n.t(messageKey, messageArguments));
            }
        }
        refreshStatus();
        refreshMatches();
    }

    public DecompiledSource currentSource() {
        return current;
    }

    private void setText(String text) {
        source.setText(text);
        JavaSyntax.highlight(source);
    }

    private void setStatus(String messageKey, Object... messageArguments) {
        statusKey = messageKey;
        statusArguments = messageArguments;
        status.setText(I18n.t(messageKey, messageArguments));
    }

    private void refreshStatus() {
        if (statusKey != null) {
            status.setText(I18n.t(statusKey, statusArguments));
        }
    }

    private void setMatches(String messageKey, Object... messageArguments) {
        matchesKey = messageKey;
        matchesArguments = messageArguments;
        matches.setText(messageKey == null ? "" : I18n.t(messageKey, messageArguments));
    }

    private void refreshMatches() {
        matches.setText(matchesKey == null ? "" : I18n.t(matchesKey, matchesArguments));
    }

    private void search() {
        source.getHighlighter().removeAllHighlights();
        occurrences = SourceSearch.findAll(source.getText(), query.getText(), caseSensitive.isSelected());
        matchIndex = -1;
        if (occurrences.isEmpty() && !query.getText().isEmpty()) {
            setMatches("source.noMatch");
        } else {
            setMatches("source.matches", occurrences.size());
        }
        if (!query.getText().isEmpty()) {
            DefaultHighlighter.DefaultHighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 239, 158));
            int limit = Math.min(occurrences.size(), 2000);
            for (int i = 0; i < limit; i++) {
                int position = occurrences.get(i);
                try {
                    source.getHighlighter().addHighlight(position, position + query.getText().length(), painter);
                } catch (javax.swing.text.BadLocationException ignored) {
                    // 文本仅在 EDT 修改，理论上不会发生。
                }
            }
            selectMatch(true);
        }
    }

    private void refreshLineNumbers() {
        java.awt.Container viewport = source.getParent();
        if (viewport instanceof javax.swing.JViewport view && view.getParent() instanceof JScrollPane scroll) {
            JComponent rowHeader = (JComponent) scroll.getRowHeader().getView();
            rowHeader.revalidate();
            rowHeader.repaint();
        }
    }

    private void selectMatch(boolean forward) {
        if (occurrences.isEmpty()) {
            return;
        }
        matchIndex = SourceSearch.next(occurrences, source.getSelectionStart(), forward);
        int position = occurrences.get(matchIndex);
        source.requestFocusInWindow();
        source.select(position, position + query.getText().length());
        setMatches("source.matchIndex", matchIndex + 1, occurrences.size());
    }

    private void copySource() {
        if (source.getText().isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(source.getText()), null);
        setStatus("source.copied");
    }

    private void rewrap() {
        JScrollPane scroll = (JScrollPane) source.getParent().getParent().getParent();
        scroll.setViewportView(wrap.isSelected() ? source : new NoWrapPane(source));
        scroll.setRowHeaderView(new LineNumbers(source));
        source.revalidate();
        source.repaint();
    }

    public record ClassEntrySelection(String displayName) { }

    private static final class NoWrapPane extends JPanel implements javax.swing.Scrollable {
        private NoWrapPane(JComponent content) {
            super(new BorderLayout());
            setBackground(content.getBackground());
            add(content);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return 300; }
        @Override public boolean getScrollableTracksViewportWidth() { return false; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class LineNumbers extends JComponent {
        private final JTextPane text;
        private final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        private LineNumbers(JTextPane text) {
            this.text = text;
            setFont(font);
            setOpaque(true);
            setBackground(Ui.PANEL);
        }

        @Override public Dimension getPreferredSize() {
            int lines = Math.max(1, text.getDocument().getDefaultRootElement().getElementCount());
            return new Dimension(Math.max(42, getFontMetrics(font).stringWidth(String.valueOf(lines)) + 18),
                    text.getPreferredSize().height + 16);
        }

        @Override protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics);
            graphics.setFont(font);
            graphics.setColor(Ui.MUTED);
            Element root = text.getDocument().getDefaultRootElement();
            java.awt.FontMetrics metrics = graphics.getFontMetrics();
            int ascent = metrics.getAscent();
            for (int i = 0; i < root.getElementCount(); i++) {
                try {
                    java.awt.Rectangle rectangle = text.modelToView2D(root.getElement(i).getStartOffset()).getBounds();
                    String line = Integer.toString(i + 1);
                    graphics.drawString(line, getWidth() - metrics.stringWidth(line) - 9,
                            rectangle.y + 8 + ascent);
                } catch (javax.swing.text.BadLocationException ignored) {
                    return;
                }
            }
        }
    }
}
