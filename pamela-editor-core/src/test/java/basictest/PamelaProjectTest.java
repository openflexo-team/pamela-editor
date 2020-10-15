package basictest;

import model.PamelaEntity;
import model.PamelaProperty;
import org.junit.Assert;
import org.junit.Test;
import project.PamelaProject;

import java.io.File;
import java.util.List;

/**
 * Created by adria on 24/01/2017.
 */
public class PamelaProjectTest {
    private static final String BasicPamelaEntity1Path = "src/test/java/basictest/BasicPamelaEntity1.java";

    @Test
    public void createEntityTest() throws Exception {
        PamelaEntity entity = PamelaProject.createEntity(new File(BasicPamelaEntity1Path));

        List<PamelaProperty> properties = entity.getProperties();

        Assert.assertNotNull(properties);
        Assert.assertFalse(properties.isEmpty());
        Assert.assertNotNull(properties.get(0).getGetter());
        Assert.assertNotNull(properties.get(0).getSetter());
    }
}
