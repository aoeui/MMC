package test;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;

import dsl.DecisionTreeLexer;
import dsl.DecisionTreeParser;

import markov.Net;
import markov.DoubleProbability;

public class TestDsl {
  public static void main(String[] args) throws Exception {
    try {
      DecisionTreeLexer lex = new DecisionTreeLexer(new ANTLRFileStream(args[0]));
      CommonTokenStream tokens = new CommonTokenStream(lex);
      DecisionTreeParser parser = new DecisionTreeParser(tokens);
    
      Net<DoubleProbability> machine = parser.net();
      System.out.println(machine);
    } catch (RuntimeException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
}
