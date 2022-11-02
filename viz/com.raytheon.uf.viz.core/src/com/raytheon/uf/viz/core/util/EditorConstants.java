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
package com.raytheon.uf.viz.core.util;

/**
 * Constants for working with editors.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 21, 2022 8956       mapeters    Initial creation
 * Nov 02, 2022 8958       mapeters    Add MAP_EDITOR_ID
 *
 * </pre>
 *
 * @author mapeters
 */
public class EditorConstants {

    public static final String COMBO_EDITOR_ID = "com.raytheon.viz.ui.editor.ComboEditor";

    public static final String MAP_EDITOR_ID = "com.raytheon.viz.ui.glmap.GLMapEditor";

    /**
     * Prevent instantiation since everything is static.
     */
    private EditorConstants() {
    }
}
