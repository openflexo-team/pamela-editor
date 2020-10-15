package transitiveClosureTest;

import model.PamelaModel;
import org.junit.Assert;
import org.junit.Test;
import project.PamelaProject;

import java.io.File;
import java.util.Objects;

/**
 * Created by adria on 12/02/2017.
 */
public class TransitiveClosureTest {
    private static final String directoryPath = "src/test/java/transitiveClosureTest/resources";

    @Test
    public void transitiveClosureWithEntityOnReturn() throws Exception {
        PamelaModel model = PamelaProject.createModel("transitiveClosureTest.resources.ChildEntity2", new File(directoryPath));
        Assert.assertEquals(5, model.getEntities().size());
        Assert.assertFalse(model.getEntities().stream().anyMatch(entity -> Objects.equals(entity.getCanonicalName(), "transitiveClosureTest.resources.ForeverAloneEntity")));
        Assert.assertTrue(model.getEntities().stream().anyMatch(entity -> Objects.equals(entity.getCanonicalName(), "transitiveClosureTest.resources.ReferencedEntity")));
        Assert.assertTrue(model.getEntities().stream().anyMatch(entity -> Objects.equals(entity.getCanonicalName(), "transitiveClosureTest.resources.ImportedEntity")));
    }
}
