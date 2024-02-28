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
package com.raytheon.viz.ui.panes;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IPane;
import com.raytheon.uf.viz.core.InputManager;

/**
 * Represents a single pane in a legacy editor (any editor that only supports
 * panes of the same type, as opposed to the combo editor). The legacy editors
 * primarily manage the {@link IDisplayPane}s directly instead of the
 * {@link IPane}s, so some {@link IPane} methods are not properly implemented.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 08, 2022 8792       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class LegacyPane extends AbstractPane {

    /**
     * Create a legacy pane for the given main canvas.
     *
     * @param mainCanvas
     *            the main canvas in this pane
     */
    public LegacyPane(IDisplayPane mainCanvas) {
        super(((VizDisplayPane) mainCanvas).getComposite());
        addCanvas(mainCanvas);
    }

    @Override
    public void addCanvas(IDisplayPane canvas) {
        // Only overridden to make public
        super.addCanvas(canvas);
    }

    @Override
    public void registerHandlers(InputManager inputManager) {
        throw new UnsupportedOperationException(
                "Legacy editors must register handlers directly on the canvases without going through LegacyPane.");
    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException(
                "Legacy editors must directly dispose of the canvases without going through LegacyPane.");
    }
}
