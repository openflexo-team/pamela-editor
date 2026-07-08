package org.openflexo.pamela.editor.ui;

import javax.swing.ImageIcon;

import org.openflexo.icon.IconFactory;
import org.openflexo.icon.IconMarker;
import org.openflexo.icon.ImageIconResource;
import org.openflexo.pamela.editor.model.SourceElement;
import org.openflexo.pamela.editor.model.SourceMetaModel;
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

	   public static final ImageIcon APPLICATION_ICON =
	            buildIcon("Icons/PamelaModel_64x64.png");
	 
	
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

    /** Intent markers used to compose action icons (element icon &oplus; marker). */
    public static final IconMarker PLUS = DUPLICATE;
    public static final IconMarker MINUS =
            new IconMarker(buildIcon("Icons/Markers/Minus.png"), 8, 0);
    public static final IconMarker GENERATE =
            new IconMarker(buildIcon("Icons/Markers/Generate.png"), 8, 0);
    public static final IconMarker IMPORT =
            new IconMarker(buildIcon("Icons/Markers/Import.gif"), 8, 0);
    public static final IconMarker REINJECT =
            new IconMarker(buildIcon("Icons/Markers/Reinject.png"), 8, 0);
    public static final IconMarker SYNC =
            new IconMarker(buildIcon("Icons/Markers/Sync.png"), 8, 0);

    /**
     * Severity markers — overlaid bottom-left on a model element's own icon
     * (browser / detailed browser / inspector / diagram) when that element has at
     * least one {@code Error}/{@code Warning} raised against it. See
     * {@code org.openflexo.pamela.editor.model.SourceMetaModel#hasErrors(SourceElement)} /
     * {@code #hasWarnings(SourceElement)}.
     */
    public static final IconMarker ERROR_MARKER =
            new IconMarker(buildIcon("Icons/Markers/Error.gif"), 0, 8);
    public static final IconMarker WARNING_MARKER =
            new IconMarker(buildIcon("Icons/Markers/Warning.gif"), 0, 8);

    // -------------------------------------------------------------------------
    // Model element icons
    // -------------------------------------------------------------------------

    /**
     * Icon for a {@link org.openflexo.pamela.editor.ui.PamelaProject} (project).
     * Amber hexagon with bold "P" — the PAMELA model badge.
     */
    public static final ImageIcon SESSION_ICON =
            buildIcon("Icons/PamelaModel_16x16.png");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.model.SourceMetaModel}.
     * Same image as the session icon — a metamodel is the content of a session.
     */
    public static final ImageIcon METAMODEL_ICON = SESSION_ICON;

    /**
     * Icon for a {@link org.openflexo.pamela.editor.diagram.PamelaClassDiagram}.
     * Reuses the network icon until a dedicated diagram icon is designed.
     */
    public static final ImageIcon DIAGRAM_ICON =
            buildIcon("Icons/ClassDiagram_16x16.png");

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
     */
    public static final ImageIcon INITIALIZER_ICON =
            buildIcon("Icons/Initializer_16x16.png");

    /**
     * Icon for a method
     */
    public static final ImageIcon METHOD_ICON =
            buildIcon("Icons/JavaIcons/methpub_obj.gif");

    /**
     * Icon for a {@link org.openflexo.pamela.editor.model.SourceImplementationClass}.
     * Eclipse JDT-style green rounded square with white "C" — class marker.
     */
    public static final ImageIcon IMPL_CLASS_ICON =
            buildIcon("Icons/Class_16x16.png");

    public static final ImageIcon SHAPE_ICON =
            buildIcon("Icons/ShapeIcon.png");

    public static final ImageIcon CONNECTOR_ICON =
            buildIcon("Icons/ConnectorIcon.png");

 
    // -------------------------------------------------------------------------
    // Spoon outline icons — Eclipse / JDT style
    // -------------------------------------------------------------------------

    // Types
    public static final ImageIcon JAVA_INTERFACE_ICON =
            buildIcon("Icons/JavaIcons/int_obj.gif");
    public static final ImageIcon JAVA_INTERFACE_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/int_default_obj.gif");
    public static final ImageIcon JAVA_CLASS_ICON =
            buildIcon("Icons/JavaIcons/class_obj.gif");
    public static final ImageIcon JAVA_CLASS_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/class_default_obj.gif");
    public static final ImageIcon JAVA_ENUM_ICON =
            buildIcon("Icons/JavaIcons/enum_obj.gif");
    public static final ImageIcon JAVA_ENUM_PRIVATE_ICON =
            buildIcon("Icons/JavaIcons/enum_private_obj.gif");
    public static final ImageIcon JAVA_ENUM_PROTECTED_ICON =
            buildIcon("Icons/JavaIcons/enum_protected_obj.gif");
    public static final ImageIcon JAVA_ENUM_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/enum_default_obj.gif");
    public static final ImageIcon JAVA_ANNOTATION_TYPE_ICON =
            buildIcon("Icons/JavaIcons/int_obj.gif");  // annotation @interface ≈ interface

    // Inner types
    public static final ImageIcon JAVA_INNER_CLASS_PUBLIC_ICON =
            buildIcon("Icons/JavaIcons/innerclass_public_obj.gif");
    public static final ImageIcon JAVA_INNER_CLASS_PROTECTED_ICON =
            buildIcon("Icons/JavaIcons/innerclass_protected_obj.gif");
    public static final ImageIcon JAVA_INNER_CLASS_PRIVATE_ICON =
            buildIcon("Icons/JavaIcons/innerclass_private_obj.gif");
    public static final ImageIcon JAVA_INNER_CLASS_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/innerclass_default_obj.gif");
    public static final ImageIcon JAVA_INNER_INTERFACE_PUBLIC_ICON =
            buildIcon("Icons/JavaIcons/innerinterface_public_obj.gif");
    public static final ImageIcon JAVA_INNER_INTERFACE_PROTECTED_ICON =
            buildIcon("Icons/JavaIcons/innerinterface_protected_obj.gif");
    public static final ImageIcon JAVA_INNER_INTERFACE_PRIVATE_ICON =
            buildIcon("Icons/JavaIcons/innerinterface_private_obj.gif");
    public static final ImageIcon JAVA_INNER_INTERFACE_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/innerinterface_default_obj.gif");

    // Methods
    public static final ImageIcon JAVA_METHOD_PUBLIC_ICON =
            buildIcon("Icons/JavaIcons/methpub_obj.gif");
    public static final ImageIcon JAVA_METHOD_PROTECTED_ICON =
            buildIcon("Icons/JavaIcons/methpro_obj.gif");
    public static final ImageIcon JAVA_METHOD_PRIVATE_ICON =
            buildIcon("Icons/JavaIcons/methpri_obj.gif");
    public static final ImageIcon JAVA_METHOD_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/methdef_obj.gif");

    // Fields
    public static final ImageIcon JAVA_FIELD_PUBLIC_ICON =
            buildIcon("Icons/JavaIcons/field_public_obj.gif");
    public static final ImageIcon JAVA_FIELD_PROTECTED_ICON =
            buildIcon("Icons/JavaIcons/field_protected_obj.gif");
    public static final ImageIcon JAVA_FIELD_PRIVATE_ICON =
            buildIcon("Icons/JavaIcons/field_private_obj.gif");
    public static final ImageIcon JAVA_FIELD_DEFAULT_ICON =
            buildIcon("Icons/JavaIcons/field_default_obj.gif");

    /** Public field in a class (backward-compat alias). */
    public static final ImageIcon FIELD_ICON = JAVA_FIELD_PUBLIC_ICON;

    /** Constructor (uses the public method icon — same visual). */
    public static final ImageIcon CONSTRUCTOR_ICON = JAVA_METHOD_PUBLIC_ICON;

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
    public static final ImageIcon INFO_ICON =
            buildIcon("Icons/Validation/Info.gif");
    public static final ImageIcon FIX_PROPOSAL_ICON =
            buildIcon("Icons/Validation/FixProposal.gif");

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Loads an icon from the classpath. Returns {@code null} if the resource
     * cannot be located (prevents NPE on missing icon files).
     */
    /**
     * Composes a base icon with intent markers (e.g. an entity icon &oplus; a
     * {@code +} marker for "new entity"). Null-safe: returns {@code base} when no
     * marker is given, and {@code null} when {@code base} is {@code null}.
     */
    public static ImageIcon decorate(ImageIcon base, IconMarker... markers) {
        if (base == null) {
            return null;
        }
        if (markers == null || markers.length == 0) {
            return base;
        }
        return IconFactory.getImageIcon(base, markers);
    }

    /**
     * Overlays {@link #ERROR_MARKER} or {@link #WARNING_MARKER} on {@code baseIcon} when
     * {@code element} has at least one such {@code Issue} raised against it (error takes
     * priority over warning). Returns {@code baseIcon} unchanged when the element resolves
     * cleanly. Null-safe (a {@code null} element or a {@code null}/unresolvable metamodel is
     * treated as "no issue").
     */
    public static ImageIcon decorateWithSeverity(ImageIcon baseIcon, SourceElement element) {
        if (element == null) {
            return baseIcon;
        }
        SourceMetaModel metaModel = element.getMetaModel();
        if (metaModel == null) {
            return baseIcon;
        }
        if (metaModel.hasErrors(element)) {
            return decorate(baseIcon, ERROR_MARKER);
        }
        if (metaModel.hasWarnings(element)) {
            return decorate(baseIcon, WARNING_MARKER);
        }
        return baseIcon;
    }

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
