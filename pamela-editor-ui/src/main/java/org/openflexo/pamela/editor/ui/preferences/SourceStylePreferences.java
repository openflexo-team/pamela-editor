package org.openflexo.pamela.editor.ui.preferences;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * Source style / templating preferences theme ({@code /generation/style}). Governs the
 * formatting choices the editor applies when it writes Java source, previously hardcoded in
 * {@code SourceCompilationUnit}. See {@code source-style-preferences-design.md}.
 *
 * <p>{@code useTabulations} / {@code tabulationSize} configure the shared Spoon environment
 * used by <b>every</b> AST pretty-print (rename, add property, new entity…), not only new-file
 * creation. {@code blankLineAfterPackage} / {@code blankLineAfterImports} / {@code
 * expandEmptyBody} apply only to the brand-new-entity print path.</p>
 */
@ModelEntity
@ImplementationClass(SourceStylePreferences.SourceStylePreferencesImpl.class)
public interface SourceStylePreferences extends PreferencesNode {

    String USE_TABULATIONS = "useTabulations";
    String TABULATION_SIZE = "tabulationSize";
    String BLANK_LINE_AFTER_PACKAGE = "blankLineAfterPackage";
    String BLANK_LINE_AFTER_IMPORTS = "blankLineAfterImports";
    String EXPAND_EMPTY_BODY = "expandEmptyBody";

    /** Indent with tabs (as opposed to spaces). Applies to every AST pretty-print. */
    @Getter(value = USE_TABULATIONS, defaultValue = "true")
    boolean getUseTabulations();

    @Setter(USE_TABULATIONS)
    void setUseTabulations(boolean useTabulations);

    /** Indentation width in spaces, used only when {@link #getUseTabulations()} is {@code false}. */
    @Getter(value = TABULATION_SIZE, defaultValue = "4")
    int getTabulationSize();

    @Setter(TABULATION_SIZE)
    void setTabulationSize(int tabulationSize);

    /** Blank line after the {@code package} statement of a brand-new entity. */
    @Getter(value = BLANK_LINE_AFTER_PACKAGE, defaultValue = "true")
    boolean getBlankLineAfterPackage();

    @Setter(BLANK_LINE_AFTER_PACKAGE)
    void setBlankLineAfterPackage(boolean blankLineAfterPackage);

    /** Blank line after the last import of a brand-new entity. */
    @Getter(value = BLANK_LINE_AFTER_IMPORTS, defaultValue = "true")
    boolean getBlankLineAfterImports();

    @Setter(BLANK_LINE_AFTER_IMPORTS)
    void setBlankLineAfterImports(boolean blankLineAfterImports);

    /** Expand a brand-new, member-less entity's body onto its own lines instead of {@code "{}"}. */
    @Getter(value = EXPAND_EMPTY_BODY, defaultValue = "true")
    boolean getExpandEmptyBody();

    @Setter(EXPAND_EMPTY_BODY)
    void setExpandEmptyBody(boolean expandEmptyBody);

    abstract class SourceStylePreferencesImpl extends PreferencesNodeImpl implements SourceStylePreferences {
    }
}
