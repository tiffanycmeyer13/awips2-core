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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IPane;

/**
 *
 * Abstract implementation of {@link IPane} for functionality shared among
 * different pane types.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2022 8790       mapeters    Initial creation
 * Apr 22, 2022 8791       mapeters    Store canvases in a map and abstract out
 *                                     a lot more functionality to here
 * Sep 30, 2022 8792       mapeters    Move dispose functionality to new
 *                                     AbstractComboPane subclass, move active
 *                                     canvas type tracking here from subclasses
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractPane implements IPane {

    protected final Composite composite;

    private CanvasType activeCanvasType = CanvasType.MAIN;

    private final Map<CanvasType, IDisplayPane> canvasMap = new HashMap<>();

    /**
     * Constructor.
     *
     * @param composite
     *            the SWT composite for this pane's area
     */
    protected AbstractPane(Composite composite) {
        this.composite = composite;
    }

    @Override
    public Composite getComposite() {
        return composite;
    }

    /**
     * Add the canvas of the given type to this pane. This just adds it to our
     * map tracking the canvases; the canvas should be fully initialized before
     * calling this.
     *
     * This should only be called once for each canvas type in this pane, and
     * only during construction.
     *
     * @param type
     *            the type of canvas to add
     * @param canvas
     *            the canvas
     */
    protected void addCanvas(IDisplayPane canvas) {
        CanvasType type = canvas.getType();
        IDisplayPane prevCanvas = canvasMap.put(type, canvas);
        if (prevCanvas != null) {
            throw new UnsupportedOperationException(
                    "Illegal replacement of canvas type: " + type);
        }

        if (type != CanvasType.MAIN) {
            canvas.addListener(SWT.MouseEnter,
                    event -> activeCanvasType = type);
            canvas.addListener(SWT.MouseExit,
                    event -> activeCanvasType = CanvasType.MAIN);
        }
    }

    @Override
    public IDisplayPane getCanvas(CanvasType type) {
        return canvasMap.get(type);
    }

    @Override
    public Map<CanvasType, IDisplayPane> getCanvasMap() {
        return Collections.unmodifiableMap(canvasMap);
    }

    @Override
    public boolean containsCanvas(IDisplayPane canvas) {
        return canvasMap.values().stream().anyMatch(c -> c == canvas);
    }

    @Override
    public CanvasType getActiveCanvasType() {
        return activeCanvasType;
    }

    @Override
    public boolean isVisible() {
        return getMainCanvas().isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        for (IDisplayPane canvas : canvasMap.values()) {
            canvas.setVisible(visible);
        }
    }

    @Override
    public void refresh() {
        canvasMap.values().forEach(IDisplayPane::refresh);
    }

    @Override
    public void setFocus() {
        getMainCanvas().setFocus();
    }

    @Override
    public void clear() {
        canvasMap.values().forEach(IDisplayPane::clear);
    }
}
