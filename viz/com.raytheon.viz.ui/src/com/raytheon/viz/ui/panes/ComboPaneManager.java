/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.viz.ui.panes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IInsetMapDisplayPaneContainer;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.IPane.CanvasType;
import com.raytheon.uf.viz.core.IPaneCreator;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener.DisplayChangeType;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
import com.raytheon.uf.viz.core.rsc.IPaneSyncedResource;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.color.IBackgroundColorChangedListener.BGColorMode;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;
import com.raytheon.viz.ui.editor.ISelectedPanesChangedListener;

/**
 *
 * Pane manager implementation that supports different pane types (e.g. map and
 * cross section) alongside each other.
 *
 * This manages {@link IPane} instances, whose implementations contain the
 * type-specific functionality.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Implement IInsetMapDisplayPaneContainer,
 *                                     abstract out background resource sharing
 * Sep 19, 2022 8792       mapeters    Add virtual cursor and new methods for
 *                                     getting panes/canvases, update addPane
 *                                     to do some resource syncing
 * Oct 10, 2022 8946       mapeters    Update map/height scale menus depending
 *                                     on the types of the contained panes
 * Oct 21, 2022 8956       mapeters    Moved background layer sharing into
 *                                     renderable display changed listener so
 *                                     it handles other places that directly
 *                                     call IDisplayPane.setRenderableDisplay
 *
 * </pre>
 *
 * @author mapeters
 */
public class ComboPaneManager extends AbstractPaneManager
        implements IInsetMapDisplayPaneContainer {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ComboPaneManager.class);

    private final List<IPane> panes = new ArrayList<>();

    private final Map<String, IPane> selectedPanes = new HashMap<>();

    private IPane activePane;

    @Override
    public void initializeComponents(IDisplayPaneContainer container,
            Composite comp) {
        panes.clear();
        paneContainer = container;
        paneContainer.addRenderableDisplayChangedListener(
                (changedCanvas, changedRenderableDisplay, type) -> {
                    /*
                     * Share/unshare background layers (e.g. map/graph outline)
                     * with other canvases of the same type
                     */
                    CanvasType canvasType = changedCanvas.getType();

                    List<IDisplayPane> otherCanvases = new ArrayList<>(
                            Arrays.asList(getCanvases(canvasType)));
                    otherCanvases.remove(changedCanvas);

                    if (type == DisplayChangeType.ADD) {
                        shareBackgroundResources(changedRenderableDisplay,
                                otherCanvases);
                    } else if (type == DisplayChangeType.REMOVE) {
                        unshareBackgroundResources(changedCanvas,
                                otherCanvases);
                    } else {
                        throw new UnsupportedOperationException(
                                "Unexpected renderable display change type: "
                                        + type);
                    }
                });

        composite = comp;
        composite.setLayout(new FormLayout());

        composite.addListener(SWT.Resize, new Listener() {
            private boolean waiting = false;

            @Override
            public void handleEvent(Event event) {
                if (waiting) {
                    return;
                }
                if (!panes.isEmpty()) {
                    waiting = true;
                    VizApp.runAsync(() -> {
                        adjustPaneLayout();
                        waiting = false;
                    });
                }
            }
        });
    }

    /**
     * Register the default input event handlers on the given pane.
     *
     * @param pane
     *            the pane to register handlers on
     */
    private void registerHandlers(IPane pane) {
        pane.registerHandlers(inputManager);
        for (IDisplayPane canvas : pane.getCanvasMap().values()) {
            canvas.addListener(SWT.MouseEnter, event -> activePane = pane);
        }
    }

    @Override
    public void setFocus() {
        IPane active = getActivePane();
        if (active == null) {
            return;
        }
        active.setFocus();
    }

    @Override
    public void hidePane(IDisplayPane canvas) {
        hidePane(getPane(canvas));
    }

    private void hidePane(IPane pane) {
        if (pane.isVisible()) {
            pane.setVisible(false);
            if (pane == getSelectedPane(IMultiPaneEditor.IMAGE_ACTION)) {
                setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
            }
            adjustPaneLayout();
            if (pane == activePane) {
                activePane = null;
            }
        }
        refresh();
    }

    @Override
    public void showPane(IDisplayPane canvas) {
        showPane(getPane(canvas));
    }

    private void showPane(IPane pane) {
        if (!pane.isVisible()) {
            pane.setVisible(true);
            setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
            adjustPaneLayout();
        }
        refresh();
    }

    @Override
    public void refresh() {
        for (IPane pane : panes) {
            pane.refresh();
        }
    }

    @Override
    public IDisplayPane[] getDisplayPanes() {
        CanvasType canvasType = activePane != null
                ? activePane.getActiveCanvasType()
                : CanvasType.MAIN;
        return getCanvases(canvasType);
    }

    @Override
    public IDisplayPane getActiveDisplayPane() {
        IPane active = getActivePane();
        if (active == null) {
            return null;
        }
        return active.getCanvas(active.getActiveCanvasType());
    }

    @Override
    public IPane getActivePane() {
        if (activePane == null) {
            for (IPane pane : panes) {
                if (pane.isVisible()) {
                    activePane = pane;
                    break;
                }
            }
            if (activePane == null && !panes.isEmpty()) {
                activePane = panes.get(0);
            }
        }
        return activePane;
    }

    @Override
    public IDisplayPane getSelectedPane(String action) {
        return getMainCanvas(selectedPanes.get(action));
    }

    @Override
    public IDisplayPane[] getSelectedPanes(String action) {
        IDisplayPane pane = getSelectedPane(action);
        if (pane == null && LOAD_ACTION.equals(action)) {
            return getCanvases(CanvasType.MAIN);
        }
        return new IDisplayPane[] { pane };
    }

    @Override
    public int getNumberofPanes() {
        return panes.size();
    }

    @Override
    public boolean isSelectedPane(String action, IDisplayPane canvas) {
        return getSelectedPane(action) == canvas;
    }

    @Override
    public void setSelectedPane(String action, IDisplayPane canvas) {
        IPane pane;
        if (canvas == null) {
            pane = null;
        } else {
            pane = getPane(canvas);
            if (pane == null) {
                throw new IllegalArgumentException(
                        "setSelectedPane called with canvas not in this IMultiPaneEditor");
            }

        }

        selectedPanes.put(action, pane);

        for (ISelectedPanesChangedListener listener : listeners) {
            listener.selectedPanesChanged(action,
                    new IDisplayPane[] { canvas });
        }

        refresh();
    }

    @Override
    public IDisplayPane addPane(IRenderableDisplay renderableDisplay) {
        return addPane(renderableDisplay, -1);
    }

    private IDisplayPane addPane(IRenderableDisplay renderableDisplay,
            int index) {
        IDescriptor descriptor = renderableDisplay.getDescriptor();
        descriptor.getResourceList().instantiateResources(descriptor, true);
        Composite paneComp = new Composite(composite, SWT.NONE);
        /*
         * Use grid layout by default, but panes can change it if need be.
         * Layout data will be set in adjustPaneLayout.
         */
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        paneComp.setLayout(gl);
        return addPane(renderableDisplay, paneComp, index);

    }

    private IDisplayPane addPane(IRenderableDisplay renderableDisplay,
            Composite canvasComp, int index) {
        IPaneCreator paneCreator = DescriptorMap
                .getPaneCreator(renderableDisplay);
        if (paneCreator == null) {
            throw new IllegalArgumentException(
                    "Unable to add pane for unknown descriptor type: "
                            + renderableDisplay.getDescriptor());
        }

        IPane pane = null;
        try {
            pane = paneCreator.createPane(paneContainer, canvasComp,
                    renderableDisplay, panes);
            registerHandlers(pane);
        } catch (VizException e) {
            statusHandler.error("Error adding pane", e);
        }

        IDisplayPane newCanvas = null;
        if (pane != null) {
            try {
                // TODO also bg canvas?
                newCanvas = pane.getMainCanvas();

                if (activePane == null) {
                    activePane = pane;
                }
                if (!panes.isEmpty()) {
                    IDisplayPane existingCanvas = panes.get(0).getMainCanvas();
                    newCanvas.getRenderableDisplay().setBackgroundColor(
                            existingCanvas.getRenderableDisplay()
                                    .getBackgroundColor());
                    newCanvas.getDescriptor().synchronizeTimeMatching(
                            existingCanvas.getDescriptor());
                } else if (paneContainer instanceof AbstractEditor) {
                    ((AbstractEditor) paneContainer).getBackgroundColor()
                            .setColor(BGColorMode.EDITOR,
                                    newCanvas.getRenderableDisplay()
                                            .getBackgroundColor());
                }

                /*
                 * Some resources need to stay in sync across all panes, let
                 * them sync the new pane
                 */
                for (IPane existingPane : panes) {
                    IDescriptor existingDescriptor = existingPane
                            .getMainCanvas().getDescriptor();
                    if (existingDescriptor != null) {
                        ResourceList rl = existingDescriptor.getResourceList();
                        List<IPaneSyncedResource> syncedRscs = rl
                                .getResourcesByTypeAsType(
                                        IPaneSyncedResource.class);
                        for (IPaneSyncedResource rsc : syncedRscs) {
                            rsc.syncPane(pane);
                        }
                    }
                }

                if (index >= 0 && index < panes.size()) {
                    panes.add(index, pane);
                } else {
                    panes.add(pane);
                }

                refreshScaleMenus(newCanvas.getRenderableDisplay());
            } catch (Throwable t) {
                statusHandler.error("Error adding pane", t);
            }
        }

        setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
        adjustPaneLayout();
        return newCanvas;
    }

    @Override
    public void removePane(IDisplayPane canvas) {
        IPane pane = getPane(canvas);
        if (pane == null) {
            throw new IllegalArgumentException(
                    "removePane called with canvas not in this pane container");
        }

        removePane(pane, true);
    }

    private void removePane(IPane pane, boolean layout) {
        panes.remove(pane);
        if (activePane == pane) {
            if (!panes.isEmpty()) {
                activePane = panes.get(0);
            } else {
                activePane = null;
            }
        }
        Iterator<IPane> paneIter = selectedPanes.values().iterator();
        while (paneIter.hasNext()) {
            if (paneIter.next() == pane) {
                paneIter.remove();
            }
        }

        pane.dispose();

        if (layout) {
            adjustPaneLayout();
        }

        refreshScaleMenus(null);
    }

    @Override
    public int displayedPaneCount() {
        return (int) panes.stream().filter(IPane::isVisible).count();
    }

    @Override
    public void clear() {
        while (panes.size() > 1) {
            removePane(panes.get(panes.size() - 1), true);
        }
        IPane pane = panes.get(0);
        showPane(pane);
        pane.getMainCanvas().getRenderableDisplay()
                .setBounds(composite.getBounds());
        pane.clear();
    }

    private IPane getPane(IDisplayPane canvas) {
        for (IPane pane : panes) {
            if (pane.containsCanvas(canvas)) {
                return pane;
            }
        }
        return null;
    }

    private IDisplayPane getMainCanvas(IPane pane) {
        if (pane == null) {
            return null;
        }
        return pane.getMainCanvas();
    }

    /**
     * Replace the pane containing the given canvas with a new pane for given
     * renderable display.
     *
     * @param canvas
     *            a canvas in the pane to replace
     * @param display
     *            the renderable display to initialize the new pane with
     */
    public void replacePane(IDisplayPane canvas, IRenderableDisplay display) {
        IPane pane = getPane(canvas);
        if (pane == null) {
            throw new IllegalArgumentException(
                    "replacePane called with canvas not in this pane container");
        }

        List<String> selectedActions = selectedPanes.entrySet().stream()
                .filter(entry -> entry.getValue() == pane).map(Entry::getKey)
                .collect(Collectors.toList());

        int index = panes.indexOf(pane);
        removePane(pane, false);
        IDisplayPane newCanvas = addPane(display, index);

        // TODO should we retain all, e.g. image?
        for (String selectedAction : selectedActions) {
            setSelectedPane(selectedAction, newCanvas);
        }
    }

    @Override
    public IDisplayPane[] getInsetPanes() {
        return getCanvases(CanvasType.INSET);
    }

    private void adjustPaneLayout() {
        if (composite == null || composite.isDisposed()) {
            return;
        }

        int[] numRowsColumns = getNumRowsColumns();
        int numRows = numRowsColumns[0];
        int numCols = numRowsColumns[1];

        /*
         * Use FormLayout to ensure rows are equal height, which GridLayout
         * doesn't let you do. Some pane types try to grab more space otherwise.
         */
        int cellNum = 0;
        for (IPane pane : panes) {
            if (pane.isVisible()) {
                FormData fd = new FormData();
                int row = cellNum / numCols;
                int col = cellNum % numCols;
                // Fill one of the rows
                fd.top = new FormAttachment(row, numRows, row == 0 ? 0 : 1);
                fd.bottom = new FormAttachment(row + 1, numRows,
                        row == numRows - 1 ? 0 : -1);
                // Fill one of the columns
                fd.left = new FormAttachment(col, numCols, col == 0 ? 0 : 1);
                fd.right = new FormAttachment(col + 1, numCols,
                        col == numCols - 1 ? 0 : -1);
                pane.getComposite().setLayoutData(fd);

                ++cellNum;
            } else {
                FormData fd = new FormData();
                fd.top = new FormAttachment(0);
                fd.bottom = new FormAttachment(0);
                fd.left = new FormAttachment(0);
                fd.right = new FormAttachment(0);
                pane.getComposite().setLayoutData(fd);
            }
        }
        composite.layout();
    }

    @Override
    public IDisplayPane[] getCanvases(CanvasType type) {
        return panes.stream().map(pane -> pane.getCanvas(type))
                .filter(Objects::nonNull).toArray(IDisplayPane[]::new);
    }

    @Override
    public List<IPane> getPanes() {
        return Collections.unmodifiableList(panes);
    }

    @Override
    public boolean handleMouseMove(int x, int y) {
        // Update virtual cursor
        Coordinate c = translateClick(x, y);

        if (c == null) {
            return false;
        }

        IDisplayPane activeCanvas = getActiveDisplayPane();
        boolean needRefresh = false;
        for (IDisplayPane canvas : getCanvasesCompatibleWithActive()) {
            if (canvas != activeCanvas) {
                ((VizDisplayPane) canvas).setVirtualCursor(c);
                needRefresh = true;
            }
        }

        if (needRefresh) {
            refresh();
        }

        return false;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        handleMouseMove(x, y);
        return false;
    }

    @Override
    public boolean handleMouseExit(Event event) {
        // Clear all virtual cursors
        for (IPane pane : getPanes()) {
            for (IDisplayPane canvas : pane.getCanvasMap().values()) {
                ((VizDisplayPane) canvas).setVirtualCursor(null);
            }
        }
        return false;
    }

    /**
     * Get the canvas in the given pane that is the same type as the active
     * canvas, if one exists.
     *
     * @param pane
     *            the pane to check for a compatible canvas
     * @return canvas of active type (or null if none)
     */
    private IDisplayPane getCanvasCompatibleWithActive(IPane pane) {
        if (activePane == pane) {
            return activePane.getActiveCanvas();
        }

        CanvasType activeCanvasType = activePane.getActiveCanvasType();
        IDescriptor origDesc = activePane.getActiveCanvas().getDescriptor();
        IDisplayPane canvas = pane.getCanvas(activeCanvasType);

        if (canvas == null || !origDesc.isCompatible(canvas.getDescriptor())) {
            return null;
        }

        /*
         * Only consider inset canvases compatible if the main canvases are
         * compatible as well.
         */
        if (activeCanvasType != CanvasType.MAIN
                && !activePane.getMainCanvas().getDescriptor()
                        .isCompatible(pane.getMainCanvas().getDescriptor())) {
            return null;
        }

        return canvas;
    }

    @Override
    public List<IDisplayPane> getCanvasesCompatibleWithActive() {
        List<IDisplayPane> canvases = new ArrayList<>();
        for (IPane pane : panes) {
            IDisplayPane canvas = getCanvasCompatibleWithActive(pane);
            if (canvas != null) {
                canvases.add(canvas);
            }
        }
        return canvases;
    }

    private void refreshScaleMenus(IRenderableDisplay newDisplay) {
        IEventBroker eventBroker = VizWorkbenchManager.getInstance()
                .getCurrentWindow().getService(IEventBroker.class);
        eventBroker.send(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC,
                UIEvents.ALL_ELEMENT_ID);

        if (newDisplay != null) {
            VizGlobalsManager.getCurrentInstance().updateUI(paneContainer,
                    newDisplay);
        }
    }
}
