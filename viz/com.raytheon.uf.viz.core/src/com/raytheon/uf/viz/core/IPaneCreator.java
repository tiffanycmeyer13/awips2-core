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
package com.raytheon.uf.viz.core;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Interface for creating an editor pane of a particular type.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 23, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Added getDefaultBackgroundDisplay,
 *                                     removed getResourceType
 *
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IPaneCreator {

    /**
     * Create the pane.
     *
     * @param paneContainer
     *            the editor/container that the new pane is going in
     * @param comp
     *            the composite for the new pane's area
     * @param display
     *            the display to initially render in the pane
     * @param panes
     *            the other panes that are already in the pane container
     * @return the created pane
     * @throws VizException
     */
    IPane createPane(IDisplayPaneContainer paneContainer, Composite comp,
            IRenderableDisplay display, List<IPane> panes) throws VizException;

    /**
     * Determine the default background display (e.g. the background map or
     * graph) that a pane should be initially created with to support the given
     * display then being loaded to it.
     *
     * This determines the "default" display in that it doesn't take into
     * account the current state (e.g. pan/zoom/scale) of panes of the same type
     * in the same editor.
     *
     * @param display
     *            the display to be loaded, to determine the background display
     *            from
     * @return the default background display to create the pane with
     */
    IRenderableDisplay getDefaultBackgroundDisplay(IRenderableDisplay display);
}
