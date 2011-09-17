package markov;

import java.util.ArrayList;
import java.util.Iterator;

import util.Partition;
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
  
  public int size() { return machines.size(); }
  public AggregateMachine<T> getMachine(int i) { return machines.get(i); }
  
  public Iterator<AggregateMachine<T>> iterator() {
    return new UnmodifiableIterator<AggregateMachine<T>>(machines.iterator());
  }
  
  public void multiply(int idx1, int idx2) {
    machines.set(idx1, machines.get(idx1).product(machines.get(idx2)));
    machines.remove(idx2);
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
    AggregateMachine<T> machine = machines.get(machineIndex);
    Partition<Integer> initialPartition = machine.getStatePartition();
    System.out.println("input partition: " + initialPartition);
    Lumping<SymbolicProbability<T>> lumper = new Lumping<SymbolicProbability<T>>(machine.computeTransitionMatrix(), initialPartition);
    lumper.runLumping();
    // TODO process results of lumping
    Partition<Integer> partition = lumper.getPartition();
    System.out.println("output partition: " + partition);
  }
  
  public void sum(Stack<String> name) {
    sum(dict.getId(name));
  }
  public void sum(String ...strings) {
    sum(Stack.makeName(strings));
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
