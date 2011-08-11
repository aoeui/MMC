package markov;

public abstract class Probability<T extends Probability<T>> implements
    Comparable<T> {
  public abstract T sum(T p);
  public abstract T product(T p);
  public abstract boolean isZero();
  public abstract boolean isOne();

  // public static Probability getNull() { return Null.INSTANCE; }

  // Must implement equals.
  public abstract boolean equals(Object o);

  public static class ProbabilityException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8215920533534041501L;

	public ProbabilityException(String str) {
      super(str);
    }

    public ProbabilityException() { }
  }

  /* public boolean isNull() { return false; }

  public 

  public static class Null {
    public final static INSTANCE = new Null();

    private Null() {}

    public final Probability sum(Probability p) {
    }

    public final Probability product(Probability p) {
    }

    public boolean equals(Object o) {
      return o == INSTANCE;
    }

    public final boolean isNull() { return true; }
  } */
}
