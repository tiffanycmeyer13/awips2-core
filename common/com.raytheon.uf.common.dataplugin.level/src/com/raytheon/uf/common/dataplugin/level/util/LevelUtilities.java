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
package com.raytheon.uf.common.dataplugin.level.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.measure.Unit;

import com.raytheon.uf.common.dataplugin.level.CompareType;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelFactory;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.time.DataTime;

import si.uom.SI;

/**
 * Level utilities
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Nov 21, 2009  3576     rjpeter     Initial version
 * Jan 30, 2014  2725     ekladstrup  Moved to common and removed
 *                                    usage of VizCommunicationException
 * Jan 23, 2014  2711     bsteffen    Get all levels from LevelFactory.
 * Sep 09, 2014  3356     njensen     Remove CommunicationException
 * Oct 28, 2022  8959     mapeters    Add setDataTimeLevel methods
 *
 * </pre>
 *
 * @author rjpeter
 */
public class LevelUtilities {
    public static boolean isPressureLevel(long levelId) {
        return isPressureLevel(
                LevelFactory.getInstance().getLevel(levelId).getMasterLevel());
    }

    public static boolean isPressureLevel(String masterLevelName) {
        return isPressureLevel(
                LevelFactory.getInstance().getMasterLevel(masterLevelName));
    }

    public static boolean isPressureLevel(Level level) {
        return isPressureLevel(level.getMasterLevel());
    }

    public static boolean isPressureLevel(MasterLevel ml) {
        Unit<?> unit = ml.getUnit();
        return unit != null && unit.isCompatible(SI.PASCAL);
    }

    /**
     * Set the level type and value on the given data time.
     *
     * @param time
     *            the time to set the level on
     * @param levelValue
     *            the level value to set
     * @param level
     *            the level to determine the level type from
     */
    public static void setDataTimeLevel(DataTime time, Double levelValue,
            Level level) {
        setDataTimeLevel(time, levelValue, level.getMasterLevel());
    }

    /**
     * Set the level type and value on the given data time.
     *
     * @param time
     *            the time to set the level on
     * @param levelValue
     *            the level value to set
     * @param masterLevel
     *            the master level to determine the level type from
     */
    public static void setDataTimeLevel(DataTime time, Double levelValue,
            MasterLevel masterLevel) {
        time.setLevel(levelValue, masterLevel.getName());
    }

    private static Map<String, NavigableSet<Level>> masterLevelToOrderedSet = null;

    /**
     * Get all single levels for a given masterlevel, ordered from lowest to
     * highest.
     *
     * @param masterLevelName
     * @return
     */
    public static synchronized NavigableSet<Level> getOrderedSetOfStandardLevels(
            String masterLevelName) {
        if (masterLevelToOrderedSet == null) {
            Comparator<Level> levelComparator = (l1, l2) -> {
                CompareType rel = l1.compare(l2);
                if (rel == CompareType.ABOVE) {
                    return 1;
                } else if (rel == CompareType.BELOW) {
                    return -1;
                } else {
                    return 0;
                }
            };
            Map<String, NavigableSet<Level>> masterLevelToOrderedSet = new HashMap<>();
            Collection<Level> allLevels = LevelFactory.getInstance()
                    .getAllLevels();
            for (Level level : allLevels) {
                NavigableSet<Level> levels = masterLevelToOrderedSet
                        .get(level.getMasterLevel().getName());
                if (levels == null) {
                    levels = new TreeSet<>(levelComparator);
                    masterLevelToOrderedSet
                            .put(level.getMasterLevel().getName(), levels);
                }
                if (level.isRangeLevel()) {

                    levels.add(level.getUpperLevel());
                    levels.add(level.getLowerLevel());
                } else {
                    levels.add(level);
                }
            }
            LevelUtilities.masterLevelToOrderedSet = masterLevelToOrderedSet;
        }
        return masterLevelToOrderedSet.get(masterLevelName);

    }
}