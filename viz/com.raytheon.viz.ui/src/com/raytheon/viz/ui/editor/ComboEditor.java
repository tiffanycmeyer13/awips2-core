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
package com.raytheon.viz.ui.editor;

import java.util.Arrays;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IInsetMapDisplayPaneContainer;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.IPane.CanvasType;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IScalableRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IScalableRenderableDisplay.ScaleType;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.actions.MultiPanes;
import com.raytheon.viz.ui.panes.ComboPaneManager;

/**
 *
 * Editor (pane container) that contains 1 or more panes and supports the
 * combination of panes of different types. For example, with a 4-pane layout,
 * this editor may contain two map panes, one cross section pane, and one time
 * series pane.
 *
 * This editor and its corresponding {@link ComboPaneManager} are written
 * generically, without any pane type-specific functionality. The type-specific
 * functionality is instead at the pane level in the {@link IPane} hierarchy.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 23, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Implement IInsetMapDisplayPaneContainer,
 *                                     update makeCompatible
 * Oct 10, 2022 8946       mapeters    Update to ensure all height scales in a
 *                                     single editor match
 * Oct 13, 2022 8955       mapeters    Make replacePane public
 * Oct 19, 2022 8956       mapeters    Get user confirmation when replacing populated panes,
 *                                     make Load to All Panes only load to compatible ones
 *                                     if there are any
 * Nov 02, 2022 8958       mapeters    Update makeCompatible to ensure we end up with a
 *                                     number of panes that is a supported layout
 * Nov 16, 2022 8956       mapeters    Move conforming of product displays from
 *                                     makeCompatible() to BundleProductLoader
 * Dec 01, 2022 8984       mapeters    Make graph panes still load with the correct
 *                                     pan/zoom state when we conform their height scale
 *
 * </pre>
 *
 * @author mapeters
 */
public class ComboEditor extends VizMultiPaneEditor
        implements IInsetMapDisplayPaneContainer {

    @Override
    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        super.init(site, input);
        setTabTitle("Combo");
    }

    @Override
    protected void validateEditorInput(EditorInput input)
            throws PartInitException {
        super.validateEditorInput(input);
        if (input.getPaneManager() != null
                && !(input.getPaneManager() instanceof ComboPaneManager)) {
            throw new PartInitException(
                    "Expected pane manager of type: " + ComboPaneManager.class);
        }
    }

    @Override
    protected ComboPaneManager getNewPaneManager() {
        return new ComboPaneManager();
    }

    private ComboPaneManager getPaneManager() {
        return (ComboPaneManager) editorInput.getPaneManager();
    }

    /**
     * Replace the pane containing the given canvas with a new pane created from
     * the given display.
     *
     * @param canvas
     *            a canvas in the pane to replace
     * @param display
     *            the display to create the new pane from
     */
    public void replacePane(IDisplayPane canvas, IRenderableDisplay display) {
        getPaneManager().replacePane(canvas, display);
    }

    /**
     * Update the new display's scale to match the pre-existing display, if
     * necessary.
     *
     * @param newDisplay
     *            the display being loaded that may need its scale updated
     * @param existingDisplay
     *            the pre-existing display to be conformed to
     * @return true if scale updated (and wasn't null before), false otherwise
     */
    private boolean conformScale(IScalableRenderableDisplay newDisplay,
            IScalableRenderableDisplay existingDisplay) {
        /*
         * Only height scales need updating, differing map scales are
         * automatically handled elsewhere.
         */
        if (newDisplay.getScaleType() == ScaleType.HEIGHT
                && existingDisplay.getScaleType() == ScaleType.HEIGHT) {
            String newDisplayScale = newDisplay.getScale();
            if (!existingDisplay.getScale().equals(newDisplayScale)) {
                newDisplay.setScale(existingDisplay.getScale());
                if (newDisplayScale != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update the scales of the new displays to match the scales of the
     * pre-existing displays, if necessary.
     *
     * @param newDisplays
     *            the displays being loaded that may need their scales updated
     * @return true if any display's scale was updated, false otherwise
     */
    public boolean conformScales(IRenderableDisplay... newDisplays) {
        boolean updatedAnyScale = false;

        IDisplayPane[] existingCanvases = getMainCanvases();
        for (IRenderableDisplay newDisplay : newDisplays) {
            if (!(newDisplay instanceof IScalableRenderableDisplay)) {
                continue;
            }
            IScalableRenderableDisplay newScalableDisplay = (IScalableRenderableDisplay) newDisplay;
            for (IDisplayPane canvas : existingCanvases) {
                IRenderableDisplay existingDisplay = canvas
                        .getRenderableDisplay();
                if (!(existingDisplay instanceof IScalableRenderableDisplay)) {
                    continue;
                }
                IScalableRenderableDisplay existingScalableDisplay = (IScalableRenderableDisplay) existingDisplay;
                boolean updatedCurrScale = conformScale(newScalableDisplay,
                        existingScalableDisplay);
                if (updatedCurrScale) {
                    updatedAnyScale = true;
                    break;
                }
            }
        }

        return updatedAnyScale;
    }

    private boolean confirmReplacingActiveResources() {
        String msg = "Loading this to the active Combo editor will replace existing"
                + " resources. Would you still like to load it to the active editor?";
        Shell shell = VizWorkbenchManager.getInstance().getCurrentWindow()
                .getShell();
        return MessageDialog.openQuestion(shell, "Replace Resources?", msg);
    }

    @Override
    public boolean makeCompatible(boolean loadToExisting,
            IRenderableDisplay... newDisplays) {
        if (this != EditorUtil.getActiveEditor()) {
            // Only load to if active
            return false;
        }

        /*
         * A null pane creator indicates that a display isn't compatible with
         * combo editor, so check all displays for that.
         */
        for (IRenderableDisplay display : newDisplays) {
            if (DescriptorMap.getPaneCreator(display) == null) {
                return false;
            }
        }

        /*
         * Convert the new displays to background versions of themselves, as the
         * actual data/products are loaded later on outside this method.
         */
        newDisplays = Arrays.stream(newDisplays)
                .map(display -> getBackgroundDisplay(display, loadToExisting))
                .toArray(IRenderableDisplay[]::new);
        IDisplayPane[] currentCanvases = getMainCanvases();

        if (!loadToExisting) {
            /*
             * The bundle to be loaded will replace all editor contents so that
             * it contains exactly/only what's in the bundle. Ensure we have the
             * correct number and type of panes.
             */
            boolean dataLoaded = Arrays.stream(currentCanvases).anyMatch(
                    canvas -> UiUtil.isProductLoaded(canvas.getDescriptor()));
            if (dataLoaded && !confirmReplacingActiveResources()) {
                return false;
            }

            for (int i = 0; i < newDisplays.length; ++i) {
                IRenderableDisplay newDisplay = newDisplays[i];
                if (i >= currentCanvases.length) {
                    addPane(newDisplay);
                } else {
                    replacePane(currentCanvases[i], newDisplay);
                }
            }

            for (int i = newDisplays.length; i < currentCanvases.length; i++) {
                removePane(currentCanvases[i]);
            }

            return true;
        }

        if (newDisplays.length == 1) {
            IRenderableDisplay newDisplay = newDisplays[0];
            IDisplayPane loadCanvas = getSelectedPane(LOAD_ACTION);
            if (loadCanvas != null) {
                // Ensure Load pane is compatible
                if (!loadCanvas.getDescriptor()
                        .isCompatible(newDisplay.getDescriptor())) {
                    if (UiUtil.isProductLoaded(loadCanvas.getDescriptor())
                            && !confirmReplacingActiveResources()) {
                        return false;
                    }
                    replacePane(loadCanvas, newDisplay);
                }
            } else {
                /*
                 * If any panes are already compatible, leave the editor alone
                 * and only those panes will be loaded to, otherwise update all
                 * panes to be compatible.
                 */
                boolean anyCompatible = Arrays.stream(currentCanvases)
                        .anyMatch(canvas -> newDisplay.getDescriptor()
                                .isCompatible(canvas.getDescriptor()));
                if (!anyCompatible) {
                    boolean dataLoaded = Arrays.stream(currentCanvases)
                            .anyMatch(canvas -> UiUtil
                                    .isProductLoaded(canvas.getDescriptor()));
                    if (dataLoaded && !confirmReplacingActiveResources()) {
                        return false;
                    }

                    for (IDisplayPane currentCanvas : currentCanvases) {
                        replacePane(currentCanvas,
                                newDisplay.createNewDisplay());
                    }
                }
            }
        } else {
            /*
             * Ignore selected Load pane if multiple displays, just load the n
             * displays to the first n panes, adding panes if necessary.
             */
            for (int i = 0; i < Math.min(newDisplays.length,
                    currentCanvases.length); ++i) {
                IDisplayPane currentCanvas = currentCanvases[i];
                IRenderableDisplay newDisplay = newDisplays[i];
                if (!currentCanvas.getDescriptor()
                        .isCompatible(newDisplay.getDescriptor())
                        && UiUtil.isProductLoaded(
                                currentCanvas.getDescriptor())) {
                    if (confirmReplacingActiveResources()) {
                        break;
                    } else {
                        return false;
                    }
                }
            }

            for (int i = 0; i < newDisplays.length; i++) {
                if (i >= currentCanvases.length) {
                    /*
                     * Add new panes for any displays beyond the previous pane
                     * count
                     */
                    addPane(newDisplays[i]);
                } else {
                    /*
                     * For displays that match up with an existing pane index,
                     * ensure the pane is compatible
                     */
                    IDisplayPane currentCanvas = currentCanvases[i];
                    IRenderableDisplay newDisplay = newDisplays[i];
                    if (!currentCanvas.getDescriptor()
                            .isCompatible(newDisplay.getDescriptor())) {
                        replacePane(currentCanvas, newDisplay);
                    }
                }
            }

            /*
             * Ensure we are a valid layout. For example, if a 3-display bundle
             * is loaded, ensure a blank 4th panel is added since a 3-panel
             * layout isn't supported.
             */
            int numPanes = getNumberofPanes();
            for (MultiPanes supportedLayout : MultiPanes.values()) {
                if (numPanes == supportedLayout.numPanes()) {
                    // The number of displays matches a supported layout
                    break;
                } else if (numPanes < supportedLayout.numPanes()) {
                    /*
                     * Layouts are in order of fewest panes to most, so if we
                     * are between the last layout and the next layout, add
                     * panes to match the next layout.
                     */
                    for (int i = numPanes; i < supportedLayout
                            .numPanes(); ++i) {
                        addPane(newDisplays[0].createNewDisplay());
                    }
                    break;
                }
            }
        }

        return true;
    }

    /**
     * Get a background display (e.g. background map/graph display) for loading
     * the given data display to. This always returns a new display instance.
     *
     * @param display
     *            the renderable display to load onto the background display
     * @param loadToExisting
     *            true if what will be loaded to the editor can be added to
     *            existing resources, false if it will replace existing
     *            resources
     * @return the background renderable display
     */
    private IRenderableDisplay getBackgroundDisplay(IRenderableDisplay display,
            boolean loadToExisting) {
        if (loadToExisting) {
            /*
             * Try to create a new background display from a compatible pane, so
             * that we match its pan/zoom/scale state.
             */
            display = display.createNewDisplay();
            conformScales(display);
            for (IDisplayPane canvas : getMainCanvases()) {
                if (canvas.getDescriptor()
                        .isCompatible(display.getDescriptor())) {
                    return canvas.getRenderableDisplay().createNewDisplay();
                }
            }
        }

        // Otherwise create a default background display from the given display
        return DescriptorMap.getPaneCreator(display)
                .getDefaultBackgroundDisplay(display);
    }

    @Override
    public IDisplayPane[] getInsetPanes() {
        return getPaneManager().getInsetPanes();
    }

    @Override
    public void setColor(BGColorMode mode, RGB newColor) {
        for (CanvasType type : CanvasType.values()) {
            setColor(getPaneManager().getCanvases(type), newColor);
        }
    }
}
