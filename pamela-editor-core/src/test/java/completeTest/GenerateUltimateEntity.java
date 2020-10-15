package completeTest;

import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import model.PamelaEntity;
import model.PamelaProperty;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.annotations.CloningStrategy;
import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import project.PamelaProject;

import java.io.File;

import static model.util.Util.enquote;

/**
 * Created by adria on 28/02/2017.
 */
public class GenerateUltimateEntity extends UltimateEntityTestBase {
    private static final String UltimateModelEntity = "src/test/java/Generated.java";
    private PamelaEntity entity;

    @Before
    public void generateUltimateEntity() throws Exception {
        entity = PamelaProject.createEntity(new File(UltimateModelEntity));

        entity.addXmlElement()
                .setContext(enquote("context"))
                .setDeprecatedXMLTags(enquote("context"))
                .setNamespace(enquote("namespace"))
                .setPrimary(true)
                .setXmlTag(enquote("xmlTag"));

        entity.addModify()
                .setForward(enquote("forward"))
                .setSynchWithForward(false);

        entity.addImports()
                .addImport(ImportedModelEntity.class.getCanonicalName());

        entity.addImplementationClass()
                .setValue(DummyImplementationClass.class.getSimpleName());

        entity.addModelEntityAnnotation()
                .setInheritInitializers(true)
                .setIsAbstract(false)
                .setInitPolicy(ModelEntity.InitPolicy.WARN_IF_NOT_INVOKED);


        PamelaProperty embedded = entity.addProperty("EMBEDDED", new DefaultJavaClass(EmbeddedModelEntity.class.getSimpleName()));
        PamelaProperty embeddedList = entity.addProperty("EMBEDDEDLIST", new DefaultJavaClass("List<EmbeddedModelEntity>"));

        embedded.addGetter()
                .setValue("EMBEDDED")
                .setCardinality(Getter.Cardinality.SINGLE)
                .setIgnoreType(false)
                .setInverse("EMBEDDED");

        embedded.addXmlElement()
                .setContext(enquote("context"))
                .setDeprecatedXMLTags(enquote("context"))
                .setNamespace(enquote("namespace"))
                .setPrimary(true)
                .setXmlTag(enquote("xmlTag"));

        embedded.addReturnedValue()
                .setValue("EMBEDDED");

        embedded.addEmbedded()
                .addClosureCondition("EMBEDDED")
                .addDeletionCondition("EMBEDDED");

        embedded.addCloningStrategy()
                .setValue(CloningStrategy.StrategyType.CLONE)
                .setFactory(enquote("factory"))
                .setCloneAfterProperty("EMBEDDED");

        embedded.addXmlAttribute()
                .setXmlTag(enquote("xmlTag"))
                .setNamespace(enquote("namespace"));

        embedded.addSetter()
                .setValue("EMBEDDED");

        embedded.addSetterPastingPoint();

        embeddedList.addGetter()
                .setValue("EMBEDDEDLIST")
                .setCardinality(Getter.Cardinality.LIST);

        embeddedList.addAdder(new DefaultJavaClass(EmbeddedModelEntity.class.getSimpleName()))
                .setValue("EMBEDDEDLIST");

        embeddedList.addAdderPastingPoint();

        embeddedList.addRemover(new DefaultJavaClass(EmbeddedModelEntity.class.getSimpleName()));

        System.out.println(PamelaProject.serialize(entity));
    }

    @Test
    public void checkGeneratedEntity() {
        checkEntityAnnotations(entity);
        checkEmbeddedProperty(entity);
        checkEmbeddedListProperty(entity);
    }
}
