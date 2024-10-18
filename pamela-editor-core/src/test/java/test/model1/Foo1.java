package test.model1;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

@ModelEntity
public interface Foo1 {

	public static final String FOO2 = "foo2";

	@Getter(value = FOO2)
	public Foo2 getFoo2();

	@Setter(value = FOO2)
	public void setFoo2(Foo2 aFoo2);

}
