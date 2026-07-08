package test.model4;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;

@ModelEntity
public interface Square extends AbstractShape {

	@Getter(value = "side")
	double getSide();
}
