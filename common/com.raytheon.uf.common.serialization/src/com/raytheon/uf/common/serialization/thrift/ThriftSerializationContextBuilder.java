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
package com.raytheon.uf.common.serialization.thrift;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.thrift.TConfiguration;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationContextBuilder;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Build a Thrift Serialization context
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 12, 2008           chammack  Initial creation
 * Jul 23, 2013  2215     njensen   Updated for thrift 0.9.0
 * Aug 06, 2013  2228     njensen   Added buildDeserializationContext(byte[],
 *                                  dsm)
 * May 27, 2021  8470     lsingh    Upgraded to Thrift 0.14.1. Added exception
 *                                  handling and TConfiguration support.
 * Jan 11, 2022  8341     randerso  Changed to use UFStatus
 * Sep 20, 2022  8341     lsingh    Changed thrift env var logs to INFO.
 *                                  Cleaned up log messages.
 *
 * </pre>
 *
 * @author chammack
 */

public class ThriftSerializationContextBuilder
        implements ISerializationContextBuilder {

    protected static final IUFStatusHandler log = UFStatus
            .getHandler(ThriftSerializationContextBuilder.class);

    /**
     * Thrift Configuration. This needs to be passed into all Thrift transport
     * objects.
     */
    private static final TConfiguration config;

    /**
     * Set up the Thrift configuration. Get the maxMessageSize, maxFrameDepth
     * and recursionDepth.
     */
    static {
        int maxMessageSize = 0;
        int maxFrameSize = 0;
        int recursionDepth = 0;

        // These values are set in /etc/profile.d/awips2Thrift.sh or
        // awips2Thrift.csh
        String maxMessageSizeStr = System.getenv("THRIFT_MAX_MESSAGE_SIZE");
        String maxFrameSizeStr = System.getenv("THRIFT_MAX_FRAME_SIZE");
        String recursionDepthStr = System.getenv("THRIFT_RECURSION_DEPTH");

        // Parse MAX_MESSAGE_SIZE
        /*
         * If MAX_MESSAGE_SIZE received is set too low, then Thrift will fail
         * during large transfers with the error: MaxMessageSize reached.
         */
        if (maxMessageSizeStr != null) {
            try {
                maxMessageSize = Integer.parseInt(maxMessageSizeStr);

                if (maxMessageSize > 0) {
                    log.info("THRIFT_MAX_MESSAGE_SIZE is set to "
                            + maxMessageSize + " bytes");
                } else {
                    log.info(
                            "THRIFT_MAX_MESSAGE_SIZE is not set or set to an invalid value. Setting THRIFT_MAX_MESSAGE_SIZE to default value of "
                                    + TConfiguration.DEFAULT_MAX_MESSAGE_SIZE
                                    + " bytes.");
                    maxMessageSize = TConfiguration.DEFAULT_MAX_MESSAGE_SIZE;
                }

            } catch (NumberFormatException e) {
                log.info(
                        "Could not parse value from THRIFT_MAX_MESSAGE_SIZE environmental variable. Value received was "
                                + maxMessageSizeStr
                                + ". Using default value of "
                                + TConfiguration.DEFAULT_MAX_MESSAGE_SIZE
                                + " bytes.");

                maxMessageSize = TConfiguration.DEFAULT_MAX_MESSAGE_SIZE;
            }

        } else {
            log.info(
                    "THRIFT_MAX_MESSAGE_SIZE environmental variable is not set. Setting THRIFT_MAX_MESSAGE_SIZE to default value of "
                            + TConfiguration.DEFAULT_MAX_MESSAGE_SIZE
                            + " bytes.");
            maxMessageSize = TConfiguration.DEFAULT_MAX_MESSAGE_SIZE;
        }

        // Parse MAX_FRAME_SIZE
        if (maxFrameSizeStr != null) {
            try {
                maxFrameSize = Integer.parseInt(maxFrameSizeStr);

                if (maxFrameSize > 0) {
                    log.info("THRIFT_MAX_FRAME_SIZE is set to " + maxFrameSize
                            + " bytes");
                } else {
                    log.info(
                            "THRIFT_MAX_FRAME_SIZE is not set or set to an invalid value. Setting THRIFT_MAX_FRAME_SIZE to default value of "
                                    + TConfiguration.DEFAULT_MAX_FRAME_SIZE
                                    + " bytes.");
                    maxFrameSize = TConfiguration.DEFAULT_MAX_FRAME_SIZE;
                }
            } catch (NumberFormatException e) {
                log.info(
                        "Could not parse value from THRIFT_MAX_FRAME_SIZE environmental variable. Value received was "
                                + maxFrameSizeStr + ". Using default value of "
                                + TConfiguration.DEFAULT_MAX_FRAME_SIZE + " bytes.");
                maxFrameSize = TConfiguration.DEFAULT_MAX_FRAME_SIZE;
            }
        } else {
            log.info(
                    "THRIFT_MAX_FRAME_SIZE environmental variable is not set. Setting THRIFT_MAX_FRAME_SIZE to default value of "
                            + TConfiguration.DEFAULT_MAX_FRAME_SIZE + " bytes.");
            maxFrameSize = TConfiguration.DEFAULT_MAX_FRAME_SIZE;

        }

        // Parse RECURSION_DEPTH
        if (recursionDepthStr != null) {
            try {
                recursionDepth = Integer.parseInt(recursionDepthStr);
                if (recursionDepth > 0) {
                    log.info("THRIFT_RECURSION_DEPTH is set to "
                            + recursionDepth);
                } else {
                    log.info(
                            "THRIFT_RECURSION_DEPTH is not set or set to an invalid value. Setting THRIFT_RECURSION_DEPTH to default value of "
                                    + TConfiguration.DEFAULT_RECURSION_DEPTH);
                    recursionDepth = TConfiguration.DEFAULT_RECURSION_DEPTH;
                }
            } catch (NumberFormatException e) {
                log.info(
                        "Could not parse value from THRIFT_RECURSION_DEPTH environmental variable. Value received was "
                                + recursionDepthStr
                                + ". Using default value of "
                                + TConfiguration.DEFAULT_RECURSION_DEPTH + ".");
                recursionDepth = TConfiguration.DEFAULT_RECURSION_DEPTH;
            }
        } else {
            log.info(
                    "THRIFT_RECURSION_DEPTH environmental variable is not set. Setting THRIFT_RECURSION_DEPTH to default value of "
                            + TConfiguration.DEFAULT_RECURSION_DEPTH);
            recursionDepth = TConfiguration.DEFAULT_RECURSION_DEPTH;
        }

        config = new TConfiguration(maxMessageSize, maxFrameSize,
                recursionDepth);
    }

    public ThriftSerializationContextBuilder() {

    }

    @Override
    public IDeserializationContext buildDeserializationContext(InputStream data,
            DynamicSerializationManager manager) throws SerializationException {
        try {
            TTransport transport = new TIOStreamTransport(config, data);
            SelfDescribingBinaryProtocol proto = new SelfDescribingBinaryProtocol(
                    transport);

            return new ThriftSerializationContext(proto, manager);
        } catch (TTransportException e) {
            throw new SerializationException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public ISerializationContext buildSerializationContext(OutputStream data,
            DynamicSerializationManager manager) throws SerializationException {
        try {
            TTransport transport = new TIOStreamTransport(config, data);
            SelfDescribingBinaryProtocol proto = new SelfDescribingBinaryProtocol(
                    transport);

            return new ThriftSerializationContext(proto, manager);
        } catch (TTransportException e) {
            throw new SerializationException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public IDeserializationContext buildDeserializationContext(byte[] data,
            DynamicSerializationManager manager) throws SerializationException {
        try {
            TTransport transport = new TMemoryInputTransport(config, data);
            SelfDescribingBinaryProtocol proto = new SelfDescribingBinaryProtocol(
                    transport);

            return new ThriftSerializationContext(proto, manager);
        } catch (TTransportException e) {
            throw new SerializationException(e.getLocalizedMessage(), e);
        }

    }

}
