/*
* Copyright (c) 2016 Martin Wawro
*
* Free for non-commercial use. See LICENSE for license details.
*/
package org.mtnwrw.pdqimg;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

/**
 * Service class to perform on-the-fly image compression.
 *
 * <p>
 * This instance is the main adapter to the native code part which implements the actual iamge
 * compression. The usage pattern for this class is rather simple: images which are to be
 * compressed must be added to the compression queue and will be compressed in FIFO order.
 * </p>
 *
 * <p>
 * The basic usage of this class is to first use the initialization method with the desired
 * number of compression threads and then queueing compression jobs to the job-queue. The
 * compression jobs must derive from the {@link CompressionQueueEntry} class and, after issued
 * to the internal queue, are handled by a couple of background threads before calling back
 * to the {@link CompressionQueueEntry#compressionDone(boolean)} function (not from within the UI
 * thread, but rather from one of the background threads), therefore asynchronously notifying the
 * caller about a (successful) compression.
 * </p>
 *
 * <p>
 * Among other things, this singleton instance features an image queue where all queue-entries
 * which should be compressed are stored in. An <em>additional</em> image queue can be found in
 * the native code, which basically mirrors the image queue here, with an important difference:
 * The image queue in this instance can be seen as endless, whereas the internal image queue in
 * the native code part has a <em>fixed</em> length and therefore is prone to <em>overflows</em>.
 * In case of overflows, the pending images are stored in this singleton and are automatically
 * scheduled to the native code part whenever an image in the native part is dequeued on completion.
 * </p>
 *
 * @author Martin Wawro
 */
public class CompressionService {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------
  /**
   * Symbolic values that control the quality of the compressed output images.
   * Lower quality usually means smaller files and vice versa.
   */
  public enum quality {
    /** Compress using low image quality (best size) */
    QUALITY_LOW(0),
    /** Compress using a medium image quality (quality/size tradeoff) */
    QUALITY_MEDIUM(1),
    /** Compress using a very good image quality (worst size) */
    QUALITY_HIGH(2);

    private final int Key;
    quality(int key) {
      Key=key;
    }
  }

  /**
   * Status codes used in conjunction with {@link #addToQueue(CompressionQueueEntry, quality)}.
   */
  public enum queuestatus {
    /** The queue entry has been successfully added to the queue */
    QUEUE_OK,
    /** The queue entry contained illegal entries */
    QUEUE_ILLEGAL_PARAMS,
    /** The input/output format (or its combination) are currently not supported */
    QUEUE_UNSUPPORTED_FORMAT,
    /** There was no memory available to add the item to the internal (native queue) */
    QUEUE_NO_MEMORY,
    /** The queue is full */
    QUEUE_BUSY,
    /** The queue is not running at all (initialization failure ?) */
    QUEUE_NOT_RUNNING
  }

  private static final String LOGTAG="CompServ";

  private static final int MAX_THREADS=8;

  //------------------------------------------------------------------------------------------------
  // Native code declarations
  //------------------------------------------------------------------------------------------------

  private native int addJob(CompressionQueueEntry entry,int quality);
  private native void removeJob(CompressionQueueEntry entry);
  private native void clearNativeQueue();
  private native boolean setupCompression(int threads);
  private native void cleanupCompression();
  private native static ByteBuffer allocateBuffer(int capacity);
  private native static void freeBuffer(ByteBuffer buff);

  //------------------------------------------------------------------------------------------------
  // Singletons
  //------------------------------------------------------------------------------------------------

  private static CompressionService Instance=null;
  private static Object InitLock = new Object();
  private static boolean NativeOK = false;

  //------------------------------------------------------------------------------------------------
  // Member variables
  //------------------------------------------------------------------------------------------------

  private Set<CompressionQueueEntry> InProgress = new HashSet<>();
  private LinkedList<CompressionQueueEntry> Pending = new LinkedList<>();
  private int CompressionThreads = 0;
  private boolean Running=false;

  //------------------------------------------------------------------------------------------------
  // Implementation
  //------------------------------------------------------------------------------------------------

  static {
    try {
      System.loadLibrary("pdqimg");
      NativeOK=true;
    } catch (UnsatisfiedLinkError ex) {
      ex.printStackTrace();
    } catch (NoSuchMethodError ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Initialize the compression service
   *
   * @param numThreads The number of threads to use for compression.
   *
   * @return On successful initialization, this function returns true. Otherwise it will return
   *         false on failure.
   */
  static public boolean initialize(int numThreads) {
    return init(numThreads);
  }

  /**
   * Retrieve singleton instance for the compression service.
   *
   * @return CompressionService instance.
   */
  static public CompressionService getInstance() {
    return Instance;
  }

  /**
   * Create/allocate output buffer for compression
   *
   * @param capacity The initial capacity (in bytes) for the output buffer. In case the buffer
   *                 turns out to be too small, the compression code will automatically
   *                 enlarge that buffer.
   *
   * @return ByteBuffer which is to be used as buffer for the output stream of the compressor
   *
   * @see #closeOutputBuffer
   */
  static public ByteBuffer createOutputBuffer(int capacity) {
    return allocateBuffer(capacity);
  }


  /**
   * Free resources taken by an output buffer
   *
   * <p>
   * This method is to be used in order to free-up the resources which are taken up by an
   * output stream buffer. Note that buffers can be reused and should only be closed in case
   * a recording session is done.
   * </p>
   *
   * @param buf The buffer which was created by createOutputBuffer()
   */
  static public void closeOutputBuffer(ByteBuffer buf) {
    freeBuffer(buf);
  }


  /**
   * Clear the compression job queue and cancel all pending/processing jobs.
   */
  public static void clearQueue() {
    if (NativeOK) {
      synchronized (Instance) {
        //----------------------------------------------
        // First remove everything from pending...
        //----------------------------------------------
        Instance.clearPending();
        //----------------------------------------------
        // ...then cancel all jobs (this function blocks)
        //----------------------------------------------
        Instance.clearNativeQueue();
        Instance.cleanupQueue();
      }
    }
  }



  /**
   * Add compression job to the internal job queue for encoding/compression.
   *
   * @param entry The job (enriched with all required information) to append to the internal
   *              compression queue.
   *
   * @param compressionQuality The compression quality (see {@link org.mtnwrw.pdqimg.CompressionService.quality})
   *                           which should be applied to this particular job.
   *
   * @return Statuscode (see {@link org.mtnwrw.pdqimg.CompressionService.queuestatus}) which
   *         indicates if the job could be successfully added to the queue or the failure reason
   *         if this was not the case.
   */
  static public queuestatus addToQueue(CompressionQueueEntry entry,quality compressionQuality) {
    if (!NativeOK) return queuestatus.QUEUE_NOT_RUNNING;
    synchronized (Instance) {
      if (!Instance.Running) return queuestatus.QUEUE_NOT_RUNNING;
      int result = Instance.addJob(entry, compressionQuality.ordinal());
      switch (result) {
        case 0:
          Instance.InProgress.add(entry);
          return queuestatus.QUEUE_OK;
        case 1:
          return queuestatus.QUEUE_ILLEGAL_PARAMS;
        case 2:
          return queuestatus.QUEUE_UNSUPPORTED_FORMAT;
        case 3:
          return queuestatus.QUEUE_NO_MEMORY;
        case 4:
          Instance.Pending.addLast(entry);
          return queuestatus.QUEUE_BUSY;
      }
      return queuestatus.QUEUE_ILLEGAL_PARAMS;
    }
  }



  /**
   * Constructor
   *
   * <p>
   * Default initializations, including initialization of the native code part.
   * </p>
   */
  private CompressionService() {
  }


  /**
   * Performs actual initialization of the compression service.
   *
   *
   * @param numThreads The number of compression threads to use.
   *
   * @return true on success, false otherwise.
   *
   */
  private static boolean init(int numThreads) {
    if (numThreads == 0) {
      numThreads = Runtime.getRuntime().availableProcessors();
    }
    synchronized (InitLock) {
      if (Instance == null) Instance = new CompressionService();
    }
    if (NativeOK) {
      synchronized (Instance) {
        if ((Instance.CompressionThreads > 0) && (Instance.CompressionThreads != numThreads)) {
          Instance.shutdown();
        }
        if (Instance.CompressionThreads != numThreads) {
          Instance.CompressionThreads = (numThreads <= MAX_THREADS) ? ((numThreads > 0) ? numThreads : 1) : MAX_THREADS;
          Instance.Running = Instance.setupCompression(Instance.CompressionThreads);
          return Instance.Running;
        }
        return true;
      }
    }
    return false;
  }


  /**
   * Internal callback function which is invoked by the native code.
   *
   * <p>
   * This callback is invoked from the native code and will dispatch that call to the
   * job instance by invoking {@link CompressionQueueEntry#compressionDone(boolean)} on that.
   * </p>
   *
   * @param entry The {@link CompressionQueueEntry} which has finished.
   *
   * @param error Error indicator, will be false if compression was successful.
   */
  private static void compressionDone(CompressionQueueEntry entry,boolean error) {
    entry.compressionDone(error);
    synchronized (Instance) {
      Instance.InProgress.remove(entry);
    }
    entry.release();
  }

  /**
   * Shuts down the compression service (including background threads).
   */
  public synchronized void shutdown() {
    synchronized (Instance) {
      Running=false;
      clearPending();
    }
    synchronized (InitLock) {
      cleanupCompression();
      CompressionThreads=0;
    }
    synchronized (Instance) {
      cleanupQueue();
    }
  }

  /**
   * Clears all pending entries in the not-yet-issued queue buffer.
   *
   * <p>
   * This function takes all entries in the queue buffer that is used to feed the native
   * queue buffer and invokes the finalization handlers (with a raised error) on these entries.
   * The entries are then discarded from the pending queue and will never be issued to the native
   * code.
   * </p>
   *
   * <p>
   * This function should be invoked with the {@link #Instance} member being locked by the
   * calling thread.
   * </p>
   */
  private void clearPending() {
    ListIterator<CompressionQueueEntry> pi = Instance.Pending.listIterator();
    while (pi.hasNext()) {
      CompressionQueueEntry q = pi.next();
      q.compressionDone(true);
      q.release();    // NOTE (mw) this closes pending image data to prevent memleaks there
      pi.remove();
    }
  }

  /**
   * Flush the entries in the currently-processed queue.
   *
   * <p>
   * This function clears all entries in the queue that mirrors the native queue without invoking
   * any callbacks. It's main functionality is to make sure that no item is overseen when flushing-
   * out the native compression queue.
   * </p>
   *
   * <p>
   * Before calling this function, the native queue should be completely flushed out (by invoking
   * the {@link #cleanupCompression()} native function and the {@link #Instance} should be locked
   * by the current thread.
   * </p>
   */
  private void cleanupQueue() {
    Iterator it = Instance.InProgress.iterator();
    while (it.hasNext()) {
      CompressionQueueEntry q = (CompressionQueueEntry) it.next();
      q.release();    // NOTE (mw) this closes pending image data to prevent memleaks there
      it.remove();
    }
  }

}
