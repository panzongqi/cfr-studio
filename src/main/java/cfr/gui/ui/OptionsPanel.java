package cfr.gui.ui;

import cfr.gui.i18n.I18n;
import cfr.gui.service.DecompilerOptions;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class OptionsPanel extends JPanel {
    private final JCheckBox comments = check("options.comments", true);
    private final JCheckBox version = check("options.version", false);
    private final JCheckBox lambdas = check("options.lambdas", true);
    private final JCheckBox innerClasses = check("options.innerClasses", true);
    private final JCheckBox illegalNames = check("options.illegalNames", true);
    private final JCheckBox antiObfuscation = check("options.antiObfuscation", false);
    private final JCheckBox lenient = check("options.lenient", false);
    private final JCheckBox boilerplate = check("options.boilerplate", true);
    private final JCheckBox locations = check("options.locations", false);
    private final JTextField classpath = new JTextField(28);
    private DecompilerOptions options = DecompilerOptions.defaults();

    public OptionsPanel(Frame owner) {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel title = new JLabel(I18n.t("options.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(3, 0, 3, 0);
        for (JCheckBox item : new JCheckBox[] { comments, version, lambdas, innerClasses,
                illegalNames, antiObfuscation, lenient, boilerplate, locations }) {
            grid.add(item, constraints);
            constraints.gridy++;
        }
        constraints.insets = new Insets(14, 0, 4, 0);
        grid.add(new JLabel(I18n.t("options.classpath.label")), constraints);
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        classpath.putClientProperty("JTextField.placeholderText", I18n.t("options.classpath.placeholder"));
        classpath.setToolTipText(I18n.t("options.classpath.tooltip"));
        classpath.getAccessibleContext().setAccessibleName(I18n.t("options.classpath.accessibility"));
        grid.add(classpath, constraints);
        add(grid, BorderLayout.CENTER);
    }

    public void setOptions(DecompilerOptions value) {
        options = value;
        comments.setSelected(value.comments());
        version.setSelected(value.showVersion());
        lambdas.setSelected(value.decodeLambdas());
        innerClasses.setSelected(value.innerClasses());
        illegalNames.setSelected(value.renameIllegalIdentifiers());
        antiObfuscation.setSelected(value.antiObfuscation());
        lenient.setSelected(value.lenient());
        boilerplate.setSelected(value.removeBoilerplate());
        locations.setSelected(value.bytecodeLocations());
        classpath.setText(value.extraClassPath());
    }

    public DecompilerOptions currentOptions() {
        return options;
    }

    public boolean apply() {
        options = DecompilerOptions.builder()
                .comments(comments.isSelected())
                .showVersion(version.isSelected())
                .decodeLambdas(lambdas.isSelected())
                .innerClasses(innerClasses.isSelected())
                .renameIllegalIdentifiers(illegalNames.isSelected())
                .antiObfuscation(antiObfuscation.isSelected())
                .lenient(lenient.isSelected())
                .removeBoilerplate(boilerplate.isSelected())
                .bytecodeLocations(locations.isSelected())
                .extraClassPath(classpath.getText())
                .build();
        return true;
    }

    private static JCheckBox check(String key, boolean selected) {
        String name = I18n.t(key + ".name");
        String description = I18n.t(key + ".desc");
        JCheckBox box = new JCheckBox("<html><b>" + name + "</b><br><span style='color:#57606a'>"
                + description + "</span></html>", selected);
        box.setToolTipText(description);
        box.getAccessibleContext().setAccessibleDescription(description);
        return box;
    }
}
