package com.druvu.jconsole.inspector.operations.widgets;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * File picker for {@code {{file}}} or {@code {{file:*.csv,*.json}}}. The returned value is a {@link String} (UTF-8)
 * when the parameter type is {@code String}, or a {@code byte[]} when the parameter type is {@code byte[]}. Falls back
 * to {@code byte[]} for unknown parameter types.
 */
@Slf4j
public final class FileWidget implements ParamWidget {

    private final JPanel panel;
    private final JLabel label;
    private final boolean wantsString;

    private Path picked;
    private byte[] pickedBytes;

    public FileWidget(List<String> globs, Class<?> parameterType, String tooltip) {
        this.wantsString = parameterType == String.class || parameterType == CharSequence.class;
        this.panel = new JPanel(new BorderLayout(4, 0));
        JButton chooseButton = new JButton("Choose file…");
        this.label = new JLabel("(none)");
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        if (tooltip != null && !tooltip.isEmpty()) {
            chooseButton.setToolTipText(tooltip);
        }

        chooseButton.addActionListener(e -> chooseFile(globs));
        panel.add(chooseButton, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
    }

    private void chooseFile(List<String> globs) {
        JFileChooser chooser = new JFileChooser();
        if (!globs.isEmpty()) {
            String[] extensions = globs.stream()
                    .map(g -> g.startsWith("*.") ? g.substring(2) : g)
                    .toArray(String[]::new);
            chooser.setFileFilter(new FileNameExtensionFilter(String.join(", ", globs), extensions));
        }
        if (chooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            byte[] bytes = Files.readAllBytes(path);
            this.picked = path;
            this.pickedBytes = bytes;
            label.setText(path.getFileName() + " (" + bytes.length + " bytes)");
        } catch (IOException ex) {
            log.warn("Could not read picked file {}: {}", path, ex.getMessage());
            JOptionPane.showMessageDialog(
                    panel, "Could not read file: " + ex.getMessage(), "File error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public Object getValue() {
        if (pickedBytes == null) {
            return wantsString ? "" : new byte[0];
        }
        return wantsString ? new String(pickedBytes, StandardCharsets.UTF_8) : pickedBytes;
    }

    /** The path the user picked, or {@code null} if no selection. Used by Phase 2.B persistence. */
    public Path pickedPath() {
        return picked;
    }
}
