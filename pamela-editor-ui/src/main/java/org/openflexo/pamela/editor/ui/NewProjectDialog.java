package org.openflexo.pamela.editor.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Modal dialog for creating a new PAMELA project.
 *
 * <p>Asks only for a project name and the location of the {@code .pamela} file.
 * Source directories and root type names are added later interactively from the
 * editor's inspector / metamodel browser.</p>
 *
 * <p>After clicking <em>Create</em>, {@link #isConfirmed()} returns {@code true}
 * and the values are available via {@link #getProjectName()} and
 * {@link #getPamelaFile()}.</p>
 */
public class NewProjectDialog extends JDialog {

    private boolean confirmed = false;
    private String projectName;
    private File pamelaFile;

    private final JTextField nameField;
    private final JTextField locationField;
    private final JButton createButton;
    private final JButton cancelButton;

    public NewProjectDialog(Window parent) {
        super(parent, loc("new_project"), ModalityType.APPLICATION_MODAL);

        nameField = new JTextField("MyModel", 30);

        locationField = new JTextField(30);
        locationField.setEditable(false);
        JButton browseButton = new JButton(loc("browse") + "…");
        browseButton.addActionListener(e -> chooseLocation());

        createButton = new JButton(loc("create"));
        cancelButton = new JButton(loc("cancel"));
        createButton.addActionListener(e -> onCreate());
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(createButton);

        // Layout
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints lc = new GridBagConstraints();
        lc.insets = new Insets(6, 4, 6, 4);
        lc.anchor = GridBagConstraints.WEST;

        lc.gridx = 0; lc.gridy = 0; lc.fill = GridBagConstraints.NONE; lc.weightx = 0;
        content.add(new JLabel(loc("project_name") + ":"), lc);
        lc.gridx = 1; lc.fill = GridBagConstraints.HORIZONTAL; lc.weightx = 1;
        content.add(nameField, lc);

        lc.gridx = 0; lc.gridy = 1; lc.fill = GridBagConstraints.NONE; lc.weightx = 0;
        content.add(new JLabel(loc("project_location") + ":"), lc);
        JPanel locationRow = new JPanel(new BorderLayout(4, 0));
        locationRow.add(locationField, BorderLayout.CENTER);
        locationRow.add(browseButton, BorderLayout.EAST);
        lc.gridx = 1; lc.fill = GridBagConstraints.HORIZONTAL; lc.weightx = 1;
        content.add(locationRow, lc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(cancelButton);
        buttons.add(createButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(420, 160));
        setLocationRelativeTo(parent);
    }

    private void chooseLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(loc("choose_project_location"));
        chooser.setFileFilter(new FileNameExtensionFilter(
                loc("pamela_files") + " (*.pamela)", "pamela"));
        chooser.setSelectedFile(new File(nameField.getText().trim() + ".pamela"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().endsWith(".pamela")) {
                f = new File(f.getParentFile(), f.getName() + ".pamela");
            }
            locationField.setText(f.getAbsolutePath());
        }
    }

    private void onCreate() {
        String name     = nameField.getText().trim();
        String location = locationField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    loc("error_name_required"), loc("validation_error"),
                    JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow();
            return;
        }
        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    loc("error_location_required"), loc("validation_error"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        projectName = name;
        pamelaFile  = new File(location);
        confirmed   = true;
        dispose();
    }

    public boolean isConfirmed()   { return confirmed; }
    public String getProjectName() { return projectName; }
    public File getPamelaFile()    { return pamelaFile; }

    private static String loc(String key) {
        return PamelaEditorApplication.PAMELA_EDITOR_LOCALIZATION.localizedForKey(key);
    }
}
