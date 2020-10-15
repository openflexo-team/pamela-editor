package basictest;

import model.PamelaEntity;
import model.PamelaProperty;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import project.PamelaProject;

import java.io.File;

/**
 * Created by adria on 23/01/2017.
 */
public class SetterTest {
    private static final String BasicPamelaEntity1Path = "src/test/java/basictest/BasicPamelaEntity1.java";
    private PamelaEntity entity;

    @Before
    public void initialize() throws Exception {
        entity = PamelaProject.createEntity(new File(BasicPamelaEntity1Path));
    }

    @Test
    public void valueTest() {
        PamelaProperty property = entity.getProperties().get(0);
        Assert.assertEquals("MYATTRIBUTE", property.getSetter().getValue());
        property.getSetter().setValue("MYOTHERATTRIBUTE");
        Assert.assertEquals("MYOTHERATTRIBUTE", property.getSetter().getValue());
    }
}
