package completeTest;

import org.openflexo.pamela.annotations.*;
import java.util.List;

import static org.openflexo.pamela.annotations.CloningStrategy.StrategyType.CLONE;
import static org.openflexo.pamela.annotations.Getter.Cardinality.SINGLE;
import static org.openflexo.pamela.annotations.Getter.Cardinality.LIST;
import static org.openflexo.pamela.annotations.ModelEntity.InitPolicy.WARN_IF_NOT_INVOKED;

/**
 * Created by adria on 27/02/2017.
 */
@XMLElement(context = "context", deprecatedXMLTags = "context", namespace = "namespace", primary = true, xmlTag = "xmlTag")
@Modify(forward = "forward", synchWithForward = false)
@Imports({@Import(ImportedModelEntity.class)})
@ImplementationClass(DummyImplementationClass.class)
@ModelEntity(inheritInitializers = true, isAbstract = false, initPolicy = WARN_IF_NOT_INVOKED)
public interface UltimateModelEntity {
    String EMBEDDED = "embedded";
    String EMBEDDEDLIST = "embeddedList";

    @Getter(value = EMBEDDED, cardinality = SINGLE, ignoreType = false, inverse = EMBEDDED)
    @XMLElement(context="context", deprecatedXMLTags = "context", namespace = "namespace", primary = true, xmlTag = "xmlTag")
    @ReturnedValue(EMBEDDED)
    @Embedded(closureConditions = { EMBEDDED }, deletionConditions = { EMBEDDED })
    @CloningStrategy(value = CLONE, factory = "factory", cloneAfterProperty = EMBEDDED)
    @XMLAttribute(xmlTag = "xmlTag", namespace = "namespace")
    EmbeddedModelEntity getEmbedded();

    @Setter(EMBEDDED)
    @PastingPoint
    void setEmbedded(EmbeddedModelEntity entity);

    @Getter(value = EMBEDDEDLIST, cardinality = LIST)
    List<EmbeddedModelEntity> getEmbeddedList();

    @PastingPoint
    @Adder(value = EMBEDDEDLIST)
    void addEmbedded(EmbeddedModelEntity entity);

    @Remover(value = EMBEDDEDLIST)
    void removeEmbedded(EmbeddedModelEntity entity);
}