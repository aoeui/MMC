package test;

import markov.AggregateMachine;
import markov.AggregateNet;
import markov.DoubleProbability;
import markov.Net;

public class Biofilm {
/*  public static Machine<DoubleProbability> buildLastFilm(int i) {
    Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>("Film" + i);
    return builder.build();
  }
  
  public static Machine<DoubleProbability> buildFilm(int i) {
    String name = "Film" + i;
    Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>(name);
    
    
    DecisionTree<TransitionVector<DoubleProbability>> tree = new DecisionTree.Branch<DoubleProbability>(predicate, consequent, alternative);
    State.Builder<DoubleProbability> stateBuilder = new State.Builder<DoubleProbability>(name, "little", tree);
    stateBuilder.setLabel("level", "little");
    builder.addState(stateBuilder.build());
    
    tree = new DecisionTree.Branch<TransitionVector<DoubleProbability>>(predicate, consequent, alternative);
    stateBuilder = new State.Builder<DoubleProbability>(name, "moderate", tree);
    stateBuilder.setLabel("level", "moderate");
    builder.addState(stateBuilder.build());

    tree = new DecisionTree.Branch<TransitionVector<DoubleProbability>>(predicate, consequent, alternative);
    stateBuilder = new State.Builder<DoubleProbability>(name, "moderate", tree);
    stateBuilder.setLabel("level", "large");
    builder.addState(stateBuilder.build());
    
    return builder.build();
  }
  
  public static Machine<DoubleProbability> buildObserver() {
    Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>("Observer");
    return builder.build();
  } */

  public static void main(String[] args) {
    try {
      long begin = System.nanoTime();

      Net<DoubleProbability> net = Net.parse("dsl/Biofilm");
      /*Net.Builder<DoubleProbability> netBuilder = Net.partialParse("dsl/Biofilm");
      for (int i = 0; i < N-1; i++) {
        netBuilder.addMachine(buildFilm(i));
      }
      netBuilder.addMachine(buildLastFilm(N-1));
      netBuilder.addMachine(buildObserver());
      
      Net<DoubleProbability> net = netBuilder.build(); */
      
      System.out.println("Parsed net");
      System.out.println(net);
      
      AggregateNet<DoubleProbability> aNet = new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
      final int N = aNet.size()-2;

      System.out.println(aNet);
      aNet = aNet.multiply(0,1);
      aNet = aNet.multiply(0,1);
      System.out.println("after multiply\n" + aNet);
      for (int i = 0; i < N-1; i++) {
        aNet = aNet.multiply(0,1);
        aNet = aNet.sum("Film" + (i+1), "level").reduce(0);
      }
      aNet = aNet.sum("Nutrient","level").sum("ShearStress","level").reduce(0);

      System.out.println("\n" + AggregateMachine.query(aNet.dict, aNet.getMachine(0)));

      /*aNet.multiply(0, 1);
      System.out.println("after multiply\n" + aNet);

      aNet.sum("Opponent","behavior");
      aNet.sum("TitForTat", "id");
      aNet.sum("TitForTat","behavior");
      System.out.println("\nafter sum\n" + aNet);
      
      System.out.println("\nreducing");
      aNet.reduce(0);
      
      aNet.sum("TitForTat","cost");
      aNet.sum("Opponent","cost");
      aNet.reduce(0); */
      
      double elapsed = ((double)(System.nanoTime()-begin))/1e9;
      System.out.println("\nTime taken = " + elapsed);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
}
