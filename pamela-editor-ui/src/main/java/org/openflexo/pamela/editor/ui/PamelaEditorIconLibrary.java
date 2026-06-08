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

    /**
     * Icon for a {@link org.openflexo.pamela.editor.ui.PamelaEditorSession} (project).
     * Amber hexagon with bold "P" — the PAMELA model badge.
     */
    public static final ImageIcon SESSION_ICON =
            buildIcon("Icons/PamelaModel_16x16.png");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.diagram.PamelaClassDiagram}.
     * Reuses the network icon until a dedicated diagram icon is designed.
     */
    public static final ImageIcon DIAGRAM_ICON =
            buildIcon("Icons/Network.png");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.model.SourcePackage}.
     * Eclipse JDT-style tan folder with horizontal content lines.
     */
    public static final ImageIcon PACKAGE_ICON =
            buildIcon("Icons/JavaIcons/package_obj.gif");

    public static final ImageIcon SOURCE_FOLDER_ICON =
            buildIcon("Icons/SourceFolder.png");

    /** Icon for a plain Java source file that is not a {@code @ModelEntity}. */
    public static final ImageIcon JAVA_FILE_ICON =
            buildIcon("Icons/JavaIcons/jcu_obj.gif");

    /**
     * Icon for a concrete {@link org.openflexo.pamela.editor.model.SourceModelEntity}.
     * Eclipse JDT-style purple rounded square with white "I" (interface marker).
     */
    public static final ImageIcon ENTITY_ICON =
            buildIcon("Icons/ModelEntity_CU.png");

    /**
     * Icon for an abstract {@link org.openflexo.pamela.editor.model.SourceModelEntity}.
     * Same shape as {@link #ENTITY_ICON} but with a lighter/desaturated purple and
     * an italic "I" to signal the abstract nature of the entity.
     */
    public static final ImageIcon ABSTRACT_ENTITY_ICON =
            buildIcon("Icons/AbstractModelEntity_CU.png");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.model.SourceModelProperty}.
     * Teal rounded square with bold "P" and yellow "@" superscript — the "@"
     * signals that the property is backed by a {@code @Getter} annotation.
     */
    public static final ImageIcon PROPERTY_ICON =
            buildIcon("Icons/Property_16x16.png");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.model.SourceModelInitializer}.
     * Eclipse JDT-style red circle with white play-triangle — method marker.
     */
    public static final ImageIcon INITIALIZER_ICON =
            buildIcon("Icons/Method_16x16.png");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.model.SourceImplementationClass}.
     * Eclipse JDT-style green rounded square with white "C" — class marker.
     */
    public static final ImageIcon IMPL_CLASS_ICON =
            buildIcon("Icons/Class_16x16.png");

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
