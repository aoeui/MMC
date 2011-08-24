package markov;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import util.Indenter;
import util.UnmodifiableIterator;

public class State<T extends Probability<T>> implements Comparable<State<?>> {
  public final String machineName;
  public final String name;
  TreeMap<String,String> labelVector;  // Essentially a vector of (labelName, character) pairs
  DecisionTree<TransitionVector<T>> transitionFunction;

  private State(String m, String name, Map<String,String> labelVector, DecisionTree<TransitionVector<T>> transitionFunction) {
    if (name.contains(Machine.SCOPE_OPERATOR)) throw new RuntimeException();
    this.machineName = m;
    this.name = name;
    this.labelVector = new TreeMap<String,String>(labelVector);
    this.transitionFunction = transitionFunction;
  }

  public String getLabel(String name) { return labelVector.get(name); }

  public Iterator<Map.Entry<String, String>> labelIterator() {
    return new UnmodifiableIterator<Map.Entry<String,String>>(labelVector.entrySet().iterator());
  }
  
  public Iterator<String> labelNameIterator() {
    return new UnmodifiableIterator<String>(labelVector.keySet().iterator());
  }

  public DecisionTree<TransitionVector<T>> getTransitionFunction() { return transitionFunction; }
  
  public int compareLabels(State<?> state) {
    if (!machineName.equals(state.machineName)) throw new RuntimeException();  // incomparable
    assert(labelVector.keySet().equals(state.labelVector.keySet()));
    Iterator<String> iterator1 = labelVector.values().iterator();
    Iterator<String> iterator2 = labelVector.values().iterator();
    while (iterator1.hasNext()) {
      String str1 = iterator1.next();
      String str2 = iterator2.next();
      int rv = str1.compareTo(str2);
      if (rv != 0) return rv;
    }
    return 0;
  }
  
  public boolean areAlphabetsEqual(State<?> state) {
    return labelVector.keySet().equals(state.labelVector.keySet());
  }

  public int hashCode() {
	  return (machineName+"."+name).hashCode();
  }
  
  public int compareTo(State<?> other) {
    int rv = machineName.compareTo(other.machineName);
    if (rv == 0) {
      rv = name.compareTo(other.name);
    }
    return rv;
  }

  public boolean equals(Object o) {
    try {
      State<?> other = (State<?>)o;
      return other.machineName.equals(machineName) && other.name.equals(name);
    } catch (Exception e) { return false; }
  }
  public String toString() {
    Indenter indenter = new Indenter();
    indent(indenter);
    return indenter.toString();
  }
  
  public void indent(Indenter indenter) {
    indenter.print("State(" + machineName + "." + name + "): {\n").indent();
    indenter.print("labels: ");
    boolean isFirst = true;
    for (Map.Entry<String, String> entry : labelVector.entrySet()) {
      if (isFirst) isFirst = false;
      else {
        indenter.print(", ");
      }
      indenter.print(entry.getKey()).print(" -> ").print(entry.getValue());
    }
    indenter.println();
    transitionFunction.indent(indenter);
    indenter.deindent().print("}\n");
  }

  public static class Builder<T extends Probability<T>> {
    public final String machineName;
    public final String name;
    TreeMap<String,String> labelVector;  // Essentially a vector of (labelName, character) pairs
    DecisionTree<TransitionVector<T>> transitionFunction;

    public Builder(String machineName, String name, DecisionTree<TransitionVector<T>> transitionFunction) {
      this.machineName = machineName;
      this.name = name;
      this.transitionFunction = transitionFunction;
      labelVector = new TreeMap<String,String>();
    }

    public void setLabel(String name, String instance) {
      if (name.contains(Machine.SCOPE_OPERATOR) || instance.contains(Machine.SCOPE_OPERATOR)) throw new RuntimeException();
      labelVector.put(name,instance);
    }
    
    public State<T> build() {
      return new State<T>(machineName, name, labelVector, transitionFunction);
    }
  }
}
