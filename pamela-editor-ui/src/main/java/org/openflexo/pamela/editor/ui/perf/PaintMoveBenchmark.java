package org.openflexo.pamela.editor.ui.perf;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openflexo.diana.Drawing.DrawingTreeNode;
import org.openflexo.diana.Drawing.ShapeNode;
import org.openflexo.diana.geom.DianaPoint;
import org.openflexo.diana.swing.control.SwingToolFactory;
import org.openflexo.diana.swing.paint.DianaPaintManager;
import org.openflexo.diana.swing.view.JDrawingView;
import org.openflexo.pamela.editor.SourceMetaModelSerializer;
import org.openflexo.pamela.editor.diagram.EntityView;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagram;
import org.openflexo.pamela.editor.diagram.PamelaClassDiagramFactory;
import org.openflexo.pamela.editor.model.SourceMetaModel;
import org.openflexo.pamela.editor.model.SourceModelEntity;
import org.openflexo.pamela.editor.ui.diagram.PamelaClassDiagramDrawing;
import org.openflexo.pamela.editor.ui.diagram.DianaDrawingEditor;
import org.openflexo.pamela.factory.EditingContextImpl;

/**
 * Headless-ish reproducible benchmark of an entity-box <em>move</em> on a
 * {@link PamelaClassDiagram}, used to objectively measure where the per-frame
 * paint cost goes (Diana paint-performance investigation).
 *
 * <p>It faithfully replays the interactive move protocol of
 * {@code org.openflexo.diana.control.actions.MoveInfo}:
 * {@code notifyObjectWillMove()} once, then one {@code setLocation(...)} per drag
 * increment, then {@code notifyObjectHasMoved()} — driving a real paint of the
 * {@link JDrawingView} after each increment and reading the
 * {@link DianaPaintManager} diagnostic counters around it.</p>
 *
 * <p>Run with {@code -Ddiana.paintdebug=true} so the counters increment
 * (they are gated behind {@link DianaPaintManager#PAINT_DEBUG}). The discriminant:
 * a healthy move shows {@code dragBlits +1} per frame and {@code liveShapeRenders +0};
 * a move that lost its drag-cache shows {@code liveShapeRenders +N} per frame
 * (N ≈ shapes in the box) and/or {@code bufferRebuilds +k}.</p>
 *
 * <p>Args: {@code [pamelaFile] [neighbours] [frames]}. Defaults:
 * {@code test/model2-full.pamela}, 0 neighbours (single box → no connectors,
 * isolates the box-move cost), 60 frames.</p>
 */
public class PaintMoveBenchmark {

    public static void main(String[] args) throws Exception {
        if (!DianaPaintManager.PAINT_DEBUG) {
            System.err.println("WARNING: -Ddiana.paintdebug=true is NOT set — counters will stay 0. "
                    + "Re-run with the flag for meaningful numbers.");
        }
        File pamelaFile = new File(args.length > 0 ? args[0]
                : "pamela-editor-core/src/test/java/test/model2-full.pamela").getCanonicalFile();
        int neighbours = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int frames = args.length > 2 ? Integer.parseInt(args[2]) : 60;
        String entityName = args.length > 3 ? args[3] : null; // qualified or simple name

        System.out.println("Loading metamodel from " + pamelaFile + " …");
        SourceMetaModel mm = SourceMetaModelSerializer.load(pamelaFile);

        SourceModelEntity richest = null;
        for (SourceModelEntity e : mm.getEntities().values()) {
            if (entityName != null) {
                if (e.getQualifiedName().equals(entityName) || e.getSimpleName().equals(entityName)) {
                    richest = e;
                    break;
                }
                continue;
            }
            if (richest == null || e.getAllProperties().size() > richest.getAllProperties().size()) {
                richest = e;
            }
        }
        if (richest == null) {
            System.err.println("No entity found in metamodel.");
            return;
        }
        final SourceModelEntity entity = richest;
        System.out.println("Richest entity: " + entity.getQualifiedName()
                + " (" + entity.getAllProperties().size() + " properties, "
                + entity.getDeclaredProperties().size() + " declared)");

        SwingUtilities.invokeAndWait(() -> {
            try {
                runBench(mm, entity, neighbours, frames);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        System.out.println("Done.");
        System.exit(0);
    }

    private static void runBench(SourceMetaModel mm, SourceModelEntity entity, int neighbours, int frames)
            throws Exception {

        EditingContextImpl ctx = new EditingContextImpl();
        PamelaClassDiagramFactory factory = new PamelaClassDiagramFactory(ctx);
        PamelaClassDiagram diagram = factory.newDiagram("paint-bench");

        PamelaClassDiagramDrawing drawing = new PamelaClassDiagramDrawing(diagram, mm, factory);
        drawing.init();

        JFrame frame = new JFrame("PaintMoveBenchmark");
        SwingToolFactory toolFactory = new SwingToolFactory(frame);
        DianaDrawingEditor editor = new DianaDrawingEditor(drawing, factory, toolFactory);
        // Faithful to the real app: a soft-selection listener is wired (drives the inspector etc.),
        // so MoveInfo's setSelectedObject(...) fires the selection cascade just like in the GUI.
        editor.setSelectionListener((lead, selection) -> { /* no-op, just exercise the path */ });
        JDrawingView<?> view = editor.getDrawingView();

        frame.setContentPane(new JScrollPane(view));
        frame.setSize(1400, 1000);
        frame.setVisible(true);

        EntityView ev = editor.addEntity(entity, 400, 350);
        // Faithful to the real app: geometry listeners (installDirtyTracking) on the moved view.
        java.beans.PropertyChangeListener dirty = e -> { /* mark-dirty no-op */ };
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.X, dirty);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.Y, dirty);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.WIDTH, dirty);
        ev.getPropertyChangeSupport().addPropertyChangeListener(EntityView.HEIGHT, dirty);

        // Optionally add neighbour entities to introduce computed connectors.
        int added = 0;
        if (neighbours > 0) {
            int gx = 60;
            for (SourceModelEntity other : mm.getEntities().values()) {
                if (other == entity) {
                    continue;
                }
                editor.addEntity(other, gx, 60);
                gx += 240;
                if (++added >= neighbours) {
                    break;
                }
            }
        }
        frame.validate();

        DrawingTreeNode<?, ?> dtn = drawing.shapeNodeForSourceElement(entity);
        if (!(dtn instanceof ShapeNode)) {
            System.err.println("Could not resolve the container ShapeNode for the entity (got " + dtn + ").");
            return;
        }
        @SuppressWarnings("unchecked")
        ShapeNode<EntityView> node = (ShapeNode<EntityView>) dtn;

        int w = Math.max(view.getWidth(), 1400);
        int h = Math.max(view.getHeight(), 1000);

        // Warm up: build the background buffer once.
        paintFull(view, w, h);

        DianaPaintManager pm = view.getPaintManager();
        System.out.println();
        System.out.println("=== Move benchmark: " + frames + " frames, " + added + " neighbour(s) ===");
        System.out.println("box children (subtree shapes): " + countSubtree(node));

        // Drive the move through the REAL interactive path (MoveInfo), exactly as the GUI does:
        // it selects the moved object (IS_SELECTED / focus cascade) and calls setLocation per step.
        // This reproduces what raw notifyObjectWillMove()+setLocation() did NOT.
        org.openflexo.diana.control.actions.MoveInfo mi =
                new org.openflexo.diana.control.actions.MoveInfo(node, editor);
        java.awt.Point startPx = mi.getInitialLocationInDrawingView();
        List<long[]> rows = new ArrayList<>(); // [frameMs, dBuf, dRegion, dBlit, dLive]
        for (int i = 1; i <= frames; i++) {
            // Count the WHOLE step: the mutation (which may invalidate synchronously) + the paint.
            long b0 = pm.getBufferRebuildCount();
            long r0 = pm.getRegionRefreshCount();
            long bl0 = pm.getDragBlitCount();
            long l0 = pm.getLiveRenderCount();
            long tm0 = System.nanoTime();
            mi.moveTo(new java.awt.Point(startPx.x + i, startPx.y + (i % 20)));
            long moveMs = (System.nanoTime() - tm0) / 1_000_000; // model-side re-route cost
            long tp0 = System.nanoTime();
            paintFull(view, w, h);
            long paintMs = (System.nanoTime() - tp0) / 1_000_000; // paint-side cost
            rows.add(new long[] { moveMs + paintMs,
                    pm.getBufferRebuildCount() - b0,
                    pm.getRegionRefreshCount() - r0,
                    pm.getDragBlitCount() - bl0,
                    pm.getLiveRenderCount() - l0,
                    moveMs, paintMs });
        }
        node.notifyObjectHasMoved();

        long sumMs = 0, sumBuf = 0, sumRegion = 0, sumBlit = 0, sumLive = 0, sumMove = 0, sumPaint = 0;
        for (long[] r : rows) {
            sumMs += r[0];
            sumBuf += r[1];
            sumRegion += r[2];
            sumBlit += r[3];
            sumLive += r[4];
            sumMove += r[5];
            sumPaint += r[6];
        }
        int n = rows.size();
        System.out.println();
        System.out.printf("PER-FRAME AVERAGES over %d frames:%n", n);
        System.out.printf("  frame time        : %.1f ms  (moveTo/re-route %.1f + paint %.1f)%n",
                (double) sumMs / n, (double) sumMove / n, (double) sumPaint / n);
        System.out.printf("  bufferRebuilds    : %.2f  (full re-render of every shape — want ~0)%n", (double) sumBuf / n);
        System.out.printf("  regionRefreshes   : %.2f%n", (double) sumRegion / n);
        System.out.printf("  dragBlits         : %.2f  (O(1) blit of the box — want ~1)%n", (double) sumBlit / n);
        System.out.printf("  liveShapeRenders  : %.2f  (live re-render of box shapes — want ~0)%n", (double) sumLive / n);
        System.out.println();
        System.out.println("MOVE VERDICT: " + verdict(sumBlit, sumLive, sumBuf, sumRegion, n));

        // ---- RESIZE phase ------------------------------------------------------
        // A resize cannot reuse the move drag-cache (the box appearance changes), and it
        // reflows the child BoxLayoutManagers on every step. Measure that path separately.
        paintFull(view, w, h); // settle buffer after the move
        System.out.println();
        System.out.println("=== Resize benchmark: " + frames + " frames ===");
        org.openflexo.diana.ShapeGraphicalRepresentation gr = node.getGraphicalRepresentation();
        double w0 = gr.getWidth();
        double h0 = gr.getHeight();
        List<long[]> rrows = new ArrayList<>();
        node.notifyObjectWillResize();
        try {
            for (int i = 1; i <= frames; i++) {
                long b0 = pm.getBufferRebuildCount();
                long r0 = pm.getRegionRefreshCount();
                long bl0 = pm.getDragBlitCount();
                long l0 = pm.getLiveRenderCount();
                long t0 = System.nanoTime();
                gr.setWidth(w0 + i);
                gr.setHeight(h0 + (i % 20));
                paintFull(view, w, h);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                rrows.add(new long[] { ms,
                        pm.getBufferRebuildCount() - b0,
                        pm.getRegionRefreshCount() - r0,
                        pm.getDragBlitCount() - bl0,
                        pm.getLiveRenderCount() - l0 });
            }
        } finally {
            node.notifyObjectHasResized();
        }
        long rMs = 0, rBuf = 0, rRegion = 0, rBlit = 0, rLive = 0;
        for (long[] r : rrows) {
            rMs += r[0]; rBuf += r[1]; rRegion += r[2]; rBlit += r[3]; rLive += r[4];
        }
        int rn = rrows.size();
        System.out.println();
        System.out.printf("PER-FRAME AVERAGES over %d frames:%n", rn);
        System.out.printf("  frame time        : %.1f ms%n", (double) rMs / rn);
        System.out.printf("  bufferRebuilds    : %.2f%n", (double) rBuf / rn);
        System.out.printf("  regionRefreshes   : %.2f%n", (double) rRegion / rn);
        System.out.printf("  dragBlits         : %.2f%n", (double) rBlit / rn);
        System.out.printf("  liveShapeRenders  : %.2f  (box shapes re-rendered live each frame)%n", (double) rLive / rn);

        frame.dispose();
    }

    private static String verdict(long blit, long live, long buf, int n) {
        return verdict(blit, live, buf, 0, n);
    }

    private static String verdict(long blit, long live, long buf, long region, int n) {
        if (live / (double) n > 1.0) {
            return "DRAG-CACHE NOT EFFECTIVE — each frame live-renders the box subtree (cost ∝ #properties). "
                    + "The capture was discarded mid-move. This is the bottleneck to fix.";
        }
        if (buf / (double) n > 0.1) {
            return "BUFFER REBUILT during the move — full re-render of the whole drawing per frame.";
        }
        if (region / (double) n > 1.5) {
            return "REGION-REFRESH STORM — the box is blitted fine, but attached connectors trigger many "
                    + "box-sized region refreshes per frame (self-connectors / duplicated connectors). "
                    + "This is the bottleneck for richly-connected entities.";
        }
        if (blit / (double) n >= 0.5) {
            return "HEALTHY — move is O(1) blit per frame, independent of #properties.";
        }
        return "INCONCLUSIVE — neither blit nor live render dominated; inspect raw counters above.";
    }

    private static int countSubtree(DrawingTreeNode<?, ?> node) {
        int c = 1;
        if (node instanceof org.openflexo.diana.Drawing.ContainerNode) {
            List<? extends DrawingTreeNode<?, ?>> children =
                    ((org.openflexo.diana.Drawing.ContainerNode<?, ?>) node).getChildNodes();
            if (children != null) {
                for (DrawingTreeNode<?, ?> ch : children) {
                    c += countSubtree(ch);
                }
            }
        }
        return c;
    }

    // Reused across frames so the benchmark does not allocate (and GC) a multi-MB image per
    // frame — that allocation noise otherwise dwarfs the actual paint cost being measured.
    private static BufferedImage benchImg;

    private static void paintFull(JDrawingView<?> view, int w, int h) {
        if (benchImg == null || benchImg.getWidth() != w || benchImg.getHeight() != h) {
            benchImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g = benchImg.createGraphics();
        try {
            g.setClip(0, 0, w, h); // a non-null clip is required for the buffer render path
            view.paint(g);
        } finally {
            g.dispose();
        }
    }
}
