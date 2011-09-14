package markov;

import java.util.ArrayList;
import java.util.Iterator;

import util.UnmodifiableIterator;

public class AggregateMachine<T extends Probability<T>> implements Iterable<AggregateState<T>> {
  private ArrayList<AggregateState<T>> states;
  
  public AggregateMachine(Dictionary dict, Machine<T> machine) {
    states = new ArrayList<AggregateState<T>>(machine.size());
    for (int i = 0; i < machine.size(); i++) {
      states.add(new AggregateState<T>(dict, machine, i));
    }
  }
  
  private AggregateMachine(ArrayList<AggregateState<T>> states) {
    this.states = states;
  }
  
  public int size() { return states.size(); }
  public AggregateState<T> getState(int i) { return states.get(i); }
  public Iterator<AggregateState<T>> iterator() {
    return new UnmodifiableIterator<AggregateState<T>>(states.iterator());
  }
  
  public AggregateMachine<T> product(AggregateMachine<T> machine) {
    ArrayList<AggregateState<T>> newStates = new ArrayList<AggregateState<T>>(size() * machine.size());
    for (int i = 0; i < size(); i++) {
      for (int j = 0; j < size(); j++) {
        newStates.add(states.get(i).combine(machine.getState(j)));
      }
    }
    return new AggregateMachine<T>(newStates);
  }
}
