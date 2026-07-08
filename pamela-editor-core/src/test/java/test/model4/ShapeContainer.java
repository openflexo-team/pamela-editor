package test.model4;

import java.util.List;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Remover;

/**
 * Root entity: its only entity-typed property is {@code List<AbstractShape>}, so a BFS that
 * does not follow {@code @Imports} never reaches {@link Circle} / {@link Square}.
 */
@ModelEntity
public interface ShapeContainer extends AccessibleProxyObject {

	@Getter(value = "shapes", cardinality = Cardinality.LIST)
	List<AbstractShape> getShapes();

	@Adder("shapes")
	void addToShapes(AbstractShape shape);

	@Remover("shapes")
	void removeFromShapes(AbstractShape shape);
}
