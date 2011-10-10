package util;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Multi {
  public final static int NUM_TASKS = Runtime.getRuntime().availableProcessors();
  public final static ExecutorService SERVICE = Executors.newFixedThreadPool(NUM_TASKS); 
  
  private Multi() {}
}
