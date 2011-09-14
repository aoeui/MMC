package test;

import markov.Alphabet;
import markov.Dictionary;
import markov.DecisionTree;
import markov.Predicate;
import markov.Predicate.CollectionType;
import markov.Romdd;
import util.Joiner;
import util.Stack;

public class TestRomdd {
  public final static DecisionTree<Boolean> TRUE = new DecisionTree.Terminal<Boolean>(true);
  public final static DecisionTree<Boolean> FALSE = new DecisionTree.Terminal<Boolean>(false);

  public final static int N = 2;

  public static void main(String[] args) {
    Dictionary dict = buildDictionary();
    Predicate.CollectionBuilder builder = new Predicate.CollectionBuilder(CollectionType.AND);
    for (int i = 0; i < N*N; i++) {
      for (int j = i+1; j < N*N; j++) {
        for (int k = 1; k <= N*N; k++) {
          Predicate.CollectionBuilder termBuilder = new Predicate.CollectionBuilder(CollectionType.AND);
          termBuilder.add(new Predicate.Atom(Integer.toString(i / N), Integer.toString(i % N), Integer.toString(k)));
          termBuilder.add(new Predicate.Atom(Integer.toString(j / N), Integer.toString(j % N), Integer.toString(k)));
          builder.add(new Predicate.Neg(termBuilder.build()));
        }
      }
    }
    Predicate pred = builder.build();
    System.out.println(pred);
    DecisionTree<Boolean> tree = new DecisionTree.Branch<Boolean>(pred, TRUE, FALSE);
    Romdd<Boolean> romdd = tree.toRomdd(dict);
    System.out.println("Reachable values = ");
    printTableOfReachableValues(romdd);
    System.out.println("Checking restrictions");
    Romdd<Boolean> next = romdd;
    for (int i = 0; i < N*N; i++) {
      Stack<String> restrict =  Stack.makeName(Integer.toString(i / N), Integer.toString(i % N));
      next = next.restrict(restrict, Integer.toString(i+1));
      System.out.println("After restricting " + Joiner.join(restrict, "::") + " to " + (i+1));
      printTableOfReachableValues(next);
    }
    
    System.out.println("Checking summations");
    next = romdd;
    for (int i = 0; i < N*N; i++) {
      next = next.sum(Romdd.OR, Stack.makeName(Integer.toString(i / N), Integer.toString(i % N)));
      System.out.println("After summing out " + i);
      printTableOfReachableValues(next);
    }
  }
  
  public static void printTableOfReachableValues(Romdd<Boolean> romdd) {
    for (Alphabet alpha : romdd.listVarNames()) {
      System.out.println(Joiner.join(alpha.name, "::") + " -> " + romdd.findChildrenReaching(alpha.name, true));
    }
  }

  // Everything has the same machine name "x"
  private static Dictionary buildDictionary() {
    Dictionary.Builder builder = new Dictionary.Builder();
    for (int row = 0; row < N; row++) {
      for (int col = 0; col < N; col++) {
        Alphabet.AltBuilder alphaBuilder = new Alphabet.AltBuilder(Integer.toString(row), Integer.toString(col));
        for (int i = 1; i <= N*N; i++) {
          alphaBuilder.addCharacter(Integer.toString(i));
        }
        builder.add(alphaBuilder.build());
      }
    }
    return builder.build();
  }
}
