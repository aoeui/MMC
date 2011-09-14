package markov;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import util.Stack;

public class AggregateState<T extends Probability<T>> {
  public final int index;  // this index exists within the context of this product
  public final int size;  // size of the machine this is from

  public final Romdd<AggregateTransitionVector<T>> transitionFunction;

  TreeMap<Integer,String> labelVector;

  public AggregateState(Dictionary dict, Machine<T> machine, int idx) {
    this.index = idx;
    this.size = machine.size();
    State<T> state = machine.get(idx);
    this.transitionFunction = state.getTransitionFunction().toRomdd(dict).remap(new VectorTransformer<T>(machine));
    labelVector = new TreeMap<Integer,String>();
    for (Iterator<Map.Entry<String, String>> entryIt = state.labelIterator(); entryIt.hasNext(); ) {
      Map.Entry<String, String> entry = entryIt.next();
      labelVector.put(dict.getId(Stack.makeName(machine.name, entry.getKey())), entry.getValue());
    }
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
    public VectorTransformer(Machine<P> m) {
      this.machine = m;
    }
    public AggregateTransitionVector<P> transform(TransitionVector<P> input) {
      return new AggregateTransitionVector<P>(machine, input);
    }
  }
}