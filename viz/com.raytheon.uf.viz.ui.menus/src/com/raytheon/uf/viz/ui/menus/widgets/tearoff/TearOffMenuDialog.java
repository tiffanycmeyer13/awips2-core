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
package com.raytheon.uf.viz.ui.menus.widgets.tearoff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.common.time.ISimulatedTimeChangeListener;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.viz.core.ContextManager;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.ui.menus.widgets.BundleContributionItem;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * "Tear-off" menus to emulate A1 behavior
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Sep 14, 2011           mnash        Initial creation
 * Jan 09, 2013  1442     rferrel      Add Simulated Time Change Listener.
 * Apr 10, 2013  15185    D. Friedman  Preserve tear-offs over perspective
 *                                     switches.
 * Aug 21, 2014  15664    snaples      Updated dispose method to fix issue when
 *                                     closing perspective with tear offs open.
 * Oct 28, 2015  5054     randerso     Changed to use getter for perspective
 *                                     manager.
 * Nov 30, 2016  6016     randerso     Change timeChanged to call updateTimes on
 *                                     the UI thread
 * May 15, 2019  7850     tgurney      Replace reference to tab character with
 *                                     BundleContributionItem.TIME_SEPARATOR
 * Oct 01, 2020  8234     randerso     Improved positioning of tear-off
 *                                     submenus. Code cleanup.
 *
 * </pre>
 *
 * @author mnash
 */

public class TearOffMenuDialog extends CaveSWTDialog {

    private static final int MENU_OFFSET = 10;

    private final List<MenuPathElement> menuPath;

    private Menu menu;

    private Composite fullComp;

    /**
     * Listener to force the dialog's items' display to be updated when user
     * changes Simulated time.
     */
    private ISimulatedTimeChangeListener stcl;

    private Listener swtListener;

    public TearOffMenuDialog(Menu menu) {
        super(VizWorkbenchManager.getInstance().getCurrentWindow().getShell(),
                SWT.DIALOG_TRIM | SWT.RESIZE, CAVE.DO_NOT_BLOCK);
        this.menuPath = getMenuPath(menu);
        this.menu = menu;
        String text = menu.getParentItem().getText();

        // handle for the & that makes key bindings
        setText(text.replace("&", ""));
    }

    @Override
    protected void initializeComponents(final Shell shell) {
        shell.setData(this);
        // allow for scrolling if necessary
        ScrolledComposite scrolledComp = new ScrolledComposite(shell,
                SWT.V_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        scrolledComp.setLayoutData(gd);
        fullComp = new Composite(scrolledComp, SWT.NONE);
        GridLayout fullLayout = new GridLayout();
        fullLayout.marginHeight = 0;
        fullLayout.marginWidth = 0;
        // don't want any space between the two controls
        fullLayout.horizontalSpacing = 0;
        fullComp.setLayout(fullLayout);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        fullComp.setLayoutData(gd);

        // go through menu items and build MenuItemComposite for each item,
        // which handles all the selection and color of the "MenuItem" in the
        // dialog
        MenuItem[] items = getTargetMenu().getItems();
        int radioGroup = 0;
        for (int i = 1; i < items.length; i++) {
            MenuItem item = items[i];
            MenuItemComposite comp = new MenuItemComposite(fullComp, SWT.NONE);

            if (item.getStyle() == SWT.RADIO) {
                comp.setData("radioGroup", radioGroup);
            } else {
                radioGroup++;
            }

            GridLayout layout = new GridLayout(2, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            comp.setLayout(layout);
            gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            comp.setLayoutData(gd);

            // add the labels to the dialog with each of the MenuItems
            comp.addLabels(item, SWT.NONE);
        }
        scrolledComp.setContent(fullComp);
        scrolledComp.setExpandHorizontal(true);
        scrolledComp.setExpandVertical(true);
        scrolledComp.setMinSize(fullComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        shell.setMinimumSize(150, fullComp.getSize().y);

        swtListener = event -> updateItems();
        shell.addListener(SWT.Show, swtListener);
        menu.addListener(SWT.Show, swtListener);
    }

    @Override
    protected void preOpened() {
        Point point = getDisplay().getCursorLocation();
        point.x -= MENU_OFFSET;
        point.y -= MENU_OFFSET;
        shell.setLocation(point);
    }

    @Override
    protected void opened() {
        final IServiceLocator locator = PlatformUI.getWorkbench();

        Listener activate = event -> ContextManager.getInstance(locator)
                .activateContexts(getPerspectiveManager());
        Listener deactivate = event -> {
            if (getDisplay().getActiveShell() == getShell()) {
                ContextManager.getInstance(locator)
                        .deactivateContexts(getPerspectiveManager());
            }
        };

        addListener(SWT.Activate, activate);
        addListener(SWT.Deactivate, deactivate);
        addListener(SWT.Close, deactivate);

        activate.handleEvent(new Event());

        stcl = () -> VizApp.runAsync(() -> updateItems());
        SimulatedTime.getSystemTime().addSimulatedTimeChangeListener(stcl);
    }

    @Override
    protected void disposed() {
        SimulatedTime.getSystemTime().removeSimulatedTimeChangeListener(stcl);
        for (Control control : fullComp.getChildren()) {
            control.dispose();
        }

        shell.removeListener(SWT.Show, swtListener);
        if (!menu.isDisposed()) {
            menu.removeListener(SWT.Show, swtListener);
        }
        super.disposed();
    }

    /**
     * Force update of item's display.
     */
    private void updateItems() {
        Menu menu = getMenuIfAvailable();
        if (menu == null) {
            return;
        }
        for (MenuItemComposite mic : getMenuItemComposites()) {
            mic.reconnect();
        }
        for (MenuItem item : menu.getItems()) {
            if (item.getData() instanceof BundleContributionItem) {
                ((BundleContributionItem) item.getData()).refreshText();
            }
        }
    }

    private List<MenuItemComposite> getMenuItemComposites() {
        List<MenuItemComposite> result = new ArrayList<>();
        for (Control c : fullComp.getChildren()) {
            if (c instanceof MenuItemComposite) {
                result.add((MenuItemComposite) c);
            }
        }
        return result;
    }

    private Menu getMenuIfAvailable() {
        if (menu == null || menu.isDisposed()) {
            menu = findMenu();
        }
        return menu;
    }

    /* package */Menu getTargetMenu() {
        Menu menu = getMenuIfAvailable();
        if (menu == null) {
            throw new IllegalStateException(String.format(
                    "Tear-off menu %s is not available", shell.getText()));
        }
        if (menu.getItems().length == 0) {
            tryToFillMenu(menu);
        }
        return menu;
    }

    private void tryToFillMenu(Menu menu) {
        /*
         * Menu may not have been created so call listeners. This still does not
         * work if all of the menu items need the workbench window to be active
         * in order to be enabled.
         */

        Shell shell = this.shell.getParent().getShell();
        shell.setActive();
        while (shell.getDisplay().readAndDispatch()) {
            // nothing
        }

        Event event = new Event();
        event.type = SWT.Show;
        menu.notifyListeners(SWT.Show, event);
        event = new Event();
        event.type = SWT.Hide;
        menu.notifyListeners(SWT.Hide, event);
    }

    private Menu findMenu() {
        /*
         * NOTE: Assuming shell.getParent().getShell() is the workbench window.
         */
        Menu container = shell.getParent().getShell().getMenuBar();
        MenuPathElement lastPathElement = null;
        for (MenuPathElement element : menuPath) {
            MenuItem mi = findItem(container, element);
            if (mi == null) {
                return null;
            }
            Menu mim = mi.getMenu();
            if (mim == null) {
                throw new IllegalStateException(
                        String.format("Could not get target menu \"%s\" in %s",
                                element.getName(),
                                lastPathElement != null
                                        ? '"' + lastPathElement.getName() + '"'
                                        : "menu bar"));
            }
            tryToFillMenu(mim);
            container = mim;
            lastPathElement = element;
        }
        return container;
    }

    /**
     * Identifies a specific item in an SWT menu. It has been observed that
     * associated data of a menu item maintains the same identity during a CAVE
     * session even if the MenuItem is recreated. However, the associated data
     * is not always unique. Menu item text is used to differentiate.
     */
    static class MenuPathElement {
        private Object data;

        private String cleanText;

        public MenuPathElement(MenuItem item) {
            data = item.getData();
            cleanText = getCleanMenuItemText(item.getText());
        }

        public int getMatchLevel(MenuItem item) {
            int level = 0;
            if (item.getData() == data) {
                ++level;
            }
            if (cleanText.equals(item.getText())) {
                ++level;
            }
            return level;
        }

        public String getName() {
            if (cleanText != null && cleanText.length() > 0) {
                return cleanText;
            }
            Object value = data;
            if (value instanceof MenuManager) {
                value = ((MenuManager) value).getId();
            } else if (value instanceof ContributionItem) {
                value = ((ContributionItem) value).getId();
            }
            return String.valueOf(value);
        }

        /**
         * Return the portion of a menu item's title that should not change over
         * time
         */
        private static String getCleanMenuItemText(String text) {
            int pos = text.indexOf(BundleContributionItem.TIME_SEPARATOR);
            if (pos >= 0) {
                return text.substring(0, pos);
            }
            return text;
        }
    }

    protected static MenuItem findItem(Menu menu, MenuPathElement pe) {
        MenuItem best = null;
        int bestLevel = 0;
        for (MenuItem item : menu.getItems()) {
            int matchLevel = pe.getMatchLevel(item);
            if (matchLevel > bestLevel) {
                bestLevel = matchLevel;
                best = item;
            }
        }
        return best;
    }

    private static List<MenuPathElement> getMenuPath(Menu menu) {
        List<MenuPathElement> data = new ArrayList<>();
        while (menu != null) {
            MenuItem mi = menu.getParentItem();
            if (mi == null) {
                break;
            }
            data.add(new MenuPathElement(mi));
            menu = menu.getParentMenu();
        }
        Collections.reverse(data);
        return data;
    }
}
