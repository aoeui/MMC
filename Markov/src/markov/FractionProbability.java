package markov;

import num.LongFraction;

public class FractionProbability extends Probability<FractionProbability> {
  public final static FractionProbability ZERO =
      new FractionProbability(LongFraction.ZERO);
  public final static FractionProbability ONE = new FractionProbability(1,1);
  public final static FractionProbability HALF = new FractionProbability(1,2);

  public final LongFraction p;

  public FractionProbability(LongFraction prob) {
    this.p = prob;
  }

  public FractionProbability(long num, long den) {
    this.p = LongFraction.create(num, den);
  }

  public FractionProbability sum(FractionProbability prob) {
    LongFraction newP = p.add(prob.p);
    if (newP.num > newP.den) {
      throw new ProbabilityException(p + " + " + prob + " = " + newP + " > 1");
    }
    return new FractionProbability(newP);
  }

  public FractionProbability product(FractionProbability prob) {
    return new FractionProbability(p.multiply(prob.p));
  }

  public boolean equals(Object o) {
    try {
      FractionProbability prob = (FractionProbability)o;
      return prob.p.equals(p);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isZero() { return p.isZero(); }
  public boolean isOne() { return p.num == p.den; }

  public int compareTo(FractionProbability other) {
    return p.compareTo(other.p);
  }

  public String toString() {
    return p.toString();
  }
}
