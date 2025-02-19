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
package com.raytheon.viz.ui.tools;

import java.util.Collections;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;

/**
 * Handle loop speed increases/decreases
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 27, 2009           mschenke  Initial creation
 * Jun 05, 2019  64619    tjensen   Renamed from PageUpDownKey to
 *                                  LoopSpeedManager
 *
 * </pre>
 *
 * @author mschenke
 */

public class LoopSpeedManager {
    static Command loopTool;
    static {
        ICommandService service = PlatformUI.getWorkbench()
                .getService(ICommandService.class);
        loopTool = service.getCommand("com.raytheon.viz.ui.tools.looping.loop");
    }

    /**
     * Execute the functionality for Speed up
     *
     * @param editor
     */
    public static void handleSpeedUp(IDisplayPaneContainer editor) {
        LoopProperties props = editor.getLoopProperties();
        int time = props.getFwdFrameTime();
        time -= LoopProperties.FRAME_STEP;
        if (time >= LoopProperties.FRAME_STEP) {
            props.setFwdFrameTime(time);
        }

        if (props.isLooping() == false) {
            executeTool();
        }
        editor.setLoopProperties(props);
    }

    /**
     * Execute the functionality for Speed down
     *
     * @param editor
     */
    public static void handleSpeedDown(IDisplayPaneContainer editor) {
        LoopProperties props = editor.getLoopProperties();
        int time = props.getFwdFrameTime();
        if (time != LoopProperties.NOT_LOOPING) {
            time += LoopProperties.FRAME_STEP;
            if (time == LoopProperties.NOT_LOOPING) {
                if (props.isLooping()) {
                    executeTool();
                }
            } else if (props.isLooping() == false) {
                executeTool();
            }
        }
        props.setFwdFrameTime(time);
        editor.setLoopProperties(props);
    }

    private static void executeTool() {
        try {
            loopTool.executeWithChecks(new ExecutionEvent(loopTool,
                    Collections.EMPTY_MAP, null, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
