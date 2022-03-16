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
package com.raytheon.uf.edex.requestsrv;

import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.auth.user.User;
import com.raytheon.uf.common.message.WsId;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.serialization.comm.RequestWrapper;
import com.raytheon.uf.common.serialization.comm.ResponseWrapper;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.SystemUtil;
import com.raytheon.uf.edex.auth.AuthManagerFactory;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.auth.resp.ResponseFactory;
import com.raytheon.uf.edex.requestsrv.logging.RequestLogger;

/**
 * Class that handles the execution of {@link IServerRequest}s. Contains the
 * actual logic to lookup and execute the {@link IRequestHandler} registered for
 * the request passed in.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#   Engineer  Description
 * ------------- --------- --------- -------------------------------------------
 * Aug 21, 2014  3541      mschenke  Initial creation
 * Feb 27, 2015  4196      njensen   Null authentication data on responses for
 *                                   backwards compatibility
 * Dec 02, 2015  4834      njensen   Stop triple-wrapping AuthExceptions
 * May 17, 2017  6217      randerso  Add support for new roles and permissions
 *                                   framework
 * Jul 18, 2017  6217      randerso  Removed support for old roles and
 *                                   permissions framework
 * Mar 09, 2020  dcs21885  brapp     Added request detail logging
 * Feb 16, 2021  8337      mchan     Wrap response in ResponseWrapper if the
 *                                   request is a RequestWrapper
 * Mar 25, 2021  8396      randerso  Temporarily remove use of ResponseWrapper
 *                                   until DR #8399 is worked.
 * Nov 18, 2021  8399      randerso  Restore use of ResponseWrapper
 *
 * </pre>
 *
 * @author mschenke
 */

public class RequestServiceExecutor {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(RequestServiceExecutor.class);

    private static final RequestServiceExecutor instance = new RequestServiceExecutor();

    public static RequestServiceExecutor getInstance() {
        return instance;
    }

    private final HandlerRegistry registry;

    private final RequestLogger reqLogger;

    public RequestServiceExecutor() {
        this(HandlerRegistry.getInstance(), RequestLogger.getInstance());
    }

    public RequestServiceExecutor(HandlerRegistry registry,
            RequestLogger reqLogger) {
        this.registry = registry;
        this.reqLogger = reqLogger;
    }

    /**
     * Executes the request passed in, delegates conversion to/from
     * {@link IServerRequest} to the {@link HandlerRegistry} set in the
     * constructor
     *
     * @param request
     * @return The result of the service execution
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object execute(IServerRequest request) throws Exception {
        boolean isRequestWrapped = false;
        boolean subjectSet = false;
        String wsidPString = null;

        try {
            if (request instanceof RequestWrapper) {
                // Check for wrapped request and get actual request to execute
                isRequestWrapped = true;
                RequestWrapper wrapper = (RequestWrapper) request;
                WsId wsid = wrapper.getWsId();
                wsidPString = wsid.toPrettyString();

                request = wrapper.getRequest();

                IUser user = new User(wsid.getUserName());

                AuthManagerFactory.getInstance().getPermissionsManager()
                        .setThreadSubject(user);
                subjectSet = true;
            }

            String id = request.getClass().getCanonicalName();
            IRequestHandler handler = registry.getRequestHandler(id);

            if (request instanceof AbstractPrivilegedRequest) {
                // Not the default role, attempt to cast handler and request
                try {
                    AbstractPrivilegedRequest privReq = (AbstractPrivilegedRequest) request;
                    AbstractPrivilegedRequestHandler privHandler = (AbstractPrivilegedRequestHandler) handler;

                    /*
                     * check handler that user is allowed to execute this
                     * request (authorization)
                     */
                    AuthorizationResponse authResp = privHandler
                            .authorized(privReq);
                    if (authResp != null && !authResp.isAuthorized()
                            && authResp.getResponseMessage() != null) {
                        Object response = ResponseFactory
                                .constructNotAuthorized(privReq,
                                        authResp.getResponseMessage());

                        /* if request was wrapped, wrap response */
                        return isRequestWrapped
                                ? new ResponseWrapper(response,
                                        SystemUtil.getHostName())
                                : response;
                    }

                    /*
                     * they've passed authorization, let the handler execute the
                     * request
                     */
                    /*
                     * TODO someday pass in updated IAuthenticationData if we
                     * have an actual implementation that uses it for security
                     */
                    Object response = ResponseFactory
                            .constructSuccessfulExecution(
                                    privHandler.handleRequest(privReq), null);

                    /* if request was wrapped, wrap response */
                    return isRequestWrapped
                            ? new ResponseWrapper(response,
                                    SystemUtil.getHostName())
                            : response;
                } catch (ClassCastException e) {
                    throw new AuthException(
                            "Roles can only be defined for requests/handlers of AbstractPrivilegedRequest/Handler, request was "
                                    + request.getClass().getName(),
                            e);

                } catch (Throwable t) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error occurred while performing privileged request "
                                    + request,
                            t);
                    throw t;
                }
            }

            reqLogger.logRequest(wsidPString, request);

            Object response = handler.handleRequest(request);

            /* if request was wrapped, wrap response */
            return isRequestWrapped
                    ? new ResponseWrapper(response, SystemUtil.getHostName())
                    : response;
        } finally {
            if (subjectSet) {
                AuthManagerFactory.getInstance().getPermissionsManager()
                        .removeThreadSubject();
            }
        }
    }

}