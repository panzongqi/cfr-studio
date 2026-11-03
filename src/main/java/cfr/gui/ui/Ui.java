package cfr.gui.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

public final class Ui {
    public static final Color TEXT = new Color(36, 41, 47);
    public static final Color MUTED = new Color(87, 96, 106);
    public static final Color BORDER = new Color(218, 222, 228);
    public static final Color ACCENT = new Color(36, 99, 175);
    public static final Color PANEL = new Color(246, 248, 250);

    private Ui() { }

    public static void initialize() {
        UIManager.put("ToolTip.font", new Font("Microsoft YaHei", Font.PLAIN, 13));
        UIManager.put("OptionPane.messageFont", new Font("Microsoft YaHei", Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont", new Font("Microsoft YaHei", Font.PLAIN, 13));
        UIManager.put("FileChooser.font", new Font("Microsoft YaHei", Font.PLAIN, 13));
        UIManager.put("Tree.rowHeight", 25);
        UIManager.put("Button.arc", 6);
        UIManager.put("Component.arc", 6);
        UIManager.put("TextComponent.arc", 5);
    }

    public static JButton button(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
        button.setFocusable(true);
        button.setMargin(new java.awt.Insets(7, 12, 7, 12));
        return button;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        return label;
    }

    public static void titled(JComponent component, String title) {
        component.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title,
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                new Font("Microsoft YaHei", Font.PLAIN, 12), MUTED));
    }

    public static Icon icon(String name) {
        return new LineIcon(name);
    }

    private record LineIcon(String name) implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            g.setColor(component.getForeground());
            g.setStroke(new BasicStroke(1.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Double p = new Path2D.Double();
            switch (name) {
                case "open" -> {
                    p.moveTo(2, 14); p.lineTo(2, 4); p.lineTo(7, 4); p.lineTo(9, 6);
                    p.lineTo(16, 6); p.lineTo(16, 14); p.closePath();
                    p.moveTo(2, 14); p.lineTo(5, 8); p.lineTo(17, 8); p.lineTo(14, 14);
                }
                case "export" -> {
                    p.moveTo(9, 2); p.lineTo(9, 11); p.moveTo(5.5, 7.5);
                    p.lineTo(9, 11); p.lineTo(12.5, 7.5);
                    p.moveTo(3, 11); p.lineTo(3, 16); p.lineTo(15, 16); p.lineTo(15, 11);
                }
                case "save" -> {
                    p.moveTo(3, 2); p.lineTo(13, 2); p.lineTo(16, 5); p.lineTo(16, 16);
                    p.lineTo(3, 16); p.closePath(); p.moveTo(5, 2); p.lineTo(5, 7);
                    p.lineTo(12, 7); p.lineTo(12, 2); p.moveTo(6, 16); p.lineTo(6, 11);
                    p.lineTo(13, 11); p.lineTo(13, 16);
                }
                case "settings" -> {
                    p.moveTo(2, 5); p.lineTo(16, 5); p.moveTo(2, 13); p.lineTo(16, 13);
                    g.draw(p); g.setColor(PANEL); g.fillOval(4, 2, 6, 6);
                    g.fillOval(9, 10, 6, 6); g.setColor(component.getForeground());
                    g.drawOval(4, 2, 6, 6); g.drawOval(9, 10, 6, 6); g.dispose(); return;
                }
                case "search" -> {
                    g.drawOval(2, 2, 10, 10); g.drawLine(10, 10, 16, 16); g.dispose(); return;
                }
                case "copy" -> {
                    p.moveTo(6, 6); p.lineTo(15, 6); p.lineTo(15, 16); p.lineTo(6, 16);
                    p.closePath(); p.moveTo(6, 3); p.lineTo(3, 3); p.lineTo(3, 13);
                }
                case "wrap" -> {
                    p.moveTo(2, 3); p.lineTo(15, 3); p.lineTo(15, 11); p.lineTo(7, 11);
                    p.moveTo(10, 7); p.lineTo(6, 11); p.lineTo(10, 15);
                    p.moveTo(2, 7); p.lineTo(2, 15); p.lineTo(5, 15);
                }
                case "refresh" -> {
                    p.moveTo(14, 6); p.lineTo(14, 2); p.lineTo(10, 2);
                    p.moveTo(14, 3); p.curveTo(8, -1, 1, 3, 2, 9);
                    p.curveTo(3, 15, 9, 18, 14, 14);
                }
                default -> { }
            }
            g.draw(p);
            g.dispose();
        }
    }
}
