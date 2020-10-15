package basictest;

import model.PamelaEntity;
import model.PamelaProperty;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openflexo.pamela.annotations.Getter;
import project.PamelaProject;

import java.io.File;

/**
 * Created by adria on 23/01/2017.
 */
public class GetterTest {
    private static final String BasicPamelaEntity1Path = "src/test/java/basictest/BasicPamelaEntity1.java";
    private PamelaEntity entity;

    @Before
    public void initialize() throws Exception {
        entity = PamelaProject.createEntity(new File(BasicPamelaEntity1Path));
    }

    @Test
    public void cardinalityTest() {
        PamelaProperty property = entity.getProperties().get(0);
        Assert.assertEquals(Getter.Cardinality.SINGLE, property.getGetter().getCardinality());
        property.getGetter().setCardinality(Getter.Cardinality.LIST);
        Assert.assertEquals(Getter.Cardinality.LIST, property.getGetter().getCardinality());
    }

    @Test
    public void valueTest() {
        PamelaProperty property = entity.getProperties().get(0);
        Assert.assertEquals("MYATTRIBUTE", property.getGetter().getValue());
        property.getGetter().setValue("MYOTHERATTRIBUTE");
        Assert.assertEquals("MYOTHERATTRIBUTE", property.getGetter().getValue());
    }

    @Test
    public void defaultValueTest() {
        PamelaProperty property = entity.getProperties().get(0);
        property.getGetter().setDefaultValue("3");
        Assert.assertEquals("3", property.getGetter().getDefaultValue());
    }

    @Test
    public void inverseTest() {
        PamelaProperty property = entity.getProperties().get(0);
        property.getGetter().setInverse("MYATTRIBUTE");
        Assert.assertEquals("MYATTRIBUTE", property.getGetter().getInverse());
    }

    @Test
    public void isStringConvertableTest() {
        PamelaProperty property = entity.getProperties().get(0);
        property.getGetter().setIsStringConvertable(true);
        Assert.assertEquals(true, property.getGetter().isStringConvertable());
    }

    @Test
    public void ignoreTypeTest() {
        PamelaProperty property = entity.getProperties().get(0);
        property.getGetter().setIgnoreType(true);
        Assert.assertEquals(true, property.getGetter().getIgnoreType());
    }

    @Test
    public void embeddedWithConditionsTest() {
        PamelaProperty property = entity.getProperties().get(0);
        property.addEmbedded().addClosureCondition("MYATTRIBUTE")
                .addDeletionCondition("MYATTRIBUTE");
        System.out.println(entity.getContainingInterface());
        Assert.assertTrue(property.getEmbedded().getClosureConditions().contains("MYATTRIBUTE"));
        Assert.assertTrue(property.getEmbedded().getDeletionConditions().contains("MYATTRIBUTE"));
    }

    @Test
    public void embeddedWithConditionsTest2() {
        PamelaProperty property = entity.getProperties().get(0);
        property.addEmbedded().addClosureCondition("MYATTRIBUTE")
                .addDeletionCondition("MYATTRIBUTE")
                .addDeletionCondition("MYOTHERATTRIBUTE")
                .addDeletionCondition("ATT");
        System.out.println(entity.getContainingInterface());
        Assert.assertTrue(property.getEmbedded().getClosureConditions().contains("MYATTRIBUTE"));
        Assert.assertTrue(property.getEmbedded().getDeletionConditions().contains("ATT"));
    }

    @Test
    public void returnedValueTest() {
        PamelaProperty property = entity.getProperties().get(0);
        property.addReturnedValue().setValue("test");
        Assert.assertTrue(property.getReturnedValue() != null);
        Assert.assertEquals("test", property.getReturnedValue().getValue());
    }
}
