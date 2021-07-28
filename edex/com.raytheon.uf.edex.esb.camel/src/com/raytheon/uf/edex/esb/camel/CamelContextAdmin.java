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
package com.raytheon.uf.edex.esb.camel;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.raytheon.uf.edex.core.IContextAdmin;

/**
 * TODO Add Description
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------
 * Dec 07, 2010           njensen   Initial creation
 * Apr 21, 2021  7849     mapeters  Inject Spring context
 *
 * </pre>
 *
 * @author njensen
 */
public class CamelContextAdmin
        implements IContextAdmin, ApplicationContextAware {

    private ApplicationContext springContext;

    private List<CamelContext> camelContexts;

    private synchronized List<CamelContext> getCamelContexts() {
        if (camelContexts == null) {
            camelContexts = new ArrayList<>();
            String[] contextNames = springContext
                    .getBeanNamesForType(CamelContext.class);
            for (String name : contextNames) {
                CamelContext cc = (CamelContext) springContext.getBean(name);
                camelContexts.add(cc);
            }
        }
        return camelContexts;
    }

    @Override
    public List<String> getAllContexts() {
        List<String> result = new ArrayList<>();
        List<CamelContext> ccs = getCamelContexts();
        for (CamelContext cc : ccs) {
            result.add(cc.getName());
        }
        return result;
    }

    @Override
    public List<String> getActiveContexts() {
        List<String> result = new ArrayList<>();
        for (CamelContext cc : getCamelContexts()) {
            if (cc.getStatus().isStarted()) {
                result.add(cc.getName());
            }
        }
        return result;
    }

    @Override
    public List<String> getInactiveContexts() {
        List<String> result = new ArrayList<>();
        for (CamelContext cc : getCamelContexts()) {
            if (cc.getStatus().isStopped()) {
                result.add(cc.getName());
            }
        }
        return result;
    }

    @Override
    public void startContext(String name) throws Exception {
        CamelContext cc = (CamelContext) springContext.getBean(name);
        cc.start();
    }

    @Override
    public void stopContext(String name) throws Exception {
        CamelContext cc = (CamelContext) springContext.getBean(name);
        cc.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        springContext = applicationContext;
    }
}
