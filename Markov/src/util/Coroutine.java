package util;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/* Threads are top level objects, meaning they can't be gc'd unless they are
   complete. In order to prevent memory leaks, make sure that either the
   iteration terminates OR end() is called after iteration is done. */
public abstract class Coroutine<T> implements Runnable,Iterable<T> {
  public final static ExecutorService SERVICE = Executors.newCachedThreadPool();
  // = Executors.newFixedThreadPool(2);  // useful for testing

  boolean cancelled = false;
  private LinkedList<T> buffer;
  // signals readiness of producer (either next element available or end is known)
  private Semaphore serverReady;

  private static class KillException extends RuntimeException {
    private static final long serialVersionUID = 1L; }

  public Coroutine() {
    buffer = new LinkedList<T>();
    serverReady = new Semaphore(0);
  }

  public abstract void init();

  private final T nextImpl() {
    serverReady.acquireUninterruptibly();
    synchronized (buffer) {
      if (buffer.size() > 0) {
        return buffer.removeFirst();
      } else {
        throw new java.util.NoSuchElementException();
      }
    }
  }

  private final boolean hasNextImpl() {
    serverReady.acquireUninterruptibly();  // consume a token
    boolean rv = false;
    synchronized (buffer) {
      rv = buffer.size() > 0;
    }
    serverReady.release();
    return rv;
  }

  public final void yield(T val) {
    if (cancelled) throw new KillException();
    synchronized (buffer) {
      buffer.add(val);
    }
    serverReady.release();
  }

  public final void run() {
    try {
      init();
    } catch (KillException endedByKill) {
      // out.println("killed");
    }
    serverReady.release();  // a list of N elements sends N+1 signals
  }

  private final void start() {
    SERVICE.submit(this);
  }

  public TerminatedIterator<T> iterator() {
    start();
    return new TerminatedIterator<T>() {
      public final boolean hasNext() { return hasNextImpl(); }
      public final T next() { return nextImpl(); }
      public final void remove() { throw new UnsupportedOperationException(); }
      public final void end() { endImpl(); }
    };
  }

  /** Call this method if iteration ends before hasNext() returns false.
   * Harmless to call after hasNext() returns false. */
  public final void endImpl() {
    this.cancelled = true;
  }
}
