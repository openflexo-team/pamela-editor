package test.model4;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;

@ModelEntity
public interface Circle extends AbstractShape {

	@Getter(value = "radius")
	double getRadius();
}
