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
package com.raytheon.edex.services;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.msgs.AbstractUtilityCommand;
import com.raytheon.uf.common.localization.msgs.AbstractUtilityResponse;
import com.raytheon.uf.common.localization.msgs.ListContextCommand;
import com.raytheon.uf.common.localization.msgs.ListUtilityCommand;
import com.raytheon.uf.common.localization.msgs.ProtectedFileCommand;
import com.raytheon.uf.common.localization.msgs.ProtectedFileResponse;
import com.raytheon.uf.common.localization.msgs.UtilityRequestMessage;
import com.raytheon.uf.common.localization.msgs.UtilityResponseMessage;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;

/**
 * Utility (localization) service
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 23, 2007            chammack    Initial Creation.    
 * Jul 30, 2007            njensen     Added delete case
 * Aug 22, 2008 1422       chammack    Pulled out serialization
 * Nov 14, 2008            njensen     Camel Refactor
 * Jul 10, 2014 2914       garmendariz Remove EnvProperties
 * Jun 22, 2017 6339       njensen     UtilityManager.listFiles() now takes a fileExtension
 * Aug 04, 2017 6379       njensen     Removed protected-ness from responses
 * 
 * </pre>
 * 
 * @author chammack
 */

public class UtilitySrv implements IRequestHandler<UtilityRequestMessage> {

    private static String UTILITY_DIR = EDEXUtil.getEdexUtility();

    @Override
    public UtilityResponseMessage handleRequest(UtilityRequestMessage msg)
            throws Exception {
        // Service each command
        List<AbstractUtilityResponse> responses = new ArrayList<>();
        AbstractUtilityCommand[] cmds = msg.getCommands();
        for (AbstractUtilityCommand cmd : cmds) {
            LocalizationContext context = cmd.getContext();
            if (cmd instanceof ListUtilityCommand) {
                ListUtilityCommand castCmd = ((ListUtilityCommand) cmd);
                responses.add(UtilityManager.listFiles(
                        castCmd.getLocalizedSite(), UTILITY_DIR, context,
                        castCmd.getSubDirectory(), castCmd.getFileExtension(),
                        castCmd.isRecursive(), castCmd.isFilesOnly()));
            } else if (cmd instanceof ProtectedFileCommand) {
                /*
                 * TODO: Remove support for ProtectedFileCommand. It's only here
                 * for backwards compatibility. At present it will always return
                 * null, implying the file is not protected.
                 */
                ProtectedFileCommand castCmd = (ProtectedFileCommand) cmd;
                ProtectedFileResponse response = new ProtectedFileResponse();
                response.setPathName(castCmd.getSubPath());
                response.setContext(castCmd.getContext());
                responses.add(response);
            } else if (cmd instanceof ListContextCommand) {
                ListContextCommand castCmd = (ListContextCommand) cmd;
                responses.add(UtilityManager.listContexts(UTILITY_DIR,
                        castCmd.getRequestLevel()));
            } else {
                throw new EdexException("Unsupported message type: "
                        + cmd.getClass().getName());
            }
        }

        AbstractUtilityResponse[] respArray = responses
                .toArray(new AbstractUtilityResponse[responses.size()]);

        UtilityResponseMessage response = new UtilityResponseMessage(respArray);

        return response;
    }

}
