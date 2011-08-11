package test;

import java.util.Arrays;
import java.util.List;

import markov.Predicate;

public class TestPredicate {
  public static void main(String[] args) {
    List<Predicate.Atom> atoms = Arrays.asList(new Predicate.Atom("M1","L1","a"),
        new Predicate.Atom("M1", "L1", "b"));
    for (Predicate p : atoms) {
      System.out.println(p);
    }
  }
}
