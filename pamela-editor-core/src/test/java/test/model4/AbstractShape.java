package test.model4;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Abstract entity whose concrete subtypes ({@link Circle}, {@link Square}) are never
 * referenced by any property type (every container property is typed on this abstract
 * type) — the only way PAMELA (and the editor) can discover them is via {@code @Imports}.
 * Mirrors {@code ConnectorView} in pamela-editor-ui ({@code diana-analysis.md §21} /
 * {@code ui-design.md §8.6}).
 */
@ModelEntity(isAbstract = true)
@Imports({ @Import(Circle.class), @Import(Square.class) })
public interface AbstractShape extends AccessibleProxyObject {

	@Getter(value = "label")
	String getLabel();
}
