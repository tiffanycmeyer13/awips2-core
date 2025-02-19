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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.maps.scales.MapScalesManager.ManagedMapScale;
import com.raytheon.viz.ui.EditorUtil;

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
 * May 17, 2015			   mjames@ucar Added functionality to add loaded 
 * 									   area definitions as bundles
 * Oct 29, 2016            mjames@ucar Added WFO submenu and cleaned up 
 * 									   imports and comments
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class MapScalePopulator extends CompoundContributionItem {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
     */
    @SuppressWarnings("null")
	@Override
    protected IContributionItem[] getContributionItems() {
        MenuManager menuMgr = new MenuManager("Scales", "mapControls");
        IDisplayPaneContainer cont = EditorUtil.getActiveVizContainer();
        
        MenuManager wfoSubMenu = new MenuManager("WFO", null);
        MenuManager radarSubMenu = new MenuManager("WSR-88D", null);
        
        if ((cont != null )
                || EditorUtil.getActiveEditor() == null) {

        	/*
             * Contribute default scales 
             * this method doesn't use event listeners, the commands are inserted to
             * CommandContributionItem/CommandContributionItemParameter
             */
        	
            for (ManagedMapScale scale : MapScalesManager.getInstance()
                    .getScales()) {
        		String[] displayName = scale.getDisplayName().split("/");
        		if (displayName.length > 1) {

	                Map<String, String> parms = new HashMap<String, String>();
	                parms.put(MapScaleHandler.SCALE_NAME_ID, scale.getDisplayName());
	                CommandContributionItem item = new CommandContributionItem(
	                        new CommandContributionItemParameter(
	                                PlatformUI.getWorkbench(), null,
	                                MapScaleHandler.SET_SCALE_COMMAND_ID, parms,
	                                null, null, null,  displayName[1], null,
	                                null, CommandContributionItem.STYLE_PUSH, null,
	                                true));
	                wfoSubMenu.add(item);
	                
	                
        		} else {

	                Map<String, String> parms = new HashMap<String, String>();
	                parms.put(MapScaleHandler.SCALE_NAME_ID, scale.getDisplayName());
	                CommandContributionItem item = new CommandContributionItem(
	                        new CommandContributionItemParameter(
	                                PlatformUI.getWorkbench(), null,
	                                MapScaleHandler.SET_SCALE_COMMAND_ID, parms,
	                                null, null, null, scale.getDisplayName(), null,
	                                null, CommandContributionItem.STYLE_PUSH, null,
	                                true));
	                menuMgr.add(item);
        		}
            }  
            menuMgr.add(wfoSubMenu);
	    }
        
        return menuMgr.getItems();
    }

}
