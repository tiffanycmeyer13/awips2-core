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

import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IInsetMapDisplayPaneContainer;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.IPane.CanvasType;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.viz.ui.EditorUtil;
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

    private void replacePane(IDisplayPane canvas, IRenderableDisplay display) {
        getPaneManager().replacePane(canvas, display);
    }

    @Override
    public boolean makeCompatible(IRenderableDisplay... newDisplays) {
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

        if (newDisplays.length == 1) {
            /*
             * If loading only one display, either load to the selected Load
             * pane if there is one, or load to all panes.
             */
            IRenderableDisplay newDisplay = newDisplays[0];
            IDisplayPane loadCanvas = getSelectedPane(LOAD_ACTION);
            if (loadCanvas != null) {
                // Ensure Load pane is compatible
                if (!loadCanvas.getDescriptor()
                        .isCompatible(newDisplay.getDescriptor())) {
                    replacePane(loadCanvas, getBackgroundDisplay(newDisplay));
                }
            } else {
                /*
                 * TODO We should probably only load to panes that are already
                 * compatible if possible. The code that loads the actual data
                 * displays will need updating for that as well.
                 */
                // Ensure all panes are compatible
                IDisplayPane[] currentCanvases = getDisplayPanes();
                for (IDisplayPane currentCanvas : currentCanvases) {
                    if (!currentCanvas.getDescriptor()
                            .isCompatible(newDisplay.getDescriptor())) {
                        replacePane(currentCanvas,
                                getBackgroundDisplay(newDisplay));
                    }
                }
            }
        } else {
            /*
             * Ignore selected Load pane if multiple displays, just load the n
             * displays to the first n panes, adding panes if necessary.
             */
            IDisplayPane[] currentCanvases = getDisplayPanes();

            for (int i = 0; i < newDisplays.length; i++) {
                if (i >= currentCanvases.length) {
                    /*
                     * Add new panes for any displays beyond the previous pane
                     * count
                     */
                    addPane(getBackgroundDisplay(newDisplays[i]));
                } else {
                    /*
                     * For displays that match up with an existing pane index,
                     * ensure the pane is compatible
                     */
                    IDisplayPane currentCanvas = currentCanvases[i];
                    IRenderableDisplay newDisplay = newDisplays[i];
                    if (!currentCanvas.getDescriptor()
                            .isCompatible(newDisplay.getDescriptor())) {
                        replacePane(currentCanvas,
                                getBackgroundDisplay(newDisplay));
                    }
                }

            }
        }
        return true;
    }

    /**
     * Get a background display (e.g. background map/graph display) for loading
     * the given data display to.
     *
     * @param display
     *            the renderable display to load onto the background display
     * @return the background renderable display
     */
    private IRenderableDisplay getBackgroundDisplay(
            IRenderableDisplay display) {
        /*
         * First try to create a new background display from a compatible pane,
         * so that we match its pan/zoom/scale state.
         */
        for (IDisplayPane canvas : getPaneManager()
                .getCanvases(CanvasType.MAIN)) {
            if (canvas.getDescriptor().isCompatible(display.getDescriptor())) {
                return canvas.getRenderableDisplay().createNewDisplay();
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
