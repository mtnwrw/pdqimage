/*
* Copyright (c) 2016, Martin Wawro
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice, this
*    list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
* The views and conclusions contained in the software and documentation are those
* of the authors and should not be interpreted as representing official policies,
* either expressed or implied, of the FreeBSD Project.
*/
package org.mtnwrw.cameraexample.util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Simple binary (boolean) semaphore.
 *
 * <p>
 * Standard text-book binary semaphore implementation using a counting semaphore and a lock.
 * </p>
 *
 * @author Martin Wawro
 */

public class BinarySemaphore {

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  /**
   * Internal (counting) semaphore.
   */
  private Semaphore Counter = null;


  //------------------------------------------------------------------------------------------------
  // Class implementation
  //------------------------------------------------------------------------------------------------


  /**
   * Constructor
   *
   * @param initialAvail If set to true, the semaphore will be available initially, otherwise
   *                     it will not.
   */
  public BinarySemaphore(boolean initialAvail) {
    if (initialAvail) Counter = new Semaphore(1,true);
    else Counter = new Semaphore(0,true);
  }

  /**
   * Non-blockingly try to acquire the semaphore.
   *
   * @return true if the semaphore was successfully acquired, false otherwise.
   */
  public boolean tryAcquire() {
    return (Counter.tryAcquire(1));
  }

  /**
   * Timed acquisition of the semaphore.
   *
   * <p>
   * This function tries to acquire the semaphore for a specified amount of time. On timeout,
   * it returns false.
   * </p>
   * @param time The amount of time-units to wait for the semaphore
   *
   * @param unit The time unit ({@link TimeUnit}) to use as basic element for the timeout.

   * @return true in case the semaphore was acquired successfully, false otherwise.
   *
   * @throws InterruptedException if the current thread was somehow interrupted during waiting
   */
  public boolean tryAcquire(long time,TimeUnit unit) throws InterruptedException {
    return Counter.tryAcquire(time,unit);
  }

  /**
   * Blockingly acquire the semaphore
   *
   * @throws InterruptedException if the current thread was somehow interrupted during waiting
   */
  public void acquire() throws InterruptedException {
    Counter.acquire(1);
  }

  /**
   * Check if the semaphore is available or not
   *
   * @return true if the semaphore can be acquired, false otherwise
   */
  public synchronized boolean available() {
    return (Counter.availablePermits()>0);
  }

  /**
   * Release the semaphore back to an acquirable (is this even a real word ?) state
   */
  public synchronized void release() {
    if (Counter.availablePermits() < 1) Counter.release(1);
  }
}
