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

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.IPane.CanvasType;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
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
 *
 * </pre>
 *
 * @author mapeters
 */
public class ComboEditor extends VizMultiPaneEditor {

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
    public boolean makeCompatible(IRenderableDisplay... displays) {
        if (this != EditorUtil.getActiveEditor()) {
            // Only load to if active
            return false;
        }

        IDisplayPane loadCanvas = getSelectedPane(LOAD_ACTION);
        if (loadCanvas != null && displays.length == 1) {
            if (!loadCanvas.getDescriptor()
                    .isCompatible(displays[0].getDescriptor())) {
                replacePane(loadCanvas, displays[0]);
            }
        } else {
            IDisplayPane[] canvases = getDisplayPanes();
            for (int i = 0; i < displays.length && i < canvases.length; i++) {
                IDescriptor currentDesc = canvases[i].getDescriptor();
                IDescriptor newDesc = displays[i].getDescriptor();
                if (!currentDesc.isCompatible(newDesc)) {
                    replacePane(canvases[i], newDesc.getRenderableDisplay());
                }
            }
            for (int i = canvases.length; i < displays.length; ++i) {
                addPane(displays[i]);
            }
        }
        return true;
    }

    @Override
    public void setColor(BGColorMode mode, RGB newColor) {
        for (CanvasType type : CanvasType.values()) {
            setColor(getPaneManager().getCanvases(type), newColor);
        }
    }
}
