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
package com.raytheon.viz.ui;

/**
 *
 * Info about the type of editor needed for a particular action.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 23, 2022 8790       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class EditorTypeInfo {

    private final String editorId;

    private final boolean strict;

    /**
     * Constructor.
     *
     * @param editorId
     *            the ID of the editor type to use
     * @param strict
     *            true if an editor with the given ID must be used, false if a
     *            compatible editor of a different type may be used instead
     */
    public EditorTypeInfo(String editorId, boolean strict) {
        this.editorId = editorId;
        this.strict = strict;
    }

    /**
     * @return the ID of the editor type to use
     */
    public String getEditorId() {
        return editorId;
    }

    /**
     * Get whether or not the editor ID is strict. If true, an editor with the
     * specified ID must be used. If false, a compatible editor of a different
     * type may be used instead, although an editor with the specified ID should
     * still take priority if available.
     *
     * @return true if editor ID is strict, false otherwise
     */
    public boolean isStrict() {
        return strict;
    }
}
