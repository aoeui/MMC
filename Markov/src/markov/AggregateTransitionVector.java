package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import util.LexicalCompare;
import util.UnmodifiableIterator;

public class AggregateTransitionVector<T extends Probability<T>> implements Comparable<AggregateTransitionVector<T>>, Iterable<T> {
  ArrayList<T> prob;  // Be careful: null entries mean ZEROs.
  
  public AggregateTransitionVector(Machine<T> machine, T zeroInstance, TransitionVector<T> vector) {
    this.prob = new ArrayList<T>();
    for (int i = 0; i < machine.size(); i++) prob.add(null);

    T sum = zeroInstance;
    for (Map.Entry<String, T> entry : vector) {
      int idx = machine.indexForState(entry.getKey());
      prob.set(idx, entry.getValue());
      sum = sum.sum(entry.getValue());
    }
    if (!sum.isOne()) throw new RuntimeException();  // transition vectors must sum to one
    for (int i = 0; i < machine.size(); i++) {
      if (prob.get(i) == null) {
        prob.set(i, zeroInstance);
      }
    }
  }
  
  private AggregateTransitionVector(ArrayList<T> arr) {
    this.prob = arr;
  }
  
  // This is not commutative. It iterates over the RHS vector first.
  public AggregateTransitionVector<T> multiply(AggregateTransitionVector<T> vect) {
    ArrayList<T> rv = new ArrayList<T>(prob.size() * vect.size());
    for (int i = 0; i < prob.size(); i++) {
      for (int j = 0; j < vect.prob.size(); j++) {
        rv.add(prob.get(i).product(vect.get(j)));
      }
    }
    if (rv.size() != size() * vect.size()) throw new RuntimeException();
    return new AggregateTransitionVector<T>(rv);
  }
  
  public T get(int idx) {
    return prob.get(idx);
  }
  
  public Iterator<T> iterator() {
    return new UnmodifiableIterator<T>(prob.iterator());
  }
  
  public int size() {
    return prob.size();
  }
  
  public int compareTo(AggregateTransitionVector<T> vect) {
    return LexicalCompare.compare(prob, vect.prob); 
  }
  
  public String toString() {
    return prob.toString();
  }
}
