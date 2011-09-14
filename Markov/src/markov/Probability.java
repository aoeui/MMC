package markov;

public abstract class Probability<T extends Probability<T>> implements
    Comparable<T> {
  public abstract T sum(T p);
  public abstract T product(T p);
  public abstract boolean isZero();
  public abstract boolean isOne();

  public static class ProbabilityException extends RuntimeException {
	private static final long serialVersionUID = -8215920533534041501L;
	public ProbabilityException(String str) {
      super(str);
    }

    public ProbabilityException() { }
  }
  
  @SuppressWarnings("unchecked")
  public static <T extends Probability<T>> Romdd.Op<T> sumInstance() {
    return Sum.INSTANCE;
  }
  
  @SuppressWarnings("unchecked")
  public static <T extends Probability<T>> Romdd.Op<T> productInstance() {
    return Product.INSTANCE;
  }
  
  private static class Sum<T extends Probability<T>> implements Romdd.Op<T> {
    @SuppressWarnings("rawtypes")
    private final static Sum INSTANCE = new Sum();
    private Sum() {}
    public T apply(T v1, T v2) { return v1.sum(v2); }
    public boolean isDominant(T value) { return false; }
  }
  
  private static class Product<T extends Probability<T>> implements Romdd.Op<T> {
    @SuppressWarnings("rawtypes")
    private final static Product INSTANCE = new Product();
    private Product() {}
    public T apply(T v1, T v2) { return v1.product(v2); }
    public boolean isDominant(T value) { return value.isZero(); }
  }
}
