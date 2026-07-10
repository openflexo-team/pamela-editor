/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.model.SourceModelProperty;

/**
 * Coverage for {@link RenamePropertyAction}'s dialog-option configuration: the per-accessor
 * rows must be gated by cardinality (SINGLE → setter/updater; LIST → adder/remover/reindexer),
 * so a LIST property never shows a setter row and a SINGLE property never shows an adder row —
 * even when the (invalid) accessor exists in source. Within the relevant roles only the
 * accessors that actually exist are shown. The actual apply lives in
 * {@link org.openflexo.pamela.editor.model.SourceModelProperty#rename} and is covered by
 * {@code TestMutations}.
 */
public class TestRenamePropertyAction {

    private static SourceMetaModel metaModel;

    @BeforeClass
    public static void loadMetaModel() throws Exception {
        File pamelaFile = new File(System.getProperty("user.dir"),
                "../pamela-editor-core/src/test/java/test/model2-full.pamela");
        assertTrue("Test metamodel not found: " + pamelaFile.getAbsolutePath(), pamelaFile.exists());
        metaModel = SourceMetaModelSerializer.load(pamelaFile);
        assertNotNull(metaModel);
    }

    /**
     * {@code AbstractNode.outgoingEdges} is a LIST property that (invalidly) also declares a
     * {@code @Setter} and has no reindexer: the dialog must show the getter + adder + remover
     * rows and hide both the setter (cardinality-inappropriate) and the reindexer (absent).
     */
    @Test
    public void testListPropertyHidesSetterShowsListAccessors() {
        SourceModelEntity node = metaModel.getEntity("test.model2.AbstractNode");
        assertNotNull(node);
        SourceModelProperty outgoing = node.getDeclaredProperties().get("outgoingEdges");
        assertNotNull(outgoing);
        assertTrue("outgoingEdges must be a LIST property", outgoing.isListCardinality());
        assertNotNull("fixture precondition: outgoingEdges has a (invalid) setter",
                outgoing.getSetterMethodName());

        RenamePropertyAction action = new RenamePropertyAction();
        action.prepareDialog(outgoing, null);

        assertTrue("getter row shown", action.getGetterOption().isPresent());
        assertTrue("adder row shown", action.getAdderOption().isPresent());
        assertTrue("remover row shown", action.getRemoverOption().isPresent());
        assertFalse("setter row hidden on a LIST", action.getSetterOption().isPresent());
        assertFalse("updater row hidden on a LIST", action.getUpdaterOption().isPresent());
        assertFalse("reindexer row hidden (no reindexer method)",
                action.getReindexerOption().isPresent());
    }

    /**
     * {@code AbstractNode.masterAnnotation} is a SINGLE property with a setter: the dialog must
     * show the getter + setter rows and hide the adder/remover/reindexer rows.
     */
    @Test
    public void testSinglePropertyShowsSetterHidesListAccessors() {
        SourceModelEntity node = metaModel.getEntity("test.model2.AbstractNode");
        assertNotNull(node);
        SourceModelProperty master = node.getDeclaredProperties().get("masterAnnotation");
        assertNotNull(master);
        assertTrue("masterAnnotation must be a SINGLE property", master.isSingleCardinality());

        RenamePropertyAction action = new RenamePropertyAction();
        action.prepareDialog(master, null);

        assertTrue("getter row shown", action.getGetterOption().isPresent());
        assertTrue("setter row shown", action.getSetterOption().isPresent());
        assertFalse("adder row hidden on a SINGLE", action.getAdderOption().isPresent());
        assertFalse("remover row hidden on a SINGLE", action.getRemoverOption().isPresent());
        assertFalse("reindexer row hidden on a SINGLE", action.getReindexerOption().isPresent());
    }
}
