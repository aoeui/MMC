package markov;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import util.Multi;
import util.Partition;
import util.Stack;
import util.UnmodifiableIterator;

public class AggregateMachine<T extends Probability<T>> implements Iterable<AggregateState<T>> {
  private List<AggregateState<T>> states;
  public final Stack<Integer> labels;
  public final Stack<Integer> labelsReferenced;
  public final Stack<Integer> labelsDropped;
  
  public final T zero;
  
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
    this.labelsDropped = Stack.<Integer>emptyInstance();
    this.zero = zeroInstance;
  }
  
  public AggregateMachine<T> relabel(int varId, Map<String,String> map) {
    if (!labels.contains(varId)) return this;

    ArrayList<AggregateState<T>> newStates = new ArrayList<AggregateState<T>>();
    for (AggregateState<T> state : states) {
      newStates.add(state.relabel(varId, map));
    }
    return new AggregateMachine<T>(newStates, zero);
  }
  
  /* This function only works if the input machine refers to no other machines */
  /* It is a static function for typing reasons T=DoubleProbability */
  public static ResultTree query(Dictionary dict, AggregateMachine<DoubleProbability> machine) {
    if (!machine.labelsReferenced.isEmpty()) throw new RuntimeException();

    TransitionMatrix<SymbolicProbability<DoubleProbability>> prob = machine.computeTransitionMatrix();
    System.out.println("Computing eigenvectors.");
    Matrix matrix = new Matrix(prob.N, prob.N);
    for (int i = 0; i < prob.N; i++) {
      for (int j = 0; j < prob.N; j++) {
        matrix.set(j, i, prob.get(i,j).value.doubleValue());
      }
    }
    EigenvalueDecomposition eig = matrix.eig();
    Matrix eigenVectors = eig.getV();
    double[] stationary = new double[prob.N];
    double sum = 0;
    for (int i = 0; i < prob.N; i++) {
      stationary[i] = eigenVectors.get(i, 0);
      sum += stationary[i];
    }
    for (int i = 0; i < prob.N; i++) {
      stationary[i] /= sum;
    }
    return machine.applyStationary(dict, stationary);
  }
  
  public ResultTree applyStationary(Dictionary dict, double[] stationary) {
    if (stationary.length != states.size()) throw new RuntimeException();
    
    ResultTree rv = ResultTree.create(dict, labels);
    int i = 0;
    for (AggregateState<T> state : states) {
      rv.increment(state.getValueStack(), stationary[i++]);
    }
    return rv;
  }
  
  public boolean providesFor(AggregateMachine<?> machine) {
    for (int label : machine.labelsReferenced) {
      if (labels.contains(label)) return true;
    }
    return false;
  }
  
  private Stack<Integer> toLabelStack(Dictionary dict, String machineName, Iterator<String> it) {
    if (!it.hasNext()) return Stack.<Integer>emptyInstance();
    
    int nextVal = dict.getId(Stack.makeName(machineName, it.next()));
    return toLabelStack(dict, machineName, it).push(nextVal);
  }
  
  private AggregateMachine(List<AggregateState<T>> states, Stack<Integer> labels, Stack<Integer> labelsReferenced, Stack<Integer> labelsDropped, T zero) {
    this.states = states;
    this.labels = labels;
    this.labelsReferenced = labelsReferenced;
    this.labelsDropped = labelsDropped;
    this.zero = zero;
  }
  
  private AggregateMachine(List<AggregateState<T>> states, T zero) {
    this.states = states;
    TreeSet<Integer> references = new TreeSet<Integer>();
    for (AggregateState<T> state : states) {
      references.addAll(state.transitionFunction.listVarIdx());
    }
    this.labels = states.get(0).getLabelNames();
    this.labelsReferenced = Stack.<Integer>makeStack(references);
    this.labelsDropped = states.get(0).getDroppedLabelNames();
    this.zero = zero;
  }
  
  public int getNumStates() { return states.size(); }
  public AggregateState<T> getState(int i) { return states.get(i); }
  public Iterator<AggregateState<T>> iterator() {
    return new UnmodifiableIterator<AggregateState<T>>(states.iterator());
  }
  
  public AggregateMachine<T> removeUnreachable() {
    boolean converged = false;
    List<AggregateState<T>> stateVect = states;
    do {
      final BitSet reached = new BitSet();
      final List<AggregateState<T>> currentStates = stateVect;
      new Multi() {
        public void init() {
          for (int i = 0; i < currentStates.size(); i++) {
            final int nextIdx = i;
            submit(new Runnable() {
              public void run() {
                BitSet nextReached = currentStates.get(nextIdx).getPossibleNext();
                synchronized (reached) {
                  reached.or(nextReached);
                }                
              }
            });
          }
        }
      }.run();
      final TreeSet<Integer> toRemove = new TreeSet<Integer>();
      for (int i = 0; i < stateVect.size(); i++) {
        if (!reached.get(i)) toRemove.add(i);
      }
      if (toRemove.size() == 0) {
        converged = true;
      } else {
        final ArrayList<AggregateState<T>> newStates = new ArrayList<AggregateState<T>>(stateVect.size()-toRemove.size());
        for (int i = 0; i < stateVect.size()-toRemove.size(); i++) {
          newStates.add(null);
        }
        new Multi() {
          public void init() {
            for (int i = 0; i < currentStates.size(); i++) {
              if (!reached.get(i)) continue;
              final int idx = i;
              submit(new Runnable() {
                public void run() {
                  AggregateState<T> state = currentStates.get(idx).removeStates(toRemove);
                  synchronized (newStates) {
                    newStates.set(state.index, state);
                  }
                }
              });
            }
          }
        }.run();
        stateVect = newStates;
      }
    } while (!converged);
    System.out.println(stateVect.size() + " reachable states");
    return new AggregateMachine<T>(stateVect, zero);
  }

  public AggregateMachine<T> reduce() {
    System.out.println("Reducing machine having " + states.size() + " states");
    Partition<Integer> initialPartition = getStatePartition();
    System.out.println("input partition(" + initialPartition.getNumBlocks() + "): " + initialPartition);
    System.out.println("Computing transition matrix");
    TransitionMatrix<SymbolicProbability<T>> matrix = computeTransitionMatrix();
    System.out.println("Done. Now, running lumping.");
    Lumping<SymbolicProbability<T>> lumper = new Lumping<SymbolicProbability<T>>(matrix, initialPartition);
    lumper.runLumping();

    Partition<Integer> partition = lumper.getPartition();    
    System.out.println("output partition: " + partition);

    ArrayList<AggregateState<T>> newStates = new ArrayList<AggregateState<T>>(partition.getNumBlocks());
    HashMap<Integer,Integer> mapping = partitionToMap(partition);
    for (int i = 0; i < partition.getNumBlocks(); i++) {
      newStates.add(states.get(partition.getBlock(i).get(0)).remap(this,partition.getNumBlocks(), mapping));
    }
    System.out.println("Reduced machine has " + newStates.size() + " states");
    return new AggregateMachine<T>(newStates, zero);
  }
  
  public static HashMap<Integer,Integer> partitionToMap(Partition<Integer> partition) {
    HashMap<Integer,Integer> rv = new HashMap<Integer,Integer>();
    for (int i = 0; i < partition.getNumBlocks(); i++) {
      for (Integer v : partition.getBlock(i)) {
        rv.put(v, i);
      }
    }
    return rv;
  }
  
  public AggregateMachine<T> drop(int varId) {
    ArrayList<AggregateState<T>> newStates = new ArrayList<AggregateState<T>>(states.size());
    for (int i = 0; i < states.size(); i++) {
      newStates.add(states.get(i).drop(varId));
    }
    return new AggregateMachine<T>(newStates, labels.remove(varId), labelsReferenced, labelsDropped.push(varId), zero);
  }
  
  public AggregateMachine<T> product(final AggregateMachine<T> machine) {
    final List<AggregateState<T>> newStates = Collections.synchronizedList(new ArrayList<AggregateState<T>>(getNumStates() * machine.getNumStates()));
    for (int i = 0; i < getNumStates() * machine.getNumStates(); i++) {
      newStates.add(null);
    }
    new Multi() {
      public void init() {
        for (int i = 0; i < getNumStates(); i++) {
          final int idx1 = i;
          for (int j = 0; j < machine.getNumStates(); j++) {
            final int idx2 = j;
            submit(new Runnable() {
              public void run() {                
                newStates.set(idx1*machine.getNumStates() + idx2, states.get(idx1).combine(machine.getState(idx2)));
              }
            });
          }
        }
      }
    }.run();
    return new AggregateMachine<T>(new ArrayList<AggregateState<T>>(newStates), zero);
  }
  
  public Partition<Integer> getStatePartition() {
    Partition.Builder<Integer> builder = new Partition.Builder<Integer>(new Comparator<Integer>() {
      public int compare(Integer v1, Integer v2) {
        return AggregateState.VAL_COMP.compare(states.get(v1), states.get(v2));
      }
    });
    for (int i = 0; i < states.size(); i++) {
      builder.add(i);
    }
    return builder.build();
  }
  
  public TransitionMatrix<SymbolicProbability<T>> computeTransitionMatrix() {
    final SymbolicProbability<T> symZero = new SymbolicProbability<T>(zero);
    final TransitionMatrix.RandomBuilder<SymbolicProbability<T>> builder = new TransitionMatrix.RandomBuilder<SymbolicProbability<T>>(states.size(), symZero);
 
    new Multi() {
      public void init() {
        for (int src = 0; src < states.size(); src++) {
          final int rowNum = src;
          submit(new Runnable() {
            public void run() {
              AggregateState<T> srcState = states.get(rowNum);
              BitSet reached = srcState.getPossibleNext();
              Romdd<AggregateTransitionVector<T>> srcVector = states.get(rowNum).transitionFunction;
              ArrayList<SymbolicProbability<T>> row = new ArrayList<SymbolicProbability<T>>();
              // Check summation here in parallel instead of in builder
              SymbolicProbability<T> sum = symZero;
              for (int dest = 0; dest < states.size(); dest++) {
                if (reached.get(dest)) {
                  SymbolicProbability<T> newProb = new SymbolicProbability<T>(srcVector, dest);
                  sum = sum.sum(newProb);
                  row.add(newProb);
                } else {
                  row.add(symZero);
                }
              }
              if (!sum.isOne()) throw new RuntimeException(sum + " is not one");
              builder.set(rowNum, row);
            }
          });
        }
      }
    }.run();
    return builder.buildNoCheck();
  }
  
  public String toString(Dictionary dict) {
    StringBuilder builder = new StringBuilder();
    for (AggregateState<T> state : states) {
      builder.append(state.toString(dict)).append('\n');
    }
    TransitionMatrix<SymbolicProbability<T>> matrix = computeTransitionMatrix();
    builder.append(matrix.toString());
    return builder.toString();
  }
}
