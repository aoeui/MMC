package test;

import markov.Net;
import markov.DoubleProbability;

public class TestDsl {
  public static void main(String[] args) {
    try {
      Net<DoubleProbability> net = Net.parse(args[0]);
      System.out.println(net);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
}
