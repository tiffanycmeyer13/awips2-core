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
package com.raytheon.uf.viz.core.drawables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.TransformFactory;
import com.raytheon.uf.common.geospatial.adapter.GridGeometryAdapter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.ResourceGroup;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.time.TimeMatchingJob;

/**
 * AbstractDescriptor
 *
 * <pre>
 *
 *    SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 15, 2007           chammack  Initial Creation.
 * Nov 30, 2007  461      bphillip  Using VizTime now for time matching
 * Oct 22, 2009  3348     bsteffen  added ability to limit number of frames
 * Jul 03, 2013  2154     bsteffen  Ensure all resource groups get removed from
 *                                  the time matcher.
 * Apr 09, 2014  2997     randerso  Stopped printing stack trace for otherwise
 *                                  ignored exception
 * May 13, 2015  4461     bsteffen  Add setFrameCoordinator
 * Nov 03, 2016  5976     bsteffen  Remove unused deprecated methods.
 * Jun 12, 2017  6297     bsteffen  Make listeners thread safe.
 * Jan 04, 2018  6753     bsteffen  Remove unneccesary time matcher operations.
 * Oct 01, 2019  69438    ksunil    When the frame changes, notify frame number
 *                                  listener.
 * Dec 02, 2019  71868    tjensen   Change updateUI call in notifyFrameChanged()
 *                                  to be async
 *
 * </pre>
 *
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractDescriptor extends ResourceGroup
        implements IDescriptor {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractDescriptor.class);

    private static class TimeManager {
        protected DataTime[] frames;

        protected AbstractTimeMatcher timeMatcher;

        protected int numberOfFrames = 12;

        public TimeManager() {
            Integer frames = ((Integer) VizGlobalsManager.getCurrentInstance()
                    .getPropery(VizConstants.FRAMES_ID));
            if (frames != null) {
                numberOfFrames = frames.intValue();
            }
        }
    }

    protected final Set<IFrameChangedListener> listeners = new CopyOnWriteArraySet<>();

    protected TimeManager timeManager = new TimeManager();

    /** The renderable display descriptor is loaded to */
    protected IRenderableDisplay renderableDisplay;

    /** The time matching map */
    protected ConcurrentHashMap<AbstractVizResource<?, ?>, DataTime[]> timeMatchingMap;

    /** The index of the currently shown time */
    private int frameIndex;

    /** The time to restore to when setting frames */
    private DataTime restoredTime = null;

    /** The number of frames */
    @XmlElement
    protected int limitedNumberOfFrames = Integer.MAX_VALUE;

    /** The frame coordination object */
    protected IFrameCoordinator frameCoordinator;

    protected MathTransform worldToPixel;

    protected MathTransform pixelToWorld;

    /** The spatial grid for the descriptor */
    private GeneralGridGeometry gridGeometry;

    public AbstractDescriptor(GeneralGridGeometry gridGeometry) {
        this();
        this.gridGeometry = gridGeometry;
        init();
    }

    /**
     * Constructor
     */
    public AbstractDescriptor() {
        super();
        frameCoordinator = new FrameCoordinator(this);
        timeMatchingMap = new ConcurrentHashMap<>();
        resourceList.addPreAddListener(new ResourceList.AddListener() {

            @Override
            public void notifyAdd(ResourcePair rp) throws VizException {
                preAddListener(rp);
            }

        });

        resourceList.addPostAddListener(new ResourceList.AddListener() {

            @Override
            public void notifyAdd(ResourcePair rp) throws VizException {
                postAddListener(rp);
            }

        });

        resourceList.addPreRemoveListener(new ResourceList.RemoveListener() {

            @Override
            public void notifyRemove(ResourcePair rp) throws VizException {
                preRemoveListener(rp.getResource());
            }

        });

        resourceList.addPostRemoveListener(new ResourceList.RemoveListener() {

            @Override
            public void notifyRemove(ResourcePair rp) throws VizException {
                postRemoveListener(rp.getResource());
                if (!resourceList.isEmpty()) {
                    TimeMatchingJob.scheduleTimeMatch(AbstractDescriptor.this);
                }
                if ((renderableDisplay != null)) {
                    IDisplayPaneContainer container = renderableDisplay
                            .getContainer();
                    if (container != null) {
                        for (IDisplayPane pane : container.getDisplayPanes()) {
                            IDescriptor descriptor = pane.getDescriptor();
                            if (descriptor != AbstractDescriptor.this) {
                                TimeMatchingJob.scheduleTimeMatch(descriptor);
                            }
                        }
                    }
                }
            }
        });
    }

    protected void postAddListener(ResourcePair rp) {
        if ((rp.getResource() != null) && (getTimeMatcher() != null)) {
            // We need to run time matching immediately beacuse order
            // constructed is important for time matching so we must do it now
            // instead of scheduling since another resource could be added by
            // the time it runs
            AbstractTimeMatcher tm = getTimeMatcher();
            tm.redoTimeMatching(rp.getResource());
            try {
                tm.redoTimeMatching(this);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void preAddListener(ResourcePair rp) {

        AbstractVizResource<?, AbstractDescriptor> resource = (AbstractVizResource<?, AbstractDescriptor>) rp
                .getResource();

        resource.setDescriptor(this);

        if (resource.getResourceData() instanceof IResourceGroup) {
            ResourceList rl = ((IResourceGroup) resource.getResourceData())
                    .getResourceList();
            synchronized (rl) {
                for (ResourcePair rp1 : rl) {
                    preAddListener(rp1);
                }
            }
        }

    }

    protected void preRemoveListener(AbstractVizResource<?, ?> resource) {

    }

    protected void postRemoveListener(AbstractVizResource<?, ?> resource) {
        /*
         * If this resource still exists somewhere on the descriptor then don't
         * process it. This occurs when a resource is moved from the descriptors
         * list to a resource group on the descriptor such as during an image
         * combine.
         */
        if (rscInGroup(this, resource)) {
            return;
        }

        if (getTimeMatcher() != null) {
            getTimeMatcher().handleRemove(resource, this);
        }

        if (resource != null) {
            synchronized (timeManager) {
                timeMatchingMap.remove(resource);
            }
            ResourceList rl = null;
            if (resource instanceof IResourceGroup) {
                rl = ((IResourceGroup) resource).getResourceList();
            } else if (resource.getResourceData() instanceof IResourceGroup) {
                rl = ((IResourceGroup) resource.getResourceData())
                        .getResourceList();
            }
            if (rl != null) {
                synchronized (rl) {
                    for (ResourcePair rp : rl) {
                        AbstractVizResource<?, ?> rsc = rp.getResource();
                        if (rsc != null) {
                            postRemoveListener(rsc);
                        }
                    }
                }
            }
        }
    }

    /**
     * Use getFramesInfo() for thread safe use!
     *
     * The times of the frames
     *
     * @return
     */
    @Override
    @Deprecated
    public DataTime[] getFrames() {
        return getFramesInfo().frameTimes;
    }

    @Override
    @Deprecated
    public int getCurrentFrame() {
        return getFramesInfo().getFrameIndex();
    }

    @Override
    public int getNumberOfFrames() {
        return Math.min(timeManager.numberOfFrames, limitedNumberOfFrames);
    }

    /**
     * used only to serialize the actual number of frames properly.
     *
     * @return
     */
    @XmlElement(name = "numberOfFrames")
    public int getNumberOfFramesSerialize() {
        return timeManager.numberOfFrames;
    }

    /**
     * used for (reverse?) serialization.
     *
     * @param val
     */
    public void setNumberOfFramesSerialize(int val) {
        timeManager.numberOfFrames = val;
    }

    @Override
    @Deprecated
    public int getFrameCount() {
        return getFramesInfo().getFrameCount();
    }

    @Override
    @Deprecated
    public void setFrame(int frame) {
        setFramesInfo(new FramesInfo(frame));
    }

    @Override
    public void setNumberOfFrames(int frameCount) {
        timeManager.numberOfFrames = frameCount;
    }

    @Override
    public boolean limitNumberOfFrames(int frameCount) {
        FramesInfo info = getFramesInfo();
        int frameIndex = info.frameIndex;
        DataTime[] frames = info.frameTimes;
        if (frameCount <= getNumberOfFrames()) {
            if ((frames != null) && (frameIndex >= 0)
                    && (frames.length > frameIndex)) {
                restoredTime = frames[frameIndex];
            }
            limitedNumberOfFrames = frameCount;
            return true;
        }
        return false;
    }

    @Override
    public boolean unlimitNumberOfFrames() {
        FramesInfo info = getFramesInfo();
        int frameIndex = info.frameIndex;
        DataTime[] frames = info.frameTimes;
        if (limitedNumberOfFrames <= getNumberOfFrames()) {
            if ((frames != null) && (frameIndex >= 0)
                    && (frames.length > frameIndex)) {
                restoredTime = frames[frameIndex];
            }
            limitedNumberOfFrames = Integer.MAX_VALUE;
            return true;
        }
        limitedNumberOfFrames = Integer.MAX_VALUE;
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        // Remove all the resources so that the removal handlers execute
        List<AbstractVizResource<?, ?>> rscs = new ArrayList<>();
        for (ResourcePair rp : resourceList) {
            rscs.add(rp.getResource());
        }

        for (AbstractVizResource<?, ?> rsc : rscs) {
            resourceList.remove(rsc);
        }

    }

    /**
     * Lock object AbstractDescriptor uses internally for frame locking
     */
    final Object getLockObject() {
        return timeManager;
    }

    @Override
    @Deprecated
    public void setDataTimes(DataTime[] dataTimes) {
        setFramesInfo(new FramesInfo(dataTimes));
    }

    /**
     * Returns reference to time matching map for reading/writing. Use
     * getTimeForResource(...) where possible if reading only!
     *
     * @return the timeMatchingMap
     */
    public Map<AbstractVizResource<?, ?>, DataTime[]> getTimeMatchingMap() {
        return timeMatchingMap;
    }

    /**
     * @return the timeMatcher
     */
    @Override
    @XmlElement
    public AbstractTimeMatcher getTimeMatcher() {
        return timeManager.timeMatcher;
    }

    /**
     * @param timeMatcher
     *            the timeMatcher to set
     */
    @Override
    public void setTimeMatcher(AbstractTimeMatcher timeMatcher) {
        this.timeManager.timeMatcher = timeMatcher;
    }

    @Override
    public void redoTimeMatching() throws VizException {
        if (timeManager.timeMatcher != null) {
            timeManager.timeMatcher.redoTimeMatching(this);
        }
    }

    @Override
    public DataTime getTimeForResource(AbstractVizResource<?, ?> rsc) {
        FramesInfo currInfo = getFramesInfo();
        return currInfo.getTimeForResource(rsc);
    }

    @Override
    public void synchronizeTimeMatching(IDescriptor other) {
        if (other instanceof AbstractDescriptor) {
            timeManager = ((AbstractDescriptor) other).timeManager;
        }
    }

    private void resetValidTimes(DataTime[] origTimes) {
        DataTime[] frames = getFramesInfo().getFrameTimes();
        if (frames != null) {
            for (DataTime dt : frames) {
                if (origTimes != null) {
                    for (DataTime origTime : origTimes) {
                        if (origTime.equals(dt)) {
                            dt.setVisible(origTime.isVisible());
                        }
                    }
                } else {
                    dt.setVisible(true);
                }
            }
        }
    }

    @Override
    public IRenderableDisplay getRenderableDisplay() {
        return renderableDisplay;
    }

    @Override
    public void setRenderableDisplay(IRenderableDisplay display) {
        if ((this.renderableDisplay == null)
                || (display.getDescriptor() == this)) {
            this.renderableDisplay = display;
        }
    }

    @Override
    public void addFrameChangedListener(IFrameChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeFrameChangedListener(IFrameChangedListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify the listeners that the frame changed
     *
     * @param oldTime
     * @param newTime
     */
    protected void notifyFrameChanged(DataTime oldTime, DataTime newTime) {
        for (IFrameChangedListener listener : listeners) {
            listener.frameChanged(this, oldTime, newTime);
        }
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                VizGlobalsManager.getCurrentInstance()
                        .updateUI(getRenderableDisplay().getContainer());
            }
        });
    }

    @Override
    public boolean isCompatible(IDescriptor other) {
        return this.getClass() == other.getClass();
    }

    @Override
    public void setFramesInfo(FramesInfo info) {
        // We should probably always verify but legacy code might set frames and
        // then index, the solution to this is to get rid of that code.
        if (info.setFrames && info.setIndex) {
            // Verify that this is valid
            String error = null;
            DataTime[] times = info.frameTimes;
            int idx = info.frameIndex;
            if ((times == null) && (idx >= 0)) {
                error = "Index should be less than zero when there are no frame times.";
            } else if ((times != null) && (idx >= times.length)) {
                error = "Index must be less than the number of frames.";
            } else if ((idx < 0) && (times != null) && (times.length > 0)) {
                error = "Index must be positive when frames are provided";
            }
            if (times != null) {
                for (DataTime time : times) {
                    if (time == null) {
                        error = "Descriptor should not contain null times";
                        break;
                    }
                }
            }

            if (error != null) {
                statusHandler.handle(Priority.SIGNIFICANT, "Error: " + error,
                        new VizException("Error setting FrameInfo"));
                return;
            }
        }
        DataTime oldTime, currTime;
        boolean frameChanged = false;
        synchronized (timeManager) {
            DataTime[] oldTimes = timeManager.frames;
            int oldIdx = this.frameIndex;
            if (info.setFrames) {
                if (info.frameTimes != null) {
                    DataTime[] newTimes = Arrays.copyOf(info.frameTimes,
                            info.frameTimes.length);
                    setFrameTimesInternal(newTimes);
                } else {
                    timeManager.frames = null;
                }
                if (!info.setIndex) {
                    setFrameInternal(frameCoordinator.determineFrameIndex(
                            oldTimes, oldIdx, timeManager.frames));
                }
            }
            if (info.setIndex) {
                setFrameInternal(info.frameIndex);
            }
            if (info.setMap) {
                timeMatchingMap = new ConcurrentHashMap<>(info.timeMap);
            }
            FramesInfo currInfo = getFramesInfo();
            FramesInfo oldInfo = new FramesInfo(oldTimes, oldIdx);
            oldTime = oldInfo.getCurrentFrame();
            currTime = currInfo.getCurrentFrame();
            if (((oldTime != null) && (!oldTime.equals(currTime)))
                    || ((currTime != null) && (!currTime.equals(oldTime)))) {
                frameChanged = true;
            }
        }
        if (frameChanged) {
            notifyFrameChanged(oldTime, currTime);
        }

    }

    @Override
    public FramesInfo getFramesInfo() {
        synchronized (timeManager) {
            DataTime[] frames = timeManager.frames;
            int idx = frameIndex;
            if (frames != null) {
                frames = Arrays.copyOf(frames, frames.length);
                if ((idx < 0) || (idx >= frames.length)) {
                    // This only happens for 4-panels with shared time managers.
                    idx = frames.length - 1;
                }
            } else {
                // It should already be -1 already but this is here for
                // certain 4 panels where the time manager is shared and the
                // index and frames are out of sync.
                idx = -1;
            }
            Map<AbstractVizResource<?, ?>, DataTime[]> timeMap = new HashMap<>(
                    timeMatchingMap);
            return new FramesInfo(frames, idx, timeMap);
        }
    }

    /**
     * @param frame
     */
    private void setFrameInternal(int frame) {
        if (frame != frameIndex) {
            this.frameIndex = frame;
        }
    }

    private void setFrameTimesInternal(DataTime[] dataTimes) {
        DataTime[] orig = timeManager.frames;
        timeManager.frames = dataTimes;
        resetValidTimes(orig);
        if (restoredTime != null) {
            boolean found = false;
            for (int i = 0; i < timeManager.frames.length; ++i) {
                DataTime time = timeManager.frames[i];
                if (time.equals(restoredTime)) {
                    frameIndex = i;
                    found = true;
                    break;
                }
            }
            if (!found) {
                setFrameInternal(timeManager.frames.length - 1);
            }
            restoredTime = null;
        }
    }

    @Override
    public IFrameCoordinator getFrameCoordinator() {
        return frameCoordinator;
    }

    public void setFrameCoordinator(IFrameCoordinator frameCoordinator) {
        this.frameCoordinator = frameCoordinator;
    }

    private void init() {
        try {
            setupTransforms();

            // reproject all resources contained in this descriptor
            ArrayList<ResourcePair> unProjectable = new ArrayList<>();
            for (ResourcePair rp : this.resourceList) {
                AbstractVizResource<?, ?> rsc = rp.getResource();
                if (rsc == null) {
                    continue;
                }
                try {
                    rsc.project(gridGeometry.getCoordinateReferenceSystem());
                } catch (VizException e) {
                    // TODO: what to do here?
                    unProjectable.add(rp);
                    statusHandler.handle(Priority.PROBLEM,
                            "Error projecting resource :: " + rsc.getName(), e);
                }
            }
            this.resourceList.removeAll(unProjectable);
        } catch (Exception e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "Error setting up Math Transforms,"
                                    + " this descriptor may not work properly",
                            e);
        }
    }

    protected void setupTransforms() throws Exception {
        GeneralGridGeometry gridGeometry = getGridGeometry();
        worldToPixel = TransformFactory.worldToGrid(gridGeometry,
                PixelInCell.CELL_CENTER);
        pixelToWorld = (worldToPixel != null ? worldToPixel.inverse() : null);
    }

    @Override
    public final CoordinateReferenceSystem getCRS() {
        if ((gridGeometry != null) && (gridGeometry.getEnvelope() != null)) {
            return gridGeometry.getEnvelope().getCoordinateReferenceSystem();
        } else {
            return null;
        }
    }

    @Override
    @XmlElement
    @XmlJavaTypeAdapter(value = GridGeometryAdapter.class)
    public final GeneralGridGeometry getGridGeometry() {
        return gridGeometry;
    }

    @Override
    public void setGridGeometry(GeneralGridGeometry geometry)
            throws VizException {
        this.gridGeometry = geometry;
        init();
    }

    @Override
    public final double[] pixelToWorld(double[] pixel) {
        double[] output = new double[3];
        double[] wpixel = pixel;

        if (pixel.length == 2) {
            wpixel = new double[] { pixel[0], pixel[1], 0 };
        }

        if (pixelToWorld != null) {
            try {
                pixelToWorld.transform(wpixel, 0, output, 0, 1);
            } catch (TransformException e) {
                // e.printStackTrace();
                return null;
            }
        } else {
            System.arraycopy(wpixel, 0, output, 0, wpixel.length);
        }

        return output;
    }

    @Override
    public final double[] worldToPixel(double[] world) {
        double[] output = new double[3];
        double[] input = world;
        if (world.length == 2) {
            input = new double[] { world[0], world[1], 0 };
        }

        if (worldToPixel != null) {
            try {
                worldToPixel.transform(input, 0, output, 0, 1);
            } catch (TransformException e) {
                return null;
            }
        } else {
            System.arraycopy(input, 0, output, 0, input.length);
        }

        return output;
    }

    @Override
    @Deprecated
    public void changeFrame(FrameChangeOperation operation,
            FrameChangeMode mode) {
        IFrameCoordinator.FrameChangeOperation fop = IFrameCoordinator.FrameChangeOperation
                .valueOf(operation.name());
        IFrameCoordinator.FrameChangeMode fmode = IFrameCoordinator.FrameChangeMode
                .valueOf(mode.name());
        getFrameCoordinator().changeFrame(fop, fmode);
    }

    protected static GeneralGridGeometry createGridGeometry(IExtent extent,
            CoordinateReferenceSystem crs) {
        GeneralEnvelope envelope = new GeneralEnvelope(2);
        envelope.setRange(0, extent.getMinX(), extent.getMaxX());
        envelope.setRange(1, extent.getMinY(), extent.getMaxY());
        envelope.setCoordinateReferenceSystem(crs);
        return new GridGeometry2D(new GeneralGridEnvelope(new int[] { 0, 0 },
                new int[] { (int) extent.getWidth(), (int) extent.getHeight() },
                false), envelope);
    }

    /**
     * Check if a resource is in the given group or any IResourceGroups that are
     * in the provided group. If any resourceData in the group is an
     * IResourceGroup then that group is also checked.
     *
     * @param group
     *            the group to check
     * @param resource
     *            the resource to check for.
     * @return true if the resource is present in the group or a subgroup.
     */
    private static boolean rscInGroup(IResourceGroup group,
            AbstractVizResource<?, ?> resource) {

        for (ResourcePair pair : group.getResourceList()) {
            if (pair.getResource() == resource) {
                return true;
            }
            AbstractResourceData resourceData = pair.getResourceData();
            if (resourceData instanceof IResourceGroup) {
                if (rscInGroup((IResourceGroup) resourceData, resource)) {
                    return true;
                }
            }
        }
        return false;
    }

}
