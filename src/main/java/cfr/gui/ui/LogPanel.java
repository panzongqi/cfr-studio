package cfr.gui.ui;

import cfr.gui.i18n.I18n;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class LogPanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final JTextArea log = new JTextArea();
    private final javax.swing.JLabel titleLabel = new javax.swing.JLabel(I18n.t("log.title"));
    private final JButton clearButton = Ui.button(I18n.t("action.clear"), null);

    public LogPanel() {
        super(new BorderLayout());
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 12));
        log.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        log.setRows(5);
        log.getAccessibleContext().setAccessibleName(I18n.t("log.title"));
        add(new JScrollPane(log));
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        header.add(titleLabel, BorderLayout.WEST);
        clearButton.addActionListener(event -> log.setText(""));
        header.add(clearButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    public void retranslate() {
        titleLabel.setText(I18n.t("log.title"));
        clearButton.setText(I18n.t("action.clear"));
        log.getAccessibleContext().setAccessibleName(I18n.t("log.title"));
    }

    public void info(String message) {
        append(I18n.t("log.level.info"), message);
    }

    public void error(String message) {
        append(I18n.t("log.level.error"), message);
    }

    private void append(String level, String message) {
        log.append("[" + LocalTime.now().format(TIME) + "] " + level + "  " + message + "\n");
        if (log.getDocument().getLength() > 300_000) {
            try {
                log.getDocument().remove(0, 100_000);
            } catch (javax.swing.text.BadLocationException ignored) {
                log.setText("");
            }
        }
        log.setCaretPosition(log.getDocument().getLength());
    }
}
