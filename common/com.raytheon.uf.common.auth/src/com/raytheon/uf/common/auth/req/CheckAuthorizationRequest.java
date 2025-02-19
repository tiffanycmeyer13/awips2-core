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
package com.raytheon.uf.common.auth.req;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * Request to determine if a user has a permission.
 *
 * Can be used in client code to determine if a user has a permission.
 * Recommended for allowing access to certain GUI elements etc. Server side
 * actions should have their own permission checks.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------
 * Feb 24, 2015  4300     randerso  Initial creation
 * Apr 18, 2017  6217     randerso  Updated for new roles/permissions
 *
 * </pre>
 *
 * @author randerso
 */

@DynamicSerialize
public class CheckAuthorizationRequest implements IServerRequest {

    @DynamicSerializeElement
    private String permission;

    /**
     * Default constructor for serialization only
     */
    public CheckAuthorizationRequest() {

    }

    /**
     * Constructor
     *
     * @param permission
     */
    public CheckAuthorizationRequest(String permission) {
        this.permission = permission;
    }

    /**
     * @return the permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @param permission
     *            the permission to set
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

}
