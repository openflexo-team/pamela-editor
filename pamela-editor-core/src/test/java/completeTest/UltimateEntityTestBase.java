package completeTest;

import model.PamelaEntity;
import model.PamelaProperty;
import org.openflexo.pamela.annotations.CloningStrategy;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;

import static model.util.Util.enquote;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by adria on 28/02/2017.
 */
public abstract class UltimateEntityTestBase {

    public void checkEntityAnnotations(PamelaEntity entity) {
        assertEquals(enquote("context"), entity.getXmlElement().getContext());
        assertEquals(enquote("context"), entity.getXmlElement().getDeprecatedXMLTags());
        assertEquals(enquote("namespace"), entity.getXmlElement().getNamespace());
        assertEquals(true, entity.getXmlElement().getPrimary());
        assertEquals(enquote("xmlTag"), entity.getXmlElement().getXmlTag());

        assertEquals(enquote("forward"), entity.getModify().getForward());
        assertEquals(false, entity.getModify().getSynchWithForward());

        assertEquals("completeTest.ImportedModelEntity", entity.getImportsAnnotation().getValue().get(0));

        assertEquals("DummyImplementationClass", entity.getImplementationClass().getValue());

        assertEquals(true, entity.getModelEntityAnnotation().getInheritInitializers());
        assertEquals(false, entity.getModelEntityAnnotation().isAbstract());
        assertEquals(ModelEntity.InitPolicy.WARN_IF_NOT_INVOKED, entity.getModelEntityAnnotation().getInitPolicy());
    }

    public void checkEmbeddedProperty(PamelaEntity entity) {
        PamelaProperty embedded = entity.getProperty("EMBEDDED");

        assertEquals("EMBEDDED", embedded.getGetter().getValue());
        assertEquals(Getter.Cardinality.SINGLE, embedded.getGetter().getCardinality());
        assertEquals(false, embedded.getGetter().getIgnoreType());
        assertEquals("EMBEDDED", embedded.getGetter().getInverse());

        assertEquals(enquote("context"), embedded.getXmlElement().getContext());
        assertEquals(enquote("context"), embedded.getXmlElement().getDeprecatedXMLTags());
        assertEquals(enquote("namespace"), embedded.getXmlElement().getNamespace());
        assertEquals(true, embedded.getXmlElement().getPrimary());
        assertEquals(enquote("xmlTag"), embedded.getXmlElement().getXmlTag());

        assertEquals("EMBEDDED", embedded.getReturnedValue().getValue());

        assertEquals("EMBEDDED", embedded.getEmbedded().getClosureConditions().get(0));
        assertEquals("EMBEDDED", embedded.getEmbedded().getDeletionConditions().get(0));

        assertEquals(CloningStrategy.StrategyType.CLONE, embedded.getCloningStrategy().getValue());
        assertEquals(enquote("factory"), embedded.getCloningStrategy().getFactory());
        assertEquals("EMBEDDED", embedded.getCloningStrategy().getCloneAfterProperty());

        assertEquals(enquote("xmlTag"), embedded.getXmlAttribute().getXmlTag());
        assertEquals(enquote("namespace"), embedded.getXmlAttribute().getNamespace());

        assertEquals("EMBEDDED", embedded.getSetter().getValue());
        assertNotNull(embedded.getSetterPastingPoint());
    }

    public void checkEmbeddedListProperty(PamelaEntity entity) {
        PamelaProperty embeddedList = entity.getProperty("EMBEDDEDLIST");

        assertEquals("EMBEDDEDLIST", embeddedList.getGetter().getValue());
        assertEquals(Getter.Cardinality.LIST, embeddedList.getGetter().getCardinality());
        assertEquals("EMBEDDEDLIST", embeddedList.getAdder().getValue());
        assertNotNull(embeddedList.getAdderPastingPoint());
        assertEquals("EMBEDDEDLIST", embeddedList.getRemover().getValue());
    }
}