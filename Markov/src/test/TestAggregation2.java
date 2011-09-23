package test;

import java.util.ArrayList;

import markov.AggregateMachine;
import markov.AggregateNet;
import markov.DecisionTree;
import markov.DoubleProbability;
import markov.Machine;
import markov.MonteCarlo;
import markov.Net;
import markov.Predicate;
import markov.ResultTree;
import markov.State;
import markov.TransitionVector;

import util.Stack;

public class TestAggregation2 {
  public final static int LEVELS=6;  // levels are 0...5
  public final static int PLAYERS=5;  // players are p1...pk  w/ k = PLAYERS
  public final static String COMMONS = "commons";
  public final static int STEPS=10000000;

  public static DecisionTree<TransitionVector<DoubleProbability>> constructTree(int startLevel) {
    /* The structure of the decision tree is going to be like:
    if p1::using=1
      if p2::using=1
        if p3::using=1 level=level-3 else level=level-2
      else
        if p3::using=1 level=level-2 else level=level-1
    else
      if p2::using=1
        if p3::using=1 level=level-2 else level=level-1
      else
        if p3::using=1 level=level-1 else level=level
    */
    return recurse(startLevel+1, 0, 1);
  }
  
  public static DecisionTree<TransitionVector<DoubleProbability>> recurse(int startLevel, int usingCount, int nextPlayer) {
    int currentLevel = startLevel-usingCount;
    if (currentLevel <= 0) {
      return makeSingleTerminal(0);
    }
    if (nextPlayer >= PLAYERS) {
      return makeBranch(nextPlayer, makeSingleTerminal(currentLevel-1), makeSingleTerminal(currentLevel));
    }
    return makeBranch(nextPlayer, recurse(startLevel, usingCount+1, nextPlayer+1), recurse(startLevel, usingCount, nextPlayer+1));
  }
  
  public static DecisionTree.Branch<TransitionVector<DoubleProbability>> makeBranch(int player, DecisionTree<TransitionVector<DoubleProbability>> cons, DecisionTree<TransitionVector<DoubleProbability>> alt) {
    Predicate pred = new Predicate.Atom("p"+player, "using", "1");
    return new DecisionTree.Branch<TransitionVector<DoubleProbability>>(pred, cons, alt);
  }
  
  public static DecisionTree.Terminal<TransitionVector<DoubleProbability>> makeSingleTerminal(int nextState) {
    TransitionVector.Builder<DoubleProbability> builder = new TransitionVector.Builder<DoubleProbability>(COMMONS);
    builder.setProbability(Integer.toString(Math.min(LEVELS-1, nextState)), DoubleProbability.ONE);
    return new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(builder.build());
  }
  
  public static AggregateNet<DoubleProbability> buildNet() throws Exception {
    Net.Builder<DoubleProbability> netBuilder = Net.partialParse("Commons");
    // create commons machine
    Machine.Builder<DoubleProbability> commons = new Machine.Builder<DoubleProbability>(COMMONS);
    // Commons machine will has a six states named "0" ... "5"
    for (int i = 0; i <= 5; i++) {
      State.Builder<DoubleProbability> builder = new State.Builder<DoubleProbability>(COMMONS, Integer.toString(i), constructTree(i));
      builder.setLabel("level", Integer.toString(i));
      commons.addState(builder.build());
    }
    netBuilder.addMachine(commons.build());
    Net<DoubleProbability> net = netBuilder.build();
    // System.out.println("Parsed net: \n" + net);
    return new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
  }

  public static void main(String[] args) {
    try {
      long begin = System.nanoTime();

      AggregateNet<DoubleProbability> aNet = buildNet();
      System.out.println(aNet);
    
      for (int i = 5; i >= 1; i--) {
        aNet = aNet.multiply(i-1, i);
        System.out.println("\nAfter multiplying p" + i + "\n" + aNet);
        aNet = aNet.sum("p" + i, "using").reduce(i-1);
        System.out.println("\nAfter reducing\n" + aNet);
      }

      /* aNet = aNet.multiply(4, 5).multiply(3, 4).multiply(2, 3).multiply(1, 2).multiply(0, 1);
      aNet = aNet.sum("p5", "using").sum("p4", "using").sum("p3", "using").sum("p2", "using").sum("p1", "using");
      aNet = aNet.reduce(0); */

//       System.out.println(aNet);
      AggregateMachine<DoubleProbability> machine = aNet.getMachine(0);
      System.out.println(AggregateMachine.query(aNet.dict, machine));

      /* double[] prod = new double[prob.N];
      for (int i = 0; i < prob.N; i++) {
        double rowSum = 0;
        for (int j = 0; j < prob.N; j++) {
          rowSum += stationary[j] * matrix.get(i, j);
        }
        prod[i] = rowSum;
        if (i != 0) System.out.print(", ");
        System.out.print(prod[i]);
      } */

      System.out.println("\nTrying Monte Carlo: ");
      runMonteCarlo();
            
      double elapsed = ((double)(System.nanoTime()-begin))/1e9;
      System.out.println("\nTime taken = " + elapsed);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
  
  public static void runMonteCarlo() throws Exception {
    AggregateNet<DoubleProbability> net = buildNet();
    MonteCarlo mc = new MonteCarlo(net);
    ArrayList<Stack<String>> names = new ArrayList<Stack<String>>();
    names.add(Stack.makeName(COMMONS, "level"));
    ResultTree rv = mc.runQuery(STEPS, names);
    System.out.println(rv.toCountString(STEPS));
  }
}
