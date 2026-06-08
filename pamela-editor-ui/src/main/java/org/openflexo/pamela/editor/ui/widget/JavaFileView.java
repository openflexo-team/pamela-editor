package org.openflexo.pamela.editor.ui.widget;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.openflexo.pamela.editor.model.SourceJavaFile;

/**
 * Read-only source code view for a {@link SourceJavaFile}.
 *
 * <p>Uses RSyntaxTextArea for Java syntax highlighting. The file content is
 * loaded lazily (via {@link SourceJavaFile#getContent()}) the first time this
 * view is constructed — no Spoon parsing occurs.</p>
 */
public class JavaFileView extends JPanel {

    private static final Logger logger = Logger.getLogger(JavaFileView.class.getPackage().getName());

    private final SourceJavaFile javaFile;

    public JavaFileView(SourceJavaFile javaFile) {
        super(new BorderLayout());
        this.javaFile = javaFile;

        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setEditable(false);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabSize(4);

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        try {
            textArea.setText(javaFile.getContent());
            textArea.setCaretPosition(0);
        } catch (Exception e) {
            logger.warning("Failed to load " + javaFile.getFile() + ": " + e.getMessage());
            textArea.setText("// Failed to load source: " + e.getMessage());
        }
    }

    public SourceJavaFile getJavaFile() {
        return javaFile;
    }
}
