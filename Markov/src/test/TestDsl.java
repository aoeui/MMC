package test;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;

import dsl.NetLexer;
import dsl.NetParser;

import markov.Net;
import markov.DoubleProbability;

public class TestDsl {
  public static void main(String[] args) throws Exception {
    try {
      NetLexer lex = new NetLexer(new ANTLRFileStream(args[0]));
      CommonTokenStream tokens = new CommonTokenStream(lex);
      NetParser parser = new NetParser(tokens);
    
      Net<DoubleProbability> machine = parser.net();
      System.out.println(machine);
    } catch (RuntimeException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
}
