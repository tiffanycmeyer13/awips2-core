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

import org.eclipse.swt.widgets.Composite;

/**
 * Extension of {@link AbstractPane} for functionality specific to combo editor
 * panes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 30, 2022 8792       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */

public abstract class AbstractComboPane extends AbstractPane {

    /**
     * Constructor.
     *
     * @param composite
     *            the SWT composite for this pane's area
     */
    protected AbstractComboPane(Composite composite) {
        super(composite);
        composite.addDisposeListener(e -> onCompositeDispose());
    }

    @Override
    public final void dispose() {
        /*
         * All custom dispose handling should be done in onCompositeDispose() so
         * that it occurs whether the SWT composite is disposed via this method
         * or via another way.
         */
        composite.dispose();
    }

    /**
     * Perform any necessary cleanup when this pane's SWT composite is disposed.
     */
    protected void onCompositeDispose() {
        /*
         * Contained canvases dispose themselves by listening for SWT canvas
         * dispose, so nothing to dispose here.
         */
    }
}
