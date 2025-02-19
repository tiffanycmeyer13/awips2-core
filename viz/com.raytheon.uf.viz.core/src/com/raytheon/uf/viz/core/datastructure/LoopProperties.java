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
package com.raytheon.uf.viz.core.datastructure;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.preferences.PreferenceConstants;

/**
 * This class is a container for the loop properties
 *
 * <pre>
 *
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Aug 30, 2007           randerso    Initial Creation.
 * Oct 22, 2013  2491     bsteffen    Remove ISerializableObject
 * Jun 23, 2014  3307     njensen     Fix xml serialization of looping field
 * Mar 12, 2018  6757     njensen     Added copy constructor
 * Jun 05, 2019  64619    tjensen     Update to line up with performance frame times
 *
 * </pre>
 *
 * @author randerso
 */
@XmlAccessorType(XmlAccessType.NONE)
public class LoopProperties {

    public enum LoopMode {
        Forward, Backward, Cycle
    }

    /** frame time increment in ms */
    public static final int FRAME_STEP = (int) (TimeUtil.MILLIS_PER_SECOND
            / VizApp.getCorePreferenceInt(PreferenceConstants.P_FPS));

    /** maximum frame time in ms */
    public static final int MAX_FRAME_TIME = (int) TimeUtil.MILLIS_PER_SECOND;

    /** value at which looping should stop */
    public static final int NOT_LOOPING = 1050;

    /**
     * Default frame time. Set it to as close to 250ms as the Performance FPS
     * allows.
     */
    public static final int DEFAULT_FRAME_TIME = Math.floorDiv(250, FRAME_STEP)
            * FRAME_STEP;

    /**
     * maximum dwell time. Set it to as close to 2500ms as the Performance FPS
     * allows
     */
    public static final int MAX_DWELL_TIME = Math.floorDiv(2500, FRAME_STEP)
            * FRAME_STEP;

    /** minimum dwell time in ms */
    public static final int MIN_DWELL_TIME = 0;

    /** frame time for forward animation in ms */
    @XmlElement
    private int fwdFrameTime = DEFAULT_FRAME_TIME;

    /** frame time for reverse animation in ms */
    @XmlElement
    private int revFrameTime = NOT_LOOPING;

    /**
     * first frame dwell time. Set it to as close to 700ms as the Performance
     * FPS allows
     */
    @XmlElement
    private int firstFrameDwell = Math.floorDiv(700, FRAME_STEP) * FRAME_STEP;

    /**
     * last frame dwell time. Set it to as close to 1500ms as the Performance
     * FPS allows
     */
    @XmlElement
    private int lastFrameDwell = Math.floorDiv(1500, FRAME_STEP) * FRAME_STEP;

    /** flag indicating if currently looping */
    private boolean isLooping = false;

    /** current loop mode */
    @XmlElement
    private LoopMode mode = LoopMode.Forward;

    private long lastDrawnTime = 0;

    private long currentDrawTime = 0;

    /**
     * Nullary constructor
     */
    public LoopProperties() {

    }

    /**
     * Copy constructor
     *
     * @param original
     */
    public LoopProperties(LoopProperties original) {
        this.firstFrameDwell = original.firstFrameDwell;
        this.fwdFrameTime = original.fwdFrameTime;
        this.isLooping = original.isLooping;
        this.lastFrameDwell = original.lastFrameDwell;
        this.mode = original.mode;
        this.revFrameTime = original.revFrameTime;
    }

    public int getFwdFrameTime() {
        return fwdFrameTime;
    }

    public void setFwdFrameTime(int fwdFrameTime) {
        this.fwdFrameTime = fwdFrameTime;
    }

    public int getRevFrameTime() {
        return revFrameTime;
    }

    public void setRevFrameTime(int revFrameTime) {
        this.revFrameTime = revFrameTime;
    }

    public int getFirstFrameDwell() {
        return firstFrameDwell;
    }

    public void setFirstFrameDwell(int firstFrameDwell) {
        this.firstFrameDwell = firstFrameDwell;
    }

    public int getLastFrameDwell() {
        return lastFrameDwell;
    }

    public void setLastFrameDwell(int lastFrameDwell) {
        this.lastFrameDwell = lastFrameDwell;
    }

    public LoopMode getMode() {
        return mode;
    }

    public void setMode(LoopMode mode) {
        this.mode = mode;
    }

    @XmlElement
    public boolean isLooping() {
        return isLooping;
    }

    public void setLooping(boolean isLooping) {
        this.isLooping = isLooping;

        if (PlatformUI.isWorkbenchRunning()) {
            ICommandService service = PlatformUI.getWorkbench()
                    .getService(ICommandService.class);

            service.refreshElements("com.raytheon.viz.ui.tools.looping.loop",
                    null);
        }
    }

    @Override
    public String toString() {
        return "LoopProperties{" + "\n   fwdFrameTime=" + fwdFrameTime
                + "\n   revFrameTime=" + revFrameTime + "\n   firstFrameDwell="
                + firstFrameDwell + "\n   lastFrameDwell=" + lastFrameDwell
                + "\n   mode=" + mode + "\n   isLooping=" + isLooping + "\n}";
    }

    /**
     * @return the shouldDraw
     */
    public boolean isShouldDraw() {
        return isLooping && lastDrawnTime == currentDrawTime;
    }

    /**
     * Sets the current time to be used when determining the amount of time we
     * have waited. This should be set once per paint so that multiple
     * descriptors will all loop at the same time
     */
    public void setCurrentDrawTime(long currentDrawTime) {
        this.currentDrawTime = currentDrawTime;
    }

    /**
     * If waitTime has elapsed since the last draw then isShouldDraw will become
     * true.
     *
     * @param waitTime
     */
    public void drawAfterWait(long waitTime) {
        if (currentDrawTime - lastDrawnTime > waitTime) {
            lastDrawnTime = currentDrawTime;
        }
    }
}
