/*
 * Copyright 2013 FH OOe Forschungs & Entwicklungs GmbH, Michael Roland.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.licel.jcardsim.base;

import at.mroland.objectstaterecovery.PersistentMemory;
import javacard.framework.TransactionException;

/**
 * Management of transactions and persistent memory state in non-Java Card environments.
 */
public class TransactionManager {

    private PersistentMemory persistentMemory;
    
    /**
     * Current depth of transaction (can be 0 or 1, as nested transactions are
     * not supported by JavaCard 2.2.2 spec).
     */
    private byte transactionDepth = 0;

    public TransactionManager(PersistentMemory memoryManager) {
        persistentMemory = memoryManager;
    }
    
    /**
     * Begins an atomic transaction. If a transaction is already in
     * progress (transaction nesting depth level != 0), a TransactionException is
     * thrown.
     * <p>Note:
     * <ul>
     * <li><em>This method may do nothing if the <code>Applet.register()</code>
     * method has not yet been invoked. In case of tear or failure prior to successful
     * registration, the Java Card runtime environment will roll back all atomically updated persistent state.</em>
     * </ul>
     * @throws TransactionException  with the following reason codes:
     * <ul>
     * <li><code>TransactionException.IN_PROGRESS</code> if a transaction is already in progress.
     * </ul>
     * @see #abortTransaction()
     * @see #commitTransaction()
     */
    public void beginTransaction() {
        if (transactionDepth != 0) {
            TransactionException.throwIt(TransactionException.IN_PROGRESS);
        }
        
        persistentMemory.memoryBarrier(false);
        
        transactionDepth = 1;
    }

    /**
     * Aborts the atomic transaction. The contents of the commit
     * buffer is discarded.
     * <p>Note:
     * <ul>
     * <li><em>This method may do nothing if the <code>Applet.register()</code>
     * method has not yet been invoked. In case of tear or failure prior to successful
     * registration, the Java Card runtime environment will roll back all atomically updated persistent state.</em>
     * <li><em>Do not call this method from within a transaction which creates new objects because
     * the Java Card runtime environment may not recover the heap space used by the new object instances.</em>
     * <li><em>Do not call this method from within a transaction which creates new objects because
     * the Java Card runtime environment may, to ensure the security of the card and to avoid heap space loss,
     * lock up the card session to force tear/reset processing.</em>
     * <li><em>The Java Card runtime environment ensures that any variable of reference type which references an object
     * instantiated from within this aborted transaction is equivalent to
     * a </em><code>null</code><em> reference.</em>
     * </ul>
     * @throws TransactionException - with the following reason codes:
     * <ul>
     * <li><code>TransactionException.NOT_IN_PROGRESS</code> if a transaction is not in progress.
     * </ul>
     * @see #beginTransaction()
     * @see #commitTransaction()
     */
    public void abortTransaction() {
        if (transactionDepth == 0) {
            TransactionException.throwIt(TransactionException.NOT_IN_PROGRESS);
        }
        
        persistentMemory.memoryBarrier(true);
        
        transactionDepth = 0;
    }

    /**
     * Commits an atomic transaction. The contents of commit
     * buffer is atomically committed. If a transaction is not in
     * progress (transaction nesting depth level == 0) then a TransactionException is
     * thrown.
     * <p>Note:
     * <ul>
     * <li><em>This method may do nothing if the <code>Applet.register()</code>
     * method has not yet been invoked. In case of tear or failure prior to successful
     * registration, the Java Card runtime environment will roll back all atomically updated persistent state.</em>
     * </ul>
     * @throws TransactionException with the following reason codes:
     * <ul>
     * <li><code>TransactionException.NOT_IN_PROGRESS</code> if a transaction is not in progress.
     * </ul>
     * @see #beginTransaction()
     * @see #abortTransaction()
     */
    public void commitTransaction() {
        if (transactionDepth == 0) {
            TransactionException.throwIt(TransactionException.NOT_IN_PROGRESS);
        }
        
        // NOTE: This memoryBarrier is unnecessary as we always save state on beginTransaction and after applet processing.
        //persistentMemory.memoryBarrier(false);
        
        transactionDepth = 0;
    }
    
    /**
     * Returns the current transaction nesting depth level. At present,
     * only 1 transaction can be in progress at a time.
     * @return 1 if transaction in progress, 0 if not
     */
    public byte getTransactionDepth() {
        return transactionDepth;
    }
    
    /**
     * Returns the number of bytes left in the commit buffer.
     * <p> Note:
     * <ul>
     * <li><em>Current method implementation returns 32767.</em>
     * </ul>
     * @return the number of bytes left in the commit buffer
     * @see #getMaxCommitCapacity()
     */
    public short getUnusedCommitCapacity() {
        return Short.MAX_VALUE;
    }
    
    /**
     * Returns the total number of bytes in the commit buffer.
     * This is approximately the maximum number of bytes of
     * persistent data which can be modified during a transaction.
     * However, the transaction subsystem requires additional bytes
     * of overhead data to be included in the commit buffer, and this
     * depends on the number of fields modified and the implementation
     * of the transaction subsystem. The application cannot determine
     * the actual maximum amount of data which can be modified during
     * a transaction without taking these overhead bytes into consideration.
     * <p> Note:
     * <ul>
     * <li><em>Current method implementation returns 32767.</em>
     * </ul>
     * @return the total number of bytes in the commit buffer
     * @see #getUnusedCommitCapacity()
     */
    public short getMaxCommitCapacity() {
        return Short.MAX_VALUE;
    }

    /**
     * Get the number of bytes available in persistent memory.
     * @return number of bytes available in persistent memory, or <code>Short.MAX_VALUE</code> if number of bytes exceeds <code>Short.MAX_VALUE</code>
     */
    public short getAvailablePersistentMemory () {
        return Short.MAX_VALUE;
    }
}
