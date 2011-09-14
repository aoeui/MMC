package markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import util.UnmodifiableIterator;

public class Machine<T extends Probability<T>> implements Iterable<State<T>> {
  public final static String MULTIPLY_STRING = " X ";
  public final static String SCOPE_OPERATOR = ".";
  public final String name;
  
  ArrayList<String> stateNames;
  ArrayList<State<T>> states;

  private Machine(String name, TreeMap<String, State<T>> states) {
    if (name.contains(SCOPE_OPERATOR)) throw new RuntimeException("Machine name cannot contain " + SCOPE_OPERATOR);
    this.name = name;
    this.stateNames = new ArrayList<String>(states.keySet());
    this.states = new ArrayList<State<T>>(states.values());
  }
  
  public State<T> get(String name) {
    Integer idx = indexForState(name);
    return idx == null ? null : states.get(idx);
  }
  
  public State<T> get(int idx) {
    return states.get(idx);
  }
  
  // Returns null if name not found
  public Integer indexForState(String name) {
    int rv = Collections.binarySearch(stateNames, name);
    if (rv >= 0) return rv;
    return null;
  }
  
  public int getStateNum(){
    return states.size();
  }
  
  public int size() { return states.size(); }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Machine:").append(name);
    for (State<T> state : states) {
      builder.append('\n').append(state);
    }
    return builder.toString();
  }

  public Iterator<State<T>> iterator() {
    return new UnmodifiableIterator<State<T>>(states.iterator());
  }

  public boolean equals(Object o) {
    try {
      return ((Machine<?>)o).name.equals(name);
    } catch (Exception e) { return false; }
  }

  public static class Builder<T extends Probability<T>> {
    public final String name;
    TreeMap<String,State<T>> states;
    
    public Builder(String name) {
      this.name = name;
      states = new TreeMap<String,State<T>>();
    }
    
    public void addState(State<T> state) {
      if (states.size() != 0) {
        State<T> check = states.firstEntry().getValue();
        if (!check.areAlphabetsEqual(state)) {
          throw new RuntimeException("States " + check + " and added state " + state + " have different alphabets");
        }
      }
      states.put(state.name, state);
    }
    
    public Machine<T> build() {
      if (states.size() == 0) throw new RuntimeException("Machine needs at least one state");
      return new Machine<T>(name, new TreeMap<String,State<T>>(states));
    }
  }
}