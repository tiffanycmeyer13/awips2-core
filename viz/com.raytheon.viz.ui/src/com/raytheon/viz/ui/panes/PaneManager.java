/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/

package com.raytheon.viz.ui.panes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.locationtech.jts.geom.Coordinate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.IPane.CanvasType;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.color.IBackgroundColorChangedListener.BGColorMode;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;
import com.raytheon.viz.ui.editor.ISelectedPanesChangedListener;

/**
 * Manages panes. If virtual cursor is not desired, override InputAdapter
 * functions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 07, 2009  830      bgonzale  Initial Creation.
 * Jan 09, 2014  2647     bsteffen  Do not change active editor on focus because
 *                                  that causes problems when switching windows.
 * Feb 11, 2016  5351     bsteffen  Use only visible panes as active panes when
 *                                  possible
 * Apr 20, 2016           mjames    Add two-column configuration.
 * Mar 02, 2017  6153     bsteffen  Reset descriptor on shared maps when
 *                                  removing a pane.
 * Jun 26, 2017  6331     bsteffen  Add null check before reseting shared maps.
 * Feb 14, 2018  6866     njensen   Don't mess with map layers while swapping
 * May 01, 2018  7064     bsteffen  Grab only visible panes for screenshots.
 * Jul 08, 2020  80637    tjensen   Reset display bounds on clear
 * Jun 07, 2021  8453     randerso  Make 2 panel display left/right vs
 *                                  top/bottom
 * Apr 01, 2022  8790     mapeters  Abstract out some functionality to new
 *                                  AbstractPaneManager
 * Apr 22, 2022  8791     mapeters  Abstract out background resource sharing
 * Sep 12, 2022  8792     mapeters  Added new methods for new combo editor,
 *                                  replaced displayPanes list with map to also
 *                                  track LegacyPanes.
 * Oct 12, 2022  8946     mapeters  Added getCanvases(CanvasType)
 * Oct 13, 2022  8955     mapeters  Update to make virtual cursors work for
 *                                  combo editor panes in the D2D side view
 * Mar 06, 2023  9073     njensen   Undo previous change (one line of code from
 *                                  8453) due to user complaints when displaying
 *                                  with 6, 8, 10, etc panes
 * May 11, 2023 2029803   mapeters  adjustPaneLayout is now an override
 *
 * </pre>
 *
 * @author bgonzale
 */
public class PaneManager extends AbstractPaneManager {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PaneManager.class);

    /**
     * Ordered map of main canvases and their associated panes.
     * {@link LinkedMap} used for convenience methods that make it easier to
     * access specific canvas keys.
     */
    protected final LinkedMap<IDisplayPane, LegacyPane> mainCanvasToPaneMap = new LinkedMap<>();

    /** The pane that currently has the active focus */
    protected IDisplayPane activatedPane;

    /** The pane that is currently selected for loads and operations */
    protected Map<String, IDisplayPane> selectedPanes = null;

    /** The pane that is currently used as the basis for the mouse cursor */
    protected IDisplayPane currentMouseHoverPane;

    private int displayedPaneCount = 0;

    protected List<IDisplayPane> lastHandledPanes = null;

    @Override
    public void initializeComponents(IDisplayPaneContainer container,
            Composite comp) {
        mainCanvasToPaneMap.clear();
        displayedPaneCount = 0;
        paneContainer = container;
        GridLayout gl = new GridLayout(0, true);
        gl.horizontalSpacing = 3;
        gl.verticalSpacing = 3;
        gl.marginHeight = 0;
        gl.marginWidth = 0;

        composite = comp;
        composite.setLayout(gl);

        composite.addListener(SWT.Resize, new Listener() {
            private boolean waiting = false;

            @Override
            public void handleEvent(Event event) {
                if (waiting) {
                    return;
                }
                if (!mainCanvasToPaneMap.isEmpty()) {
                    waiting = true;
                    VizApp.runAsync(() -> {
                        adjustPaneLayout();
                        waiting = false;
                    });
                }
            }
        });

        mainCanvasToPaneMap.clear();
    }

    protected void registerHandlers(final IDisplayPane pane) {
        pane.addListener(SWT.MouseUp, inputManager);
        pane.addListener(SWT.MouseDown, inputManager);
        pane.addListener(SWT.MouseMove, inputManager);
        pane.addListener(SWT.MouseWheel, inputManager);
        pane.addListener(SWT.MouseHover, inputManager);
        pane.addListener(SWT.MouseDoubleClick, inputManager);
        pane.addListener(SWT.KeyDown, inputManager);
        pane.addListener(SWT.KeyUp, inputManager);
        pane.addListener(SWT.MenuDetect, inputManager);
        pane.addListener(SWT.MouseExit, inputManager);
        pane.addListener(SWT.MouseEnter, inputManager);

        pane.addListener(SWT.MouseEnter,
                event -> currentMouseHoverPane = activatedPane = pane);
    }

    @Override
    public void setFocus() {
        IDisplayPane pane = getActiveDisplayPane();
        if (pane != null) {
            pane.setFocus();
        }
    }

    @Override
    public void hidePane(IDisplayPane pane) {
        if (pane.isVisible()) {
            VizDisplayPane glPane = (VizDisplayPane) pane;
            --displayedPaneCount;
            pane.setVisible(false);
            ((GridData) glPane.getCanvas().getParent()
                    .getLayoutData()).exclude = true;
            if (pane == getSelectedPane(IMultiPaneEditor.IMAGE_ACTION)) {
                setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
            }
            adjustPaneLayout();
            if (pane == activatedPane) {
                activatedPane = null;
            }
        }
        refresh();
    }

    @Override
    public void showPane(IDisplayPane pane) {
        if (!pane.isVisible()) {
            VizDisplayPane glPane = (VizDisplayPane) pane;
            ++displayedPaneCount;
            pane.setVisible(true);
            ((GridData) glPane.getCanvas().getParent()
                    .getLayoutData()).exclude = false;
            setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
            adjustPaneLayout();
        }
        refresh();
    }

    @Override
    protected void adjustPaneLayout() {
        if (composite == null || composite.isDisposed()) {
            return;
        }

        int[] numRowsColumns = getNumRowsColumns();
        int numRows = numRowsColumns[0];
        int numColums = numRowsColumns[1];
        GridLayout gl = new GridLayout(numColums, true);
        int width = composite.getBounds().width;
        int height = composite.getBounds().height;

        if (numColums > 0 && numRows > 0) {
            gl.horizontalSpacing = width % numColums == 0 ? 2 : 3;
            gl.verticalSpacing = height % numRows == 0 ? 2 : 3;
        }
        gl.marginHeight = 0;
        gl.marginWidth = 0;

        composite.setLayout(gl);
        composite.layout();
    }

    /**
     * Perform a refresh asynchronously
     *
     */
    @Override
    public void refresh() {
        for (IDisplayPane pane : mainCanvasToPaneMap.keySet()) {
            pane.refresh();
        }
    }

    public IRenderableDisplay[] getRenderableDisplays() {
        List<IRenderableDisplay> rDisplays = new ArrayList<>();
        for (IDisplayPane pane : mainCanvasToPaneMap.keySet()) {
            rDisplays.add(pane.getRenderableDisplay());
        }
        return rDisplays.toArray(new IRenderableDisplay[rDisplays.size()]);
    }

    @Override
    public IDisplayPane[] getDisplayPanes() {
        return mainCanvasToPaneMap.keySet().toArray(IDisplayPane[]::new);
    }

    @Override
    public IDisplayPane getActiveDisplayPane() {
        if (activatedPane == null) {
            for (IDisplayPane pane : mainCanvasToPaneMap.keySet()) {
                if (pane.isVisible()) {
                    activatedPane = pane;
                    break;
                }
            }
            if (activatedPane == null && !mainCanvasToPaneMap.isEmpty()) {
                activatedPane = mainCanvasToPaneMap.firstKey();
            }
        }
        return activatedPane;
    }

    @Override
    public IDisplayPane getSelectedPane(String action) {
        return selectedPanes == null ? null : selectedPanes.get(action);
    }

    @Override
    public IDisplayPane[] getSelectedPanes(String action) {
        IDisplayPane pane = getSelectedPane(action);
        if (pane == null && LOAD_ACTION.equals(action)) {
            return getDisplayPanes();
        }
        return new IDisplayPane[] { pane };
    }

    @Override
    public int getNumberofPanes() {
        return mainCanvasToPaneMap.size();
    }

    @Override
    public boolean isSelectedPane(String action, IDisplayPane pane) {
        return getSelectedPane(action) == pane;
    }

    @Override
    public void setSelectedPane(String action, IDisplayPane pane) {
        if (pane != null && !mainCanvasToPaneMap.containsKey(pane)) {
            throw new IllegalArgumentException(
                    "setSelectedPane called with pane not in this IMultiPaneEditor");
        }
        if (selectedPanes == null) {
            selectedPanes = new HashMap<>();
        }
        selectedPanes.put(action, pane);

        for (ISelectedPanesChangedListener listener : listeners) {
            listener.selectedPanesChanged(action, new IDisplayPane[] { pane });
        }

        refresh();
    }

    protected IDisplayPane addPane(IRenderableDisplay renderableDisplay,
            Composite canvasComp) {
        shareBackgroundResources(renderableDisplay,
                mainCanvasToPaneMap.asList());

        VizDisplayPane pane = null;
        try {
            pane = new VizDisplayPane(paneContainer, canvasComp,
                    CanvasType.MAIN, renderableDisplay, true);
            registerHandlers(pane);
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM, "Error adding pane", e);
        }

        if (pane != null) {
            try {
                if (activatedPane == null) {
                    activatedPane = pane;
                }
                ++displayedPaneCount;
                if (!mainCanvasToPaneMap.isEmpty()) {
                    pane.getRenderableDisplay()
                            .setBackgroundColor(mainCanvasToPaneMap.firstKey()
                                    .getRenderableDisplay()
                                    .getBackgroundColor());
                } else if (paneContainer instanceof AbstractEditor) {
                    ((AbstractEditor) paneContainer).getBackgroundColor()
                            .setColor(BGColorMode.EDITOR,
                                    pane.getRenderableDisplay()
                                            .getBackgroundColor());
                }

                if (!mainCanvasToPaneMap.isEmpty()) {
                    pane.getDescriptor().synchronizeTimeMatching(
                            mainCanvasToPaneMap.firstKey().getDescriptor());
                }
                mainCanvasToPaneMap.put(pane, new LegacyPane(pane));
            } catch (Throwable t) {
                statusHandler.handle(Priority.PROBLEM, "Error adding pane", t);
            }
        }

        setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
        adjustPaneLayout();
        return pane;
    }

    @Override
    public IDisplayPane addPane(IRenderableDisplay renderableDisplay) {
        renderableDisplay.getDescriptor().getResourceList()
                .instantiateResources(renderableDisplay.getDescriptor(), true);
        Composite canvasComp = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        canvasComp.setLayout(gl);
        canvasComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        return addPane(renderableDisplay, canvasComp);
    }

    @Override
    public void removePane(IDisplayPane pane) {
        if (!mainCanvasToPaneMap.containsKey(pane)) {
            throw new IllegalArgumentException(
                    "removePane called with pane not in this IDisplayPaneContainer");
        }
        boolean wasVisible = pane.isVisible();
        mainCanvasToPaneMap.remove(pane);
        if (activatedPane == pane && !mainCanvasToPaneMap.isEmpty()) {
            activatedPane = mainCanvasToPaneMap.firstKey();
        } else {
            activatedPane = null;
        }
        if (selectedPanes != null) {

            Iterator<IDisplayPane> it = selectedPanes.values().iterator();
            while (it.hasNext()) {
                if (it.next() == pane) {
                    it.remove();
                }
            }
        }

        // Undo map sharing that was done in addPane
        unshareBackgroundResources(pane, mainCanvasToPaneMap.asList());

        pane.dispose();

        if (wasVisible) {
            --displayedPaneCount;
        }

        if (pane == getSelectedPane(IMultiPaneEditor.IMAGE_ACTION)) {
            setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
        }

        adjustPaneLayout();
    }

    @Override
    public int displayedPaneCount() {
        return displayedPaneCount;
    }

    @Override
    public void clear() {
        while (mainCanvasToPaneMap.size() > 1) {
            removePane(mainCanvasToPaneMap.lastKey());
        }
        IDisplayPane pane = mainCanvasToPaneMap.firstKey();
        showPane(pane);
        pane.getRenderableDisplay().setBounds(composite.getBounds());
        pane.clear();
    }

    public void dispose() {
        activatedPane = null;
        currentMouseHoverPane = null;
        displayedPaneCount = 0;
        selectedPanes = null;
        composite = null;
    }

    /**
     * PaneManager handle mouse move, sets the virtual cursor on the display
     * pane
     */
    @Override
    public boolean handleMouseMove(int x, int y) {
        Coordinate c = translateClick(x, y);

        if (c == null) {
            return false;
        }

        /*
         * getDisplayPanes() doesn't work here for combo editor panes in the D2D
         * side view
         */
        lastHandledPanes = getCanvasesCompatibleWithActive();
        for (IDisplayPane pane : lastHandledPanes) {
            if (currentMouseHoverPane != pane) {
                ((VizDisplayPane) pane).setVirtualCursor(c);
            } else {
                ((VizDisplayPane) pane).setVirtualCursor(null);
            }
        }

        if (displayedPaneCount > 1) {
            refresh();
        }

        return false;
    }

    /**
     * Default calls handle mouse move
     */
    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        handleMouseMove(x, y);
        return false;
    }

    /**
     * Sets the virtual cursor on the display panes to null
     */
    @Override
    public boolean handleMouseExit(Event event) {
        if (lastHandledPanes != null) {
            for (IDisplayPane pane : lastHandledPanes) {
                ((VizDisplayPane) pane).setVirtualCursor(null);
            }
        }
        return false;
    }

    @Override
    public List<IPane> getPanes() {
        return List.copyOf(mainCanvasToPaneMap.values());
    }

    @Override
    public IPane getActivePane() {
        IDisplayPane activeCanvas = getActiveDisplayPane();
        if (activeCanvas != null) {
            IPane activePane = mainCanvasToPaneMap.get(activeCanvas);
            return activePane;
        }
        return null;
    }

    @Override
    public List<IDisplayPane> getCanvasesCompatibleWithActive() {
        return Arrays.asList(getDisplayPanes());
    }

    @Override
    public IDisplayPane[] getCanvases(CanvasType type) {
        if (type == CanvasType.MAIN) {
            return mainCanvasToPaneMap.keySet().toArray(IDisplayPane[]::new);
        }
        return new IDisplayPane[0];
    }
}
