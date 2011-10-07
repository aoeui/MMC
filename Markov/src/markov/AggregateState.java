package markov;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import util.LexicalCompare;
import util.Stack;
import util.UnmodifiableIterator;

public class AggregateState<T extends Probability<T>> {
  public final int index;  // this index exists within the context of this product
  public final int size;  // size of the machine this is from

  public final Romdd<AggregateTransitionVector<T>> transitionFunction;

  private final TreeMap<Integer,String> labelVector;
  private final TreeMap<Integer,Stack<String>> droppedLabels;

  public AggregateState(Dictionary dict, T zeroInstance, Machine<T> machine, int idx) {
    this.index = idx;
    this.size = machine.size();
    State<T> state = machine.get(idx);
    this.transitionFunction = state.getTransitionFunction().toRomdd(dict).remap(new VectorTransformer<T>(machine, zeroInstance));
    labelVector = new TreeMap<Integer,String>();
    for (Iterator<Map.Entry<String, String>> entryIt = state.labelIterator(); entryIt.hasNext(); ) {
      Map.Entry<String, String> entry = entryIt.next();
      labelVector.put(dict.getId(Stack.makeName(machine.name, entry.getKey())), entry.getValue());
    }
    droppedLabels = new TreeMap<Integer,Stack<String>>();
  }
  
  public AggregateState<T> removeStates(final SortedSet<Integer> toRemove) {
    Romdd<AggregateTransitionVector<T>> transition = transitionFunction.remap(new Romdd.Mapping<AggregateTransitionVector<T>,AggregateTransitionVector<T>>() {
      public AggregateTransitionVector<T> transform(AggregateTransitionVector<T> input) {
        return input.removeStates(toRemove);
      }
    });
    int count = 0;
    for (int removed : toRemove) {
      if (removed < index) count++;
      else break;
    }
    return new AggregateState<T>(index-count, size-toRemove.size(), transition, labelVector, droppedLabels);
  }

  public TreeSet<AggregateTransitionVector<T>> getPossibleTransitions() {
    return transitionFunction.getOutputs();
  }
  
  // Range is the size of the partition used to generate the map
  public AggregateState<T> remap(final int range, final Map<Integer,Integer> map) {
    class VectorMapper implements Romdd.Mapping<AggregateTransitionVector<T>, AggregateTransitionVector<T>> {
      public AggregateTransitionVector<T> transform(AggregateTransitionVector<T> input) {
        return input.remap(range, map);
      }
    }
    return new AggregateState<T>(map.get(index), range, transitionFunction.remap(new VectorMapper()), labelVector, droppedLabels);
  }
  
  public AggregateState<T> relabel(int var, final Map<String,String> map) {
    String val = labelVector.get(var);
    if (val == null) return this;
    String newVal = map.get(val);
    if (newVal == null || newVal.equals(val)) return this;
    
    TreeMap<Integer,String> newLabels = new TreeMap<Integer,String>(labelVector);
    newLabels.put(var, newVal);
    return new AggregateState<T>(index, size, transitionFunction, newLabels, droppedLabels);
  }
  
  public Romdd<AggregateTransitionVector<T>> applyRestrictions(Romdd<AggregateTransitionVector<T>> input) {
    Romdd<AggregateTransitionVector<T>> rv = input;
    for (Map.Entry<Integer,String> entry : labelVector.entrySet()) {
      rv = rv.restrict(entry.getKey(), entry.getValue());
    }
    return rv;
  }
  
  public Stack<Integer> getLabelNames() {
    return Stack.<Integer>makeStack(labelVector.keySet());
  }
  
  public Stack<Integer> getDroppedLabelNames() {
    return Stack.<Integer>makeStack(droppedLabels.keySet());
  }
  
  public Stack<String> getValueStack() {
    return Stack.<String>makeStack(getLabelValueIterator());
  }
  
  public Iterator<String> getLabelValueIterator() {
    return new UnmodifiableIterator<String>(labelVector.values().iterator());
  }
  
  public String getValue(int varId) {
    return labelVector.get(varId);
  }
  
  public AggregateState<T> drop(int varId) {
    TreeMap<Integer,String> newLabelVector = new TreeMap<Integer,String>(labelVector);
    String str = newLabelVector.remove(varId);
    if (str == null) return this;
    TreeMap<Integer,Stack<String>> newDroppedVector = new TreeMap<Integer,Stack<String>>(droppedLabels);
    Stack<String> currentVal = newDroppedVector.get(varId);
    if (currentVal == null) currentVal = Stack.<String>emptyInstance();
    newDroppedVector.put(varId, currentVal.push(str));
    
    return new AggregateState<T>(index, size, transitionFunction, newLabelVector, newDroppedVector);
  }
  
  private AggregateState(int index, int size, Romdd<AggregateTransitionVector<T>> transitionFunction, TreeMap<Integer,String> labelVector, TreeMap<Integer,Stack<String>> dropped) {
    this.index = index;
    this.size = size;
    this.transitionFunction = transitionFunction;
    this.labelVector = labelVector;
    this.droppedLabels = dropped;
  }
  
  // This combines two states from different aggregate machines.
  // It assumes that this object is the left argument in the product.
  public AggregateState<T> combine(AggregateState<T> state) {
    Romdd<AggregateTransitionVector<T>> left = transitionFunction;
    for (Map.Entry<Integer, String> entry : state.labelVector.entrySet()) {
      left = left.restrict(entry.getKey(), entry.getValue());
    }
    Romdd<AggregateTransitionVector<T>> right = state.transitionFunction;
    for (Map.Entry<Integer, String> entry : labelVector.entrySet()) {
      right = right.restrict(entry.getKey(), entry.getValue());
    }
    Romdd<AggregateTransitionVector<T>> result = Romdd.<AggregateTransitionVector<T>>apply(new MultiplyVectors<T>(), left, right);
    TreeMap<Integer,String> newLabelVector = new TreeMap<Integer,String>(labelVector);
    newLabelVector.putAll(state.labelVector);
    TreeMap<Integer,Stack<String>> newDropped = new TreeMap<Integer,Stack<String>>(droppedLabels);
    for (Map.Entry<Integer, Stack<String>> entry : state.droppedLabels.entrySet()) {
      Stack<String> current = newDropped.get(entry.getKey());
      if (current == null) {
        newDropped.put(entry.getKey(), entry.getValue());
      } else {
        newDropped.put(entry.getKey(), current.concat(entry.getValue()));
      }
    }
    newDropped.putAll(state.droppedLabels);
    return new AggregateState<T>(index*state.size + state.index, size*state.size, result, newLabelVector, newDropped);
  }
  
  // Does not output information about the transition vector, which is more complex
  public String toString(Dictionary dict) {
    StringBuilder builder = new StringBuilder();
    builder.append("S[").append(index).append("]: ");
    boolean isFirst = true;
    for (Map.Entry<Integer, String> entry : labelVector.entrySet()) {
      if (isFirst) isFirst = false;
      else builder.append(", ");
      builder.append(dict.getAlpha(entry.getKey()).name.toString(Alphabet.SCOPE)).append("=").append(entry.getValue());
    }
    builder.append('\n');
    for (int i = 0; i < Integer.toString(index).length()+2; i++) {
      builder.append(' ');
    }
    builder.append(" - ");
    isFirst = true;
    for (Map.Entry<Integer, Stack<String>> entry : droppedLabels.entrySet()) {
      if (isFirst) isFirst = false;
      else builder.append(", ");
      builder.append(dict.getAlpha(entry.getKey()).name.toString(Alphabet.SCOPE)).append("=").append(entry.getValue().toString("|"));
    }
    return builder.toString();
  }

  // Need to be careful with this because it is not commutative
  private static class MultiplyVectors<T extends Probability<T>> implements Romdd.Op<AggregateTransitionVector<T>> {
    private MultiplyVectors() {}
    public AggregateTransitionVector<T> apply(AggregateTransitionVector<T> v1, AggregateTransitionVector<T> v2) {
      return v1.multiply(v2);
    }
    public boolean isDominant(AggregateTransitionVector<T> value) { return false; }
  }
  
  private static class VectorTransformer<P extends Probability<P>> implements Romdd.Mapping<TransitionVector<P>, AggregateTransitionVector<P>> {
    Machine<P> machine;
    P zeroInstance;
    public VectorTransformer(Machine<P> m, P zeroInstance) {
      this.machine = m;
      this.zeroInstance = zeroInstance;
    }
    public AggregateTransitionVector<P> transform(TransitionVector<P> input) {
      return new AggregateTransitionVector<P>(machine, zeroInstance, input);
    }
  }
  
  public final static Comparator<AggregateState<?>> VAL_COMP = new StateComparator();
  
  private static class StateComparator implements Comparator<AggregateState<?>> {
    // Assume this is called on states from the same machine.
    public int compare(AggregateState<?> o1, AggregateState<?> o2) {
      return LexicalCompare.compare(o1.getLabelValueIterator(), o2.getLabelValueIterator());
    }
  }
}
