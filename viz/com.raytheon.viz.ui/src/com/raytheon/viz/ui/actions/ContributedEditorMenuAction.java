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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Every type of user contributed action to the editor should extend this class
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 21, 2012            mnash       Initial creation
 * Mar 02, 2015  4204      njensen     Added perspectiveId and getWorkbenchPart()
 * Dec 23, 2015  5189      bsteffen    Track the workbench part instead of the presentation part.
 * Apr 01, 2022  8790      mapeters    Update shouldBeVisible to check perspective, remove
 *                                     unused/deprecated getWorkbenchPart()
 *
 * </pre>
 *
 * @author mnash
 */
public class ContributedEditorMenuAction extends Action {

    protected IWorkbenchPart part;

    protected String perspectiveId;

    /**
     *
     */
    public ContributedEditorMenuAction() {
        super();
    }

    /**
     * @param text
     * @param image
     */
    public ContributedEditorMenuAction(String text, ImageDescriptor image) {
        super(text, image);
    }

    /**
     * @param text
     * @param style
     */
    public ContributedEditorMenuAction(String text, int style) {
        super(text, style);
    }

    /**
     * @param text
     */
    public ContributedEditorMenuAction(String text) {
        super(text);
    }

    /**
     * @return true if this menu item should be visible, false if it should be
     *         hidden
     */
    public boolean shouldBeVisible() {
        if (perspectiveId == null) {
            return true;
        }

        // Check if we are in the correct perspective
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        if (activeWindow != null) {
            IWorkbenchPage activePage = activeWindow.getActivePage();
            if (activePage != null) {
                IPerspectiveDescriptor activePerspective = activePage
                        .getPerspective();
                if (activePerspective != null) {
                    return perspectiveId.equals(activePerspective.getId());
                }
            }
        }

        // Hide if no currently active window/page/perspective
        return false;
    }

    /**
     * @param part
     *            the part to set
     */
    public void setPart(IWorkbenchPart part) {
        this.part = part;
    }

    /**
     * @return the part
     */
    public IWorkbenchPart getPart() {
        return part;
    }

    /**
     * @return the perspective to include this menu item in, or null to include
     *         it in all perspectives
     */
    public String getPerspectiveId() {
        return perspectiveId;
    }

    /**
     * Set the perspective to include this menu item in, or null to include it
     * in all perspectives.
     *
     * @param perspectiveId
     *            the perspective ID to set
     */
    public void setPerspectiveId(String perspectiveId) {
        this.perspectiveId = perspectiveId;
    }
}
