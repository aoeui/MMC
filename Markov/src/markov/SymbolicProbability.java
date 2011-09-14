package markov;

import util.Ptr;

// Assume that probabilities are all Double
public class SymbolicProbability<T extends Probability<T>> extends Probability<SymbolicProbability<T>> { 
  public final static SymbolicProbability<DoubleProbability> ZERO = new SymbolicProbability<DoubleProbability>(new Romdd.Terminal<DoubleProbability>(DoubleProbability.ZERO));
  public final static SymbolicProbability<DoubleProbability> ONE = new SymbolicProbability<DoubleProbability>(new Romdd.Terminal<DoubleProbability>(DoubleProbability.ONE));

  public final Romdd<T> prob;
  private final T value;  // this is null if prob is not a constant 

  /** Given the transition function of a particular state and an index, this constructor returns a mapping from
   * the input label space to probabilities.  */
  public SymbolicProbability(Romdd<AggregateTransitionVector<T>> stateTransition, int stateIdx) {
    this(stateTransition.remap(new TransitionFilter<T>(stateIdx)));
  }
  
  public SymbolicProbability(Romdd<T> vect) {
    this.prob = vect;
    final Ptr<T> vPtr = new Ptr<T>();
    prob.accept(new Romdd.Visitor<T>() {
      public void visitTerminal(Romdd.Terminal<T> term) {
        vPtr.value = term.output;
      }
      public void visitNode(Romdd.Node<T> node) { }
    });
    value = vPtr.value;
  }

  public int compareTo(SymbolicProbability<T> o) {
    return prob.compareTo(o.prob);
  }

  public SymbolicProbability<T> sum(SymbolicProbability<T> p) {
    return new SymbolicProbability<T>(Romdd.<T>apply(Probability.<T>sumInstance(), prob, p.prob));
  }

  public SymbolicProbability<T> product(SymbolicProbability<T> p) {
    return new SymbolicProbability<T>(Romdd.<T>apply(Probability.<T>productInstance(), prob, p.prob));
  }

  public boolean isZero() {
    return value != null && value.isZero();
  }

  public boolean isOne() {
    return value != null && value.isOne();
  }
  
  public static class TransitionFilter<T extends Probability<T>> implements Romdd.Mapping<AggregateTransitionVector<T>, T> {
    public final int stateIdx;
    public TransitionFilter(int stateIdx) {
      this.stateIdx = stateIdx;
    }
    public T transform(AggregateTransitionVector<T> input) {
      return input.get(stateIdx);  // this is never null for AggregateTransitionVector
    }
  }
}
