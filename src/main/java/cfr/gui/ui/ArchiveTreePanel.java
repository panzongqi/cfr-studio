package cfr.gui.ui;

import cfr.gui.i18n.I18n;
import cfr.gui.service.ClassEntry;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;

public final class ArchiveTreePanel extends JPanel {
    private final JTextField filter = new JTextField();
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final JTree tree = new JTree(model);
    private final JLabel heading = new JLabel(I18n.t("tree.heading"));
    private final JLabel countLabel = Ui.muted(I18n.t("tree.notOpened"));
    private List<ClassEntry> entries = List.of();
    private final Timer filterTimer;
    private final Consumer<ClassEntry> selectionListener;
    private ClassEntry selected;
    private String fileLabel;

    public ArchiveTreePanel(Consumer<ClassEntry> selectionListener) {
        super(new BorderLayout(0, 8));
        this.selectionListener = selectionListener;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
        setBackground(Ui.PANEL);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));

        filter.putClientProperty("JTextField.placeholderText", I18n.t("tree.filterPlaceholder"));
        filter.setToolTipText(I18n.t("tree.filterTooltip"));
        filter.getAccessibleContext().setAccessibleName(I18n.t("tree.filterAccessibility"));
        filter.setColumns(12);
        JPanel search = new JPanel(new BorderLayout(6, 0));
        search.setOpaque(false);
        search.add(new JLabel(Ui.icon("search")), BorderLayout.WEST);
        search.add(filter);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(search, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ClassTreeRenderer());
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        tree.setToggleClickCount(1);
        tree.getAccessibleContext().setAccessibleName(I18n.t("tree.treeAccessibility"));
        tree.addTreeSelectionListener(event -> {
            Object value = tree.getLastSelectedPathComponent();
            if (value instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof ClassEntry entry) {
                selected = entry;
                selectionListener.accept(entry);
            }
        });
        add(new JScrollPane(tree), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);
        filterTimer = new Timer(250, event -> rebuild());
        filterTimer.setRepeats(false);
        filter.getDocument().addDocumentListener(new SimpleDocumentListener(filterTimer::restart));
    }

    public void setContent(String fileName, List<ClassEntry> entries, int totalClasses) {
        this.entries = entries;
        fileLabel = fileName;
        root.setUserObject(fileName);
        selected = null;
        countLabel.setText(I18n.t("tree.classCount", totalClasses));
        filter.setText("");
        rebuild();
    }

    public void clear() {
        setContent(I18n.t("tree.fileContents"), List.of(), 0);
        fileLabel = null;
        countLabel.setText(I18n.t("tree.notOpened"));
    }

    public void setBusy(boolean busy) {
        tree.setEnabled(!busy);
        filter.setEnabled(!busy);
    }

    public void retranslate() {
        heading.setText(I18n.t("tree.heading"));
        filter.putClientProperty("JTextField.placeholderText", I18n.t("tree.filterPlaceholder"));
        filter.setToolTipText(I18n.t("tree.filterTooltip"));
        filter.getAccessibleContext().setAccessibleName(I18n.t("tree.filterAccessibility"));
        filter.revalidate();
        filter.repaint();
        tree.getAccessibleContext().setAccessibleName(I18n.t("tree.treeAccessibility"));
        if (fileLabel == null) {
            root.setUserObject(I18n.t("tree.fileContents"));
            model.nodeChanged(root);
        }
        refreshCount();
        tree.repaint();
    }

    private void refreshCount() {
        if (fileLabel == null) {
            countLabel.setText(I18n.t("tree.notOpened"));
            return;
        }
        String query = filter.getText().trim().toLowerCase(java.util.Locale.ROOT);
        if (query.isEmpty()) {
            countLabel.setText(I18n.t("tree.classCount", entries.size()));
            return;
        }
        int shown = (int) entries.stream().filter(entry -> entry.displayName()
                .toLowerCase(java.util.Locale.ROOT).contains(query)).count();
        countLabel.setText(I18n.t("tree.filteredCount", shown, entries.size()));
    }

    private void rebuild() {
        root.removeAllChildren();
        String query = filter.getText().trim().toLowerCase(java.util.Locale.ROOT);
        for (ClassEntry entry : entries) {
            if (!query.isEmpty()
                    && !entry.displayName().toLowerCase(java.util.Locale.ROOT).contains(query)) {
                continue;
            }
            addEntry(entry);
        }
        model.reload();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        refreshCount();
    }

    private void addEntry(ClassEntry entry) {
        String[] parts = entry.displayName().split("\\.");
        DefaultMutableTreeNode parent = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            DefaultMutableTreeNode child = null;
            for (int j = 0; j < parent.getChildCount(); j++) {
                var candidate = (DefaultMutableTreeNode) parent.getChildAt(j);
                if (!(candidate.getUserObject() instanceof ClassEntry)
                        && candidate.getUserObject().equals(part)) {
                    child = candidate;
                    break;
                }
            }
            if (child == null) {
                child = new DefaultMutableTreeNode(i == parts.length - 1 ? entry : part);
                parent.add(child);
            }
            parent = child;
        }
    }

    private static final class ClassTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                       boolean expanded, boolean leaf, int row, boolean focus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(
                    tree, value, selected, expanded, leaf, row, focus);
            if (value instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof ClassEntry entry) {
                String name = entry.displayName();
                label.setText(name.substring(name.lastIndexOf('.') + 1));
                label.setIcon(Ui.icon("class"));
                label.setToolTipText(name);
            }
            label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            return label;
        }
    }
}
