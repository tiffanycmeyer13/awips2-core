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
package com.raytheon.viz.ui.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.viz.ui.panes.AbstractPaneManager;

/**
 * Editor input for Viz editors
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 12, 2011            mschenke    Initial creation
 * Apr 01, 2022 8790       mapeters    Handle pane manager refactor
 *
 * </pre>
 *
 * @author mschenke
 */
public class EditorInput implements IEditorInput {

    private String name = "Viz Editor";

    private String toolTip;

    private ImageDescriptor imageDescriptor;

    private IRenderableDisplay[] renderableDisplays;

    private LoopProperties loopProperties;

    private AbstractPaneManager paneManager;

    /**
     * Construct an editor input with a renderable display and loop properties
     *
     * @param renderableDisplay
     */
    public EditorInput(LoopProperties props,
            IRenderableDisplay... renderableDisplays) {
        loopProperties = props;
        if (renderableDisplays.length == 0) {
            throw new IllegalArgumentException(
                    "You must supply at least one renderable display");
        }

        for (IRenderableDisplay display : renderableDisplays) {
            if (display == null) {
                throw new IllegalArgumentException(
                        "Renderable display must not be null");
            }
        }

        this.renderableDisplays = renderableDisplays;
    }

    /**
     * Construct an editor input with a renderable display
     *
     * @param renderableDisplay
     */
    public EditorInput(IRenderableDisplay... renderableDisplays) {
        this(new LoopProperties(), renderableDisplays);
    }

    public IRenderableDisplay[] getRenderableDisplays() {
        return renderableDisplays;
    }

    public void setRenderableDisplays(IRenderableDisplay[] renderable) {
        this.renderableDisplays = renderable;
    }

    public LoopProperties getLoopProperties() {
        return loopProperties;
    }

    public void setLoopProperties(LoopProperties loopProperties) {
        this.loopProperties = loopProperties;
    }

    public AbstractPaneManager getPaneManager() {
        return paneManager;
    }

    public void setPaneManager(AbstractPaneManager paneManager) {
        this.paneManager = paneManager;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    public void setImageDescriptor(ImageDescriptor imageDescriptor) {
        this.imageDescriptor = imageDescriptor;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return imageDescriptor != null ? imageDescriptor
                : ImageDescriptor.getMissingImageDescriptor();
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return toolTip == null ? getName() : toolTip;
    }

    public void setToolTipText(String toolTip) {
        this.toolTip = toolTip;
    }

}
