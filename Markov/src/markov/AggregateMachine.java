package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import util.Stack;
import util.UnmodifiableIterator;

public class AggregateMachine<T extends Probability<T>> implements Iterable<AggregateState<T>> { 
  private ArrayList<AggregateState<T>> states;
  public final Stack<Integer> labels;
  public final Stack<Integer> labelsReferenced;
  
  public AggregateMachine(Dictionary dict, T zeroInstance, Machine<T> machine) {
    states = new ArrayList<AggregateState<T>>(machine.size());
    TreeSet<Integer> references = new TreeSet<Integer>();
    for (int i = 0; i < machine.size(); i++) {
      AggregateState<T> newState = new AggregateState<T>(dict, zeroInstance, machine, i);
      states.add(newState);
      references.addAll(newState.transitionFunction.listVarIdx());
    }
    this.labels = toLabelStack(dict, machine.name, machine.get(0).labelNameIterator());
    this.labelsReferenced = Stack.<Integer>makeStack(references);
  }
  
  private Stack<Integer> toLabelStack(Dictionary dict, String machineName, Iterator<String> it) {
    if (!it.hasNext()) return Stack.<Integer>emptyInstance();
    
    int nextVal = dict.getId(Stack.makeName(machineName, it.next()));
    return toLabelStack(dict, machineName, it).push(nextVal);
  }
  
  private AggregateMachine(ArrayList<AggregateState<T>> states, Stack<Integer> labels, Stack<Integer> labelsReferenced) {
    this.states = states;
    this.labels = labels;
    this.labelsReferenced = labelsReferenced;
  }
  
  private AggregateMachine(ArrayList<AggregateState<T>> states) {
    this.states = states;
    TreeSet<Integer> references = new TreeSet<Integer>();
    for (AggregateState<T> state : states) {
      references.addAll(state.transitionFunction.listVarIdx());
    }
    this.labels = states.get(0).getLabelNames();
    this.labelsReferenced = Stack.<Integer>makeStack(references);
  }
  
  public int size() { return states.size(); }
  public AggregateState<T> getState(int i) { return states.get(i); }
  public Iterator<AggregateState<T>> iterator() {
    return new UnmodifiableIterator<AggregateState<T>>(states.iterator());
  }
  
  public AggregateMachine<T> drop(int varId) {
    ArrayList<AggregateState<T>> newStates = new ArrayList<AggregateState<T>>(states.size());
    for (int i = 0; i < states.size(); i++) {
      newStates.add(states.get(i).drop(varId));
    }
    return new AggregateMachine<T>(newStates, labels.remove(varId), labelsReferenced);
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
  
  public TransitionMatrix<SymbolicProbability<T>> computeTransitionMatrix() {
    TransitionMatrix.Builder<SymbolicProbability<T>> builder = new TransitionMatrix.Builder<SymbolicProbability<T>>(states.size());
    for (int src = 0; src < states.size(); src++) {
      Romdd<AggregateTransitionVector<T>> srcVector = states.get(src).transitionFunction;
      ArrayList<SymbolicProbability<T>> row = new ArrayList<SymbolicProbability<T>>();
      for (int dest = 0; dest < states.size(); dest++) {
        row.add(new SymbolicProbability<T>(srcVector, dest));
      }
      builder.addRow(row);
    }
    return builder.build();
  }
}
