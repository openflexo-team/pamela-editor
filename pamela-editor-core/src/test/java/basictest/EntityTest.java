package basictest;

import model.PamelaEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import project.PamelaProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by adria on 24/01/2017.
 */
public class EntityTest {
    private static final String BasicPamelaEntity1Path = "src/test/java/basictest/BasicPamelaEntity1.java";
    private PamelaEntity entity;

    @Before
    public void initialize() throws Exception {
        entity = PamelaProject.createEntity(new File(BasicPamelaEntity1Path));
    }

    @Test
    public void implementationClassTest() {
        entity.addImplementationClass()
                .setValue(BasicImpl.class.getCanonicalName());
        Assert.assertNotNull(entity.getImplementationClass());
        System.out.println(PamelaProject.serialize(entity));
    }

    @Test
    public void implementationClassTest2() {
        entity.addImplementationClass()
                .setValue(BasicImpl.class.getSimpleName());
        entity.removeImplementationClass();
        Assert.assertNull(entity.getImplementationClass());
        System.out.println(PamelaProject.serialize(entity));
    }

    @Test
    public void importsTest() {
        List<String> imports = new ArrayList<>();
        imports.add(String.class.getCanonicalName());
        imports.add(Integer.class.getCanonicalName());
        imports.add(Map.class.getCanonicalName());
        entity.addImports().setValue(imports);
        Assert.assertNotNull(entity.getImportsAnnotation());
        System.out.println(PamelaProject.serialize(entity));
    }

    @Test
    public void nullImportsTest() {
        entity.addImports();
        entity.getImportsAnnotation();
        System.out.println(PamelaProject.serialize(entity));
    }

    @Test
    public void emptyImportsTest() {
        entity.addImports();
        entity.getImportsAnnotation();
        System.out.println(PamelaProject.serialize(entity));
    }
}
