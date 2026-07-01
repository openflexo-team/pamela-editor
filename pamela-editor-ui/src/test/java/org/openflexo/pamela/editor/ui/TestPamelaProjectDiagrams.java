/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;

/**
 * Regression test for the bug reported after the source-folders browser work: a newly
 * created diagram did not appear under its {@link PamelaProject} node in the
 * {@code MetaModelBrowser} / {@code DetailedBrowser}.
 *
 * <p>Root cause: {@link PamelaProject} did not implement
 * {@code org.openflexo.toolbox.HasPropertyChangeSupport}, so the browser's per-cell
 * "project.diagrams" children-binding listener — which attaches directly to the
 * {@link PamelaProject} instance — had nothing to listen to. The former workaround (firing
 * {@code "projects"} on the application) only forces a refresh of project nodes that have
 * never been expanded/loaded yet (see {@code FIBBrowserModel.updateSync}); once a project
 * node is loaded (the normal case when the user right-clicks it to add a diagram), that
 * trick is a no-op. The fix is for {@link PamelaProject} to fire {@code "diagrams"} on
 * itself whenever the list changes.</p>
 */
public class TestPamelaProjectDiagrams {

    private PamelaProject newProject() throws Exception {
        File pamelaFile = File.createTempFile("proj", ".pamela");
        pamelaFile.deleteOnExit();
        return new PamelaProject(pamelaFile, null);
    }

    @Test
    public void testAddDiagramFiresDiagramsPropertyChangeOnTheProjectItself() throws Exception {
        PamelaProject project = newProject();
        List<PropertyChangeEvent> events = new ArrayList<>();
        project.getPropertyChangeSupport().addPropertyChangeListener("diagrams", events::add);

        PamelaClassDiagram diagram = project.getDiagramFactory().newDiagram("Overview");
        project.addDiagram(diagram);

        assertEquals(1, events.size());
        assertTrue(project.getDiagrams().contains(diagram));
        assertEquals(project.getDiagrams(), events.get(0).getNewValue());
    }

    @Test
    public void testRemoveDiagramFiresDiagramsPropertyChangeOnTheProjectItself() throws Exception {
        PamelaProject project = newProject();
        PamelaClassDiagram diagram = project.getDiagramFactory().newDiagram("Overview");
        project.addDiagram(diagram);

        List<PropertyChangeEvent> events = new ArrayList<>();
        project.getPropertyChangeSupport().addPropertyChangeListener("diagrams", events::add);

        project.removeDiagram(diagram);

        assertEquals(1, events.size());
        assertTrue(project.getDiagrams().isEmpty());
    }
}
