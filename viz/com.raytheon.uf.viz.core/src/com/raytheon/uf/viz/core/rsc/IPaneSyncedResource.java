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
package com.raytheon.uf.viz.core.rsc;

import com.raytheon.uf.viz.core.IPane;

/**
 * Interface identifying a resource that should be kept in sync with resources
 * of the same type in other panes in the same editor.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 29, 2022 8792       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IPaneSyncedResource {

    /**
     * Update/create resources of the same type in the given pane so that they
     * are in sync with this resource.
     *
     * @param pane
     *            the pane to sync with this resource
     */
    void syncPane(IPane pane);
}
