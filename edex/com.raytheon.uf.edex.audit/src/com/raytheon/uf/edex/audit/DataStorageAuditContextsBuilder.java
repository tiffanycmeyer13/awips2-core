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
package com.raytheon.uf.edex.audit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.GenericBeansException;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.raytheon.uf.common.datastorage.audit.DataStorageAuditUtils;
import com.raytheon.uf.edex.core.IMessageProducer;
import com.raytheon.uf.edex.esb.camel.context.ContextManager;

/**
 * Class that dynamically initializes clustered contexts for auditor
 * routes.
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 03, 2023 9019       mapeters    Initial creation
 * Feb 10, 2023 9019       smoorthy    Migrate to separate plugin
 */

public class DataStorageAuditContextsBuilder
        implements ApplicationContextAware, BeanFactoryPostProcessor {

    private final IMessageProducer messageProducer;

    private ApplicationContext applicationContext;

    public DataStorageAuditContextsBuilder(IMessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (int i = 1; i <= DataStorageAuditUtils.NUM_QUEUES; ++i) {
            DataStorageAuditer auditor = new DataStorageAuditer(messageProducer,
                    i);
            SpringCamelContext camelContext = new SpringCamelContext(
                    applicationContext);
            String camelContextId = "clusteredDataStorageAuditContext" + i;
            camelContext.setName(camelContextId);
            DataStorageAuditRouteBuilder routeBuilder = new DataStorageAuditRouteBuilder(
                    auditor);
            try {
                camelContext.addRoutes(routeBuilder);
            } catch (Exception e) {
                throw new GenericBeansException(
                        "Error configuring data storage audit routes", e);
            }
            beanFactory.initializeBean(camelContext, camelContextId);
            beanFactory.registerSingleton(camelContextId, camelContext);
            ContextManager.getInstance().registerClusteredContext(camelContext);
            ContextManager.getInstance()
                    .registerContextStateProcessor(camelContext, auditor);
        }
    }

    private static class DataStorageAuditRouteBuilder extends RouteBuilder {

        private final DataStorageAuditer auditor;

        public DataStorageAuditRouteBuilder(DataStorageAuditer auditor) {
            this.auditor = auditor;
        }

        @Override
        public void configure() throws Exception {
            int id = auditor.getId();
            String auditorBeanId = "dataStorageAuditorImpl" + id;
            bindToRegistry(auditorBeanId, auditor);

            String auditEventQueueUri = DataStorageAuditUtils.QUEUE_JMS_PREFIX
                    + DataStorageAuditUtils.QUEUE_ROOT_NAME + id
                    + "?threadName=DataStorageAudit" + id;
            from(auditEventQueueUri)
                    .bean("serializationUtil", "transformFromThrift")
                    .bean(auditorBeanId, "processEvent");

            String auditerCron = System
                    .getProperty("data.storage.auditer.cleanup.cron");
            String auditCleanupQuartzUri = "quartz://DataStorageAuditCleanup"
                    + id + "/?cron=" + auditerCron;
            from(auditCleanupQuartzUri).bean(auditorBeanId, "cleanup");
        }
    }
}
