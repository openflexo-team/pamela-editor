package org.openflexo.pamela.editor.ui.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.openflexo.pamela.editor.model.SourceCompilationUnit;
import org.openflexo.pamela.editor.model.SourceJavaFile;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Central tab view that displays the Java source code of a {@link SourceModelEntity}.
 *
 * <p>Uses {@link RSyntaxTextArea} (3.3.x) for Java syntax highlighting and
 * line numbers.  The view is read-only in the first sprint.</p>
 *
 * <p>When a {@link SourceModelProperty} is selected in the browser, calling
 * {@link #highlightProperty(SourceModelProperty)} highlights the getter (and
 * any setter / adder / remover) methods in the text area and scrolls to the
 * getter.</p>
 */
@SuppressWarnings("serial")
public class SourceCodeView extends JPanel {

    private static final Logger logger =
            Logger.getLogger(SourceCodeView.class.getPackage().getName());

    /** Background colour used for property method highlights. */
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 160); // soft yellow

    private final SourceModelEntity entity;
    private final RSyntaxTextArea textArea;
    private final List<Object> highlightTags = new ArrayList<>();

    public SourceCodeView(SourceModelEntity entity) {
        super(new BorderLayout());
        this.entity = entity;

        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setEditable(false);   // read-only in the first sprint
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabSize(4);

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        loadSource();
    }

    // -------------------------------------------------------------------------
    // Source loading
    // -------------------------------------------------------------------------

    /**
     * Loads the source text from the entity's {@link SourceCompilationUnit}.
     * Falls back to an empty string when the file cannot be read.
     */
    private void loadSource() {
        SourceCompilationUnit cu = entity.getCompilationUnit();
        if (cu == null) {
            textArea.setText("// Source not available");
            return;
        }
        try {
            java.io.File file = cu.getFile();
            if (file == null || !file.exists()) {
                textArea.setText("// File not found: " + cu.getPrimaryTypeName());
                return;
            }
            String content = readFile(file);
            textArea.setText(content);
            textArea.setCaretPosition(0);
        } catch (Exception e) {
            logger.warning("Failed to load source for " + entity.getQualifiedName() + ": " + e.getMessage());
            textArea.setText("// Failed to load source: " + e.getMessage());
        }
    }

    private static String readFile(java.io.File file) throws java.io.IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Highlight API (ui-design.md §6.3)
    // -------------------------------------------------------------------------

    /**
     * Highlights all method declarations that belong to the given property
     * (getter, setter, adder, remover, etc.) and scrolls to the getter.
     *
     * <p>The highlight is approximate: we search for the method name as a
     * plain-text token in the source, find the line, and highlight the full
     * line.  A proper AST-based range lookup is deferred.</p>
     *
     * @param property the property whose methods should be highlighted
     */
    public void highlightProperty(SourceModelProperty property) {
        clearHighlights();
        if (property == null) {
            return;
        }

        String source = textArea.getText();
        if (source == null || source.isEmpty()) {
            return;
        }

        List<String> methodNames = gatherMethodNames(property);
        int firstOffset = -1;

        for (String methodName : methodNames) {
            int offset = findMethodOffset(source, methodName);
            if (offset >= 0) {
                int lineEnd = source.indexOf('\n', offset);
                if (lineEnd < 0) {
                    lineEnd = source.length();
                }
                try {
                    Object tag = textArea.getHighlighter()
                            .addHighlight(offset, lineEnd,
                                    new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(HIGHLIGHT_COLOR));
                    highlightTags.add(tag);
                    if (firstOffset < 0) {
                        firstOffset = offset;
                    }
                } catch (BadLocationException e) {
                    // safe to ignore — the offset was computed from the same string
                }
            }
        }

        if (firstOffset >= 0) {
            scrollToOffset(firstOffset);
        }
    }

    /** Removes all active property highlights. */
    public void clearHighlights() {
        for (Object tag : highlightTags) {
            textArea.getHighlighter().removeHighlight(tag);
        }
        highlightTags.clear();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns the entity displayed by this view. */
    public SourceModelEntity getEntity() {
        return entity;
    }

    /** The tab title — simple name of the entity. */
    public String getTitle() {
        return entity.getSimpleName();
    }

    /**
     * Scrolls the source view to the given 1-based line number and moves the
     * caret there. Called from the Spoon outline view when the user clicks an
     * element (ui-design.md §19.3, decision D6).
     *
     * @param line 1-based line number; values &lt; 1 are ignored
     */
    public void scrollToLine(int line) {
        if (line < 1) {
            return;
        }
        try {
            int offset = textArea.getLineStartOffset(line - 1);
            textArea.setCaretPosition(offset);
            textArea.scrollRectToVisible(textArea.modelToView(offset));
            textArea.requestFocus();
        } catch (BadLocationException e) {
            // line out of range — ignore silently
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Collects all non-null method names declared on the given property.
     */
    private static List<String> gatherMethodNames(SourceModelProperty property) {
        List<String> names = new ArrayList<>();
        addIfNotNull(names, property.getGetterMethodName());
        addIfNotNull(names, property.getSetterMethodName());
        addIfNotNull(names, property.getAdderMethodName());
        addIfNotNull(names, property.getRemoverMethodName());
        addIfNotNull(names, property.getReindexerMethodName());
        addIfNotNull(names, property.getUpdaterMethodName());
        return names;
    }

    private static void addIfNotNull(List<String> list, String s) {
        if (s != null && !s.isEmpty()) {
            list.add(s);
        }
    }

    /**
     * Returns the character offset in {@code source} where a method named
     * {@code methodName} is declared, or {@code -1} if not found.
     *
     * <p>Simple heuristic: looks for {@code " methodName("} or
     * {@code "\t methodName("} patterns.</p>
     */
    private static int findMethodOffset(String source, String methodName) {
        // Search for "methodName(" to find the declaration
        String pattern = methodName + "(";
        int idx = source.indexOf(pattern);
        while (idx >= 0) {
            // Try to find the start of the line
            int lineStart = source.lastIndexOf('\n', idx - 1) + 1;
            return lineStart;
        }
        return -1;
    }

    /** Scrolls the text area to make the given character offset visible. */
    private void scrollToOffset(int offset) {
        try {
            textArea.setCaretPosition(offset);
            textArea.scrollRectToVisible(
                    textArea.modelToView(offset));
        } catch (BadLocationException e) {
            // safe to ignore
        }
    }
}
