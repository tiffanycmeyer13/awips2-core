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
package com.raytheon.viz.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.util.EditorConstants;
import com.raytheon.viz.ui.UiUtil.ContainerPart.Container;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.EditorInput;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.raytheon.viz.ui.statusline.VizActionBarAdvisor;

/**
 * UiUtil - contains UI utility methods
 *
 * <pre>
 *
 *    SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 30, 2007            chammack    Initial Creation.
 * 12/02/2008   1450       randerso    Added getEditors method
 * 12/05/2008              ebabin      Changed findView to not always assume
 *                                      view has a secondaryid.
 *                                     Added hideView method for quickly hiding views.
 * Dec 21, 2015 5191       bsteffen    Updated layoutId for Eclipse 4.
 * Mar 31, 2016 5519       bsteffen    Fix coolbar update on eclipse 4.
 * May 03, 2016 3292       bsteffen    Preserve editor order in getActiveDisplayMap.
 * Mar 12, 2018 6757       njensen     Copy active editor's loop properties for new editor
 * Sep 25, 2023          srcarter@ucar Open multipanel windows in new editor (from MJ)
 * Apr 01, 2022 8790       mapeters    Update determination of editor type to open, move
 *                                     makeCompatible() to editor hierarchy
 * Apr 22, 2022 8791       mapeters    Further update determination of editor type to open
 * Sep 13, 2022 8792       mapeters    Add isDescriptorCompatibleWithActive() and
 *                                     isDescriptorActive()
 * Oct 19, 2022 8956       mapeters    Add isProductLoaded(), update createOrOpenEditor* methods
 * Nov 02, 2022 8958       mapeters    Editor creation updates for Combo editors to prevent missing
 *                                     map backgrounds and to support loading bundles with a number
 *                                     of displays that doesn't match an available pane layout
 * May 11, 2023 2029803    mapeters    Add getNumRowsColumns/isSquareLayout, remove code for
 *                                     enforcing valid panel counts
 *
 * </pre>
 *
 * @author chammack
 */
public class UiUtil {

    public static final String SECONDARY_ID_SEPARATOR = ":";

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(UiUtil.class);

    private static final String DEFAULT_MAP_EDITOR_ID = "com.raytheon.viz.ui.glmap.GLMapEditor";

    public static class ContainerPart {

        public static class Container {
            public String layoutId;

            public IRenderableDisplay[] displays;
        }

        public String id;

        public List<Container> containers;

        private ContainerPart(String id, List<Container> containers) {
            this.id = id;
            this.containers = containers;
        }
    }

    /**
     * Get a map of all active CAVE panes, keyed by the editor or view
     *
     * @return the pane map
     */
    @SuppressWarnings("restriction")
    public static List<ContainerPart> getActiveDisplayMap() {
        List<ContainerPart> parts = new ArrayList<>();
        Map<String, ContainerPart> partMap = new LinkedHashMap<>();

        IWorkbenchWindow window = VizWorkbenchManager.getInstance()
                .getCurrentWindow();

        if (window != null) {
            IWorkbenchPage pages[] = window.getPages();
            for (IWorkbenchPage page : pages) {
                IEditorReference[] refs = page.getEditorReferences();

                // Pull out editors
                for (IEditorReference ref : refs) {
                    IEditorPart part = ref.getEditor(false);
                    if (part == null) {
                        continue;
                    }

                    if (part instanceof IDisplayPaneContainer) {
                        IDisplayPaneContainer container = (IDisplayPaneContainer) part;
                        IRenderableDisplay[] editorDisplays = getDisplaysFromContainer(
                                container);
                        if (editorDisplays != null
                                && editorDisplays.length > 0) {
                            ContainerPart cp = partMap.get(ref.getId());
                            if (cp == null) {
                                List<Container> list = new ArrayList<>();
                                cp = new ContainerPart(ref.getId(), list);
                                partMap.put(ref.getId(), cp);
                            }
                            Container c = new Container();
                            c.displays = editorDisplays;
                            if (page instanceof WorkbenchPage) {
                                MPart modelPart = ((WorkbenchPage) page)
                                        .findPart(part);
                                c.layoutId = modelPart.getParent()
                                        .getElementId();
                            }
                            cp.containers.add(c);
                        }
                    }
                }

                // Pull out views
                IViewReference[] viewReferences = page.getViewReferences();
                for (IViewReference ref : viewReferences) {
                    IViewPart view = ref.getView(false);
                    if (view == null) {
                        continue;
                    }

                    if (view instanceof IDisplayPaneContainer) {
                        IDisplayPaneContainer container = (IDisplayPaneContainer) view;
                        IRenderableDisplay[] displays = getDisplaysFromContainer(
                                container);

                        if (displays != null && displays.length > 0) {
                            String id = ref.getId() + SECONDARY_ID_SEPARATOR
                                    + ref.getSecondaryId();
                            ContainerPart cp = partMap.get(id);
                            if (cp == null) {
                                List<Container> list = new ArrayList<>();
                                cp = new ContainerPart(id, list);
                                partMap.put(id, cp);
                            }
                            Container c = new Container();
                            c.displays = displays;
                            cp.containers.add(c);
                        }
                    }
                }
            }
        }
        parts.addAll(partMap.values());
        return parts;
    }

    /**
     * Return the list of displays from a display container
     *
     * @param container
     *            the container to retrieve from
     * @return the list of displays
     */
    public static IRenderableDisplay[] getDisplaysFromContainer(
            IDisplayPaneContainer container) {
        List<IRenderableDisplay> displays = new ArrayList<>();

        IDisplayPane[] panes = container.getDisplayPanes();
        for (IDisplayPane pane : panes) {
            if (pane != null) {
                IRenderableDisplay display = pane.getRenderableDisplay();
                if (display != null) {
                    displays.add(display);
                }
            }
        }

        return displays.toArray(new IRenderableDisplay[displays.size()]);

    }

    /**
     * Get a reference to a view given the id.
     *
     * @param view
     *            the id of the view to find
     * @param createIfNotFound
     *            if not found, should the view be opened
     * @return
     */
    public static IViewPart findView(String view, boolean createIfNotFound) {
        Validate.notNull(view);

        IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
                .getWorkbenchWindows();
        String id = null;
        String secondaryId = null;

        if (view.contains(SECONDARY_ID_SEPARATOR)) {
            id = view.split(SECONDARY_ID_SEPARATOR)[0];
            secondaryId = view.split(SECONDARY_ID_SEPARATOR)[1];
        } else {
            id = view;
        }

        // Search all windows for view
        for (IWorkbenchWindow window : windows) {
            IViewPart viewPart = findView(window, view, false);
            if (viewPart != null) {
                return viewPart;
            }
        }

        if (createIfNotFound) {
            try {
                return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage()
                        .showView(id, secondaryId, IWorkbenchPage.VIEW_VISIBLE);
            } catch (PartInitException e) {
                statusHandler.error("Error creating view with IDs: " + id + ", "
                        + secondaryId, e);
                return null;
            }
        }

        return null;
    }

    /**
     * Given the window, find the view and create if not found and desired
     *
     * @param windowToSearch
     * @param view
     * @param createIfNotFound
     * @return The found or created view part or null if not found and should
     *         not create
     */
    public static IViewPart findView(IWorkbenchWindow windowToSearch,
            String view, boolean createIfNotFound) {
        Validate.notNull(view);
        Validate.notNull(windowToSearch);

        String id = null;
        String secondaryId = null;

        if (view.contains(SECONDARY_ID_SEPARATOR)) {
            id = view.split(SECONDARY_ID_SEPARATOR)[0];
            secondaryId = view.split(SECONDARY_ID_SEPARATOR)[1];
        } else {
            id = view;
        }

        IWorkbenchPage pages[] = windowToSearch.getPages();
        for (IWorkbenchPage page : pages) {
            IViewReference[] refs = page.getViewReferences();

            for (IViewReference r : refs) {
                if (id.equals(r.getId())
                        && ((secondaryId != null && r.getSecondaryId() != null
                                && secondaryId.equals(r.getSecondaryId()))
                                || (secondaryId == null
                                        || r.getSecondaryId() == null))) {
                    return (IViewPart) r.getPart(true);
                }
            }
        }

        if (createIfNotFound) {
            try {
                return windowToSearch.getActivePage().showView(id);
            } catch (PartInitException e) {
                statusHandler.error("Error creating view with ID: " + id, e);
                return null;
            }
        }

        return null;
    }

    /**
     * Given the id, determine if the id corresponds to a view
     *
     * @param id
     *            the id to check
     * @return true if id corresponds to view, false otherwise
     */
    public static boolean isView(String id) {
        IViewRegistry registry = PlatformUI.getWorkbench().getViewRegistry();

        String parsedId = id;

        if (id.contains(SECONDARY_ID_SEPARATOR)) {
            parsedId = id.split(SECONDARY_ID_SEPARATOR)[0];
        }

        IViewDescriptor d = registry.find(parsedId);

        if (d == null) {
            return false;
        }

        return true;

    }

    /**
     * Given the id, determine if the id corresponds to an editor
     *
     * @param id
     *            the id to check
     * @return true if id corresponds to an editor, false otherwise
     */
    public static boolean isEditor(String id) {
        IEditorRegistry registry = PlatformUI.getWorkbench()
                .getEditorRegistry();

        IEditorDescriptor d = registry.findEditor(id);

        if (d == null) {
            return false;
        }

        return true;

    }

    /**
     * @return the currently active window
     */
    public static IWorkbenchWindow getCurrentWindow() {
        return VizWorkbenchManager.getInstance().getCurrentWindow();
    }

    /**
     * Given the editor type info and the renderable displays, create or open an
     * editor with the given displays on the active window.
     *
     * @param editorTypeInfo
     *            info used to help determine the type of editor to open (e.g.
     *            map, cross section, combo) - must not be null, although its
     *            contained editor ID can be
     * @param loadToExisting
     *            true if what will be loaded to the editor can be added to
     *            existing resources, false if it will replace existing
     *            resources
     * @param tryPerspectiveManagerOnCreate
     *            if we need to create a new editor, try having the active
     *            perspective manager open its default editor and see if it's
     *            the appropriate editor type first, otherwise close it and fall
     *            back to {@link #createEditor}
     * @param displays
     *            the displays to load to the editor, one per pane
     * @return the created or opened editor
     */
    public static AbstractEditor createOrOpenEditor(
            EditorTypeInfo editorTypeInfo, boolean loadToExisting,
            boolean tryPerspectiveManagerOnCreate,
            IRenderableDisplay... displays) {
        String editorId = editorTypeInfo.getEditorId();
        if (editorId == null) {
            editorId = DEFAULT_MAP_EDITOR_ID;
        }

        // Check the current editor first
        IEditorPart activeEditorPart = EditorUtil.getActiveEditor();
        LoopProperties loopProps = null;
        if (activeEditorPart instanceof AbstractEditor && displays.length < 2) {
            AbstractEditor currentEditor = (AbstractEditor) activeEditorPart;
            /*
             * Copy the current editor's loop properties in case we open a new
             * editor at the end of this method
             */
            loopProps = new LoopProperties(currentEditor.getLoopProperties());
            if (currentEditor.getEditorSite().getId().equals(editorId)
                    || !editorTypeInfo.isStrict()) {
                if (currentEditor.makeCompatible(loadToExisting, displays)) {
                    return currentEditor;
                }
            }
        }

        IWorkbenchPage activePage = getCurrentWindow().getActivePage();
        IEditorReference[] references = {};
        if (activePage != null) {
            references = activePage.getEditorReferences();
        }

        // Next check non-active editors that match the preferred editor ID
        for (IEditorReference ref : references) {
            if (editorId.equals(ref.getId()) && displays.length < 2) {
                IEditorPart editorPart = ref.getEditor(false);
                if (editorPart != activeEditorPart
                        && editorPart instanceof AbstractEditor) {
                    AbstractEditor aEditor = (AbstractEditor) editorPart;
                    if (aEditor.makeCompatible(loadToExisting, displays)) {
                        activePage.bringToTop(aEditor);
                        return aEditor;
                    }
                }
            }
        }

        /*
         * Next, if the preferred editor ID isn't strict, check non-active
         * editors that don't match the preferred ID
         */
        if (!editorTypeInfo.isStrict()) {
            for (IEditorReference ref : references) {
                if (!editorId.equals(ref.getId())) {
                    IEditorPart editorPart = ref.getEditor(false);
                    if (editorPart != activeEditorPart
                            && editorPart instanceof AbstractEditor) {
                        AbstractEditor aEditor = (AbstractEditor) editorPart;
                        if (aEditor.makeCompatible(loadToExisting, displays)) {
                            activePage.bringToTop(aEditor);
                            return aEditor;
                        }
                    }
                }
            }
        }

        if (tryPerspectiveManagerOnCreate) {
            /*
             * This part allows the perspective manager to make an editor which
             * may have some customizations, such as including base maps and
             * setting the map projection to something user friendly. If you try
             * to load D2D map data without this when an incompatible editor is
             * active and no Map editor is open, it loads without the background
             * map.
             */
            AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                    .getInstance().getActivePerspectiveManager();
            if (mgr != null) {
                AbstractEditor editor = mgr.openNewEditor();
                if (editor != null) {
                    if ((editorId.equals(editor.getSite().getId())
                            || !editorTypeInfo.isStrict())
                            && editor.makeCompatible(loadToExisting,
                                    displays)) {
                        return editor;
                    } else {
                        activePage.closeEditor(editor, false);
                    }
                }
            }
        }

        if (EditorConstants.COMBO_EDITOR_ID.equals(editorId)
                || EditorConstants.MAP_EDITOR_ID.equals(editorId)) {
            /*
             * Map product displays don't include the background map with them,
             * so create the editor with background versions of the displays
             * first and let the calling code then load the product resources.
             *
             * Non-map displays don't need this because background displays are
             * included in their product displays. Also for non-map displays, we
             * want to create the editor with the actual products below so that
             * the logic in LoadBundleHandler.execute for closing new, empty
             * editors works correctly.
             */
            displays = Arrays.stream(displays)
                    .map(display -> DescriptorMap.getPaneCreator(display)
                            .getDefaultBackgroundDisplay(display))
                    .toArray(IRenderableDisplay[]::new);
        }

        /*
         * If we get here, an editor of the desired type doesn't exist or has a
         * different number of panes. Construct a new one.
         */
        return createEditor(getCurrentWindow(), editorId, loopProps, displays);
    }

    /**
     * Given the editor type info and the renderable displays, create or open an
     * editor with the given displays on the active window.
     *
     * @param editorTypeInfo
     *            info used to help determine the type of editor to open (e.g.
     *            map, cross section, combo) - must not be null, although its
     *            contained editor ID can be
     * @param loadToExisting
     *            true if what will be loaded to the editor can be added to
     *            existing resources, false if it will replace existing
     *            resources
     * @param displays
     *            the displays to load to the editor, one per pane
     * @return the created or opened editor
     */
    public static AbstractEditor createOrOpenEditor(
            EditorTypeInfo editorTypeInfo, boolean loadToExisting,
            IRenderableDisplay... displays) {
        return createOrOpenEditor(editorTypeInfo, loadToExisting, true,
                displays);
    }

    /**
     * Create or open an editor that supports the given bundle then being loaded
     * to it. This does not load the bundle.
     *
     * @param bundle
     *            the bundle to create or open an editor for
     * @param loadToExisting
     *            true if what will be loaded to the editor can be added to
     *            existing resources, false if it will replace existing
     *            resources
     * @return the editor to load the bundle to
     */
    public static AbstractEditor createOrOpenEditorForBundle(Bundle bundle,
            boolean loadToExisting) {
        String editorId = bundle.getEditor();
        /*
         * Editor ID is strict if it's in the bundle, it's not if it's just
         * determined from the descriptor type.
         */
        boolean strictEditorId = editorId != null;
        AbstractRenderableDisplay[] displays = bundle.getDisplays();
        if (editorId == null && displays != null && displays.length > 0) {
            editorId = DescriptorMap.getEditorId(displays);
        }

        EditorTypeInfo editorTypeInfo = new EditorTypeInfo(editorId,
                strictEditorId);
        AbstractEditor editor = UiUtil.createOrOpenEditor(editorTypeInfo,
                loadToExisting, displays);

        return editor;
    }

    /**
     * Opens a new editor with the specified displays on the currently active
     * window
     *
     * @param editorId
     * @param displays
     * @return
     */
    public static AbstractEditor createEditor(String editorId,
            IRenderableDisplay... displays) {
        return createEditor(getCurrentWindow(), editorId, displays);
    }

    /**
     * Opens a new editor with the specified displays on the specified window
     *
     * @param windowToLoadTo
     * @param editorId
     * @param displays
     * @return
     */
    public static AbstractEditor createEditor(IWorkbenchWindow windowToLoadTo,
            String editorId, IRenderableDisplay... displays) {
        return createEditor(windowToLoadTo, editorId, null, displays);
    }

    public static AbstractEditor createEditor(IWorkbenchWindow windowToLoadTo,
            String editorId, LoopProperties loopProps,
            IRenderableDisplay... displays) {
        if (editorId == null) {
            editorId = DEFAULT_MAP_EDITOR_ID;
        }
        if (windowToLoadTo == null) {
            windowToLoadTo = getCurrentWindow();
        }
        AbstractEditor aEditor = null;
        if (loopProps == null) {
            loopProps = new LoopProperties();
        }

        EditorInput cont = new EditorInput(loopProps, displays);
        try {
            IWorkbenchPage activePage = windowToLoadTo.getActivePage();
            if (activePage != null) {
                aEditor = (AbstractEditor) activePage.openEditor(cont,
                        editorId);
            }
        } catch (PartInitException e) {
            UiPlugin.getDefault().getLog()
                    .log(new Status(IStatus.ERROR, UiPlugin.PLUGIN_ID,
                            "Error creating and opening editor " + editorId,
                            e));
        }
        return aEditor;
    }

    /**
     * Find all editors for a perspective in a window
     *
     * @param window
     * @return array of AbstractEditors in the perspective
     */
    public static AbstractEditor[] getEditors(IWorkbenchWindow window,
            String perspectiveId) {
        VizPerspectiveListener listener = VizPerspectiveListener
                .getInstance(window);
        if (listener != null) {
            AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                    .getInstance(window).getPerspectiveManager(perspectiveId);

            if (mgr != null) {
                return mgr.getPerspectiveEditors();
            }
        }
        return new AbstractEditor[0];
    }

    /**
     * Force update the size and layout of all the coolbar items. This is
     * necessary when a coolbar item changes size to prevent other items from
     * being hidden.
     *
     * @param window
     */
    public static void updateWindowCoolBar(IWorkbenchWindow window) {
        try {
            VizActionBarAdvisor advisor = VizActionBarAdvisor
                    .getInstance(window);
            if (advisor != null) {
                ICoolBarManager cbm = advisor.getCoolBar();
                IContributionItem[] items = cbm.getItems();
                for (IContributionItem item : items) {
                    if (item instanceof ToolBarContributionItem) {
                        ((ToolBarContributionItem) item).getToolBarManager()
                                .update(true);
                    } else {
                        /*
                         * This does not work on all types of items, for example
                         * it no longer works for ToolBarContributionItems.
                         * Unfortunately it is the only available generic API
                         * for requesting a resize so try anyway.
                         */
                        item.update(ICoolBarManager.SIZE);
                    }
                }
            }
        } catch (Throwable t) {
            statusHandler.debug("Error updating cool bar", t);
        }
    }

    /**
     * Determine if the given descriptor is compatible with the active
     * descriptor in the given pane container.
     *
     * @param descriptor
     *            descriptor to compare with the active descriptor
     * @param paneContainer
     *            pane container to get the active descriptor from
     * @return true if the descriptor is compatible, false otherwise
     */
    public static boolean isDescriptorCompatibleWithActive(
            IDescriptor descriptor, IDisplayPaneContainer paneContainer) {
        if (paneContainer != null) {
            for (IDisplayPane canvas : paneContainer
                    .getCanvasesCompatibleWithActive()) {
                if (canvas.getDescriptor() == descriptor) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine if the given descriptor is the active descriptor in the given
     * pane container.
     *
     * @param descriptor
     *            descriptor to check if it matches the active descriptor
     * @param paneContainer
     *            pane container to get the active descriptor from
     * @return true if the descriptor is active in the pane container, false
     *         otherwise
     */
    public static boolean isDescriptorActive(IDescriptor descriptor,
            IDisplayPaneContainer paneContainer) {
        if (paneContainer != null) {
            IDisplayPane canvas = paneContainer.getActiveDisplayPane();
            if (canvas != null && canvas.getDescriptor() == descriptor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if any product resource (not background or system) is loaded to
     * the given descriptor.
     *
     * @param descriptor
     *            the descriptor to check
     * @return true if any product resource is loaded, false otherwise
     */
    public static boolean isProductLoaded(IDescriptor descriptor) {
        if (descriptor != null) {
            for (ResourcePair resourcePair : descriptor.getResourceList()) {
                ResourceProperties props = resourcePair.getProperties();
                if (props != null && !props.isMapLayer()
                        && !props.isSystemResource()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the number of rows and columns to organize the given number of panes
     * in for the given orientation.
     *
     * @param numPanes
     *            number of panes to calculate rows/columns for
     * @param horizontal
     *            true to make columns >= rows, false to make rows >= columns
     *
     * @return int array consisting of { numRows, numColumns }
     */
    public static int[] getNumRowsColumns(int numPanes, boolean horizontal) {
        if (numPanes <= 0) {
            /*
             * 0 can be passed in when rotating panels in an editor and we hide
             * all panels before showing the next panel.
             */
            return new int[] { 0, 0 };
        }
        int smallDim;
        for (smallDim = (int) Math.sqrt(numPanes); smallDim >= 1; --smallDim) {
            if (numPanes % smallDim == 0) {
                break;
            }
        }
        int bigDim = numPanes / smallDim;
        if (horizontal) {
            // Make columns >= rows
            return new int[] { smallDim, bigDim };
        }
        // Make rows >= columns
        return new int[] { bigDim, smallDim };
    }

    /**
     * Determine if the given number of panes produces a square pane layout
     * (number of rows equals number of columns).
     *
     * @param numPanes
     *            number of panes to check the layout for
     * @return true if pane layout is square, false otherwise
     */
    public static boolean isSquareLayout(int numPanes) {
        int[] numRowsColumns = getNumRowsColumns(numPanes, false);
        return numRowsColumns[0] == numRowsColumns[1];
    }
}
