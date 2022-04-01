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
package com.raytheon.viz.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.raytheon.viz.ui.IRenameablePart;

/**
 * Action for renaming an IRenameablePart, typically tabs.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 25, 2015  4204      njensen     Initial creation
 * Mar 04, 2015  4204      njensen     Added validation
 * Mar 24, 2022  8790      mapeters    Moved perspective visibility checking to superclass,
 *                                     perspectiveId no longer has to be set
 *
 * </pre>
 *
 * @author njensen
 */
public class RenameTabAction extends ContributedEditorMenuAction {

    protected final IInputValidator validator = new TabNameValidator();

    public RenameTabAction() {
        super("Rename Tab", IAction.AS_PUSH_BUTTON);
    }

    @Override
    public boolean shouldBeVisible() {
        return super.shouldBeVisible() && getPart() instanceof IRenameablePart;
    }

    @Override
    public void run() {
        IWorkbenchPart wbPart = getPart();
        if (wbPart instanceof IRenameablePart) {
            IRenameablePart partToRename = (IRenameablePart) wbPart;
            String currentName = partToRename.getPartName();
            InputDialog userInput = new InputDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                    "Rename Tab", null, currentName, validator);
            if (userInput.open() == Window.OK) {
                String newName = userInput.getValue();
                partToRename.setPartName(newName);
            }
        }
    }

    private static class TabNameValidator implements IInputValidator {

        @Override
        public String isValid(String newText) {
            String result = null;
            if (newText == null || newText.length() < 1) {
                result = "Must enter a tab name.";
            } else if (newText.length() > 64) {
                result = "Must enter a shorter tab name.";
            }
            return result;
        }
    }

}
