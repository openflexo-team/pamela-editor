/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;

/**
 * Standalone visual demo of the selector widget family (no wiring into the editor application).
 * Loads a {@code .pamela} metamodel and shows one row per selector so the popup / filter / browser
 * behaviour can be validated by hand. See {@code selector-widgets-design.md}.
 *
 * <p>Usage: {@code SelectorWidgetsDemo [path/to/model.pamela]}. With no argument it tries the
 * {@code test/model2-full.pamela} of {@code pamela-editor-core}.</p>
 */
public class SelectorWidgetsDemo {

	public static void main(String[] args) throws Exception {
		File pamelaFile = resolvePamelaFile(args);
		if (pamelaFile == null || !pamelaFile.exists()) {
			System.err.println("No .pamela file found. Pass one as the first argument.");
			return;
		}
		System.out.println("Loading metamodel from " + pamelaFile.getAbsolutePath());
		final SourceMetaModel metaModel = SourceMetaModelSerializer.load(pamelaFile);

		SourceModelEntity anyEntity = firstEntity(metaModel, false, false);
		SourceModelEntity entityWithInitializers = firstEntity(metaModel, true, false);
		SourceModelEntity entityWithOperations = firstEntity(metaModel, false, true);

		final SourceModelEntity entityForInit = entityWithInitializers != null ? entityWithInitializers : anyEntity;
		final SourceModelEntity entityForOps = entityWithOperations != null ? entityWithOperations : anyEntity;
		final SourceModelEntity entityForMethods = anyEntity;

		SwingUtilities.invokeLater(() -> {
			JPanel content = new JPanel(new GridBagLayout());
			content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

			ModelEntitySelector entitySelector = new ModelEntitySelector(null);
			entitySelector.setMetaModel(metaModel);
			addRow(content, 0, "ModelEntitySelector (metamodel)", entitySelector);

			ModelPropertySelector propertySelector = new ModelPropertySelector(null);
			propertySelector.setMetaModel(metaModel);
			addRow(content, 1, "ModelPropertySelector (metamodel)", propertySelector);

			InitializerSelector initializerSelector = new InitializerSelector(null);
			initializerSelector.setEntity(entityForInit);
			addRow(content, 2, "InitializerSelector ("
					+ (entityForInit != null ? entityForInit.getSimpleName() : "no entity") + ")", initializerSelector);

			OperationSelector operationSelector = new OperationSelector(null);
			operationSelector.setEntity(entityForOps);
			addRow(content, 3, "OperationSelector ("
					+ (entityForOps != null ? entityForOps.getSimpleName() : "no entity") + ")", operationSelector);

			JavaClassSelector classSelector = new JavaClassSelector(null);
			classSelector.setMetaModel(metaModel);
			addRow(content, 4, "JavaClassSelector (metamodel)", classSelector);

			JavaMethodSelector methodSelector = new JavaMethodSelector(null);
			methodSelector.setEntity(entityForMethods);
			addRow(content, 5, "JavaMethodSelector ("
					+ (entityForMethods != null ? entityForMethods.getSimpleName() : "no entity") + ")", methodSelector);

			JFrame frame = new JFrame("PAMELA editor — selector widgets demo");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setContentPane(content);
			frame.pack();
			frame.setSize(Math.max(560, frame.getWidth()), frame.getHeight());
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	private static void addRow(JPanel content, int row, String label, Component selector) {
		GridBagConstraints lc = new GridBagConstraints();
		lc.gridx = 0;
		lc.gridy = row;
		lc.anchor = GridBagConstraints.WEST;
		lc.insets = new Insets(6, 6, 6, 12);
		content.add(new JLabel(label), lc);

		GridBagConstraints sc = new GridBagConstraints();
		sc.gridx = 1;
		sc.gridy = row;
		sc.fill = GridBagConstraints.HORIZONTAL;
		sc.weightx = 1.0;
		sc.insets = new Insets(6, 0, 6, 6);
		selector.setPreferredSize(new Dimension(260, 26));
		content.add(selector, sc);
	}

	private static SourceModelEntity firstEntity(SourceMetaModel mm, boolean needInitializers, boolean needOperations) {
		for (SourceModelEntity e : mm.getEntities().values()) {
			if (needInitializers && e.getInitializers().isEmpty()) {
				continue;
			}
			if (needOperations && e.getDeclaredCustomMethods().isEmpty()) {
				continue;
			}
			return e;
		}
		return null;
	}

	private static File resolvePamelaFile(String[] args) {
		if (args.length > 0) {
			return new File(args[0]);
		}
		String[] candidates = {
				"pamela-editor/pamela-editor-core/src/test/java/test/model2-full.pamela",
				"../pamela-editor-core/src/test/java/test/model2-full.pamela",
				"pamela-editor-core/src/test/java/test/model2-full.pamela",
		};
		for (String c : candidates) {
			File f = new File(c);
			if (f.exists()) {
				return f;
			}
		}
		return null;
	}
}
