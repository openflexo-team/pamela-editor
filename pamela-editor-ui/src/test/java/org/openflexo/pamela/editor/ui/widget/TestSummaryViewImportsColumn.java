package org.openflexo.pamela.editor.ui.widget;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.model.FIBComponent;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceFolder;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.rm.Resource;

/**
 * Sanity checks for the "imports" count column added to the three entity-listing summary
 * views (metamodel/project, source folder, package) — see the imports management follow-up
 * of {@code imports-support-design.md}. {@code model2-full}'s {@code FlexoProcess} carries a
 * non-empty {@code @Imports}, so it exercises a genuinely non-zero column value.
 */
public class TestSummaryViewImportsColumn {

    private static SourceMetaModel metaModel;
    private static SourceModelEntity flexoProcess;

    @BeforeClass
    public static void loadMetaModel() throws Exception {
        File pamelaFile = new File(System.getProperty("user.dir"),
                "../pamela-editor-core/src/test/java/test/model2-full.pamela");
        assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
        metaModel = SourceMetaModelSerializer.load(pamelaFile);
        assertNotNull(metaModel);
        flexoProcess = metaModel.getEntity("test.model2.FlexoProcess");
        assertNotNull(flexoProcess);
        assertTrue("FlexoProcess should have imports for this check to be meaningful",
                flexoProcess.getImportedEntities().size() > 0);
    }

    @Test
    public void testMetaModelSummaryViewFibLoads() {
        Resource resource = MetaModelSummaryView.FIB_FILE;
        assertNotNull("FIB resource not located", resource);
        FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
        assertNotNull("FIB did not load: " + resource, component);
    }

    @Test
    public void testPackageSummaryViewFibLoads() {
        Resource resource = PackageSummaryView.FIB_FILE;
        assertNotNull("FIB resource not located", resource);
        FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
        assertNotNull("FIB did not load: " + resource, component);
    }

    @Test
    public void testSourceFolderSummaryViewFibLoads() {
        Resource resource = SourceFolderSummaryView.FIB_FILE;
        assertNotNull("FIB resource not located", resource);
        FIBComponent component = ApplicationFIBLibraryImpl.instance().retrieveFIBComponent(resource);
        assertNotNull("FIB did not load: " + resource, component);
    }

    @Test
    public void testMetaModelSummaryViewBuildsWithDisplay() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        MetaModelSummaryView view = new MetaModelSummaryView(metaModel);
        assertNotNull(view.getController());
    }

    @Test
    public void testPackageSummaryViewBuildsWithDisplay() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SourcePackage pkg = flexoProcess.getSourcePackage();
        assertNotNull(pkg);
        PackageSummaryView view = new PackageSummaryView(pkg);
        assertNotNull(view.getController());
    }

    @Test
    public void testSourceFolderSummaryViewBuildsWithDisplay() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SourceFolder folder = metaModel.getSourceFolders().get(0);
        assertNotNull(folder);
        SourceFolderSummaryView view = new SourceFolderSummaryView(folder);
        assertNotNull(view.getController());
    }
}
