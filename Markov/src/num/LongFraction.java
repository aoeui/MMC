package num;

public class LongFraction implements Comparable<LongFraction> {
  public final static LongFraction DIVIDE_BY_ZERO = DivideByZero.INSTANCE;
  public final static LongFraction ZERO = Zero.INSTANCE;

  public final long num;
  public final long den;

  private LongFraction(long num, long den) {
    this.num = num;
    this.den = den;
  }
  
  public double doubleValue() {
    if (den == 0) throw new RuntimeException();
    return (double)num/(double)den;
  }

  public static LongFraction create(long num, long den) {
    if (den == 0) { return DIVIDE_BY_ZERO; }
    if (num == 0) { return ZERO; }

    boolean sn = num >= 0;
    boolean sd = den >= 0;
    if (!sn) { num = -num; }
    if (!sd) { den = -den; }

    long gcd = gcd(num, den);
    return new LongFraction(((sn==sd) ? num : -num)/gcd, den/gcd);
  }

  public LongFraction add(LongFraction f) {
    if (f.isZero()) { return this; }
    if (f.isDivideByZero()) { return f; }

    return doAdd(f.num, f.den);
  }

  /** Assumes that b_num != 0 and b_den > 0 **/
  private LongFraction doAdd(long b_num, long b_den) {
    assert (b_num != 0 && b_den > 0);
    long newNum = num*b_den + b_num*den;
    long newDen = den*b_den;
    long gcd = gcd(newNum, newDen);
    return new LongFraction(newNum/gcd, newDen/gcd);
  }

  public LongFraction subtract(LongFraction f) {
    if (f.isZero()) { return this; }
    if (f.isDivideByZero()) { return f; }

    return doAdd(-f.num, f.den);
  }

  public LongFraction multiply(LongFraction f) {
    if (f.isZero() || f.isDivideByZero()) return f;

    return doTimes(f.num, f.den);
  }
  private LongFraction doTimes(long b_num, long b_den) {
    long gcd_a = gcd(Math.abs(num), b_den);
    long gcd_b = gcd(Math.abs(b_num), den);

    return new LongFraction((num/gcd_a)*(b_num/gcd_b),
        (den/gcd_b)*(b_den/gcd_a));
  }
  public LongFraction divide(LongFraction f) {
    if (f.isZero()) return DIVIDE_BY_ZERO;
    if (f.isDivideByZero()) throw new DivideByZeroException();

    return doTimes(f.num > 0 ? f.den : -f.den, Math.abs(f.num));
  }

  public LongFraction inverse() { return new LongFraction(den, num); }

  public LongFraction squared() { return new LongFraction(num*num, den*den); }
  public boolean equals(Object o) {
    try {
      LongFraction f = (LongFraction)o;
      return num == f.num && den == f.den;
    } catch (Exception e) {
      return false;
    }
  }

  public int compareTo(LongFraction f) {
    if (f.isDivideByZero()) throw new DivideByZeroException();
    if (f.isZero()) return num > 0 ? 1 : -1;
    long diff = num * f.den - f.num * den;
    if (diff == 0l) return 0;
    return (diff > 0) ? 1 : -1;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(num).append('/').append(den);
    return buf.toString();
  }

  public boolean isDivideByZero() { return false; }
  public boolean isZero() { return false; }

  public static void main(String[] args) {
    System.out.println("gcd(0,1) = " + gcd(0,1));
    System.out.println("gcd(0,2) = " + gcd(0,2));
    System.out.println("gcd(0,0) = " + gcd(0,0));
    System.out.println("gcd(2,0) = " + gcd(2,0));
    System.out.println("gcd(1,0) = " + gcd(1,0));
    LongFraction half = create(1,2);
    System.out.println("1/2 + 1/2 = " + half.add(half));
  }

  public static class DivideByZero extends LongFraction {
    public final static DivideByZero INSTANCE = new DivideByZero();

    private DivideByZero() { super(1, 0); }
    public LongFraction add(LongFraction f) { return this; }
    public LongFraction subtract(LongFraction f) { return this; }
    public LongFraction multiply(LongFraction f) { return this; }
    public LongFraction divide(LongFraction f) { return this; }
    public LongFraction inverse(LongFraction f) {
      throw new DivideByZeroException();
    }
    public LongFraction squared() { return this; }

    public final boolean isDivideByZero() { return true; }

    public int compareTo(LongFraction f) { throw new DivideByZeroException(); }
  }

  public static class DivideByZeroException extends RuntimeException {
	private static final long serialVersionUID = -3504946716942656554L;

	public DivideByZeroException() { }
  }

  public static class Zero extends LongFraction {
    public final static Zero INSTANCE = new Zero();

    private Zero() { super(0,1); }

    public LongFraction add(LongFraction f) { return f; }
    public LongFraction subtract(LongFraction f) {
      return new LongFraction(-f.num, f.den);
    }
    public LongFraction multiply(LongFraction f) { return this; }
    public LongFraction divide(LongFraction f) {
      return f.isZero() ? DIVIDE_BY_ZERO : ZERO;
    }
    public LongFraction inverse() { return DIVIDE_BY_ZERO; }
    public LongFraction squared() { return this; }

    public String toString() { return "0"; }

    public final boolean isZero() { return true; }

    public int compareTo(LongFraction f) {
      if (f.isZero()) return 0;
      return f.num > 0 ? -1 : 1;
    }
  }

  public static long gcd(long a, long b) {
    if (a == b) return a;
    long bigger;
    long smaller;
    if (a > b) {
      bigger = a;
      smaller = b;
    } else {
      bigger = b;
      smaller = a;
    }
    if (smaller == 0) { return bigger; }
    long remainder = bigger % smaller;
    if (remainder == 0) {
      return smaller;
    } else {
      return gcd(smaller, remainder);
    }
  }
  public static long lcm(long a, long b) {
    if (a == 0 || b == 0) return 0;
    return (a*b)/gcd(a,b);
  }
}
