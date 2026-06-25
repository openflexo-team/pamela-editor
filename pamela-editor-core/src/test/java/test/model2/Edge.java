package test.model2;

import java.util.List;

import org.openflexo.pamela.annotations.Adder;
import org.openflexo.pamela.annotations.CloningStrategy;
import org.openflexo.pamela.annotations.Embedded;
import org.openflexo.pamela.annotations.CloningStrategy.StrategyType;
import org.openflexo.pamela.annotations.Getter.Cardinality;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ImplementationClass;
import org.openflexo.pamela.annotations.Initializer;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Operation;
import org.openflexo.pamela.annotations.Parameter;
import org.openflexo.pamela.annotations.PastingPoint;
import org.openflexo.pamela.annotations.Remover;
import org.openflexo.pamela.annotations.ReturnedValue;
import org.openflexo.pamela.annotations.Setter;
import org.openflexo.pamela.annotations.XMLElement;

@ModelEntity(isAbstract = true)
@ImplementationClass(EdgeImpl.class)
public interface Edge extends WKFObject {

	public static final String START_NODE = "startNode";
	public static final String END_NODE = "endNode";

	@Initializer
	public Edge init(@Parameter(START_NODE) AbstractNode start, @Parameter(END_NODE) AbstractNode end);

	@Initializer
	public Edge init(@Parameter(TestModelObject.NAME) String name, @Parameter(START_NODE) AbstractNode start,
			@Parameter(END_NODE) AbstractNode end);

	@Override
	@Getter(PROCESS)
	@ReturnedValue("startNode.process")
	public FlexoProcess getProcess();

	@Getter(value = START_NODE, inverse = AbstractNode.OUTGOING_EDGES)
	@XMLElement(context = "Start")
	@CloningStrategy(StrategyType.IGNORE)
	public AbstractNode getStartNode();

	@Setter(START_NODE)
	public void setStartNode(AbstractNode node);

	@Getter(value = END_NODE, inverse = AbstractNode.INCOMING_EDGES)
	@XMLElement(context = "End")
	@CloningStrategy(StrategyType.IGNORE)
	public AbstractNode getEndNode();

	@Setter(END_NODE)
	public void setEndNode(AbstractNode node);
	
	public void thisMethodIsNotAPamelaMethod();
	
	@Operation
	public void thisMethodIsAnExplicitOperation();
	
	// Could be promoted to a property
	public String getDescription();
	
	// A good candidate for a setter
	public void setDescription(String aDescription);
		
	List<String> getFoos();

	void addToFoos(String node);

	void removeFromFoos(String node);

	public int getNewProperty();
}
