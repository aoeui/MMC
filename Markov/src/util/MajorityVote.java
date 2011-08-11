package util;

public class MajorityVote {
  public static <T> T vote(Iterable<T> gen) {
    T candidate = null;
    int count = 0;
    for (T test : gen) {
      if (count == 0) {
        candidate = test;
        count = 1;
      } else if (candidate.equals(test)) {
        count++;
      } else {
        count--;
      }
    }
    return candidate;
  }

  public static <T> boolean isMajority(Iterable<T> gen, T candidate) {
    int count = 0;
    int size = 0;
    for (T test : gen) {
      size++;
      if (test.equals(candidate)) {
        count++;
      }
    }
    return count > (size/2);
  }

  public static void main(String[] args) {
    java.util.List<String> test = java.util.Arrays.<String>asList(
        "A", "A", "A", "C", "C", "B", "B", "C", "C", "C", "B", "C", "C");
    String rv = MajorityVote.<String>vote(test);
    System.out.println("rv = " + rv);
    System.out.println("isMajority = " + MajorityVote.<String>isMajority(test,
        rv));
  }
}
