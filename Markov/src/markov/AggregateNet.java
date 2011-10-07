package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import util.Stack;
import util.UnmodifiableIterator;

public class AggregateNet<T extends Probability<T>> implements Iterable<AggregateMachine<T>> {
  public final Dictionary dict;
  ArrayList<AggregateMachine<T>> machines;
  
  // The problem is that transition vectors in Machine have implied zero. Because of the
  // parameterization of Probability, it is very hard to get static information out.
  // Zero has to be passed explicitly here.
  public AggregateNet(Net<T> net, T zeroInstance) {
    this.dict = net.dictionary;
    
    this.machines = new ArrayList<AggregateMachine<T>>(net.N);
    for (Machine<T> machine : net) {
      machines.add(new AggregateMachine<T>(dict, zeroInstance, machine));
    }
  }
  
  /* Used to merge values of a particular variable we no longer wish to distinguish.
   * Does not operate on transition vectors as that would involve changing the Romdd.
   * Call only when the label is no longer referenced */
  public AggregateNet<T> relabel(int varId, Map<String,String> map) {
    for (int i = 0; i < machines.size(); i++) {
      AggregateMachine<T> machine = machines.get(i);
      if (machine.labelsReferenced.contains(varId)) throw new RuntimeException(); // cannot sum a variable that is still referenced
    }
    ArrayList<AggregateMachine<T>> newMachines = new ArrayList<AggregateMachine<T>>();
    for (AggregateMachine<T> machine : machines) {
      newMachines.add(machine.relabel(varId, map));
    }
    return new AggregateNet<T>(dict, newMachines);
  }
  
  private AggregateNet(Dictionary dict, ArrayList<AggregateMachine<T>> machines) {
    this.dict = dict;
    this.machines = machines;
  }
  
  public int size() { return machines.size(); }
  public AggregateMachine<T> getMachine(int i) { return machines.get(i); }
  
  public Iterator<AggregateMachine<T>> iterator() {
    return new UnmodifiableIterator<AggregateMachine<T>>(machines.iterator());
  }
  
  public AggregateNet<T> multiply(int idx1, int idx2) {
    ArrayList<AggregateMachine<T>> newMachines = new ArrayList<AggregateMachine<T>>(machines);
    AggregateMachine<T> m1 = machines.get(idx1);
    AggregateMachine<T> m2 = machines.get(idx2);
    System.out.println("Taking the product of two machines sizes " + m1.getNumStates() + " and " + m2.getNumStates() + " = " + m1.getNumStates()*m2.getNumStates());
    newMachines.set(idx1, machines.get(idx1).product(machines.get(idx2)));
    newMachines.remove(idx2);
    return new AggregateNet<T>(dict, newMachines);
  }
  
  public int machineProviding(int varId) {
    for (int i = 0; i < machines.size(); i++) {
      if (machines.get(i).labels.contains(varId)) return i;
    }
    throw new RuntimeException();
  }
  
  // Low level sum operator, 'sums' out variables that are not referenced.
  // Does not compute products to make the sum feasible, though that *could* be done
  public AggregateNet<T> sum(int varId) {
    int providerIndex = -1;
    for (int i = 0; i < machines.size(); i++) {
      AggregateMachine<T> machine = machines.get(i);
      if (machine.labelsReferenced.contains(varId)) throw new RuntimeException(); // cannot sum a variable that is still referenced
      if (machine.labels.contains(varId)) {
        if (providerIndex != -1) throw new RuntimeException(); // cannot have two nodes supplying same variable
        providerIndex = i;
      }
    }
    if (providerIndex == -1) return this;
    ArrayList<AggregateMachine<T>> newMachines = new ArrayList<AggregateMachine<T>>(machines);
    // Since the variable is not referenced, it can just be dropped!
    newMachines.set(providerIndex, machines.get(providerIndex).drop(varId));
    return new AggregateNet<T>(dict, newMachines);
  }
  
  public AggregateNet<T> reduce(int machineIndex) {
    ArrayList<AggregateMachine<T>> newMachines = new ArrayList<AggregateMachine<T>>(machines);
    newMachines.set(machineIndex, machines.get(machineIndex).removeUnreachable().reduce());
    return new AggregateNet<T>(dict, newMachines);
  }
  
  public AggregateNet<T> sum(Stack<String> name) {
    return sum(dict.getId(name));
  }
  public AggregateNet<T> sum(String ...strings) {
    return sum(Stack.makeName(strings));
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < machines.size(); i++) {
      if (i != 0) builder.append("\n\n");
      builder.append(machines.get(i).toString(dict));
    }
    return builder.toString();
  }
}
