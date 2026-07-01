package test.model3;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

/** One literal property, one constant property → entity style MIXED. */
@ModelEntity
public interface MixedEntity extends AccessibleProxyObject {

	public static final String AGE = "age";

	@Getter(value = "name")
	String getName();

	@Setter("name")
	void setName(String name);

	@Getter(value = AGE)
	String getAge();

	@Setter(AGE)
	void setAge(String age);
}
