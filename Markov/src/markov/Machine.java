package markov;

import java.util.Iterator;
import java.util.TreeMap;

import util.UnmodifiableIterator;

public class Machine<T extends Probability<T>> implements Iterable<State<T>> {
  public final String name;
  TreeMap<String, State<T>> states;

  private Machine(String name, TreeMap<String, State<T>> states) {
    this.name = name;
    this.states = states;
  }
  
  public State<T> get(String name) {
    return states.get(name);
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Machine:").append(name);
    for (State<T> state : states.values()) {
      builder.append('\n').append(state);
    }
    return builder.toString();
  }

  public Iterator<State<T>> iterator() {
    return new UnmodifiableIterator<State<T>>(states.values().iterator());
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