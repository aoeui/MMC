package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import util.FilterIterator;
import util.Joiner;
import util.UnmodifiableIterator;

public class TransitionVector<T extends Probability<T>>
    implements Iterable<Map.Entry<String, T>>, Comparable<TransitionVector<T>>  {
  public final String machineName;
  private final TreeMap<String,T> map;  // maps state name to probability
  
  private TransitionVector(String machineName, TreeMap<String, T> map) {
    this.machineName = machineName;
    this.map = new TreeMap<String,T>(map);
  }
  
  public T getProbability(String stateName) {
    return map.get(stateName);
  }
  
  public Iterator<Map.Entry<String, T>> zeroIncludingIterator() {
    return new UnmodifiableIterator<Map.Entry<String,T>>(map.entrySet().iterator());
  }
  
  // Default iterator ignores zeros.
  public Iterator<Map.Entry<String, T>> iterator() {
    return new FilterIterator<Map.Entry<String,T>>(map.entrySet().iterator()) {
      public boolean allowElement(Map.Entry<String, T> entry) {
        return !entry.getValue().isZero();
      }
    };
  }
  
  public int compareTo(TransitionVector<T> other) {
    Iterator<Map.Entry<String, T>> myIt = iterator();
    Iterator<Map.Entry<String, T>> hisIt = other.iterator();
    int rv = 0;
    while (rv == 0 && myIt.hasNext() && hisIt.hasNext()) {
      Map.Entry<String, T> myEntry = myIt.next();
      Map.Entry<String, T> hisEntry = hisIt.next();
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
      return map.equals(other.map);
    } catch (Exception e) { return false; }
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean isFirst = true;
    for (Map.Entry<String, T> entry : map.entrySet()) {
      if (isFirst) isFirst = false;
      else {
        builder.append(", ");
      }
      builder.append("p[").append(entry.getKey()).append("]=").append(entry.getValue());
    }
    return builder.toString();
  }

  public TransitionVector<T> times(TransitionVector<T> monoid) {
    boolean[] sequence = computeSequence(monoid);
    Builder<T> builder = new Builder<T>(mergeNames(sequence, machineName, monoid.machineName));
    
    for (Map.Entry<String, T> myEntry : this) {
      for (Map.Entry<String, T> entry : monoid) {
        builder.setProbability(mergeNames(sequence, myEntry.getKey(), entry.getKey()), myEntry.getValue().product(entry.getValue()));
      }
    }    
    return builder.build();
  }

  private boolean[] computeSequence(TransitionVector<T> arg) {
    String[] myNameArr = machineName.split(Machine.MULTIPLY_STRING);
    String[] nameArr = arg.machineName.split(Machine.MULTIPLY_STRING);
    boolean[] sequence = new boolean[myNameArr.length + nameArr.length];

    String prev = null;
    int myIdx = 0, idx = 0;
    while (myIdx < myNameArr.length && idx < nameArr.length) {
      int comp = myNameArr[myIdx].compareTo(nameArr[idx]);
      if (comp == 0) throw new RuntimeException();
      boolean nextSeq = comp > 0;
      String next = nextSeq ? myNameArr[myIdx] : nameArr[idx];
      sequence[myIdx + idx] = nextSeq;
      if (nextSeq) myIdx++; else idx++;

      if (prev != null && next.compareTo(prev) <= 0) throw new RuntimeException();  // verifies uniqueness and order of machine names
      prev = next;
    }
    return sequence;
  }

  private static String mergeNames(boolean[] sequence, String name1, String name2) {
    String[] list1 = name1.split(Machine.MULTIPLY_STRING);
    String[] list2 = name2.split(Machine.MULTIPLY_STRING);
    if (list1.length + list2.length != sequence.length) throw new RuntimeException();

    ArrayList<String> joined = new ArrayList<String>(sequence.length);

    int idx1=0, idx2=0;
    for (int i = 0; i < sequence.length; i++) {
      joined.add(sequence[i] ? list1[idx1++] : list2[idx2++]);
    }
    return Joiner.join(joined, Machine.MULTIPLY_STRING);
  }

  public static class Builder<T extends Probability<T>> {
    final String machineName;
    TreeMap<String,T> map;
    
    public Builder(String machineName) {
      this.machineName = machineName;
      this.map = new TreeMap<String,T>();
    }
    
    public void setProbability(String state, T prob) {
      map.put(state, prob);
    }
    
    public TransitionVector<T> build() {
      if (map.size() == 0) throw new RuntimeException();
      T sum = null;
      for (T prob : map.values()) {
        sum = (sum == null) ? prob : sum.sum(prob);
      }
      if (!sum.isOne()) throw new RuntimeException();
      
      return new TransitionVector<T>(machineName, map);
    }
  }
}