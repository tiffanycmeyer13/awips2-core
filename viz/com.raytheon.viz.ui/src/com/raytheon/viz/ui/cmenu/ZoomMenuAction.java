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
package com.raytheon.viz.ui.cmenu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.map.IMapDescriptor;

/**
 * ZoomMenuAction
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- ---------------------------------------
 * Dec 14, 2007             chammack    Initial Creation.
 * Jan 04, 2023 8989        mapeters    Only zoom canvases that are compatible
 *                                      with the active canvas
 *
 * </pre>
 *
 * @author chammack
 */
public class ZoomMenuAction extends AbstractRightClickAction
        implements IMenuCreator {

    private Menu menu;

    private int mapWidth;

    private int currentWidth;

    private static final double[] ZOOM_LEVELS = { 1, Math.sqrt(2), 2,
            2 * Math.sqrt(2), 4, 6.3, 10, 16 };

    /**
     * Constructor
     */
    public ZoomMenuAction(IDisplayPaneContainer container) {
        super(SWT.DROP_DOWN);
        setContainer(container);
    }

    @Override
    public void run() {

    }

    @Override
    public String getText() {
        return "Zoom";
    }

    @Override
    public void dispose() {
        if (menu != null) {
            menu.dispose();
        }
    }

    @Override
    public Menu getMenu(Menu parent) {
        if (menu != null) {
            menu.dispose();
        }

        menu = new Menu(parent);

        fillMenu(menu);
        return menu;
    }

    private class ZoomInternalAction extends Action {

        private final DecimalFormat df = new DecimalFormat("0.0x");

        private final int width;

        private final boolean preSelected;

        /**
         * Constructor
         *
         * @param width
         */
        public ZoomInternalAction(int width) {
            super("", Action.AS_RADIO_BUTTON);
            this.width = width;
            preSelected = width == currentWidth;
            this.setChecked(preSelected);
        }

        @Override
        public void run() {
            // Don't do anything if we were selected already
            if (preSelected) {
                return;
            }

            IDisplayPane activeCanvas = getContainer().getActiveDisplayPane();

            int mouseX = activeCanvas.getLastClickX();
            int mouseY = activeCanvas.getLastClickY();

            double[] c2 = activeCanvas.screenToGrid(mouseX, mouseY, 0.0);
            c2 = activeCanvas.getDescriptor().pixelToWorld(c2);
            double zoomLevel = (double) width / mapWidth;
            for (IDisplayPane canvas : getContainer()
                    .getCanvasesCompatibleWithActive()) {
                canvas.getRenderableDisplay().getExtent().reset();
                if (zoomLevel < 1.0) {
                    canvas.getRenderableDisplay().recenter(c2);
                    canvas.getRenderableDisplay().zoom(zoomLevel);
                } else {
                    canvas.getRenderableDisplay()
                            .scaleToClientArea(canvas.getBounds());
                }
                canvas.refresh();
            }

            getContainer().refresh();
        }

        @Override
        public String getText() {
            IDescriptor descriptor = container.getActiveDisplayPane()
                    .getDescriptor();
            if (descriptor instanceof IMapDescriptor) {
                return "" + (width) + " km";
            } else {
                double value = (double) mapWidth / width;
                return df.format(value);
            }
        }

    }

    @Override
    public Menu getMenu(Control parent) {
        if (menu != null) {
            menu.dispose();
        }

        menu = new Menu(parent);

        fillMenu(menu);

        return menu;
    }

    private void fillMenu(Menu menu) {
        IDescriptor descriptor = container.getActiveDisplayPane()
                .getDescriptor();
        if (descriptor instanceof IMapDescriptor) {
            mapWidth = ((IMapDescriptor) descriptor).getMapWidth() / 1000;
        } else {
            mapWidth = 10000;
        }

        IDisplayPane activeCanvas = getContainer().getActiveDisplayPane();
        currentWidth = (int) (mapWidth
                * activeCanvas.getRenderableDisplay().getZoom());

        // Create the basic list of zoom ratios
        List<Integer> widths = new ArrayList<>();

        for (double d : ZOOM_LEVELS) {
            int width = (int) (mapWidth / d);
            /*
             * If this width is close enough to the current width (within 1%)
             * then substitute in current width to avoid near duplicates
             */
            if (Math.abs(width - currentWidth) < mapWidth / 100) {
                width = currentWidth;
            }
            widths.add(width);
        }

        // Add in the current zoom ratio if necessary
        if (!widths.contains(currentWidth)) {
            widths.add(currentWidth);
            Collections.sort(widths);
            Collections.reverse(widths);
        }

        // Create the menu items
        for (int width : widths) {
            ActionContributionItem aci = new ActionContributionItem(
                    new ZoomInternalAction(width));
            aci.fill(menu, -1);

        }
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return this;
    }

    /**
     * @return the number of zoom level options that this menu provides
     */
    public static int getNumZoomLevels() {
        return ZOOM_LEVELS.length;
    }
}
