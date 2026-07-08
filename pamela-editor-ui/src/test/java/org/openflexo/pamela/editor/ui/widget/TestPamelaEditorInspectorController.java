package org.openflexo.pamela.editor.ui.widget;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * Sanity check for the {@code Inspectors/PamelaModel} group — in particular the "Implementation
 * class" row added to {@code SourceModelEntity.inspector}
 * ({@code implementation-class-support-design.md §4}). Loads the real inspector group
 * ({@link PamelaEditorInspectorController}) and inspects entities both with and without a
 * current implementation class, exercising the {@code data.implementationClass.simpleName} /
 * {@code enable="data.implementationClass != null"} bindings.
 */
public class TestPamelaEditorInspectorController {

    private static SourceMetaModel metaModel;

    @BeforeClass
    public static void loadMetaModel() throws Exception {
        File pamelaFile = new File(System.getProperty("user.dir"),
                "../pamela-editor-core/src/test/java/test/model2-full.pamela");
        assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
        metaModel = SourceMetaModelSerializer.load(pamelaFile);
        assertNotNull(metaModel);
    }

    @Test
    public void testInspectEntityWithImplementationClass() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SourceModelEntity flexoProcess = metaModel.getEntity("test.model2.FlexoProcess");
        assertNotNull(flexoProcess);
        assertNotNull("FlexoProcess should have an implementation class for this check to be meaningful",
                flexoProcess.getImplementationClass());

        PamelaEditorInspectorController controller = new PamelaEditorInspectorController();
        controller.inspectObject(flexoProcess);
        assertNotNull(controller.getRootPane());
    }

    @Test
    public void testInspectEntityWithoutImplementationClass() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SourceModelEntity abstractNode = metaModel.getEntity("test.model2.AbstractNode");
        assertNotNull(abstractNode);
        assertNull("AbstractNode should have no implementation class for this check to be meaningful",
                abstractNode.getImplementationClass());

        PamelaEditorInspectorController controller = new PamelaEditorInspectorController();
        controller.inspectObject(abstractNode);
        assertNotNull(controller.getRootPane());
    }
}
