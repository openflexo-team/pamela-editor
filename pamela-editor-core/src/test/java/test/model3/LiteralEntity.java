package test.model3;

import java.util.List;

import org.openflexo.pamela.AccessibleProxyObject;
import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.Setter;

/** All property identifiers are string literals → entity style LITERAL. */
@ModelEntity
public interface LiteralEntity extends AccessibleProxyObject {

	@Getter(value = "name")
	String getName();

	@Setter("name")
	void setName(String name);

	@Getter(value = "items", cardinality = Cardinality.LIST)
	List<String> getItems();

	@Adder("items")
	void addToItems(String item);

	@Remover("items")
	void removeFromItems(String item);
}
