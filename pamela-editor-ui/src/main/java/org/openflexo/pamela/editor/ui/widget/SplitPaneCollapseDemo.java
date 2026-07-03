/**
 *
 * Copyright (c) 2024-, Openflexo
 *
 * This file is part of Pamela-editor, a component of the software infrastructure
 * developed at Openflexo.
 *
 */

package org.openflexo.pamela.editor.ui.widget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * Standalone visual demo of {@link JSplitPane#setOneTouchExpandable(boolean)} — a plain-Swing
 * (no gina/diana dependency) split pane whose divider carries two small "collapse / expand"
 * triangle buttons. Clicking the collapse triangle drives the bottom pane's height to zero
 * (its own button/label header row stays visible thanks to {@code resizeWeight = 0}); clicking
 * the same spot again (now the opposite triangle) restores the previous divider location.
 *
 * <p>Unlike {@link FlexoCollabsiblePanel} (used today for the validation panel: fixed height,
 * pure show/hide, no user resize), a {@code JSplitPane} keeps the divider user-draggable while
 * expanded — this demo is the "real split pane, collapsible in one click" alternative.</p>
 *
 * <p>Usage: {@code java org.openflexo.pamela.editor.ui.widget.SplitPaneCollapseDemo}, or
 * {@code ./gradlew :pamela-editor-ui:splitPaneDemo}.</p>
 */
public class SplitPaneCollapseDemo {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("JSplitPane one-touch-expandable demo");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new BorderLayout());

			// Main content, the pane that should keep the room.
			JLabel mainContent = new JLabel("Main content (top pane, grows on resize)", JLabel.CENTER);
			mainContent.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

			// Bottom pane — same shape as the validation panel content (a table).
			JTable table = new JTable(4, 3);
			JScrollPane bottom = new JScrollPane(table);
			bottom.setBorder(BorderFactory.createTitledBorder("Bottom pane (draggable AND one-click collapsible)"));

			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainContent, bottom);
			splitPane.setOneTouchExpandable(true); // <-- the built-in one-click collapse/expand triangles
			splitPane.setContinuousLayout(true);
			splitPane.setResizeWeight(1.0); // extra space on frame-resize goes to the top pane
			splitPane.setDividerLocation(260);

			frame.add(splitPane, BorderLayout.CENTER);

			// An EXTERNAL button, e.g. a "View > Show panel" menu item, driving the very same
			// collapse/expand — proves the control does not have to live on the divider itself.
			JButton externalToggle = new JButton("Toggle from outside");
			final int[] lastDividerLocation = { splitPane.getDividerLocation() };
			externalToggle.addActionListener(e -> {
				int max = splitPane.getMaximumDividerLocation();
				boolean isCollapsed = splitPane.getDividerLocation() >= max - splitPane.getDividerSize();
				if (isCollapsed) {
					splitPane.setDividerLocation(lastDividerLocation[0]);
				}
				else {
					lastDividerLocation[0] = splitPane.getDividerLocation();
					splitPane.setDividerLocation(max);
				}
			});
			JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
			toolbar.add(externalToggle);
			frame.add(toolbar, BorderLayout.NORTH);

			frame.setPreferredSize(new Dimension(600, 420));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
