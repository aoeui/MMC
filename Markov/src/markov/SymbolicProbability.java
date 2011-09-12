package markov;

import util.Ptr;

// Assume that probabilities are all Double
public class SymbolicProbability extends Probability<SymbolicProbability> { 
  public final Romdd<DoubleProbability> prob;
  private final DoubleProbability value;  // this is null if prob is not a constant 

  public SymbolicProbability(Romdd<TransitionVector<DoubleProbability>> stateTransition, String stateName) {
    this(stateTransition.remap(new TransitionFilter(stateName)));
  }
  
  public SymbolicProbability(Romdd<DoubleProbability> vect) {
    this.prob = vect;
    final Ptr<DoubleProbability> vPtr = new Ptr<DoubleProbability>();
    prob.accept(new Romdd.Visitor<DoubleProbability>() {
      public void visitTerminal(Romdd.Terminal<DoubleProbability> term) {
        vPtr.value = term.output;
      }
      public void visitNode(Romdd.Node<DoubleProbability> node) { }
    });
    value = vPtr.value;
  }

  public int compareTo(SymbolicProbability o) {
    return prob.compareTo(o.prob);
  }

  public SymbolicProbability sum(SymbolicProbability p) {
    return new SymbolicProbability(Romdd.<DoubleProbability>apply(DoubleProbability.SUM, prob, p.prob));
  }

  public SymbolicProbability product(SymbolicProbability p) {
    return new SymbolicProbability(Romdd.<DoubleProbability>apply(DoubleProbability.PROD, prob, p.prob));
  }

  public boolean isZero() {
    return value != null && value.isZero();
  }

  public boolean isOne() {
    return value != null && value.isOne();
  }

  public boolean equals(Object o) {
    try {
      return compareTo((SymbolicProbability)o) == 0;
    } catch (Exception e) {
      return false;
    }
  }
  
  public static class TransitionFilter implements Romdd.Mapping<TransitionVector<DoubleProbability>, DoubleProbability> {
    public final String name;
    public TransitionFilter(String name) {
      this.name = name;
    }
    public DoubleProbability transform(TransitionVector<DoubleProbability> input) {
      DoubleProbability rv = input.getProbability(name);
      return rv == null ? DoubleProbability.ZERO : rv;
    }
  }
}
