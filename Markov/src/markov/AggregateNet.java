package markov;

import java.util.ArrayList;
import java.util.Iterator;

import util.Stack;
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
  
  /** Returns the index of the machine that was affected. */
  public int sum(int varId) {
    AggregateMachine<T> provider = null;
    int providerIndex = -1;
    for (int i = 0; i < machines.size(); i++) {
      AggregateMachine<T> machine = machines.get(i);
      if (machine.labelsReferenced.contains(varId)) throw new RuntimeException(); // cannot sum a variable that is still referenced
      if (machine.labels.contains(varId)) {
        if (provider != null) throw new RuntimeException(); // cannot have two nodes supplying same variable
        provider = machine;
        providerIndex = i;
      }
    }
    // Since the variable is not referenced, it can just be dropped!
    machines.set(providerIndex, provider.drop(varId));
    return providerIndex;
  }
  
  public void reduce(int machineIndex) {
    // Okay, here's where the lumping code will have to be called
  }
  
  public void sum(Stack<String> name) {
    sum(dict.getId(name));
  }
}
