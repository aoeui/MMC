package markov;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import util.LexicalCompare;
import util.Stack;
import util.UnmodifiableIterator;

public class AggregateState<T extends Probability<T>> {
  public final int index;  // this index exists within the context of this product
  public final int size;  // size of the machine this is from

  public final Romdd<AggregateTransitionVector<T>> transitionFunction;

  private TreeMap<Integer,String> labelVector;

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
  }
  
  public Stack<Integer> getLabelNames() {
    return Stack.<Integer>makeStack(labelVector.keySet());
  }
  
  public Iterator<String> getLabelValueIterator() {
    return new UnmodifiableIterator<String>(labelVector.values().iterator());
  }
  
  public AggregateState<T> drop(int varId) {
    TreeMap<Integer,String> newLabelVector = new TreeMap<Integer,String>(labelVector);
    newLabelVector.remove(varId);
    return new AggregateState<T>(index, size, transitionFunction, newLabelVector);
  }
  
  private AggregateState(int index, int size, Romdd<AggregateTransitionVector<T>> transitionFunction, TreeMap<Integer,String> labelVector) {
    this.index = index;
    this.size = size;
    this.transitionFunction = transitionFunction;
    this.labelVector = labelVector;
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
    return new AggregateState<T>(index*state.size + state.index, size*state.size, result, newLabelVector);
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
