package markov;

public interface Monoid<T extends Monoid<T>> {
  public T times(T monoid);
}
