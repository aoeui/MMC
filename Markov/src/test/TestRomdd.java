package test;

import markov.Alphabet;
import markov.Dictionary;
import markov.Domain;
import markov.DecisionTree;
import markov.Predicate;
import markov.Predicate.CollectionType;
import markov.Romdd;

public class TestRomdd {
  public final static DecisionTree<Boolean> TRUE = new DecisionTree.Terminal<Boolean>(true);
  public final static DecisionTree<Boolean> FALSE = new DecisionTree.Terminal<Boolean>(false);

  public final static int N = 3;

  public static void main(String[] args) {
    Dictionary dict = buildDictionary();
    Predicate.CollectionBuilder builder = new Predicate.CollectionBuilder(CollectionType.AND);
    for (int i = 0; i < N; i++) {
      for (int j = i+1; j < N; j++) {
        for (int k = 1; k <= N; k++) {
          Predicate.CollectionBuilder termBuilder = new Predicate.CollectionBuilder(CollectionType.AND);
          termBuilder.add(new Predicate.Atom("x", Integer.toString(i), Integer.toString(k)));
          termBuilder.add(new Predicate.Atom("x", Integer.toString(j), Integer.toString(k)));
          builder.add(new Predicate.Neg(termBuilder.build()));
        }
      }
    }
    Romdd<Boolean> romdd = new DecisionTree.Branch<Boolean>(builder.build(), TRUE, FALSE).toRomdd(dict);
    System.out.println("Reachable values = ");
    printTableOfReachableValues(romdd);
    System.out.println("Checking restrictions");
    Romdd<Boolean> next = romdd;
    for (int i = 0; i < N-1; i++) {
      String restrict = "x." + i;
      next = next.restrict(restrict, Integer.toString(i+1));
      System.out.println("After restricting " + restrict + " to " + (i+1));
      printTableOfReachableValues(romdd);
    }
    
    System.out.println("Checking summations");
    next = romdd;
    for (int i = 0; i < N-1; i++) {
      next = next.sum(Sudoku.OrOp.INSTANCE, "x." + i);
      System.out.println("After summing out " + "x." + i);
      printTableOfReachableValues(next);
    }
  }
  
  public static void printTableOfReachableValues(Romdd<Boolean> romdd) {
    for (String varName : romdd.listVarNames()) {
      System.out.println(varName + " -> " + romdd.findChildrenReaching(varName, true));
    }
  }

  // Everything has the same machine name "x"
  private static Dictionary buildDictionary() {
    Dictionary.Builder builder = new Dictionary.Builder();
    for (int row = 0; row < N; row++) {
      Domain.AltBuilder domBuilder = new Domain.AltBuilder("x");
      for (int col = 0; col < N; col++) {
        Alphabet.AltBuilder alphaBuilder = new Alphabet.AltBuilder("x", Integer.toString(col));
        for (int i = 1; i <= N; i++) {
          alphaBuilder.addCharacter(Integer.toString(i));
        }
        domBuilder.add(alphaBuilder.build());
      }
      builder.add(domBuilder.build());
    }
    return builder.build();
  }
}
