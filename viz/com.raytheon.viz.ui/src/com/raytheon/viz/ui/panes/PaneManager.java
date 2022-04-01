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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.viz.ui.color.IBackgroundColorChangedListener.BGColorMode;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;
import com.raytheon.viz.ui.editor.ISelectedPanesChangedListener;

/**
 * Manages panes. If virtual cursor is not desired, override InputAdapter
 * functions
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
 *
 * </pre>
 *
 * @author bgonzale
 */
public class PaneManager extends AbstractPaneManager {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PaneManager.class);

    /** The display pane */
    protected final List<VizDisplayPane> displayPanes = new ArrayList<>();

    /** The pane that currently has the active focus */
    protected IDisplayPane activatedPane;

    /** The pane that is currently selected for loads and operations */
    protected Map<String, IDisplayPane> selectedPanes = null;

    /** The pane that is currently used as the basis for the mouse cursor */
    protected IDisplayPane currentMouseHoverPane;

    private int displayedPaneCount = 0;

    protected IDisplayPane[] lastHandledPanes = null;

    @Override
    public void initializeComponents(IDisplayPaneContainer container,
            Composite comp) {
        displayPanes.clear();
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
                if (!displayPanes.isEmpty()) {
                    waiting = true;
                    VizApp.runAsync(() -> {
                        adjustPaneLayout();
                        waiting = false;
                    });
                }
            }
        });

        displayPanes.clear();
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
        for (IDisplayPane pane : displayPanes) {
            pane.refresh();
        }
    }

    public IRenderableDisplay[] getRenderableDisplays() {
        List<IRenderableDisplay> rDisplays = new ArrayList<>();
        for (IDisplayPane pane : displayPanes) {
            rDisplays.add(pane.getRenderableDisplay());
        }
        return rDisplays.toArray(new IRenderableDisplay[rDisplays.size()]);
    }

    @Override
    public IDisplayPane[] getDisplayPanes() {
        return displayPanes.toArray(new VizDisplayPane[displayPanes.size()]);
    }

    @Override
    public IDisplayPane getActiveDisplayPane() {
        if (activatedPane == null) {
            for (VizDisplayPane pane : displayPanes) {
                if (pane.isVisible()) {
                    activatedPane = pane;
                    break;
                }
            }
            if (activatedPane == null && !displayPanes.isEmpty()) {
                activatedPane = displayPanes.get(0);
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
        return displayPanes.size();
    }

    @Override
    public boolean isSelectedPane(String action, IDisplayPane pane) {
        return getSelectedPane(action) == pane;
    }

    @Override
    public void setSelectedPane(String action, IDisplayPane pane) {
        if (pane != null && !displayPanes.contains(pane)) {
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

        if (renderableDisplay != null && !renderableDisplay.isSwapping()) {
            if (!displayPanes.isEmpty()) {
                for (ResourcePair rp : renderableDisplay.getDescriptor()
                        .getResourceList()) {
                    if (rp.getProperties().isMapLayer()) {
                        renderableDisplay.getDescriptor().getResourceList()
                                .remove(rp);
                    }
                }

                for (IDisplayPane gp : displayPanes) {
                    for (ResourcePair rp : gp.getDescriptor()
                            .getResourceList()) {
                        if (rp.getProperties().isMapLayer()) {
                            renderableDisplay.getDescriptor().getResourceList()
                                    .add(rp);
                        }
                    }
                }

            }
        }

        VizDisplayPane pane = null;
        try {
            pane = (VizDisplayPane) createNewPane(renderableDisplay,
                    canvasComp);
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
                if (!displayPanes.isEmpty()) {
                    pane.getRenderableDisplay().setBackgroundColor(
                            displayPanes.get(0).getRenderableDisplay()
                                    .getBackgroundColor());
                } else if (paneContainer instanceof AbstractEditor) {
                    ((AbstractEditor) paneContainer).getBackgroundColor()
                            .setColor(BGColorMode.EDITOR,
                                    pane.getRenderableDisplay()
                                            .getBackgroundColor());
                }

                if (!displayPanes.isEmpty()) {
                    pane.getDescriptor().synchronizeTimeMatching(
                            displayPanes.get(0).getDescriptor());
                }
                displayPanes.add(pane);
            } catch (Throwable t) {
                statusHandler.handle(Priority.PROBLEM, "Error adding pane", t);
            }
        }

        setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
        adjustPaneLayout();
        return pane;
    }

    protected IDisplayPane createNewPane(IRenderableDisplay renderableDisplay,
            Composite canvasComp) throws VizException {
        return new VizDisplayPane(paneContainer, canvasComp, renderableDisplay,
                true);
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
        if (!displayPanes.contains(pane)) {
            throw new IllegalArgumentException(
                    "removePane called with pane not in this IDisplayPaneContainer");
        }
        boolean wasVisible = pane.isVisible();
        displayPanes.remove(pane);
        if (activatedPane == pane && !displayPanes.isEmpty()) {
            activatedPane = displayPanes.get(0);
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
        if (pane.getRenderableDisplay() != null
                && !pane.getRenderableDisplay().isSwapping()) {
            IDescriptor descriptor = pane.getDescriptor();
            if (descriptor != null) {
                for (ResourcePair rp : descriptor.getResourceList()) {
                    if (rp.getProperties().isMapLayer()) {
                        AbstractVizResource<?, ?> resource = rp.getResource();
                        if (resource != null
                                && resource.getDescriptor() == descriptor) {
                            resetDescriptor(rp);
                        }
                    }
                }
            }
        }

        pane.dispose();

        if (wasVisible) {
            --displayedPaneCount;
        }

        if (pane == getSelectedPane(IMultiPaneEditor.IMAGE_ACTION)) {
            setSelectedPane(IMultiPaneEditor.IMAGE_ACTION, null);
        }

        adjustPaneLayout();
    }

    /**
     * Set the descriptor for a resource pair to one of the descriptors in
     * displayPanes. The descriptor is only changed if the resource is already
     * in one of the panes. If none of the panes contain the resource then it is
     * not changed.
     *
     * @return true if the descriptor was changed.
     */
    private boolean resetDescriptor(ResourcePair rp) {
        for (IDisplayPane remainingPane : displayPanes) {
            if (remainingPane.getDescriptor().getResourceList().contains(rp)) {
                /*
                 * Because the resource is already on the descriptor it is safe
                 * to assume that the descriptor is the correct type for the
                 * resource. There is no way to tell the compiler that we know
                 * the generics are compatible except an unchecked cast.
                 */
                @SuppressWarnings("unchecked")
                AbstractVizResource<?, IDescriptor> resource = (AbstractVizResource<?, IDescriptor>) rp
                        .getResource();
                resource.setDescriptor(remainingPane.getDescriptor());
                return true;
            }
        }
        return false;
    }

    @Override
    public int displayedPaneCount() {
        return displayedPaneCount;
    }

    @Override
    public void clear() {
        while (displayPanes.size() > 1) {
            removePane(displayPanes.get(displayPanes.size() - 1));
        }
        IDisplayPane pane = displayPanes.get(0);
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

        lastHandledPanes = getDisplayPanes();
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
}
