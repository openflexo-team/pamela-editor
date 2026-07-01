package test.model3;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/**
 * Getter references a constant, setter uses a literal → the entity style stays CONSTANT (the getter
 * is authoritative) but a consistency {@code Warning} is fired for the property.
 */
@ModelEntity
public interface InconsistentEntity extends AccessibleProxyObject {

	public static final String LABEL = "label";

	@Getter(value = LABEL)
	String getLabel();

	@Setter("label")
	void setLabel(String label);
}
