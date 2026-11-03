package cfr.gui.ui;

import cfr.gui.i18n.I18n;
import cfr.gui.service.DecompilerOptions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

/** 参数对话框的轻量封装。 */
final class JDialogPlaceholder {
    private final JDialog dialog;
    private final OptionsPanel panel;
    private boolean accepted;

    JDialogPlaceholder(Frame owner, DecompilerOptions options) {
        dialog = new JDialog(owner, I18n.t("options.title"), true);
        panel = new OptionsPanel(owner);
        panel.setOptions(options);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JButton apply = Ui.button(I18n.t("options.apply"), null);
        JButton cancel = Ui.button(I18n.t("action.cancel"), null);
        apply.addActionListener(event -> {
            accepted = panel.apply();
            if (accepted) dialog.dispose();
        });
        cancel.addActionListener(event -> dialog.dispose());
        buttons.add(cancel);
        buttons.add(apply);
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(apply);
        dialog.setSize(510, 680);
        dialog.setMinimumSize(new java.awt.Dimension(480, 600));
        dialog.setLocationRelativeTo(owner);
    }

    boolean showDialog() {
        dialog.setVisible(true);
        return accepted;
    }

    DecompilerOptions options() {
        return panel.currentOptions();
    }
}
