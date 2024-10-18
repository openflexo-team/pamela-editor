package test.model1;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

@ModelEntity
public interface Foo2 {

	public static final String NAME = "name";

	@Getter(NAME)
	public String getName();

	@Setter(NAME)
	public void setName(String aName);

}
