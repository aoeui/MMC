package markov;

import java.math.BigDecimal;
import java.math.MathContext;

import num.LongFraction;

public class DoubleProbability extends Probability<DoubleProbability> {
  public final static SumOp SUM = new SumOp();
  public final static ProdOp PROD = new ProdOp();

  // public final static BigDecimal MARGIN = new BigDecimal(1e-12);
  // public final static BigDecimal ONE_MARGIN = new BigDecimal(1e-11);
  
  public final static DoubleProbability ZERO = new DoubleProbability(LongFraction.ZERO);
  public final static DoubleProbability ONE = new DoubleProbability(1,1);
  public final static DoubleProbability HALF = new DoubleProbability(1,2);
  public final static DoubleProbability THIRD = new DoubleProbability(1,3);

  public final static BigDecimal MARGIN = THIRD.value.ulp().multiply(BigDecimal.valueOf(100));

  private final BigDecimal value;
  
  // private final double p;  // guaranteed >= 0 

  public DoubleProbability(double d) {
    if (d < 0) throw new RuntimeException("Probability must be >= 0 " + d);
    this.value = new BigDecimal(d);
  }
  public DoubleProbability(LongFraction prob) {
    if ((prob.num >= 0 ^ prob.den > 0)) throw new RuntimeException("Probability must be >= 0 " + prob.num + "/" + prob.den);
    
    BigDecimal num = new BigDecimal(prob.num, MathContext.DECIMAL128);
    BigDecimal den = new BigDecimal(prob.den, MathContext.DECIMAL128);
    this.value = num.divide(den, MathContext.DECIMAL128);
  }
  
  private DoubleProbability(BigDecimal bd) {
    if (bd.compareTo(BigDecimal.ZERO) < 0) throw new RuntimeException();
    this.value = bd;
  }
  
  public double doubleValue() { return value.doubleValue(); }

  public DoubleProbability(long num, long den) {
    this(LongFraction.create(num, den));
  }
  
  public DoubleProbability zeroInstance() { return ZERO; }
  
  public DoubleProbability subtract(DoubleProbability prob) {
    return new DoubleProbability(value.subtract(prob.value));
  }

  public DoubleProbability sum(DoubleProbability prob) {
    return new DoubleProbability(value.add(prob.value, MathContext.DECIMAL128));
  }

  public DoubleProbability product(DoubleProbability prob) {
    return new DoubleProbability(value.multiply(prob.value, MathContext.DECIMAL128));
  }

  public boolean equals(Object o) {
    try {
      BigDecimal value2 = ((DoubleProbability)o).value;
      return value.subtract(value2, MathContext.DECIMAL128).abs().compareTo(MARGIN) < 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isZero() {
    return value.compareTo(MARGIN) < 0;
  }
  public boolean isOne() { return ONE.value.subtract(value, MathContext.DECIMAL128).abs().compareTo(MARGIN) < 0; }

  public int compareTo(DoubleProbability other) {
    BigDecimal diff = value.subtract(other.value, MathContext.DECIMAL128);
    if (diff.abs().compareTo(MARGIN) < 0) return 0;
    return diff.compareTo(BigDecimal.ZERO);
  }

  public String toString() {
    return value.toString();
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
