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
package com.raytheon.viz.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IEditorPart;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.util.EditorConstants;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

/**
 *
 * Utility methods/constants for the combo editor.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation
 * Oct 21, 2022 8956       mapeters    Moved editor ID constant to EditorConstants
 *
 * </pre>
 *
 * @author mapeters
 */
public class ComboEditorUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ComboEditorUtil.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private ComboEditorUtil() {
    }

    /**
     * Open a new combo editor that has the same number and type of panes as the
     * given active editor. If the active editor is null or an incompatible
     * type, this gets the default number/type of panes from the current
     * perspective manager.
     *
     * @param activeEditorPart
     *            the currently active editor part
     * @return the new combo editor
     */
    public static AbstractEditor openNewComboEditor(
            IEditorPart activeEditorPart) {
        AbstractEditor newEditor = null;
        if (activeEditorPart instanceof AbstractEditor) {
            AbstractEditor activeEditor = (AbstractEditor) activeEditorPart;
            List<IRenderableDisplay> displays = new ArrayList<>();
            for (IDisplayPane pane : activeEditor.getDisplayPanes()) {
                IRenderableDisplay toClone = pane.getRenderableDisplay();
                IRenderableDisplay newDisplay = toClone.createNewDisplay();
                if (newDisplay != null) {
                    displays.add(newDisplay);
                }
            }
            newEditor = UiUtil.createEditor(EditorConstants.COMBO_EDITOR_ID,
                    displays.toArray(new IRenderableDisplay[displays.size()]));
            if (newEditor != null) {
                // Reset extents on renderable displays when getting new editor
                for (IDisplayPane pane : newEditor.getDisplayPanes()) {
                    pane.getRenderableDisplay().getView().getExtent().reset();
                    pane.getRenderableDisplay()
                            .scaleToClientArea(pane.getBounds());
                }
            }
        } else {
            AbstractVizPerspectiveManager perspManager = VizPerspectiveListener
                    .getCurrentPerspectiveManager();
            if (perspManager != null) {
                newEditor = perspManager
                        .openNewEditor(EditorConstants.COMBO_EDITOR_ID);
            }
            if (newEditor == null) {
                StringBuilder msg = new StringBuilder(
                        "Opening new empty combo editor not supported by perspective");
                if (perspManager != null) {
                    msg.append(": ").append(perspManager.getPerspectiveId());
                }
                statusHandler.error(msg.toString(),
                        new VizException("Operation not supported"));
            }
        }

        return newEditor;
    }
}
