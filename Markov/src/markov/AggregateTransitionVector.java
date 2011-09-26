package markov;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import util.LexicalCompare;

public class AggregateTransitionVector<T extends Probability<T>> implements Comparable<AggregateTransitionVector<T>>, Iterable<T> {
  TreeMap<Integer,T> prob;
  public final int size;
  
  public final T zero;
  
  public AggregateTransitionVector(Machine<T> machine, T zeroInstance, TransitionVector<T> vector) {
    this.prob = new TreeMap<Integer,T>();
    this.zero = zeroInstance;
    this.size = machine.size();
    
    T sum = zeroInstance;
    for (Map.Entry<String, T> entry : vector) {
      if (entry.getValue().isZero()) continue;
      int idx = machine.indexForState(entry.getKey());
      prob.put(idx, entry.getValue());
      sum = sum.sum(entry.getValue());
    }
    if (!sum.isOne()) throw new RuntimeException();  // transition vectors must sum to one
  }
  
  public AggregateTransitionVector<T> removeStates(SortedSet<Integer> toRemove) {
    TreeMap<Integer,T> newProb = new TreeMap<Integer,T>();
    for (Map.Entry<Integer, T> entry : prob.entrySet()) {
      if (toRemove.contains(entry.getKey())) continue;
      
      newProb.put(entry.getKey()-toRemove.headSet(entry.getKey()).size(), entry.getValue());
    }
    return new AggregateTransitionVector<T>(newProb, zero, size-toRemove.size());
  }

  public AggregateTransitionVector<T> remap(int range, Map<Integer,Integer> map) {
    TreeMap<Integer,T> newProb = new TreeMap<Integer,T>();
    
    for (Map.Entry<Integer, T> entry : prob.entrySet()) {
      int newIdx = map.get(entry.getKey());
      T val = newProb.get(newIdx);
      newProb.put(newIdx, val == null ? entry.getValue() : val.sum(entry.getValue()));
    }
    return new AggregateTransitionVector<T>(newProb, zero, range);
  }
  
  private AggregateTransitionVector(TreeMap<Integer,T> arr, T zero, int size) {
    this.prob = arr;
    this.zero = zero;
    this.size = size;
  }
  
  // This is not commutative. It iterates over the RHS vector first.
  // The new indices are given by idx1*size2+idx2
  public AggregateTransitionVector<T> multiply(AggregateTransitionVector<T> vect) {
    TreeMap<Integer,T> prod = new TreeMap<Integer,T>();

    for (Map.Entry<Integer, T> entry1 : prob.entrySet()) {
      for (Map.Entry<Integer, T> entry2 : vect.prob.entrySet()) {
        prod.put(entry1.getKey()*vect.size + entry2.getKey(), entry1.getValue().product(entry2.getValue()));
      }
    }
    return new AggregateTransitionVector<T>(prod, zero, size*vect.size);
  }

  public T get(int idx) {
    T rv = prob.get(idx);
    return rv == null ? zero : rv;
  }
  
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      int next = 0;
      public boolean hasNext() { return next < size; }
      public T next() { return get(next++); }
      public void remove() { throw new UnsupportedOperationException(); }
    };
  }
    
  public int compareTo(AggregateTransitionVector<T> vect) {
    return LexicalCompare.compare(iterator(), vect.iterator()); 
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < size; i++) {
      if (i != 0) builder.append(", ");
      builder.append(get(i));
    }
    builder.append(']');
    return builder.toString();
  }
}
