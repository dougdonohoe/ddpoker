/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * 
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials) 
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 * 
 * For inquiries regarding commercial licensing of this source code or 
 * the use of names, logos, images, text, or other assets, please contact 
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.db.dao.impl;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.db.PagedList;
import com.donohoedigital.db.dao.BaseDao;
import com.donohoedigital.db.model.BaseModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: donohoe
 * Date: Mar 13, 2008
 * Time: 11:01:56 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class JpaBaseDao<T extends BaseModel<ID>, ID extends Serializable> implements BaseDao<T, ID>
{
    // the model class we are managing
    private Class<T> persistentModelClass;

    // entity manager provided via Spring
    protected EntityManager entityManager;

    /**
     * Construct.  Upon creation, determine the actual model class (must do at runtime due to java generic's
     * type erasure).
     */
    @SuppressWarnings("unchecked")
    public JpaBaseDao()
    {
        persistentModelClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * Entity manager - set by Spring
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * Return the model class that is being persisted.
     */
    public Class<T> getPersistentModelClass()
    {
        return persistentModelClass;
    }

    ///
    /// BaseDao Implementation
    ///

    /**
     * return name of class for use in queries
     */
    public String getName()
    {
        return persistentModelClass.getSimpleName();
    }

    public Query createQuery(String query)
    {
        return entityManager.createQuery(query);
    }

    public Query createNativeQuery(String query)
    {
        return entityManager.createNativeQuery(query);
    }

    public T get(ID id)
    {
        return entityManager.find(persistentModelClass, id);
    }

    public void save(T entity)
    {
        entityManager.persist(entity);
    }

    public T update(T entity)
    {
        return entityManager.merge(entity);
    }

    public void refresh(T entity)
    {
        entityManager.refresh(entity);
    }

    public void delete(T entity)
    {
        // if not attached, get an attached version
        if (!entityManager.contains(entity))
        {
            entity = get(entity.getId());
        }
        entityManager.remove(entity);
    }

    public void deleteAll()
    {
        entityManager.createQuery("delete from " + getName()).executeUpdate();
    }

    public void flush()
    {
        entityManager.flush();
    }

    public void clear()
    {
        entityManager.clear();
    }

    @SuppressWarnings({"unchecked", "JpaQlInspection"})
    public List<T> getAll()
    {
        return entityManager.createQuery("select x from " + getName() + " x order by x.id").getResultList();
    }

    ////
    //// Common convienence methods for subclasses
    ////

    /**
     * For calls returning single item using JPQL.  Caller must format query properly with numbered parameters (?#).
     */
    @SuppressWarnings({"unchecked"})
    protected T getSingleItem(String query, Object... params)
    {
        Query selectQuery = entityManager.createQuery(query);
        setParametersFromVarargs(selectQuery, params);
        List<T> list = (List<T>) selectQuery.getResultList();
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    /**
     * For calls returning single item using native SQL.  Caller must format query properly with numbered parameters (?#).
     */
    @SuppressWarnings({"unchecked"})
    protected T getSingleItemNative(String query, Object... params)
    {
        Query selectQuery = entityManager.createNativeQuery(query, getPersistentModelClass());
        setParametersFromVarargs(selectQuery, params);
        List<T> list = (List<T>) selectQuery.getResultList();
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    /**
     * For calls returning non-paged lists.  Caller must format query properly with numbered parameters (?#).
     */
    protected List<T> getList(String query, Object... params)
    {
        return getPagedList(query, null, 0, 0, params);
    }

    /**
     * For calls returning non-paged lists with native query.  Caller must format query properly with numbered parameters (?#).
     */
    protected List<T> getListNative(String query, Object... params)
    {
        return getPagedList(query, null, 0, 0, true, params);
    }

    /**
     * For 'select count(x)...' JPQL methods.  Caller must format query properly with numbered parameters (?#).
     */
    protected int getCount(String query, Object... params)
    {
        Query countQuery = entityManager.createQuery(query);
        setParametersFromVarargs(countQuery, params);
        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    /**
     * For 'select count(x)...' native SQL methods.  Caller must format query properly with numbered parameters (?#).
     */
    protected int getCountNative(String query, Object... params)
    {
        Query countQuery = entityManager.createNativeQuery(query);
        setParametersFromVarargs(countQuery, params);
        Long rowCount = (Long) countQuery.getSingleResult();
        return rowCount.intValue();
    }

    /**
     * For calls returning paged lists using JPQL.  Caller must format query properly with numbered parameters (?#).
     *
     * @param count    Count returned from previous call to getCount or previous call to this.  Passing this in
     *                 saves an additional count query (used when paging through a list - only do the count at the start).
     *                 May be null if isCountNeeded() is false
     * @param offset   starting row
     * @param pagesize max rows to return.  <= 0 means return all rows.  If <= 0, offset must be 0
     */
    protected PagedList<T> getPagedList(String query, Integer count, int offset, int pagesize, Object... params)
    {
        return getPagedList(query, count, offset, pagesize, false, params);
    }

    /**
     * For calls returning paged lists using native SQL.  Caller must format query properly with numbered parameters (?#).
     *
     * @param count    Count returned from previous call to getCount or previous call to this.  Passing this in
     *                 saves an additional count query (used when paging through a list - only do the count at the start).
     *                 May be null if isCountNeeded() is false
     * @param offset   starting row
     * @param pagesize max rows to return.  <= 0 means return all rows.  If <= 0, offset must be 0
     */
    protected PagedList<T> getPagedListNative(String query, Integer count, int offset, int pagesize, Object... params)
    {
        return getPagedList(query, count, offset, pagesize, true, params);
    }

    /**
     * For calls returning paged lists.  Caller must format query properly with numbered parameters (?#).
     *
     * @param count       Count returned from previous call to getCount or previous call to this.  Passing this in
     *                    saves an additional count query (used when paging through a list - only do the count at the start).
     *                    May be null if isCountNeeded() is false
     * @param offset      starting row
     * @param pagesize    max rows to return.  <= 0 means return all rows.  If <= 0, offset must be 0
     * @param nativeQuery if true, indicates the query is native sql.   Otherwise it is JPQL
     */
    @SuppressWarnings({"unchecked"})
    private PagedList<T> getPagedList(String query, Integer count, int offset, int pagesize, boolean nativeQuery, Object... params)
    {
        // validate
        if (offset < 0)
        {
            throw new ApplicationError("Offset (" + offset + ") cannot be negative");
        }
        if (pagesize <= 0 && offset != 0)
        {
            throw new ApplicationError("Offset (" + offset + ") must be 0 if pagesize <= 0");
        }

        // create query
        Query selectQuery;
        if (nativeQuery)
        {
            selectQuery = entityManager.createNativeQuery(query, getPersistentModelClass());
        }
        else
        {
            selectQuery = entityManager.createQuery(query);
        }
        setParametersFromVarargs(selectQuery, params);
        selectQuery.setFirstResult(offset);
        if (pagesize > 0) selectQuery.setMaxResults(pagesize);

        // run the query
        List<T> results = (List<T>) selectQuery.getResultList();

        // if count should be provided (paging call), throw exception; otherwise get result size (all rows)
        if (isCountNeeded(count, pagesize))
        {
            throw new ApplicationError("Count must not be null - caller should call getCount()");
        }
        else if (count == null || count <= 0)
        {
            count = results.size();
        }

        // return all results
        PagedList<T> list = new PagedList<T>(results);
        list.setTotalSize(count);

        return list;
    }

    /**
     * Determine if count needs to be determined (if count is null or <= 0 and pagesize > 0)
     */
    protected boolean isCountNeeded(Integer count, int pagesize)
    {
        return (count == null || count <= 0) && pagesize > 0;
    }

    /**
     * Sets indexed params from varargs.  Vararg can contain sub-array(s)
     * of Object[].
     */
    @SuppressWarnings({"ChainOfInstanceofChecks"})
    private void setParametersFromVarargs(Query query, Object... params)
    {
        int paramnum = 1;
        for (Object param : params)
        {
            if (param instanceof Object[])
            {
                Object[] array = (Object[]) param;
                for (Object item : array)
                {
                    query.setParameter(paramnum++, item);
                }
            }
            else if (param instanceof String[])
            {
                String[] array = (String[]) param;
                for (String item : array)
                {
                    query.setParameter(paramnum++, item);
                }
            }
            else
            {
                query.setParameter(paramnum++, param);
            }
        }
    }

    /**
     * get in clause parameter list of given length starting at given index
     */
    protected String getInClause(int start, int length)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            if (sb.length() > 0) sb.append(", ");
            sb.append('?').append(i + start);
        }
        sb.insert(0, "(");
        sb.append(")");
        return sb.toString();
    }
}
