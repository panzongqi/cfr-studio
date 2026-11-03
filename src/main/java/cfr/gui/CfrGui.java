package cfr.gui;

import cfr.gui.i18n.I18n;
import cfr.gui.service.ClassEntry;
import cfr.gui.service.InputFile;
import cfr.gui.ui.MainFrame;
import cfr.gui.ui.Ui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

public final class CfrGui {
    private CfrGui() { }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println(I18n.t("runtime.lookAndFeelFailed", exception.getMessage()));
        }
        Ui.initialize();
        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler(CfrGui::report);
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            if (args.length > 0) {
                frame.openInput(Path.of(args[0]));
            }
        });
    }

    private static void report(Thread thread, Throwable throwable) {
        throwable.printStackTrace();
        JOptionPane.showMessageDialog(null, I18n.t("runtime.unhandled", throwable.getMessage()),
                I18n.t("runtime.unhandledTitle"), JOptionPane.ERROR_MESSAGE);
    }
}
