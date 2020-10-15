package completeTest;

import model.PamelaEntity;
import org.junit.Before;
import org.junit.Test;
import project.PamelaProject;

import java.io.File;


/**
 * Created by adria on 27/02/2017.
 */
public class ParseAllTest extends UltimateEntityTestBase {
    private static final String UltimateModelEntity = "src/test/java/completeTest/UltimateModelEntity.java";
    private PamelaEntity entity;

    @Before
    public void initialize() throws Exception {
        entity = PamelaProject.createEntity(new File(UltimateModelEntity));
    }

    @Test
    public void checkModel() {
        checkEntityAnnotations(entity);
        checkEmbeddedProperty(entity);
        checkEmbeddedListProperty(entity);
    }

}
