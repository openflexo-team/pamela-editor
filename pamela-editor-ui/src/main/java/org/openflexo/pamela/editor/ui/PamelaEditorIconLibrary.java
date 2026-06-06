package org.openflexo.pamela.editor.ui;

import javax.swing.ImageIcon;

import org.openflexo.icon.IconMarker;
import org.openflexo.icon.ImageIconResource;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * Graphical resources used throughout the PAMELA editor UI.
 *
 * <p>All constants are either loaded from the classpath via
 * {@link ResourceLocator} or resolved to {@code null} when the icon file is
 * not yet available.  Callers must handle {@code null} icons gracefully.</p>
 */
public class PamelaEditorIconLibrary {

    // -------------------------------------------------------------------------
    // Action icons (pre-existing)
    // -------------------------------------------------------------------------

    public static final ImageIcon DELETE_ICON =
            buildIcon("Icons/Actions/Delete.gif");
    public static final ImageIcon HELP_ICON =
            buildIcon("Icons/Actions/Help.gif");
    public static final ImageIcon REFRESH_ICON =
            buildIcon("Icons/Actions/Refresh.gif");
    public static final ImageIcon INSPECT_ICON =
            buildIcon("Icons/Actions/Inspect.gif");
    public static final ImageIcon COPY_ICON =
            buildIcon("Icons/Actions/Copy.gif");
    public static final ImageIcon CUT_ICON =
            buildIcon("Icons/Actions/Cut.gif");
    public static final ImageIcon PASTE_ICON =
            buildIcon("Icons/Actions/Paste.gif");
    public static final ImageIcon UNDO_ICON =
            buildIcon("Icons/Actions/Undo.gif");
    public static final ImageIcon REDO_ICON =
            buildIcon("Icons/Actions/Redo.gif");

    public static final ImageIcon TOP_ICON =
            buildIcon("Icons/Actions/Top.png");
    public static final ImageIcon BOTTOM_ICON =
            buildIcon("Icons/Actions/Bottom.png");
    public static final ImageIcon UP_ICON =
            buildIcon("Icons/Actions/Up.png");
    public static final ImageIcon DOWN_ICON =
            buildIcon("Icons/Actions/Down.png");

    public static final IconMarker DELETE =
            new IconMarker(buildIcon("Icons/Markers/Delete.png"), 8, 8);
    public static final IconMarker DUPLICATE =
            new IconMarker(buildIcon("Icons/Markers/Plus.png"), 8, 0);

    // -------------------------------------------------------------------------
    // Model element icons
    // -------------------------------------------------------------------------
    // These use existing icons as stand-ins until dedicated model icons are
    // designed.  Replace the path with a proper icon file when available.

    /** Icon for a {@link org.openflexo.pamela.editor.ui.PamelaEditorSession} (project). */
    public static final ImageIcon SESSION_ICON =
            buildIcon("Icons/MetaModel_128x128.png");

    /** Icon for a {@link org.openflexo.pamela.editor.diagram.PamelaClassDiagram}. */
    public static final ImageIcon DIAGRAM_ICON =
            buildIcon("Icons/Network.png");

    /** Icon for a {@link org.openflexo.pamela.editor.model.SourcePackage}. */
    public static final ImageIcon PACKAGE_ICON =
            buildIcon("Icons/Info.png");

    /** Icon for a concrete {@link org.openflexo.pamela.editor.model.SourceModelEntity}. */
    public static final ImageIcon ENTITY_ICON =
            buildIcon("Icons/Actions/OK.gif");

    /** Icon for an abstract {@link org.openflexo.pamela.editor.model.SourceModelEntity}. */
    public static final ImageIcon ABSTRACT_ENTITY_ICON =
            buildIcon("Icons/Actions/Warning.gif");

    /** Icon for a {@link org.openflexo.pamela.editor.model.SourceModelProperty}. */
    public static final ImageIcon PROPERTY_ICON =
            buildIcon("Icons/Actions/Right.gif");

    /** Icon for a {@link org.openflexo.pamela.editor.model.SourceModelInitializer}. */
    public static final ImageIcon INITIALIZER_ICON =
            buildIcon("Icons/Actions/Up.gif");

    /** Icon for a {@link org.openflexo.pamela.editor.model.SourceImplementationClass}. */
    public static final ImageIcon IMPL_CLASS_ICON =
            buildIcon("Icons/Actions/Forward.gif");

    // -------------------------------------------------------------------------
    // Validation icons (pre-existing)
    // -------------------------------------------------------------------------

    public static final ImageIcon FIXABLE_ERROR_ICON =
            buildIcon("Icons/Validation/FixableError.gif");
    public static final ImageIcon UNFIXABLE_ERROR_ICON =
            buildIcon("Icons/Validation/UnfixableError.gif");
    public static final ImageIcon FIXABLE_WARNING_ICON =
            buildIcon("Icons/Validation/FixableWarning.gif");
    public static final ImageIcon UNFIXABLE_WARNING_ICON =
            buildIcon("Icons/Validation/UnfixableWarning.gif");

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Loads an icon from the classpath. Returns {@code null} if the resource
     * cannot be located (prevents NPE on missing icon files).
     */
    private static ImageIcon buildIcon(String path) {
        Resource r = ResourceLocator.locateResource(path);
        if (r == null) {
            return null;
        }
        try {
            return new ImageIconResource(r);
        } catch (Exception e) {
            return null;
        }
    }
}
