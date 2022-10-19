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
package com.raytheon.viz.ui.actions;

import org.eclipse.ui.IEditorPart;

import com.raytheon.viz.ui.ComboEditorUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;

/**
 *
 * Action for opening a new combo editor from the editor tab context menu.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class NewComboEditorContextMenuAction
        extends ContributedEditorMenuAction {

    @Override
    public void run() {
        IEditorPart part = VizWorkbenchManager.getInstance().getActiveEditor();
        ComboEditorUtil.openNewComboEditor(part);
    }
}
