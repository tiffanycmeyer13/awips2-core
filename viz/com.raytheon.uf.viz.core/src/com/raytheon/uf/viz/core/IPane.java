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

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import com.raytheon.uf.viz.core.IPane.CanvasType;

/**
 *
 * Interface representing a single pane in an editor, e.g. the single map pane
 * within a single panel map editor, or one cross section pane out of the four
 * panes in a 4-panel editor.
 *
 * A pane implementation may contain multiple canvases, such as a cross section
 * pane containing the main graph canvas and the inset map canvas, whereas a map
 * pane just has a single map canvas. All panes must have a main canvas.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Remove getResourceType; add getMainCanvas(),
 *                                     getCanvasMap(), and CanvasType.SECONDARY_INSET
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IPane {

    /**
     * Enum indicating the general type/position of a display canvas.
     */
    public enum CanvasType {
        MAIN, INSET, SECONDARY_INSET
    }

    /**
     * @return true if this pane is currently visible, false otherwise
     */
    boolean isVisible();

    /**
     * @param visible
     *            the visibility state of this pane to set
     */
    void setVisible(boolean visible);

    /**
     * Determine if this pane contains the given canvas.
     *
     * @param canvas
     * @return true if this pane contains the canvas, false otherwise
     */
    boolean containsCanvas(IDisplayPane canvas);

    /**
     * Get the type of the active canvas in this pane. This is only applicable
     * if this pane as a whole is active.
     *
     * @return the active canvas type
     */
    CanvasType getActiveCanvasType();

    /**
     * Get the canvas of the given type.
     *
     * @param type
     *            the canvas type
     * @return the canvas of the given type, or null if no such canvas is in
     *         this pane
     */
    IDisplayPane getCanvas(CanvasType type);

    /**
     * Get the main canvas in this pane.
     *
     * This is just a convenience method that calls
     * {@link #getCanvas(CanvasType.MAIN)}, since all panes must have a main
     * canvas.
     *
     * @return the main canvas in this pane
     */
    default IDisplayPane getMainCanvas() {
        return getCanvas(CanvasType.MAIN);
    }

    /**
     * Get an unmodifiable mapping of canvas types to canvases that are in this
     * pane.
     *
     * @return the unmodifiable canvas map
     */
    Map<CanvasType, IDisplayPane> getCanvasMap();

    /**
     * Refresh this pane.
     */
    void refresh();

    /**
     * Set focus on this pane.
     */
    void setFocus();

    /**
     * Clear all data from this pane.
     */
    void clear();

    /**
     * Dispose this pane and all contained resources.
     */
    void dispose();

    /**
     * Register event handlers. The given listener should be attached to
     * standard event types, but implementations are free to attach any custom
     * event handlers as well.
     *
     * @param listener
     *            the listener to attach to standard events
     */
    void registerHandlers(Listener listener);

    /**
     * Get the SWT composite for this pane's area.
     *
     * @return the SWT composite
     */
    Composite getComposite();
}
