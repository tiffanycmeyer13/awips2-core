package com.raytheon.uf.viz.core.requests;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.WebService;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.auth.resp.SuccessfulExecution;
import com.raytheon.uf.common.auth.resp.UserNotAuthenticated;
import com.raytheon.uf.common.auth.resp.UserNotAuthorized;
import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.serialization.ExceptionWrapper;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.serialization.comm.RemoteServiceRequest;
import com.raytheon.uf.common.serialization.comm.RequestWrapper;
import com.raytheon.uf.common.serialization.comm.ServiceException;
import com.raytheon.uf.common.serialization.comm.response.ServerErrorResponse;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.core.exception.VizCommunicationException;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;

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

/**
 * The thrift client. used to send requests to the RemoteRequestServer. Make
 * sure request type has registered a handler to handle the request on the
 * server.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 03, 2009           mschenke  Initial creation
 * Jul 24, 2012           njensen   Enhanced logging
 * Nov 15, 2012  1322     djohnson  Publicize ability to specify specific
 *                                  httpAddress.
 * Jan 24, 2013  1526     njensen   Switch from using postBinary() to
 *                                  postDynamicSerialize()
 * Jan 27, 2016  5170     tjensen   Added logging of stats to sendRequest
 * Oct 19, 2017  6316     njensen   Get uniqueId from RequestWrapper
 * May 09, 2019  7766     kbisanz   Log long request messages instead of
 *                                  printing to STDOUT
 * Feb 16, 2021  8337     mchan     Added performance log to capture the request
 *                                  and how long it took to complete
 * Mar 25, 2021  8396     randerso  Temporarily remove logging of processing
 *                                  host until DR #8399 is worked.
 *
 * </pre>
 *
 * @author mschenke
 */

public class ThriftClient {

    private abstract static class ThriftServiceHandler
            implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            Class<?> clazz = null;
            for (Class<?> interfaze : proxy.getClass().getInterfaces()) {
                if (interfaze.getAnnotation(WebService.class) != null) {
                    clazz = interfaze;
                }
            }

            RemoteServiceRequest request = new RemoteServiceRequest();
            request.setInterfaceToUse(clazz.getCanonicalName());
            request.setMethod(method.getName());
            request.setArguments(args);
            try {
                return handleRequest(request);
            } catch (VizException e) {
                throw new ServiceException(e);
            }
        }

        protected abstract Object handleRequest(RemoteServiceRequest request)
                throws VizException;
    }

    private static InvocationHandler httpHandler = new ThriftServiceHandler() {

        @Override
        protected Object handleRequest(RemoteServiceRequest request)
                throws VizException {
            return ThriftClient.sendRequest(request);
        }
    };

    private static InvocationHandler localizationHandler = new ThriftServiceHandler() {

        @Override
        protected Object handleRequest(RemoteServiceRequest request)
                throws VizException {
            return ThriftClient.sendLocalizationRequest(request);
        }
    };

    private static final long BAD_LOG_TIME = Integer
            .getInteger("thriftclient.log.bad.ms", 5000);

    private static INotAuthHandler defaultHandler = UserController
            .getNotAuthHandler();

    private static final Logger logger = LoggerFactory
            .getLogger("CaveRequestLogger");

    /**
     * Construct a thrift web service object that sends method calls to the http
     * server to be executed. EVERY FUNCTION CALL MADE TO INTERFACE MAY THROW A
     * VizException, be safe and catch them!
     *
     * @param interfaze
     * @return Implementation of interface passed in
     */
    public static <T> T constructServiceObject(Class<T> interfaze)
            throws VizException {
        return constructServiceObject(interfaze, httpHandler);
    }

    /**
     * Construct a thrift web service object that sends method calls to the
     * localization server to be executed. EVERY FUNCTION CALL MADE TO INTERFACE
     * MAY THROW A VizException, be safe and catch them!
     *
     * @param interfaze
     * @return Implementation of interface passed in
     */
    public static <T> T constructLocalizationServiceObject(Class<T> interfaze)
            throws VizException {
        return constructServiceObject(interfaze, localizationHandler);
    }

    /**
     * Send a request to the localization server
     *
     * @param request
     * @return
     * @throws VizException
     */
    public static Object sendLocalizationRequest(IServerRequest request)
            throws VizException {
        if (request instanceof AbstractPrivilegedRequest) {
            return sendPrivilegedLocalizationRequest(
                    (AbstractPrivilegedRequest) request);
        }
        return sendRequest(request,
                LocalizationManager.getInstance().getLocalizationServer());
    }

    /**
     * Send a request to the http server
     *
     * @param request
     * @return
     * @throws VizException
     */
    public static Object sendRequest(IServerRequest request)
            throws VizException {
        if (request instanceof AbstractPrivilegedRequest) {
            return sendPrivilegedRequest((AbstractPrivilegedRequest) request);
        }
        return sendRequest(request, VizApp.getHttpServer());
    }

    /**
     * Send the privileged request to the http server using the controller's
     * handler
     *
     * @param request
     * @return
     * @throws VizException
     */
    public static Object sendPrivilegedRequest(
            AbstractPrivilegedRequest request) throws VizException {
        return sendPrivilegedRequest(request, defaultHandler);
    }

    /**
     * Send the privileged request to the localization server using the
     * controller's handler
     *
     * @param request
     * @return
     * @throws VizException
     */
    public static Object sendPrivilegedLocalizationRequest(
            AbstractPrivilegedRequest request) throws VizException {
        return sendPrivilegedLocalizationRequest(request, defaultHandler);
    }

    /**
     * Send the privileged request to the localization server using a custom
     * INotAuthHandler
     *
     * @param request
     * @param handler
     * @return
     * @throws VizException
     */
    public static Object sendPrivilegedLocalizationRequest(
            AbstractPrivilegedRequest request, INotAuthHandler handler)
            throws VizException {
        return sendPrivilegedRequest(request, handler,
                LocalizationManager.getInstance().getLocalizationServer());
    }

    /**
     * Send the privileged request to the http server using a custom
     * INotAuthHandler
     *
     * @param request
     * @param handler
     * @return
     * @throws VizException
     */
    public static Object sendPrivilegedRequest(
            AbstractPrivilegedRequest request, INotAuthHandler handler)
            throws VizException {
        return sendPrivilegedRequest(request, handler, VizApp.getHttpServer());
    }

    /**
     * Send a privileged request to the given server, wraps expected privileged
     * response types
     *
     * @param request
     * @param handler
     * @param server
     * @return
     * @throws VizException
     */
    private static Object sendPrivilegedRequest(
            AbstractPrivilegedRequest request, INotAuthHandler handler,
            String server) throws VizException {
        Object rval = ThriftClient.sendRequest(request, server);
        if (rval instanceof UserNotAuthorized) {
            rval = handler.notAuthorized((UserNotAuthorized) rval);
        } else if (rval instanceof UserNotAuthenticated) {
            rval = handler.notAuthenticated((UserNotAuthenticated) rval);
        } else if (rval instanceof SuccessfulExecution) {
            SuccessfulExecution response = (SuccessfulExecution) rval;
            UserController.updateUserData(response.getUpdatedData());
            rval = response.getResponse();
        }
        return rval;
    }

    public static Object sendRequest(IServerRequest request, String httpAddress)
            throws VizException {
        return sendRequest(request, httpAddress, "/thrift");
    }

    /**
     * Sends an IServerRequest to the server at the specified URI.
     *
     * @param request
     *            the request to send
     * @param httpAddress
     *            the http address
     * @param uri
     *            the URI at the address
     * @return the object the server returns
     * @throws VizException
     */
    private static Object sendRequest(IServerRequest request,
            String httpAddress, String uri) throws VizException {
        String url = httpAddress + uri;
        RequestWrapper wrapper = new RequestWrapper(request, VizApp.getWsId());
        String requestId = wrapper.getUniqueId();
        String requestStr = request.toString();
        logger.info("Sending request to URL {}: id[{}] {}", url, requestId,
                requestStr);

        Object response = null;
        try {
            long t0 = System.currentTimeMillis();
            response = HttpClient.getInstance().postDynamicSerialize(url,
                    wrapper, true);
            long durationInMillis = System.currentTimeMillis() - t0;

            /*
             * TODO: restore logging of "processed by host" when DR #8399 is
             * worked
             */
            // String processingServerHost = "unknown";
            // if (response instanceof ResponseWrapper) {
            // ResponseWrapper responseWrapper = ((ResponseWrapper) response);
            // processingServerHost = responseWrapper.getHost();
            // response = responseWrapper.getResponse();
            // }
            //
            // logger.info("Request processed by host {} took {}ms. id[{}]",
            // processingServerHost, durationInMillis, requestId);
            logger.info("Request took {}ms. id[{}]", durationInMillis,
                    requestId);

            if (durationInMillis >= BAD_LOG_TIME && logger.isDebugEnabled()) {
                StackTraceElement[] stackTrace = Thread.currentThread()
                        .getStackTrace();
                StringBuilder stackTraceBuilder = new StringBuilder(
                        "(NOT AN ERROR) ThriftClient Diagnostic Stack For Long Requests:\n");
                for (StackTraceElement traceElement : stackTrace) {
                    stackTraceBuilder.append("\tat " + traceElement + "\n");
                }

                logger.debug(stackTraceBuilder.toString());
            }

        } catch (IOException | CommunicationException e) {
            throw new VizCommunicationException(
                    "unable to post request to server", e);
        } catch (Exception e) {
            throw new VizException("unable to post request to server", e);
        }

        if (response instanceof ServerErrorResponse) {
            ServerErrorResponse resp = (ServerErrorResponse) response;
            Throwable serverException = ExceptionWrapper
                    .unwrapThrowable(resp.getException());
            throw new ServerRequestException(serverException.getMessage(),
                    serverException);
        }
        return response;
    }

    private static String getHost(String httpAddress) {
        String host;
        try {
            host = new URL(httpAddress).getHost();
        } catch (MalformedURLException e) {
            // if the given httpAddress is malformed then just return the given
            // httpAddress as the host
            host = httpAddress;
        }
        return host;
    }

    @SuppressWarnings("unchecked")
    private static <T> T constructServiceObject(Class<T> interfaze,
            InvocationHandler handler) throws VizException {
        try {
            Validate.notNull(interfaze, "Interface must not be null");
            Validate.isTrue(interfaze.isInterface(),
                    "Class must be an interface");
            Validate.notNull(interfaze.getAnnotation(WebService.class),
                    "Interface must have " + WebService.class + " annotation");

            return (T) Proxy.newProxyInstance(interfaze.getClassLoader(),
                    new Class[] { interfaze }, handler);
        } catch (Throwable t) {
            throw new VizException("Error constructing service object: "
                    + t.getLocalizedMessage(), t);
        }
    }
}