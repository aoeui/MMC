package markov;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import util.FilterIterator;
import util.UnmodifiableIterator;;

public class TransitionVector<T extends Probability<T>>
    implements Iterable<Map.Entry<State<T>, T>>, Comparable<TransitionVector<T>> {
  public final Machine<T> machine;
  private final TreeMap<State<T>,T> map;
  
  private TransitionVector(Machine<T> machine, TreeMap<State<T>, T> map) {
    this.machine = machine;
    this.map = new TreeMap<State<T>,T>(map);
  }
  
  public T getProbability(State<T> state) {
    return map.get(state);
  }
  
  public Iterator<Map.Entry<State<T>, T>> iterator() {
    return new UnmodifiableIterator<Map.Entry<State<T>,T>>(map.entrySet().iterator());
  }
  
  public Iterator<Map.Entry<State<T>, T>> nonZeroIterator() {
    return new FilterIterator<Map.Entry<State<T>,T>>(map.entrySet().iterator()) {
      public boolean allowElement(Map.Entry<State<T>, T> entry) {
        return !entry.getValue().isZero();
      }
    };
  }
  
  public int compareTo(TransitionVector<T> other) {
    Iterator<Map.Entry<State<T>, T>> myIt = nonZeroIterator();
    Iterator<Map.Entry<State<T>, T>> hisIt = other.nonZeroIterator();
    int rv = 0;
    while (rv == 0 && myIt.hasNext() && hisIt.hasNext()) {
      Map.Entry<State<T>, T> myEntry = myIt.next();
      Map.Entry<State<T>, T> hisEntry = hisIt.next();
      int comp = myEntry.getKey().compareTo(hisEntry.getKey());
      if (comp == 0) {
        rv = myEntry.getValue().compareTo(hisEntry.getValue());
      } else {
        rv = comp < 0 ? 1 : -1;  // if comp < 0, my key is earlier and I have a greater entry
      }
    }
    if (rv == 0) {
      if (myIt.hasNext()) {
        rv = 1;
      } else if (hisIt.hasNext()) {
        rv = -1;
      }
    }
    return rv;
  }
  
  public boolean equals(Object o) {
    try {
      TransitionVector<?> other = ((TransitionVector<?>)o);
      if (!other.machine.equals(machine)) return false;
      return map.equals(other.map);
    } catch (Exception e) { return false; }
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean isFirst = true;
    for (Map.Entry<State<T>, T> entry : map.entrySet()) {
      if (isFirst) isFirst = false;
      else {
        builder.append(", ");
      }
      builder.append("p[").append(entry.getKey().name).append("]=").append(entry.getValue());
    }
    return builder.toString();
  }

  public static class Builder<T extends Probability<T>> {
    Machine<T> machine;
    TreeMap<State<T>,T> map;
    
    public Builder(Machine<T> machine) {
      this.machine = machine;
      this.map = new TreeMap<State<T>,T>();
    }
    
    public void setProbability(State<T> state, T prob) {
      map.put(state, prob);
    }
    
    public TransitionVector<T> build() {
      if (map.size() == 0) throw new RuntimeException();
      T sum = null;
      for (T prob : map.values()) {
        sum = (sum == null) ? prob : sum.sum(prob);
      }
      if (!sum.isOne()) throw new RuntimeException();
      
      return new TransitionVector<T>(machine, map);
    }
  }
}
