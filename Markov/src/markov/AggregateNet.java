package markov;

import java.util.ArrayList;
import java.util.Iterator;

import util.UnmodifiableIterator;

public class AggregateNet<T extends Probability<T>> implements Iterable<AggregateMachine<T>> {
  public final Dictionary dict;
  ArrayList<AggregateMachine<T>> machines;
  
  public AggregateNet(Net<T> net) {
    this.dict = net.dictionary;
    
    this.machines = new ArrayList<AggregateMachine<T>>(net.N);
    for (Machine<T> machine : net) {
      machines.add(new AggregateMachine<T>(dict, machine));
    }
  }
  
  public int size() { return machines.size(); }
  public AggregateMachine<T> getMachine(int i) { return machines.get(i); }
  
  public Iterator<AggregateMachine<T>> iterator() {
    return new UnmodifiableIterator<AggregateMachine<T>>(machines.iterator());
  }
}
