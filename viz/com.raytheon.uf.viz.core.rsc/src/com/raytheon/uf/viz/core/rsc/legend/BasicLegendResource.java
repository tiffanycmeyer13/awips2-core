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
package com.raytheon.uf.viz.core.rsc.legend;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.GroupNamingCapability;

/**
 * An abstract class used to encapsulate common functionality shared between
 * multiple subclasses of {@code AbstractLegendResource}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 24, 2022  8745      dgilling     Initial creation: Code adapted from
 *                                      D2DLegendResource and GFELegendResource.
 *
 * </pre>
 *
 * @author dgilling
 */

public abstract class BasicLegendResource<T extends AbstractResourceData>
        extends AbstractLegendResource<T> {

    /** String for when no data is available */
    protected static final String NO_DATA = "No Data Available";

    /** String for when resource has data but not for current frame */
    protected static final String NOT_LOADED = "Not Loaded";

    /** Color for non visible resources */
    protected static final RGB GRAY = new RGB(127, 127, 127);

    protected BasicLegendResource(T resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
    }

    protected String getResourceGroupLegendString(
            AbstractVizResource<?, ?> rscGroup, IDescriptor descriptor,
            FramesInfo info, AbstractVizResource<?, ?> basis) {
        String groupName = rscGroup.getName();
        StringBuilder s = new StringBuilder();
        ResourceList list = ((IResourceGroup) rscGroup.getResourceData())
                .getResourceList();
        boolean tmb = false;
        boolean timeAgnostic = false;
        DataTime timeToUse = null;
        DataTime currTime = info.getFrameTimes()[info.getFrameIndex()];
        if (!rscGroup.hasCapability(GroupNamingCapability.class)
                || groupName == null) {
            for (ResourcePair rp : list) {
                if (rp.getResource() != null
                        && rp.getProperties().isVisible()) {
                    String name = getLegendString(rp, descriptor, info, true,
                            false);
                    if (timeToUse == null) {
                        timeToUse = info.getTimeForResource(rp.getResource());
                    }
                    if (s.length() == 0) {
                        s.append(name);
                    } else if (NO_DATA.contentEquals(s)) {
                        s = new StringBuilder(name);
                    } else if (s.indexOf(name) < 0 && !NO_DATA.equals(name)) {
                        s.append(" + ").append(name);
                    }
                }

                if (!tmb && rp.getResource() != null
                        && rp.getResource() == basis) {
                    tmb = true;
                }
            }

        } else {
            for (ResourcePair rp : list) {
                if (rp.getResource() != null && rp.getResource() == basis) {
                    tmb = true;
                    break;
                }
            }

            if (rscGroup.isTimeAgnostic()) {
                /*
                 * Group name is not null, we have the group naming capability,
                 * and we are time agnostic, set s to group name
                 */
                s = new StringBuilder(groupName);
                timeAgnostic = true;
            } else {
                /*
                 * Group name is not null, we have the group naming capability,
                 * and we are NOT time agnostic look at each resource and see if
                 * their time is that of the descriptor's frame
                 */
                for (ResourcePair rp : list) {
                    DataTime rscTime = descriptor
                            .getTimeForResource(rp.getResource());
                    if (currTime.equals(rscTime)) {
                        s = new StringBuilder(groupName);
                        timeToUse = currTime;

                        break;
                    }
                }

                if (s.length() == 0) {
                    /*
                     * None of our grouped resources have time as time match
                     * basis, grab first time that is not null in resource list
                     */
                    for (ResourcePair rp : list) {
                        DataTime rscTime = descriptor
                                .getTimeForResource(rp.getResource());
                        if (rscTime != null) {
                            s = new StringBuilder(groupName);
                            timeToUse = currTime;
                            break;
                        }
                    }
                }
            }
        }

        if (s.length() == 0) {
            s.append(NO_DATA);
        } else if (timeToUse != null) {
            if (!currTime.getLegendString()
                    .equals(timeToUse.getLegendString())) {
                s.append(NO_DATA);
            } else {
                s.append(" ").append(timeToUse.getLegendString());
            }
        } else if (!timeAgnostic) {
            s.append(" ").append(NOT_LOADED);
        }

        if (tmb) {
            s.insert(0, "* ");
        }

        return s.toString();
    }

    protected abstract String getLegendString(ResourcePair rp,
            IDescriptor descriptor, FramesInfo info, boolean fromResourceGroup,
            boolean includeTime);
}
