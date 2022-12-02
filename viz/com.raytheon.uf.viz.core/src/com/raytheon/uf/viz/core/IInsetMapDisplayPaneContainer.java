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
package com.raytheon.uf.viz.core;

/**
 * Interface for IDisplayPaneContainer objects for getting at the inset panes
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 07, 2011            mschenke    Initial creation
 * Apr 22, 2022 8791       mapeters    Moved from com.raytheon.uf.viz.xy.map,
 *                                     removed unused getInsetPanes(IDisplayPane)
 *
 * </pre>
 *
 * @author mschenke
 */
public interface IInsetMapDisplayPaneContainer {

    /**
     * Get the inset panes on the display
     *
     * @return
     */
    IDisplayPane[] getInsetPanes();
}
