package util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public abstract class Multi {
  public final static int NUM_TASKS = Runtime.getRuntime().availableProcessors();
  public final static ExecutorService SERVICE = Executors.newFixedThreadPool(NUM_TASKS); 
  
  private ArrayList<Callable<Object>> tasks;
  
  public Multi() {
    this.tasks = new ArrayList<Callable<Object>>();
  }
  
  public abstract void init();
  
  public final void run() {
    init();
    try {
      for (Future<Object> future : SERVICE.invokeAll(tasks)) {
        future.get();  // this is called simply to trigger the exception
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public final void submit(Runnable task) {
    tasks.add(new CallableAdapter(task));
  }
  
  private static class CallableAdapter implements Callable<Object> {
    public final Runnable task;
    
    public CallableAdapter(Runnable task) {
      this.task = task;
    }
    
    public Object call() {
      task.run();
      return null;
    }
  }
  
  private static class MultiIterate extends Multi {
    Iterator<Runnable> it;
    public MultiIterate(Iterator<Runnable> it) {
      this.it = it;
    }
    
    public void init() {
      while (it.hasNext()) {
        submit(it.next());
      }
    }
  }
  
  /* These convenience methods will simplify client code */
  public static void runAll(Iterator<Runnable> it) {
    new MultiIterate(it).run();
  }
  
  public static void runAll(Iterable<Runnable> gen) {
    runAll(gen.iterator());
  }
}
