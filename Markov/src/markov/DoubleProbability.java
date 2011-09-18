package markov;

import java.util.Formatter;

import num.LongFraction;

public class DoubleProbability extends Probability<DoubleProbability> {
  public final static SumOp SUM = new SumOp();
  public final static ProdOp PROD = new ProdOp();

  public final static double MARGIN = 1e-5;

  public final static DoubleProbability ZERO = new DoubleProbability(LongFraction.ZERO);
  public final static DoubleProbability ONE = new DoubleProbability(1,1);
  public final static DoubleProbability HALF = new DoubleProbability(1,2);

  public final double p;  // guaranteed >= 0 

  public DoubleProbability(double d) {
    if (d < 0) throw new RuntimeException("Probability must be >= 0 " + d);
    this.p = d;
  }
  public DoubleProbability(LongFraction prob) {
    this(prob.doubleValue());
  }

  public DoubleProbability(long num, long den) {
    this((double)num/(double)den);
    if (den == 0) throw new RuntimeException("Divide by Zero.");
  }
  
  public DoubleProbability zeroInstance() { return ZERO; }

  public DoubleProbability sum(DoubleProbability prob) {
    return new DoubleProbability(p+prob.p);
  }

  public DoubleProbability product(DoubleProbability prob) {
    return new DoubleProbability(p*prob.p);
  }

  public boolean equals(Object o) {
    try {
      DoubleProbability prob = (DoubleProbability)o;
      if (Math.abs(p) < MARGIN) {
        return Math.abs(prob.p) < MARGIN;
      } else if (Math.abs(prob.p) < MARGIN) {
        return false;
      }
      return Math.abs((p-prob.p)/(p+prob.p)) < MARGIN;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isZero() { return p < MARGIN; }
  public boolean isOne() { return Math.abs(p - 1) < MARGIN; }

  public int compareTo(DoubleProbability other) {
    double diff = p - other.p;
    if (Math.abs(diff) < MARGIN || Math.abs(diff / (p+other.p)) < MARGIN) return 0;
    
    return diff > 0 ? 1 : -1;
  }

  public String toString() {
    Formatter f = new Formatter();
    f.format("%5g", p);
    return f.toString();
  }
  

  private static class SumOp implements Romdd.Op<DoubleProbability> { 
    private SumOp() {}
    public DoubleProbability apply(DoubleProbability v1, DoubleProbability v2) {
      return v1.sum(v2);
    }
    public boolean isDominant(DoubleProbability value) {
      return false;
    }
  }

  private static class ProdOp implements Romdd.Op<DoubleProbability> {
    private ProdOp() {}
    public DoubleProbability apply(DoubleProbability v1, DoubleProbability v2) {
      return v1.product(v2);
    }
    public boolean isDominant(DoubleProbability value) {
      return value.isZero();
    }
  }
}
