/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.Error;
import org.openflexo.pamela.editor.model.Information;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.Warning;
import org.openflexo.rm.Resource;

/**
 * Sanity checks for the bottom validation strip (validation-log-panel-design.md): the FIB
 * loads (well-formed XML, bindings resolve against {@link SourceMetaModel}), the per-severity
 * counters partition {@code issuesCount}, and icon resolution covers all three severities.
 * The interactive show/hide and click-to-navigate behaviour is validated by hand.
 */
public class TestValidationPanel {

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
    public void testFibLoads() {
        Resource resource = ValidationPanel.FIB_FILE;
        assertNotNull("FIB resource not located", resource);
        FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
        assertNotNull("FIB did not load: " + resource, component);
    }

    @Test
    public void testHeaderFibLoads() {
        Resource resource = ValidationHeaderView.FIB_FILE;
        assertNotNull("FIB resource not located", resource);
        FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
        assertNotNull("FIB did not load: " + resource, component);
    }

    @Test
    public void testSeverityCountsPartitionIssuesCount() {
        assertEquals(metaModel.getIssuesCount(),
                metaModel.getErrorsCount() + metaModel.getWarningsCount() + metaModel.getInformationCount());
    }

    @Test
    public void testIconResolutionCoversAllSeverities() {
        FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(ValidationPanel.FIB_FILE);
        ValidationFIBController controller = new ValidationFIBController(component);
        assertNotNull(controller.iconForObject(new Error("an error")));
        assertNotNull(controller.iconForObject(new Warning("a warning")));
        assertNotNull(controller.iconForObject(new Information("an info")));
    }

    @Test
    public void testPanelBuildsWithDisplay() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        ValidationPanel panel = new ValidationPanel(metaModel);
        assertNotNull(panel.getController());
        assertTrue(panel.getController() instanceof ValidationFIBController);
    }

    @Test
    public void testHeaderViewBuildsWithDisplay() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        ValidationHeaderView header = new ValidationHeaderView(metaModel);
        assertNotNull(header.getController());
        assertTrue(header.getController() instanceof ValidationFIBController);
    }
}
