/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastorage.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Abstract class for {@link IDataStorageAuditer} proxy implementations that
 * just send data storage events to the necessary JMS endpoints, where they'll
 * be picked up by the actual auditer implementation. At the moment, there are 5
 * URI's where storage events can be sent, leading to 5 different Auditor
 * instances. Events are mapped to a URI via traceId, where each event contains
 * the same traceIds.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Jun 28, 2022 8865       mapeters    Consolidate exception handling
 * Aug 24, 2022 8920       mapeters    Optimizations; Swap key/values for statuses.
 * Sep 26, 2022 8920       smoorthy    Scale Auditor; Send to one of multiple URIs.
 * Jan 05, 2023 8994       smoorthy    Add ability to disable sending audit events,
 *                                     primarily for debugging purposes.
 * Feb 03, 2023 9019       mapeters    Adjust for configurable number of audit threads.
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractDataStorageAuditerProxy
        implements IDataStorageAuditer {

    /**
     * If environment variable AUDITOR_PROXY_ENABLED is set to "true", this
     * proxy sends audit events to the auditor queues for processing. If set to
     * anything else, sending is disabled.
     */
    private final boolean auditorProxyEnabled = Boolean
            .parseBoolean(System.getenv("AUDITOR_PROXY_ENABLED"));

    protected final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private static final String[] URI_LIST = IntStream
            .rangeClosed(1, DataStorageAuditUtils.NUM_QUEUES)
            .mapToObj(i -> DataStorageAuditUtils.QUEUE_ROOT_NAME + i)
            .toArray(String[]::new);

    public AbstractDataStorageAuditerProxy() {
        if (auditorProxyEnabled) {
            statusHandler.info("sending enabled for auditor proxy");
        } else {
            statusHandler.info("sending disabled for auditor proxy");
        }
    }

    @Override
    public void processDataIds(MetadataAndDataId[] dataIds) {
        if (dataIds != null && dataIds.length > 0) {
            DataStorageAuditEvent event = new DataStorageAuditEvent();
            event.setDataIds(dataIds);
            processEvent(event);
        }
    }

    @Override
    public void processMetadataStatuses(
            Map<MetadataStatus, String[]> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            DataStorageAuditEvent event = new DataStorageAuditEvent();
            event.setMetadataStatuses(statuses);
            processEvent(event);
        }
    }

    @Override
    public void processDataStatuses(Map<DataStatus, String[]> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            DataStorageAuditEvent event = new DataStorageAuditEvent();
            event.setDataStatuses(statuses);
            processEvent(event);
        }

    }

    @Override
    public void processEvent(DataStorageAuditEvent event) {

        // return if Proxy is off
        if (!auditorProxyEnabled) {
            return;
        }

        /*
         * split up event into multiple events so that each event has has the
         * same traceId. Index in the array determines which resource to send
         * to.
         */
        DataStorageAuditEvent[] separatedEvents = splitDataStorageAuditEvent(
                event);

        for (int i = 0; i < separatedEvents.length; i++) {

            DataStorageAuditEvent sepEvent = separatedEvents[i];
            String dest = URI_LIST[i];

            try {
                if (sepEvent != null) {
                    send(sepEvent, dest);
                }
            } catch (Exception e) {
                statusHandler.error(
                        "Error sending event to " + dest + ": " + event, e);
            }
        }
    }

    private int traceIdToIndex(String traceId) {
        return Math.abs(traceId.hashCode()) % URI_LIST.length;
    }

    /**
     * Send the given audit event to the appropriate queue.
     *
     * @param event
     *            the audit event to send
     * @throws Exception
     *             if sending the event fails
     */
    protected abstract void send(DataStorageAuditEvent event, String uri)
            throws Exception;

    private Map<Integer, Map<MetadataStatus, List<String>>> mapMetadataStatusToQueueGroups(
            Map<MetadataStatus, String[]> metadataStatuses) {

        /* split metadataStatuses into multiple maps based on traceId mapping */

        // maps corresponding to the queues to send to
        Map<Integer, Map<MetadataStatus, List<String>>> mapOfMaps = new HashMap<>();

        // populate maps with traceIds for specific queues
        for (Entry<MetadataStatus, String[]> entry : metadataStatuses
                .entrySet()) {
            MetadataStatus status = entry.getKey();
            for (String traceId : entry.getValue()) {
                int index = traceIdToIndex(traceId);
                Map<MetadataStatus, List<String>> map = mapOfMaps
                        .computeIfAbsent(index, x -> new HashMap<>());
                map.computeIfAbsent(status, x -> new ArrayList<>())
                        .add(traceId);
            }
        }
        return mapOfMaps;
    }

    private Map<Integer, Map<DataStatus, List<String>>> mapDataStatusToQueueGroups(
            Map<DataStatus, String[]> dataStatuses) {

        /* split dataStatuses into multiple maps based on traceId mapping */

        // maps corresponding to the queues to send to
        Map<Integer, Map<DataStatus, List<String>>> mapOfMaps = new HashMap<>();

        // populate maps based on traceIdToIndex mapping
        for (Entry<DataStatus, String[]> entry : dataStatuses.entrySet()) {
            DataStatus status = entry.getKey();
            for (String traceId : entry.getValue()) {
                int index = traceIdToIndex(traceId);
                Map<DataStatus, List<String>> map = mapOfMaps
                        .computeIfAbsent(index, x -> new HashMap<>());
                map.computeIfAbsent(status, x -> new ArrayList<>())
                        .add(traceId);
            }
        }
        return mapOfMaps;
    }

    private Map<Integer, List<MetadataAndDataId>> mapMetadataAndDataIdtoQueueGroups(
            MetadataAndDataId[] dataIds) {
        /*
         * split dataIds into multiple lists, where elements are added based on
         * traceId mapping
         */
        // intialize the separate lists per queue
        Map<Integer, List<MetadataAndDataId>> allDataIdsMap = new HashMap<>();

        // add element to appropriate list based on traceId mapping
        for (MetadataAndDataId dataId : dataIds) {
            String traceId = dataId.getMetaId().getTraceId();
            int index = traceIdToIndex(traceId);
            allDataIdsMap.computeIfAbsent(index, x -> new ArrayList<>())
                    .add(dataId);
        }
        return allDataIdsMap;
    }

    private DataStorageAuditEvent[] splitDataStorageAuditEvent(
            DataStorageAuditEvent event) {

        DataStorageAuditEvent[] separatedEvents = new DataStorageAuditEvent[URI_LIST.length];

        // the event contains MetadataAndDataIds
        MetadataAndDataId[] dataIds = event.getDataIds();
        if (dataIds != null && dataIds.length > 0) {
            /*
             * split up dataIds so that same traceIds are grouped together and
             * go to a specific queue
             */
            Map<Integer, List<MetadataAndDataId>> dataIdLists = mapMetadataAndDataIdtoQueueGroups(
                    dataIds);
            for (Entry<Integer, List<MetadataAndDataId>> dataIdList : dataIdLists
                    .entrySet()) {
                int queueIndex = dataIdList.getKey();
                List<MetadataAndDataId> dataIdGroup = dataIdList.getValue();

                if (!dataIdGroup.isEmpty()) {
                    DataStorageAuditEvent sepEvent = separatedEvents[queueIndex];
                    if (sepEvent == null) {
                        sepEvent = new DataStorageAuditEvent();
                        separatedEvents[queueIndex] = sepEvent;
                    }

                    sepEvent.setDataIds(
                            dataIdGroup.toArray(new MetadataAndDataId[0]));
                }
            }
        }

        // metaStatuses
        Map<MetadataStatus, String[]> metaStatuses = event
                .getMetadataStatuses();
        if (metaStatuses != null && !metaStatuses.isEmpty()) {
            /*
             * split up status map so same traceIds are grouped together and go
             * to the same queue
             */
            Map<Integer, Map<MetadataStatus, List<String>>> metadataStatusMaps = mapMetadataStatusToQueueGroups(
                    metaStatuses);

            for (Entry<Integer, Map<MetadataStatus, List<String>>> metadataStatusMap : metadataStatusMaps
                    .entrySet()) {

                int queueIndex = metadataStatusMap.getKey();
                Map<MetadataStatus, List<String>> map = metadataStatusMap
                        .getValue();

                // create new map with Arrays for the traceIds to stay
                // consistent.
                Map<MetadataStatus, String[]> groupedStatuses = new HashMap<>();

                for (Entry<MetadataStatus, List<String>> entry : map
                        .entrySet()) {
                    groupedStatuses.put(entry.getKey(),
                            entry.getValue().toArray(String[]::new));
                }

                if (!groupedStatuses.isEmpty()) {
                    DataStorageAuditEvent sepEvent = separatedEvents[queueIndex];
                    if (sepEvent == null) {
                        sepEvent = new DataStorageAuditEvent();
                        separatedEvents[queueIndex] = sepEvent;
                    }
                    sepEvent.setMetadataStatuses(groupedStatuses);
                }
            }
        }

        // dataStatuses
        Map<DataStatus, String[]> dataStatuses = event.getDataStatuses();
        if (dataStatuses != null && !dataStatuses.isEmpty()) {
            /*
             * split up status map so same traceIds are grouped together and go
             * to the same queue
             */
            Map<Integer, Map<DataStatus, List<String>>> dataStatusMaps = mapDataStatusToQueueGroups(
                    dataStatuses);

            for (Entry<Integer, Map<DataStatus, List<String>>> dataStatusMap : dataStatusMaps
                    .entrySet()) {

                int queueIndex = dataStatusMap.getKey();
                Map<DataStatus, List<String>> map = dataStatusMap.getValue();

                // create new map with Arrays for the traceIds to stay
                // consistent.
                Map<DataStatus, String[]> groupedStatuses = new HashMap<>();
                for (Entry<DataStatus, List<String>> entry : map.entrySet()) {
                    groupedStatuses.put(entry.getKey(),
                            entry.getValue().toArray(String[]::new));
                }

                if (!groupedStatuses.isEmpty()) {
                    DataStorageAuditEvent sepEvent = separatedEvents[queueIndex];
                    if (sepEvent == null) {
                        sepEvent = new DataStorageAuditEvent();
                        separatedEvents[queueIndex] = sepEvent;
                    }
                    sepEvent.setDataStatuses(groupedStatuses);
                }
            }
        }
        return separatedEvents;
    }

}
