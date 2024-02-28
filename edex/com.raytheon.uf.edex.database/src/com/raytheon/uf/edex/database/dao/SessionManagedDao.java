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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.edex.database.DataAccessLayerException;

/**
 * A CoreDao mimic that is session managed.
 *
 * In theory, this DAO should never open its own transaction or commit/rollback
 * transactions. Any number of DAOs should be utilizable in the same transaction
 * from a service that demarcates the transaction boundaries. This is supported
 * by the constructor indicating to use a MANDATORY propagation behavior by
 * default.
 *
 * In reality, various methods override the default propagation behavior and can
 * open/commit/rollback their own transactions.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 07, 2013  1543     djohnson  Initial creation
 * Mar 18, 2013  1802     bphillip  Added additional database functions.
 *                                  Enforcing mandatory transaction propogation
 * Mar 27, 2013  1802     bphillip  Changed transaction propagation of query
 *                                  methods
 * Apr 09, 2013  1802     bphillip  Modified how arguments are passed in to
 *                                  query methods
 * May 01, 2013  1967     njensen   Fixed autoboxing for Eclipse 3.8
 * Jun 24, 2013  2106     djohnson  Use IDENTIFIER generic for method signature.
 * Oct 08, 2013  1682     bphillip  Added the createCriteria method
 * Dec 09, 2013  2613     bphillip  Added flushAndClearSession method
 * Jan 17, 2014  2459     mpduff    Added null check to prevent NPE.
 * Feb 13, 2014  2769     bphillip  Added read-only flag to query methods and
 *                                  loadById method
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * Sep 01, 2016  5846     rjpeter   Support IN style hql queries
 * Sep 09, 2019  6140     randerso  Fix queries with maxResults.
 * Apr 14, 2021  7849     mapeters  Refactor to use TransactionTemplate.execute
 *                                  instead of Transactional annotations (for
 *                                  easier creation of admin/non-admin versions)
 *
 * </pre>
 *
 * @author djohnson
 */
@Repository
public abstract class SessionManagedDao<IDENTIFIER extends Serializable, ENTITY extends IPersistableDataObject<IDENTIFIER>>
        extends AbstractDao implements ISessionManagedDao<IDENTIFIER, ENTITY> {

    /** The region in the cache which stores the queries */
    private static final String QUERY_CACHE_REGION = "Queries";

    protected final TransactionDefinition requiredTransactionDef = getTransactionDef(
            Propagation.REQUIRED);

    protected final TransactionDefinition requiredReadOnlyTransactionDef = getTransactionDef(
            Propagation.REQUIRED, true);

    public SessionManagedDao(IDaoConfigFactory daoConfigFactory,
            boolean admin) {
        super(daoConfigFactory.forDatabase(DaoConfig.DEFAULT_DB_NAME, admin),
                new DefaultTransactionDefinition(
                        TransactionDefinition.PROPAGATION_MANDATORY));
        // Set DAO class to entity class instead of using value from DaoConfig
        setDaoClass(getEntityClass());
    }

    public SessionManagedDao(IDaoConfigFactory daoConfigFactory) {
        this(daoConfigFactory, false);
    }

    @Override
    public void create(final ENTITY obj) {
        runInTransaction(() -> {
            getCurrentSession().save(obj);
        });

    }

    @Override
    public void update(final ENTITY obj) {
        runInTransaction(() -> {
            getCurrentSession().update(obj);
        });

    }

    @Override
    public void createOrUpdate(final ENTITY obj) {
        runInTransaction(() -> {
            getCurrentSession().saveOrUpdate(obj);
        });
    }

    @Override
    public void persistAll(final Collection<ENTITY> objs) {
        runInTransaction(() -> {
            Session session = getCurrentSession();
            for (ENTITY obj : objs) {
                session.saveOrUpdate(obj);
            }
        });
    }

    @Override
    public void delete(final ENTITY obj) {
        runInTransaction(() -> {
            if (obj != null) {
                Object toDelete = getCurrentSession().merge(obj);
                getCurrentSession().delete(toDelete);
            }
        });
    }

    @Override
    public void deleteAll(final Collection<ENTITY> objs) {
        runInTransaction(() -> {
            for (ENTITY obj : objs) {
                delete(obj);
            }
        });
    }

    @Override
    public ENTITY getById(IDENTIFIER id) {
        return supplyInTransaction(requiredReadOnlyTransactionDef, () -> {
            Class<ENTITY> entityClass = getEntityClass();
            return entityClass.cast(getCurrentSession().get(entityClass, id));
        });
    }

    @Override
    public ENTITY loadById(IDENTIFIER id) {
        return supplyInTransaction(requiredReadOnlyTransactionDef, () -> {
            final Class<ENTITY> entityClass = getEntityClass();
            return entityClass.cast(getCurrentSession().load(entityClass, id));
        });
    }

    @Override
    public List<ENTITY> getAll() {
        return query("from " + getEntityClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public List<ENTITY> loadAll() {
        return supplyInTransaction(requiredReadOnlyTransactionDef, () -> {
            return getCurrentSession()
                    .createQuery("FROM " + getEntityClass().getName()).list();
        });

    }

    public ENTITY uniqueResult(String queryString) {
        return uniqueResult(queryString, new Object[0]);
    }

    /**
     * Internal convenience method for returning a single result.
     *
     * @param queryString
     * @param params
     * @return
     */
    protected ENTITY uniqueResult(String queryString, Object... params) {
        final List<ENTITY> results = executeHQLQuery(queryString, params);
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            logger.warn("More than one result returned for query ["
                    + queryString + "], only returning the first!");
        }
        return results.get(0);
    }

    public List<ENTITY> query(String queryString) {
        return executeHQLQuery(queryString);
    }

    /**
     * Internal convenience method for querying.
     *
     * @param queryString
     * @param params
     * @return
     */
    public List<ENTITY> query(String queryString, Object... params) {
        return executeHQLQuery(queryString, 0, params);
    }

    public List<ENTITY> query(String queryString, Integer maxResults,
            Object... params) {
        return executeHQLQuery(queryString, maxResults, params);
    }

    /**
     * Executes an HQL query in a new Hibernate session
     *
     * @param <T>
     *            An object type to query for
     * @param queryString
     *            The query to execute
     * @return The results of the HQL query
     * @throws DataAccessLayerException
     *             If errors are encountered during the HQL query
     */
    public <T extends Object> List<T> executeHQLQuery(String queryString) {
        return executeHQLQuery(queryString, 0);
    }

    /**
     * Executes an HQL query with a map of name value pairs with which to
     * substitute into the query. This method is a convenience method for
     * executing prepared statements
     *
     * @param <T>
     *            The return object type
     * @param queryString
     *            The prepared HQL query to execute. This query contains values
     *            that will be substituted according to the names and values
     *            found in the params map
     * @param params
     *            The named parameters to substitute into the HQL query
     * @return List containing the results of the query
     * @throws DataAccessLayerException
     *             If Hibernate errors occur during the execution of the query
     */
    public <T extends Object> List<T> executeHQLQuery(String queryString,
            Object... params) {
        return executeHQLQuery(queryString, 0, params);
    }

    /**
     * Executes a named parameter query with the option to limit the maximum
     * number of results. The params argument contains the parameter names in
     * alternating fashion. The parameter name comes first followed by the
     * parameter value.
     * <p>
     * For example, to execute this query 'SELECT obj.field FROM object obj
     * WHERE obj.id=:id' you would call: <br>
     * executeHQLQuery("SELECT obj.field FROM object obj WHERE obj.id=:id", 0,
     * ":id",idValue)
     *
     * @param <T>
     *            An object type to query for
     * @param queryString
     *            The query string, possibly containg name parameters
     * @param maxResults
     *            The maximum number of results to return
     * @param params
     *            The named parameter pairs
     * @return The results of the query
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> List<T> executeHQLQuery(final String queryString,
            Integer maxResults, Object... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Wrong number of arguments submitted to executeHQLQuery.");
        }
        return supplyInTransaction(requiredReadOnlyTransactionDef, () -> {
            Query query = getCurrentSession().createQuery(queryString);
            if (maxResults != null && maxResults > 0) {
                query.setMaxResults(maxResults);
            }
            for (int i = 0; i < params.length; i += 2) {
                if (params[i + 1] instanceof Collection<?>) {
                    query.setParameterList((String) params[i],
                            (Collection<?>) params[i + 1]);
                } else {
                    query.setParameter((String) params[i], params[i + 1]);
                }
            }
            return query.list();
        });
    }

    /**
     * Executes an HQL query in a new Hibernate session
     *
     * @param <T>
     *            An object type to query for
     * @param queryString
     *            The query to execute
     * @return The results of the HQL query
     * @throws DataAccessLayerException
     *             If errors are encountered during the HQL query
     */
    public int executeHQLStatement(String queryString)
            throws DataAccessLayerException {
        return executeHQLStatement(queryString, new Object[0]);
    }

    /**
     * Executes an HQL query
     *
     * @param <T>
     *            The return object type
     * @param queryString
     *            A StringBuilder instance containing an HQL query to execute
     * @return List containing the results of the query
     * @throws DataAccessLayerException
     *             If Hibernate errors occur during execution of the query
     */
    public int executeHQLStatement(StringBuilder queryString)
            throws DataAccessLayerException {
        return executeHQLStatement(queryString.toString(), new Object[0]);
    }

    /**
     * Executes a named parameter statement(non-query). The params argument
     * contains the parameter names in alternating fashion. The parameter name
     * comes first followed by the parameter value.
     *
     * @param <T>
     *            An object type to query for
     * @param queryString
     *            The query string, possibly containg name parameters
     * @param maxResults
     *            The maximum number of results to return
     * @param params
     *            The named parameter pairs
     * @return The results of the query
     */
    public int executeHQLStatement(final String statement, Object... params)
            throws DataAccessLayerException {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Wrong number of arguments submitted to executeHQLStatement.");
        }
        try {
            return supplyInTransaction(() -> {
                Query query = getSessionFactory().getCurrentSession()
                        .createQuery(statement).setCacheable(true)
                        .setCacheRegion(QUERY_CACHE_REGION);
                for (int i = 0; i < params.length; i += 2) {
                    if (params[i + 1] instanceof Collection<?>) {
                        query.setParameterList((String) params[i],
                                (Collection<?>) params[i + 1]);
                    } else {
                        query.setParameter((String) params[i], params[i + 1]);
                    }
                }
                return query.executeUpdate();
            });
        } catch (Throwable t) {
            throw new DataAccessLayerException(
                    "Error executing HQL Statement [" + statement + "]", t);
        }
    }

    /**
     * Executes a criteria query. This method expects a DetachedQuery instance.
     * The DetachedQuery is attached to a new session and executed
     *
     * @param <T>
     *            An Object type
     * @param criteria
     *            The DetachedCriteria instance to execute
     * @return The results of the query
     * @throws DataAccessLayerException
     *             If errors occur in Hibernate while executing the query
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> List<T> executeCriteriaQuery(
            final DetachedCriteria criteria) {
        if (criteria == null) {
            return Collections.emptyList();
        }
        return supplyInTransaction(requiredReadOnlyTransactionDef, () -> {
            return criteria.getExecutableCriteria(getCurrentSession()).list();
        });
    }

    public void evict(ENTITY entity) {
        runInTransaction(() -> {
            getCurrentSession().evict(entity);
        });
    }

    public ENTITY load(Serializable id) {
        return supplyInTransaction(() -> {
            return getCurrentSession().load(getEntityClass(), id);
        });

    }

    /**
     * Low-level method to execute a unit of work.
     *
     * @param work
     *            the work
     */
    public void executeWork(Work work) {
        runInTransaction(() -> {
            getCurrentSession().doWork(work);
        });
    }

    /**
     * Creates and returns a criteria instance
     *
     * @return The criteria instance
     */
    protected Criteria createCriteria() {
        return supplyInTransaction(() -> {
            return getCurrentSession().createCriteria(getEntityClass());
        });
    }

    /**
     * Get the hibernate dialect.
     *
     * @return the dialect.
     */
    public Dialect getDialect() {
        return supplyInTransaction(() -> {
            return ((SessionFactoryImplementor) getSessionFactory())
                    .getJdbcServices().getDialect();
        });
    }

    /**
     * Flushes and clears the current Hibernate Session
     */
    public void flushAndClearSession() {
        runInTransaction(() -> {
            getCurrentSession().flush();
            getCurrentSession().clear();
        });
    }

    @Override
    public Session getCurrentSession() {
        return supplyInTransaction(() -> {
            return super.getCurrentSession();
        });
    }

    /**
     * Return the entity class type.
     *
     * @return the entity class type
     */
    protected abstract Class<ENTITY> getEntityClass();
}
