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
package com.raytheon.uf.viz.core.maps.scales;

import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.maps.scales.MapScalesManager.ManagedMapScale;

/**
 * UI populator for map scales
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 7, 2010             mschenke    Initial creation
 * Mar 21, 2013       1638 mschenke    Made map scales not tied to d2d
 * Oct 10, 2013       2104 mschenke    Switched to use MapScalesManager
 * Oct 03, 2022       8946 mapeters    Updated for new combo editor
 * Mar 06, 2024          srcarter@ucar Attempt to add back WFO submenu from mjames
 *
 * </pre>
 *
 * @author mschenke
 */
public class MapScalePopulator extends CompoundContributionItem {

    @SuppressWarnings("null")
	@Override
    protected IContributionItem[] getContributionItems() {
        MenuManager menuMgr = new MenuManager("Scales", "mapControls");
        
        MenuManager wfoSubMenu = new MenuManager("WFO", null);
        
        for (ManagedMapScale scale : MapScalesManager.getInstance()
                .getScales()) {
            
            String displayName = scale.getDisplayName();
            String[] displayNameVal = displayName.split("/");
            
            if(displayNameVal.length > 1) {
                displayName = displayNameVal[1];
            }
            
            Map<String, String> parms = Map.of(VizConstants.MAP_SCALE_ID,
                    scale.getDisplayName());
            CommandContributionItem item = new CommandContributionItem(
                    new CommandContributionItemParameter(
                            PlatformUI.getWorkbench(), null,
                            MapScaleHandler.SET_SCALE_COMMAND_ID, parms, null,
                            null, null, displayName, null, null,
                            CommandContributionItem.STYLE_PUSH, null, true));
            
            if(displayNameVal.length > 1) {
                wfoSubMenu.add(item);
            }else {
                menuMgr.add(item);
            }
            menuMgr.add(wfoSubMenu);
        }
        
        return menuMgr.getItems();
    }

}
