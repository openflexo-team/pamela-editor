package basictest;

import model.PamelaEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import project.PamelaProject;

import java.io.File;
import java.util.Collections;

/**
 * Created by adria on 17/02/2017.
 */
public class ImportsTest {
    private static final String EntityPath = "src/test/java/basictest/EntityWithImports.java";
    private PamelaEntity entity;

    @Before
    public void initialize() throws Exception {
        entity = PamelaProject.createEntity(new File(EntityPath));
    }

    @Test
    public void importsTest() {
        String importedEntity = entity.getImportsAnnotation().getValue().get(0);
        Assert.assertNotNull(importedEntity);
        System.out.println(importedEntity);
    }

    @Test
    public void setImportsTest() {
        String newImport = "basictest.NewEntity";
        entity.getImportsAnnotation().setValue(Collections.singletonList(newImport));
        Assert.assertEquals(newImport, entity.getImportsAnnotation().getValue().get(0));
        System.out.println(entity.getContainingInterface());
    }

    @Test
    public void addImportTest() {
        String newImport = "basictest.NewEntity";
        entity.getImportsAnnotation().addImport(newImport);
        Assert.assertTrue(entity.getImportsAnnotation().getValue().size() == 2);
        System.out.println(entity.getContainingInterface());
    }

    @Test
    public void removeImportTest() {
        entity.getImportsAnnotation().removeImport("basictest.BasicPamelaEntity1");
        Assert.assertTrue(entity.getImportsAnnotation().getValue().size() == 0);
    }
}
