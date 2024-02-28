package com.raytheon.uf.viz.localization.perspective;

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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page XML validation in the Localization perspective
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Mar 27, 2023  2029546    hvandam     Initial creation
 *
 * </pre>
 *
 * @author hvandam
 */

public class LocalizationPerspectivePreferences extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    @Override
    public void init(IWorkbench workbench) {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        setPreferenceStore(store);
        setDescription("XML Validation Preferences");
    }

    @Override
    protected void createFieldEditors() {
        BooleanFieldEditor editor = new BooleanFieldEditor(
                Activator.P_LOCALIZATION_VALIDATE_XML,
                "Enable well-formed XML validation on save",
                getFieldEditorParent());
        addField(editor);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!event.getNewValue().equals(event.getOldValue())) {
            super.propertyChange(event);
        }
    }
}
