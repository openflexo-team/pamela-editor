package test.model4;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Import;
import org.openflexo.pamela.annotations.Imports;
import org.openflexo.pamela.annotations.ModelEntity;

/**
 * Root entity exercising the single-element sugar form
 * {@code @Imports(@Import(...))} (no array braces) — verified to be a bare
 * {@code CtAnnotation} value in Spoon, not a {@code CtNewArray}.
 */
@ModelEntity(isAbstract = true)
@Imports(@Import(Circle.class))
public interface SingleImportShape extends AccessibleProxyObject {

	@Getter(value = "label")
	String getLabel();
}
