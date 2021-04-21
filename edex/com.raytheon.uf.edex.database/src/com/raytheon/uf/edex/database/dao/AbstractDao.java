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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The base implementation of all Data Access Objects. Provides functionality
 * for executing arbitrary actions within transaction contexts.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 14, 2021  7849     mapeters  Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractDao {

    private static final Propagation[] PROP_VALUES = Propagation.values();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final SessionFactory sessionFactory;

    private final HibernateTransactionManager txManager;

    protected Class<?> daoClass;

    private final TransactionDefinition defaultTransactionDef;

    private final Map<TransactionDefinition, TransactionTemplate> txTemplates = new HashMap<>();

    public AbstractDao(DaoConfig config,
            TransactionDefinition defaultTransactionDef) {
        this.defaultTransactionDef = defaultTransactionDef;
        txManager = config.getTxManager();
        sessionFactory = config.getSessionFactory();
        daoClass = config.getDaoClass();
    }

    /**
     * Execute runnable within provided transaction context.
     *
     * @param transactionDef
     *            the transaction context to use
     * @param runnable
     *            the runnable to execute
     */
    protected void runInTransaction(TransactionDefinition transactionDef,
            Runnable runnable) {
        TransactionTemplate txTemplate = getTransactionTemplate(transactionDef);
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                runnable.run();
            }
        });
    }

    /**
     * Execute runnable within default transaction context.
     *
     * @param runnable
     *            the runnable to execute
     */
    protected void runInTransaction(Runnable runnable) {
        runInTransaction(defaultTransactionDef, runnable);
    }

    /**
     * Execute supplier within provided transaction context and return result.
     *
     * @param transactionDef
     *            the transaction context to use
     * @param supplier
     *            the supplier to execute
     */
    protected <T> T supplyInTransaction(TransactionDefinition transactionDef,
            Supplier<T> supplier) {
        TransactionTemplate txTemplate = getTransactionTemplate(transactionDef);
        return txTemplate.execute(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus status) {
                return supplier.get();
            }
        });
    }

    /**
     * Execute supplier within default transaction context and return result.
     *
     * @param supplier
     *            the supplier to execute
     */
    protected <T> T supplyInTransaction(Supplier<T> supplier) {
        return supplyInTransaction(defaultTransactionDef, supplier);
    }

    /**
     * Get a transaction definition with the given propagation behavior. The
     * rest of the definition will match this DAO's default transaction
     * definition.
     *
     * @param propagation
     *            the propagation behavior
     * @return the transaction definition
     */
    protected TransactionDefinition getTransactionDef(Propagation propagation) {
        return getTransactionDef(propagation,
                defaultTransactionDef.isReadOnly());
    }

    /**
     * Get a transaction definition with the given read-only status. The rest of
     * the definition will match this DAO's default transaction definition.
     *
     * @param readOnly
     *            the read-only status
     * @return the transaction definition
     */
    protected TransactionDefinition getTransactionDef(boolean readOnly) {
        return getTransactionDef(
                convertPropagationToEnum(
                        defaultTransactionDef.getPropagationBehavior()),
                readOnly);
    }

    /**
     * Get a transaction definition with the given propagation behavior and
     * read-only status. The rest of the definition will match this DAO's
     * default transaction definition.
     *
     * @param propagation
     *            the propagation behavior
     * @param readOnly
     *            the read-only status
     * @return the transaction definition
     */
    protected TransactionDefinition getTransactionDef(Propagation propagation,
            boolean readOnly) {
        DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition(
                defaultTransactionDef);
        transactionDef.setPropagationBehavior(propagation.value());
        transactionDef.setReadOnly(readOnly);
        return transactionDef;
    }

    /**
     * Get the transaction template to use for the given transaction definition.
     *
     * @param transactionDef
     *            the transaction definition
     * @return the transaction template
     */
    protected TransactionTemplate getTransactionTemplate(
            TransactionDefinition transactionDef) {
        // Convert to consistent implementation for consistent hashCode/equals
        transactionDef = new DefaultTransactionDefinition(transactionDef);
        TransactionTemplate txTemplate;
        synchronized (txTemplates) {
            txTemplate = txTemplates.get(transactionDef);
            if (txTemplate == null) {
                txTemplate = new TransactionTemplate(txManager, transactionDef);
                txTemplates.put(transactionDef, txTemplate);
            }
        }
        return txTemplate;
    }

    /**
     * @return the hibernate session factory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * @return the current hibernate session
     */
    public Session getCurrentSession() {
        return getSessionFactory().getCurrentSession();
    }

    /**
     * Gets the object class associated with this dao
     *
     * @return the object class associated with this dao
     */
    public Class<?> getDaoClass() {
        return daoClass;
    }

    /**
     * Sets the object class associated with this dao
     *
     * @param daoClass
     *            The object class to assign to this dao
     */
    public void setDaoClass(Class<?> daoClass) {
        this.daoClass = daoClass;
    }

    /**
     * Convert propagation behavior int value to enum value
     *
     * @param propagationBehavior
     *            the propagation int value to convert (a constant from
     *            {@link TransactionDefinition})
     * @return the corresponding {@link Propagation} enum value
     */
    private static Propagation convertPropagationToEnum(
            int propagationBehavior) {
        for (Propagation prop : PROP_VALUES) {
            if (prop.value() == propagationBehavior) {
                return prop;
            }
        }

        throw new IllegalArgumentException(
                "Invalid propagation behavior int: " + propagationBehavior);
    }
}
