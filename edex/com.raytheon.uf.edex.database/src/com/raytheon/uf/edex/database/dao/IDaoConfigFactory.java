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
package com.raytheon.uf.edex.database.dao;

/**
 * Interface defining a factory for creating Data Access Object configuration
 * objects.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 21, 2021 7849       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IDaoConfigFactory {

    /**
     * Gets a DaoConfig object for the specified database
     *
     * @param dbName
     *            The database name
     * @return A DaoConfig instance for the specified database
     */
    DaoConfig forDatabase(String dbName);

    /**
     * Gets a DaoConfig object for the specified database. If admin will login
     * as a super user, otherwise will use a normal user login.
     *
     * @param dbName
     *            The database name
     * @param admin
     *            Whether to login as a super user or not
     * @return A DaoConfig instance for the specified database
     */
    DaoConfig forDatabase(String dbName, boolean admin);

    /**
     * Gets a DaoConfig object for the specified class using the default session
     * factory and default transaction manager.
     *
     * @param clazz
     *            The class for which to create the DaoConfig object
     * @return A DaoConfig instance using the specified class, default session
     *         factory and default transaction manager.
     */
    DaoConfig forClass(Class<?> clazz);

    /**
     * Gets a DaoConfig object for the specified class using the default session
     * factory and default transaction manager. If admin, will login as a super
     * user, otherwise a normal user login.
     *
     * @param clazz
     *            The class for which to create the DaoConfig object
     * @param admin
     *            Whether to login as a super user or not
     * @return A DaoConfig instance using the specified class
     */
    DaoConfig forClass(Class<?> clazz, boolean admin);

    /**
     * Gets a DaoConfig object for the specified class and database
     *
     * @param dbName
     *            The database name
     * @param clazz
     *            The class object
     * @return A DaoConfig instance with the specified database name and class
     *         name
     */
    DaoConfig forClass(String dbName, Class<?> clazz);

    /**
     * Gets a DaoConfig object for the specified class and database
     *
     * @param dbName
     *            The database name
     * @param clazz
     *            The class object
     * @param admin
     *            Whether to login as a super user or not
     * @return A DaoConfig instance with the specified database name and class
     *         name
     */
    DaoConfig forClass(String dbName, Class<?> clazz, boolean admin);
}
