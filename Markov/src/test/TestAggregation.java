package test;

import markov.AggregateNet;
import markov.Net;
import markov.DoubleProbability;

public class TestAggregation {
  public static void main(String[] args) {
    try {
      long begin = System.nanoTime();

      Net<DoubleProbability> net = Net.parse(args[0]);
      System.out.println("Parsed net");
      System.out.println(net);
      AggregateNet<DoubleProbability> aNet = new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
      System.out.println(aNet);
      aNet.multiply(0, 1);
      System.out.println("after multiply\n" + aNet);

      aNet.sum("Opponent","behavior");
      aNet.sum("TitForTat", "id");
      aNet.sum("TitForTat","behavior");
      System.out.println("\nafter sum\n" + aNet);
      
      System.out.println("\nreducing");
      aNet.reduce(0);
      
      aNet.sum("TitForTat","cost");
      aNet.sum("Opponent","cost");
      aNet.reduce(0);
      
      double elapsed = ((double)(System.nanoTime()-begin))/1e9;
      System.out.println("\nTime taken = " + elapsed);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
}
