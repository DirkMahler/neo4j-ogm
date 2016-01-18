/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session.transaction;

import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.exception.TransactionManagerException;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.transaction.AbstractTransaction;
import org.neo4j.ogm.transaction.Transaction;
import org.neo4j.ogm.transaction.TransactionManager;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class DefaultTransactionManager implements TransactionManager {

    private final Driver driver;

    private static final ThreadLocal<Transaction> TRANSACTION_THREAD_LOCAL = new ThreadLocal<>();

    public DefaultTransactionManager() {
        this(Components.driver());
    }

    public DefaultTransactionManager(Driver driver) {
        this.driver = driver;
        this.driver.setTransactionManager(this);
        TRANSACTION_THREAD_LOCAL.remove();
    }

    /**
     * Opens a new TRANSACTION_THREAD_LOCAL against a database instance.
     *
     * Instantiation of the TRANSACTION_THREAD_LOCAL is left to the driver
     *
     * @return a new {@link Transaction}
     */
    public Transaction openTransaction() {
        if (TRANSACTION_THREAD_LOCAL.get() == null) {
            TRANSACTION_THREAD_LOCAL.set(driver.newTransaction());
        } else {
            ((AbstractTransaction) TRANSACTION_THREAD_LOCAL.get()).extend();
        }
        return TRANSACTION_THREAD_LOCAL.get();
    }


    /**
     * Rolls back the specified TRANSACTION_THREAD_LOCAL.
     *
     * The actual job of rolling back the TRANSACTION_THREAD_LOCAL is left to the relevant driver. if
     * this is successful, the TRANSACTION_THREAD_LOCAL is detached from this thread.
     *
     * If the specified TRANSACTION_THREAD_LOCAL is not the correct one for this thread, throws an exception
     *
     * @param transaction the TRANSACTION_THREAD_LOCAL to rollback
     */
    public void rollback(Transaction transaction) {
        if (transaction != getCurrentTransaction()) {
            throw new TransactionManagerException("Transaction is not current for this thread");
        }
        TRANSACTION_THREAD_LOCAL.remove();
    }

    /**
     * Commits the specified TRANSACTION_THREAD_LOCAL.
     *
     * The actual job of committing the TRANSACTION_THREAD_LOCAL is left to the relevant driver. if
     * this is successful, the TRANSACTION_THREAD_LOCAL is detached from this thread.
     *
     * If the specified TRANSACTION_THREAD_LOCAL is not the correct one for this thread, throws an exception
     *
     * @param tx the TRANSACTION_THREAD_LOCAL to commit
     */
    public void commit(Transaction tx) {
        if (tx != getCurrentTransaction()) {
            throw new TransactionManagerException("Transaction is not current for this thread");
        }
        TRANSACTION_THREAD_LOCAL.remove();
    }

    /**
     * Returns the current TRANSACTION_THREAD_LOCAL for this thread, or null if none exists
     *
     * @return this thread's TRANSACTION_THREAD_LOCAL
     */
    public Transaction getCurrentTransaction() {
        return TRANSACTION_THREAD_LOCAL.get();
    }


    /**
     *
     */
    public boolean isExtended(Transaction tx) {
        if (tx == getCurrentTransaction()) {
            if ( ((AbstractTransaction) tx).extensions() == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean canCommit()  {
        AbstractTransaction tx = (AbstractTransaction) getCurrentTransaction();
        if (tx.extensions() == 0) {
            if (tx.status() == Transaction.Status.COMMIT_PENDING || tx.status() == Transaction.Status.OPEN || tx.status() == Transaction.Status.PENDING) {
                return true;
            }
        }
        return false;
    }

    public boolean canRollback()  {
        AbstractTransaction tx = (AbstractTransaction) getCurrentTransaction();
        if (tx.extensions() == 0) {
            if (tx.status() == Transaction.Status.ROLLBACK_PENDING || tx.status() == Transaction.Status.COMMIT_PENDING || tx.status() == Transaction.Status.OPEN || tx.status() == Transaction.Status.PENDING) {
                return true;
            }
        }
        return false;
    }
}