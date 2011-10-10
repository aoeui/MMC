package util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Multi {
  public final static int NUM_TASKS = Runtime.getRuntime().availableProcessors();
  public final static ExecutorService SERVICE = Executors.newFixedThreadPool(NUM_TASKS); 
  
  private Multi() {}
  
  /* These convenience methods will simplify client code */
  public static void runAll(Iterator<Runnable> it) {
    ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
    while (it.hasNext()) {
      final Runnable next = it.next();
      tasks.add(new Callable<Object>() {
        public Object call() {
          next.run();
          return null;
        }
      });
    }
    try {
      for (Future<Object> future : SERVICE.invokeAll(tasks)) {
        future.get();  // this is called simply to trigger the exception
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void runAll(Iterable<Runnable> gen) {
    runAll(gen.iterator());
  }
}
