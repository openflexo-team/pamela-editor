package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

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

    @Getter(SOURCE_QUALIFIED_NAME)
    String getSourceQualifiedName();

    @Setter(SOURCE_QUALIFIED_NAME)
    void setSourceQualifiedName(String sourceQualifiedName);

    @Getter(TARGET_QUALIFIED_NAME)
    String getTargetQualifiedName();

    @Setter(TARGET_QUALIFIED_NAME)
    void setTargetQualifiedName(String targetQualifiedName);

    /** Label X offset relative to the connector centre. */
    @Getter(value = LABEL_X, defaultValue = "0.0")
    double getLabelX();

    @Setter(LABEL_X)
    void setLabelX(double labelX);

    /** Label Y offset relative to the connector centre. */
    @Getter(value = LABEL_Y, defaultValue = "0.0")
    double getLabelY();

    @Setter(LABEL_Y)
    void setLabelY(double labelY);

    /**
     * The label rendered on the connector, or {@code null} for none. Computed from the
     * resolved model element. Not managed by PAMELA — see {@link ConnectorViewImpl} and
     * the subtype implementations.
     */
    String getLabel();

    /**
     * Whether this view carries data worth persisting (currently: a moved label). The
     * drawing promotes a transient view into the diagram's {@code connectorViews}
     * collection once this becomes {@code true}. Not managed by PAMELA.
     */
    boolean isPersistable();
}
