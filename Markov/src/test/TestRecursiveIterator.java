package test;

import static java.lang.System.out;

import java.util.Random;

import util.Closure;
import util.TerminatedIterator;

public class TestRecursiveIterator {
  public static Random rng = new Random();

  public static void main(String[] args) {
    Closure<Integer> test = new NullTest();
    for (int i : test) {
      out.println("null iterated " + i);
    }
    for (int i = 0; i < 100000; i++) {
      int rv = runTest();
      out.println("Success (" + rv + ") " + i);
    }
    out.println("Done");
  }

  public static int runTest() {
    int num = rng.nextInt(5);
    TerminatedIterator<Integer> it = new TestIterator().iterator();
    int count = 0;
    while (it.hasNext()) {
      int n = it.next();
      if (n != count++) { out.println("ERROR"); }
      if (n >= num) break;
    }
    if (rng.nextInt(2) == 0) {
      try {
        Thread.sleep(rng.nextInt(50));
      } catch (InterruptedException ignore) {}
    }
    it.end();
    return count;
  }
  
  public static class NullTest extends Closure<Integer> {
    public void init() {}
  }

  public static class TestIterator extends Closure<Integer> {
    public static int COUNT = 0;
    
    public final int id = COUNT++;

    public void init() {
      int i = 0;
      while (true) {
        /* try {
          Thread.sleep(rng.nextInt(50));
        } catch (InterruptedException e) {} */
        // out.println(id + " yielding " + i);
        yield(i++);
      }
    }
  }
}
