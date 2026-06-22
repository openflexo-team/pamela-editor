package org.openflexo.pamela.editor.diagram;

import java.util.List;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.editor.ui.preferences.ConnectorStylePreference;

/**
 * Abstract base for the runtime view of one connector of a {@link PamelaClassDiagram}.
 *
 * <p>A connector view <em>represents</em> a relationship of the source meta-model, the
 * same way an {@link EntityView} represents a {@code SourceModelEntity}:
 * {@link PropertyView} represents a {@code SourceModelProperty} (association / composition),
 * {@link InheritanceView} represents an inheritance link.</p>
 *
 * <p><b>Existence is computed</b>, not persisted: a connector is shown as soon as both of
 * its endpoint entities are present (and resolved) on the diagram — minus, for properties,
 * the source {@link EntityView}'s {@code hiddenProperties} list. Consequently a connector
 * view is, by default, a <em>transient</em> drawable, interned by the drawing for Diana's
 * reconciliation stability.</p>
 *
 * <p><b>Persistence is minimal</b>: a connector view is added to the diagram's
 * {@code connectorViews} collection (and serialized) <em>only</em> when it carries
 * non-default data — currently a moved label ({@link #getLabelX()}/{@link #getLabelY()}
 * different from 0). {@link InheritanceView}s have no such data and are never persisted.</p>
 *
 * <p>The {@code labelX}/{@code labelY} offset is relative to the connector centre (Diana's
 * {@code absoluteTextX}/{@code absoluteTextY}), so it survives moves of the endpoint boxes.</p>
 */
@ModelEntity(isAbstract = true)
@ImplementationClass(ConnectorViewImpl.class)
@Imports({ @Import(PropertyView.class), @Import(InheritanceView.class) })
public interface ConnectorView extends AccessibleProxyObject {

    String SOURCE_QUALIFIED_NAME = "sourceQualifiedName";
    String TARGET_QUALIFIED_NAME = "targetQualifiedName";
    String LABEL_X = "labelX";
    String LABEL_Y = "labelY";
    String STYLE_ID = "styleId";

    @Getter(SOURCE_QUALIFIED_NAME)
    String getSourceQualifiedName();

    @Setter(SOURCE_QUALIFIED_NAME)
    void setSourceQualifiedName(String sourceQualifiedName);

    @Getter(TARGET_QUALIFIED_NAME)
    String getTargetQualifiedName();

    @Setter(TARGET_QUALIFIED_NAME)
    void setTargetQualifiedName(String targetQualifiedName);

    /** Label X offset relative to the connector centre. */
    @Getter(value = LABEL_X, defaultValue = "0.0", ignoreForEquality = true)
    double getLabelX();

    @Setter(LABEL_X)
    void setLabelX(double labelX);

    /** Label Y offset relative to the connector centre. */
    @Getter(value = LABEL_Y, defaultValue = "0.0", ignoreForEquality = true)
    double getLabelY();

    @Setter(LABEL_Y)
    void setLabelY(double labelY);

    /**
     * Reference to the applied {@link ConnectorStylePreference} — its
     * {@link ConnectorStylePreference#getId() id}. Empty (the default) means "use the
     * kind default" ({@link #getDefaultStyleId()}), so unset / legacy connectors keep the
     * standard appearance. This is the only style data persisted on the connector — never the
     * individual graphical properties (those live in the preferences).
     */
    @Getter(value = STYLE_ID, defaultValue = "", ignoreForEquality = true)
    String getStyleId();

    @Setter(STYLE_ID)
    void setStyleId(String styleId);

    /**
     * The label rendered on the connector, or {@code null} for none. Computed from the
     * resolved model element. Not managed by PAMELA — see {@link ConnectorViewImpl} and
     * the subtype implementations.
     */
    String getLabel();

    /**
     * The id of the default style for this connector's kind, taken from the connector-style
     * preferences ({@link InheritanceView} → inheritance default, {@link PropertyView} →
     * association default). Used as the fallback when {@link #getStyleId()} is empty / unknown.
     * Not managed by PAMELA; implemented per subtype.
     */
    String getDefaultStyleId();

    /**
     * The resolved {@link ConnectorStylePreference}: {@link #getStyleId()} looked up in the
     * preferences, falling back to {@link #getDefaultStyleId()}, then to the first available
     * style. May be {@code null} only if no styles are defined at all. Not managed by PAMELA.
     */
    ConnectorStylePreference getStyle();

    /**
     * Sets the applied style (writes its id into {@link #setStyleId(String)}). A {@code null}
     * style clears the reference (→ kind default). Not managed by PAMELA.
     */
    void setStyle(ConnectorStylePreference style);

    /**
     * The styles offered in the graphical inspector drop-down — all styles defined in the
     * connector-style preferences. Not managed by PAMELA.
     */
    List<ConnectorStylePreference> getAvailableStyles();

    /**
     * Whether this view carries data worth persisting (a moved label, or a non-default
     * style). The drawing promotes a transient view into the diagram's {@code connectorViews}
     * collection once this becomes {@code true}. Not managed by PAMELA.
     */
    boolean isPersistable();

    /**
     * Transient back-reference to the diagram that owns/draws this connector (set during the
     * drawing walk, like {@code EntityView.entity}). Used to resolve and capture the diagram's
     * embedded connector styles. Not serialized, not managed by PAMELA.
     */
    PamelaClassDiagram getOwningDiagram();

    void setOwningDiagram(PamelaClassDiagram diagram);
}
