package org.openflexo.pamela.editor.diagram;

import java.util.List;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelInitializer;
import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Represents one entity placed on a {@link PamelaClassDiagram}.
 *
 * <p>The five PAMELA-managed properties ({@code qualifiedName}, {@code x},
 * {@code y}, {@code width}, {@code height}) are persisted in the sidecar
 * {@code .diagram} file.</p>
 *
 * <p>{@code entity} is <em>transient</em>: it is resolved at load time from
 * the {@link org.openflexo.pamela.editor.model.SourceMetaModel} and is never
 * serialized.  It is declared as a plain interface method (no {@code @Getter}
 * annotation) so that PAMELA delegates to the implementation class.</p>
 */
@ModelEntity
@ImplementationClass(EntityViewImpl.class)
public interface EntityView extends AccessibleProxyObject {

    String QUALIFIED_NAME = "qualifiedName";
    String X = "x";
    String Y = "y";
    String WIDTH = "width";
    String HEIGHT = "height";

    @Getter(QUALIFIED_NAME)
    String getQualifiedName();

    @Setter(QUALIFIED_NAME)
    void setQualifiedName(String qualifiedName);

    @Getter(value = X, defaultValue = "100.0")
    double getX();

    @Setter(X)
    void setX(double x);

    @Getter(value = Y, defaultValue = "100.0")
    double getY();

    @Setter(Y)
    void setY(double y);

    @Getter(value = WIDTH, defaultValue = "200.0")
    double getWidth();

    @Setter(WIDTH)
    void setWidth(double width);

    @Getter(value = HEIGHT, defaultValue = "120.0")
    double getHeight();

    @Setter(HEIGHT)
    void setHeight(double height);

    /**
     * Transient reference to the resolved {@link SourceModelEntity}.
     * Not managed by PAMELA — implemented in {@link EntityViewImpl}.
     */
    SourceModelEntity getEntity();

    void setEntity(SourceModelEntity entity);

    /**
     * The label rendered in the UML box: the entity simple name, prefixed with
     * an {@code «abstract»} stereotype line when the entity is abstract.
     * Computed from the resolved {@link #getEntity()}; falls back to the simple
     * part of {@link #getQualifiedName()} when the entity is not yet resolved.
     * Not managed by PAMELA — implemented in {@link EntityViewImpl}.
     */
    String getDisplayLabel();

    /**
     * Single-line display name suitable for browser tree labels.
     * Returns the entity simple name when resolved; the simple part of
     * {@link #getQualifiedName()} otherwise (with a {@code (?)}) suffix).
     * Not managed by PAMELA — implemented in {@link EntityViewImpl}.
     */
    String getEntitySimpleName();

    /**
     * Declared properties of the resolved entity, or an empty list when unresolved.
     * Safe to use in FIB {@code &lt;Children&gt;} bindings.
     * Not managed by PAMELA — implemented in {@link EntityViewImpl}.
     */
    List<SourceModelProperty> getDisplayedProperties();

    /**
     * Initializers of the resolved entity, or an empty list when unresolved.
     * Safe to use in FIB {@code &lt;Children&gt;} bindings.
     * Not managed by PAMELA — implemented in {@link EntityViewImpl}.
     */
    List<SourceModelInitializer> getDisplayedInitializers();
}
