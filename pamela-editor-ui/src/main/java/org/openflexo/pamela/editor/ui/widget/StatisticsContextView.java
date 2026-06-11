package org.openflexo.pamela.editor.ui.widget;

import java.awt.CardLayout;

import javax.swing.JPanel;

import org.openflexo.gina.ApplicationFIBLibrary.ApplicationFIBLibraryImpl;
import org.openflexo.gina.swing.utils.FIBJPanel;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourcePackage;
import org.openflexo.pamela.editor.ui.PamelaEditorFIBController;
import org.openflexo.rm.Resource;
import org.openflexo.rm.ResourceLocator;

/**
 * ContextPanel sub-view that shows statistical counters for a
 * {@link SourceMetaModel} or a {@link SourcePackage} (ui-design.md §19.2).
 *
 * <p>Uses a {@link CardLayout} to switch between the two FIB panels without
 * rebuilding them on each selection change.</p>
 */
@SuppressWarnings("serial")
public class StatisticsContextView extends JPanel {

    private static final String CARD_EMPTY    = "empty";
    private static final String CARD_METAMODEL = "metamodel";
    private static final String CARD_PACKAGE   = "package";

    private static final Resource METAMODEL_FIB =
            ResourceLocator.locateResource("Fib/MetaModelStatisticsContextView.fib");
    private static final Resource PACKAGE_FIB =
            ResourceLocator.locateResource("Fib/PackageStatisticsContextView.fib");

    private final CardLayout cards = new CardLayout();
    private final FIBJPanel<SourceMetaModel>  metaModelPanel;
    private final FIBJPanel<SourcePackage>    packagePanel;

    public StatisticsContextView() {
        setLayout(cards);

        add(new JPanel(), CARD_EMPTY);

        metaModelPanel = new FIBJPanel<SourceMetaModel>(METAMODEL_FIB, null,
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION) {
            @Override public Class<SourceMetaModel> getRepresentedType() { return SourceMetaModel.class; }
            @Override public void delete() {}
        };
        add(metaModelPanel, CARD_METAMODEL);

        packagePanel = new FIBJPanel<SourcePackage>(PACKAGE_FIB, null,
                ApplicationFIBLibraryImpl.instance(),
                PamelaEditorFIBController.EDITOR_LOCALIZATION) {
            @Override public Class<SourcePackage> getRepresentedType() { return SourcePackage.class; }
            @Override public void delete() {}
        };
        add(packagePanel, CARD_PACKAGE);

        cards.show(this, CARD_EMPTY);
    }

    /** Switch to the SourceMetaModel statistics card. */
    public void showFor(SourceMetaModel metaModel) {
        metaModelPanel.setEditedObject(metaModel);
        cards.show(this, CARD_METAMODEL);
    }

    /** Switch to the SourcePackage statistics card. */
    public void showFor(SourcePackage pkg) {
        packagePanel.setEditedObject(pkg);
        cards.show(this, CARD_PACKAGE);
    }

    /** Hide (empty card). */
    public void clear() {
        cards.show(this, CARD_EMPTY);
    }
}
