package cfr.gui.ui;

import cfr.gui.i18n.I18n;
import cfr.gui.service.ArchiveService;
import cfr.gui.service.ClassEntry;
import cfr.gui.service.DecompiledSource;
import cfr.gui.service.DecompilerOptions;
import cfr.gui.service.DecompilerService;
import cfr.gui.service.InputFile;
import cfr.gui.service.SourceCache;
import cfr.gui.service.SourceExporter;
import cfr.gui.service.TaskManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MainFrame extends JFrame {
    private final ArchiveService archiveService = new ArchiveService();
    private final DecompilerService decompilerService = new DecompilerService();
    private final SourceExporter exporter = new SourceExporter();
    private final SourceCache cache = new SourceCache(40);
    private final TaskManager tasks = new TaskManager();
    private final ArchiveTreePanel tree = new ArchiveTreePanel(this::selectClass);
    private final SourcePanel source = new SourcePanel(this::saveCurrent);
    private final LogPanel logs = new LogPanel();
    private final JButton openButton = Ui.button(I18n.t("action.open"), Ui.icon("open"));
    private final JButton exportButton = Ui.button(I18n.t("action.exportAll"), Ui.icon("export"));
    private final JButton settingsButton = Ui.button(I18n.t("action.options"), Ui.icon("settings"));
    private final JButton cancelButton = Ui.button(I18n.t("action.cancel"), null);
    private final JLabel versionLabel = Ui.muted(I18n.t("app.subtitle"));
    private final JComboBox<I18n.Language> languageBox = new JComboBox<>(I18n.Language.values());
    private final JLabel status = new JLabel(I18n.t("status.ready"));
    private final JProgressBar progress = new JProgressBar();
    private String statusKey = "status.ready";
    private Object[] statusArguments = new Object[0];
    private InputFile input;
    private ClassEntry selectedClass;
    private ArchiveService.ArchiveContent content;
    private DecompilerOptions options = DecompilerOptions.defaults();
    private boolean busy;
    private boolean closing;
    private long openedVersion;

    public MainFrame() {
        super(I18n.t("app.title"));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new java.awt.Dimension(940, 620));
        setLocationRelativeTo(null);
        setJMenuBar(createMenu());
        setContentPane(createContent());
        configureActions();
        configureDrop();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { closeWindow(); }
        });
        I18n.addListener(this::retranslate);
        logs.info(I18n.t("log.ready"));
        exportButton.setEnabled(false);
        cancelButton.setVisible(false);
        progress.setVisible(false);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(openButton);
        buttons.add(exportButton);
        buttons.add(settingsButton);
        toolbar.add(buttons, BorderLayout.WEST);
        JPanel languages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        languages.setOpaque(false);
        languageBox.setToolTipText(I18n.t("app.languageTooltip"));
        languageBox.setSelectedItem(I18n.language());
        languages.add(languageBox);
        languages.add(versionLabel);
        toolbar.add(languages, BorderLayout.EAST);
        root.add(toolbar, BorderLayout.NORTH);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, source);
        main.setResizeWeight(0.26);
        main.setDividerLocation(340);
        main.setContinuousLayout(true);
        main.setBorder(null);
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, main, logs);
        vertical.setResizeWeight(0.78);
        vertical.setDividerLocation(600);
        vertical.setContinuousLayout(true);
        SwingUtilities.invokeLater(() -> vertical.setDividerLocation(0.8));
        vertical.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        root.add(vertical, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.BORDER),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 12f));
        bottom.add(status, BorderLayout.CENTER);
        progress.setIndeterminate(true);
        progress.setPreferredSize(new java.awt.Dimension(150, 16));
        bottom.add(progress, BorderLayout.EAST);
        bottom.add(cancelButton, BorderLayout.WEST);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private JMenuBar createMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu(I18n.t("menu.file"));
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem open = new JMenuItem(I18n.t("menu.file.open"));
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        open.addActionListener(event -> chooseInput());
        JMenuItem save = new JMenuItem(I18n.t("menu.file.save"));
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        save.addActionListener(event -> saveCurrent());
        JMenuItem exit = new JMenuItem(I18n.t("menu.file.exit"));
        exit.addActionListener(event -> closeWindow());
        file.add(open);
        file.add(save);
        file.addSeparator();
        file.add(exit);
        JMenu help = new JMenu(I18n.t("menu.help"));
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem about = new JMenuItem(I18n.t("menu.help.about"));
        about.addActionListener(event -> JOptionPane.showMessageDialog(this,
                I18n.t("about.message"),
                I18n.t("about.title"), JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(file);
        bar.add(help);
        return bar;
    }

    private void retranslate() {
        setTitle(I18n.t("app.title"));
        setJMenuBar(createMenu());
        openButton.setText(I18n.t("action.open"));
        exportButton.setText(I18n.t("action.exportAll"));
        settingsButton.setText(I18n.t("action.options"));
        cancelButton.setText(I18n.t("action.cancel"));
        versionLabel.setText(I18n.t("app.subtitle"));
        languageBox.setToolTipText(I18n.t("app.languageTooltip"));
        languageBox.setSelectedItem(I18n.language());
        refreshStatus();
        tree.retranslate();
        source.retranslate();
        logs.retranslate();
    }

    private void configureActions() {
        openButton.addActionListener(event -> chooseInput());
        exportButton.addActionListener(event -> exportAll());
        settingsButton.addActionListener(event -> showOptions());
        cancelButton.addActionListener(event -> {
            tasks.cancelCurrent();
            setBusy(false, "status.cancelRequested");
        });
        languageBox.addActionListener(event -> {
            if (languageBox.getSelectedItem() instanceof I18n.Language language) {
                I18n.setLanguage(language);
            }
        });
    }

    private void configureDrop() {
        setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport support) {
                return !busy && support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
            }

            @Override public boolean importData(TransferSupport support) {
                try {
                    @SuppressWarnings("unchecked")
                    List<java.io.File> files = (List<java.io.File>) support.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (files.size() == 1) {
                        openInput(files.getFirst().toPath());
                    }
                    return files.size() == 1;
                } catch (Exception exception) {
                    showError("error.readDropped", exception);
                    return false;
                }
            }
        });
    }

    private void chooseInput() {
        if (busy) {
            return;
        }
        JFileChooser chooser = new JFileChooser(input == null ? null : input.path().toFile());
        chooser.setDialogTitle(I18n.t("dialog.openJavaFile"));
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.t("dialog.filterJavaFiles"), "jar", "war", "class"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openInput(chooser.getSelectedFile().toPath());
        }
    }

    public void openInput(Path path) {
        if (busy || closing) {
            return;
        }
        tasks.cancelCurrent();
        setBusy(true, "status.reading");
        source.showMessage("source.reading");
        tasks.submit(new TaskManager.Task<>(() -> {
            InputFile opened = InputFile.open(path);
            ArchiveService.ArchiveContent inspected = archiveService.inspect(opened);
            return new Opened(opened, inspected);
        }, opened -> {
            input = opened.input();
            content = opened.content();
            selectedClass = null;
            openedVersion++;
            tree.setContent(input.path().getFileName().toString(), content.entries(), content.totalClasses());
            tree.setBusy(false);
            setBusy(false, "status.opened", input.path().getFileName());
            source.showMessage(content.entries().isEmpty()
                    ? "source.emptyArchive" : "source.selectPrompt", content.totalClasses());
            logs.info(I18n.t("log.opened", input.path(), content.totalClasses()));
        }, throwable -> {
            tree.clear();
            tree.setBusy(false);
            setBusy(false, "status.openFailed");
            showError("error.openFile", throwable);
        }));
    }

    private void selectClass(ClassEntry entry) {
        if (busy || input == null) {
            return;
        }
        if (entry.equals(selectedClass) && source.currentSource() != null) {
            return;
        }
        tasks.cancelCurrent();
        selectedClass = entry;
        DecompilerOptions capturedOptions = options;
        var cached = cache.get(input, entry, capturedOptions);
        if (cached.isPresent()) {
            source.showSource(new SourcePanel.ClassEntrySelection(entry.displayName()), cached.get());
            setStatus("status.cacheLoaded", entry.displayName());
            return;
        }
        setBusy(true, "status.decompiling", entry.displayName());
        source.showMessage("source.decompiling", entry.displayName());
        long version = openedVersion;
        InputFile capturedInput = input;
        tasks.submit(new TaskManager.Task<>(() -> captureErrors(() -> {
            Path classFile = capturedInput.kind() == InputFile.Kind.CLASS
                    ? capturedInput.path() : null;
            DecompiledSource result = classFile != null
                    ? decompilerService.decompileClass(classFile, capturedOptions)
                    : decompilerService.decompileClass(capturedInput, entry, capturedOptions);
            cache.put(capturedInput, entry, capturedOptions, result);
            return result;
        }), result -> {
            if (input == capturedInput && entry.equals(selectedClass) && options.equals(capturedOptions)
                    && version == openedVersion) {
                source.showSource(new SourcePanel.ClassEntrySelection(entry.displayName()), result);
                result.diagnostics().forEach(logs::error);
                setBusy(false, "status.decompileDone", entry.displayName());
                logs.info(I18n.t("log.decompiled", entry.displayName()));
            }
        }, throwable -> {
            if (entry.equals(selectedClass)) {
                source.showMessage("source.cannotDecompile", entry.displayName());
                setBusy(false, "status.decompileFailed");
                showError("error.decompileFailed", throwable);
            }
        }));
    }

    private void showOptions() {
        if (busy) {
            return;
        }
        JDialogPlaceholder dialog = new JDialogPlaceholder(this, options);
        if (dialog.showDialog()) {
            DecompilerOptions previous = options;
            options = dialog.options();
            if (!options.equals(previous)) {
                logs.info(I18n.t("log.optionsUpdated"));
                if (selectedClass != null && source.currentSource() != null) {
                    ClassEntry current = selectedClass;
                    selectedClass = null;
                    selectClass(current);
                }
            }
        }
    }

    private void saveCurrent() {
        DecompiledSource current = source.currentSource();
        if (busy || current == null) {
            JOptionPane.showMessageDialog(this, I18n.t("dialog.saveFirst"),
                    I18n.t("dialog.saveFirstTitle"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(input.path().getParent().toFile());
        chooser.setDialogTitle(I18n.t("dialog.saveJavaSource"));
        chooser.setSelectedFile(input.path().getParent().resolve(exporter.defaultClassName(current)).toFile());
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.t("dialog.filterJavaSource"), "java"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destination = chooser.getSelectedFile().toPath();
        if (Files.exists(destination) && JOptionPane.showConfirmDialog(this,
                I18n.t("dialog.overwrite", destination), I18n.t("dialog.overwriteTitle"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            exporter.saveCurrent(destination, current);
            logs.info(I18n.t("log.saved", destination));
            setStatus("status.saved");
        } catch (IOException exception) {
            showError("error.saveFailed", exception);
        }
    }

    private void exportAll() {
        if (busy || input == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser(input.path().getParent().toFile());
        chooser.setDialogTitle(I18n.t("dialog.exportDir"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setSelectedFile(input.path().getParent().resolve(removeExtension(input.path()) + "-src").toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destination = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (Files.exists(destination) && (!Files.isDirectory(destination) || hasChildren(destination))) {
            JOptionPane.showMessageDialog(this, I18n.t("dialog.exportDirNotEmpty"),
                    I18n.t("dialog.exportDirTitle"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        tasks.cancelCurrent();
        setBusy(true, "status.exporting");
        InputFile capturedInput = input;
        DecompilerOptions capturedOptions = options;
        tasks.submit(new TaskManager.Task<>(() -> captureErrors(() -> {
            Files.createDirectories(destination);
            return decompilerService.export(capturedInput, capturedOptions, destination,
                    count -> SwingUtilities.invokeLater(() -> setStatus("status.exportProgress", count)),
                    Thread.currentThread()::isInterrupted);
        }), result -> {
            setBusy(false, "status.exportDone", result.classes());
            result.diagnostics().forEach(logs::error);
            logs.info(I18n.t("log.exported", destination, result.classes()));
            JOptionPane.showMessageDialog(this, I18n.t("dialog.exportDone", result.classes(), destination)
                    + (result.diagnostics().isEmpty() ? "" : I18n.t("dialog.exportDoneDiagnostics")),
                    I18n.t("dialog.exportDoneTitle"), JOptionPane.INFORMATION_MESSAGE);
        }, throwable -> {
            setBusy(false, "status.exportFailed");
            showError("error.exportFailed", throwable);
        }));
    }

    private boolean hasChildren(Path directory) {
        if (!Files.isDirectory(directory)) {
            return true;
        }
        try (var stream = Files.list(directory)) {
            return stream.findAny().isPresent();
        } catch (IOException exception) {
            return true;
        }
    }

    private String removeExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private <T> T captureErrors(TaskManager.Work<T> work) throws Exception {
        PrintStream originalError = System.err;
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        synchronized (System.class) {
            System.setErr(new PrintStream(errorOutput, true, StandardCharsets.UTF_8));
            try {
                return work.get();
            } finally {
                System.setErr(originalError);
                String errors = errorOutput.toString(StandardCharsets.UTF_8).trim();
                if (!errors.isEmpty()) {
                    SwingUtilities.invokeLater(() -> errors.lines().forEach(logs::error));
                }
            }
        }
    }

    private void setBusy(boolean value, String messageKey, Object... messageArguments) {
        busy = value;
        setStatus(messageKey, messageArguments);
        progress.setVisible(value);
        cancelButton.setVisible(value);
        openButton.setEnabled(!value);
        exportButton.setEnabled(!value && input != null);
        settingsButton.setEnabled(!value);
        source.setEnabled(!value);
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

    private void showError(String titleKey, Throwable throwable) {
        String title = I18n.t(titleKey);
        String message = throwable.getMessage() == null ? throwable.toString() : throwable.getMessage();
        logs.error(I18n.t("log.errorMessage", title, message));
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void closeWindow() {
        if (busy && JOptionPane.showConfirmDialog(this,
                I18n.t("dialog.exitConfirm"), I18n.t("dialog.exitTitle"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        closing = true;
        tasks.close();
        dispose();
        System.exit(0);
    }

    private record Opened(InputFile input, ArchiveService.ArchiveContent content) { }
}
