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
package com.raytheon.uf.viz.core.drawables;

/**
 * Extension of {@link IRenderableDisplay} interface for renderable displays
 * that have a concept of scale that can be modified.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 22, 2022 8946       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IScalableRenderableDisplay extends IRenderableDisplay {

    public enum ScaleType {
        /*
         * Displays with ScaleType of NONE probably shouldn't implement this but
         * do
         */
        MAP, HEIGHT, NONE
    }

    /**
     * @return the current scale of this display
     */
    String getScale();

    /**
     * Set the scale of this display.
     *
     * @param scale
     *            the scale to set
     */
    void setScale(String scale);

    /**
     * Get the type of scales that this display uses.
     *
     * @return the scale type
     */
    ScaleType getScaleType();
}
