package org.openflexo.pamela.editor.diagram;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * Represents one entity placed on a {@link PamelaClassDiagram}.
 *
 * <p>The five PAMELA-managed properties ({@code qualifiedName}, {@code x},
 * {@code y}, {@code width}, {@code height}) are persisted in the sidecar
 * {@code .diagram.json} file.</p>
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
}
