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

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate5.HibernateTransactionManager;

import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Configuration settings for a data access object.<br>
 * This object contains the required information to correctly instantiate a
 * valid data access object.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 11, 0007  600      bphillip  Initial Check in
 * Oct 10, 2012  1261     djohnson  Add ability for test overriding of bean
 *                                  lookups.
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * Jun 20, 2016  5679     rjpeter   Add admin database account.
 * Feb 26, 2019  6140     tgurney   Hibernate 5 upgrade
 * Apr 21, 2021  7849     mapeters  Remove/deprecate factory methods
 *
 * </pre>
 *
 * @author bphillip
 */
public abstract class DaoConfig {

    /** The default database name */
    public static final String DEFAULT_DB_NAME = "metadata";

    /** The admin prefix */
    private static final String ADMIN = "admin_";

    /** The session factory suffix */
    private static final String SESSION_FACTORY = "SessionFactory";

    /** The transaction manager suffix */
    private static final String TX_MANAGER = "TxManager";

    private static final SpringBeanLocator DEFAULT_LOCATOR = new SpringBeanLocator() {
        @Override
        public <T> T lookupBean(Class<T> resultClass, String beanName) {
            return resultClass.cast(EDEXUtil.getESBComponent(beanName));
        }
    };

    /**
     * The default data access object configuration. This configuration
     * specifies the metadata database
     */
    public static final DaoConfig DEFAULT = DaoConfig
            .forDatabase(DEFAULT_DB_NAME);

    /**
     * Retrieve the transaction manager.
     *
     * @return the transaction manager
     */
    public abstract HibernateTransactionManager getTxManager();

    /**
     * Retrieve the session factory.
     *
     * @return the session factory
     */
    public abstract SessionFactory getSessionFactory();

    /**
     * Retrieve the class type this DAO manages.
     *
     * @return the class type
     */
    public abstract Class<?> getDaoClass();

    /**
     * Gets a DaoConfig object for the specified class using the default session
     * factory and default transaction manager.
     *
     * @param className
     *            The class for which to create the DaoConfig object
     * @return A DaoConfig instance using the specified class, default session
     *         factory and default transaction manager.
     * @deprecated Use corresponding method in a Spring-injected
     *             {@link IDaoConfigFactory} instance, which will use the
     *             correct Spring application context to lookup beans
     */
    @Deprecated
    public static DaoConfig forClass(Class<?> className) {
        return forClass(DEFAULT_DB_NAME, className);
    }

    /**
     * Gets a DaoConfig object for the specified class and database
     *
     * @param dbName
     *            The database name
     * @param className
     *            The class object
     * @return A DaoConfig instance with the specified database name and class
     *         name
     * @deprecated Use corresponding method in a Spring-injected
     *             {@link IDaoConfigFactory} instance, which will use the
     *             correct Spring application context to lookup beans
     */
    @Deprecated
    public static DaoConfig forClass(String dbName, Class<?> className) {
        return new SpringLookupDaoConfig(dbName, className, false,
                DEFAULT_LOCATOR);
    }

    /**
     * Gets a DaoConfig object for the specified database
     *
     * @param dbName
     *            The database name
     * @return A DaoConfig instance for the specified database
     * @deprecated Use corresponding method in a Spring-injected
     *             {@link IDaoConfigFactory} instance, which will use the
     *             correct Spring application context to lookup beans
     */
    @Deprecated
    public static DaoConfig forDatabase(String dbName) {
        return forClass(dbName, null);
    }

    protected static class SpringLookupDaoConfig extends DaoConfig {

        /**
         * The class for which the desired data access object is to be used for
         */
        private final Class<?> daoClass;

        /** The name of the Hibernate session factory to use */
        private final String sessionFactoryName;

        /** The name of the Hibernate transaction manager to use */
        private final String txManagerName;

        private final SpringBeanLocator locator;

        /**
         * Constructs a DaoConfig object for the specified database using the
         * specified class name. The appropriate session factory and transaction
         * manager will be determined from the database name and accessed via
         * the Spring bean locator. If admin, the database login will be as a
         * super user, otherwise a normal user login will be used.
         *
         * @param dbName
         *            The database name
         * @param daoClass
         *            The class object
         * @param admin
         *            Whether to login as a super user or not
         * @param locator
         *            The Spring bean locator
         */
        protected SpringLookupDaoConfig(String dbName, Class<?> daoClass,
                boolean admin, SpringBeanLocator locator) {
            this.daoClass = daoClass;
            String prefix = "";
            if (admin) {
                prefix = ADMIN;
            }
            this.sessionFactoryName = prefix + dbName + SESSION_FACTORY;
            this.txManagerName = prefix + dbName + TX_MANAGER;
            this.locator = locator;
        }

        @Override
        public HibernateTransactionManager getTxManager() {
            return locator.lookupBean(HibernateTransactionManager.class,
                    txManagerName);
        }

        @Override
        public SessionFactory getSessionFactory() {
            return locator.lookupBean(SessionFactory.class, sessionFactoryName);
        }

        @Override
        public Class<?> getDaoClass() {
            return daoClass;
        }
    }
}
