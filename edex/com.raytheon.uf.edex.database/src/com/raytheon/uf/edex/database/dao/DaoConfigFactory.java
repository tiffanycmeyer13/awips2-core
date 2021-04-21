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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.dao.DaoConfig.SpringLookupDaoConfig;

/**
 * Factory for creating Data Access Object configuration objects.
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
public class DaoConfigFactory
        implements IDaoConfigFactory, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private SpringBeanLocator springBeanLocator = new SpringBeanLocator() {
        @Override
        public <T> T lookupBean(Class<T> resultClass, String beanName) {
            return resultClass.cast(
                    EDEXUtil.getESBComponent(applicationContext, beanName));
        }
    };

    /**
     * DO NOT CALL. Only for instantiation via a spring bean, which will
     * correctly set the application context via the
     * {@link ApplicationContextAware} interface.
     */
    public DaoConfigFactory() {
    }

    /**
     * Create a factory of DAO configs for the given Spring application context.
     *
     * @param applicationContext
     *            the Spring application context
     */
    public DaoConfigFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public DaoConfig forDatabase(String dbName) {
        return forDatabase(dbName, false);
    }

    @Override
    public DaoConfig forDatabase(String dbName, boolean admin) {
        return forClass(dbName, null, admin);
    }

    @Override
    public DaoConfig forClass(Class<?> clazz) {
        return forClass(clazz, false);
    }

    @Override
    public DaoConfig forClass(Class<?> clazz, boolean admin) {
        return forClass(DaoConfig.DEFAULT_DB_NAME, clazz, admin);
    }

    @Override
    public DaoConfig forClass(String dbName, Class<?> clazz) {
        return forClass(dbName, clazz, false);
    }

    @Override
    public DaoConfig forClass(String dbName, Class<?> clazz, boolean admin) {
        return new SpringLookupDaoConfig(dbName, clazz, admin,
                springBeanLocator);
    }
}
