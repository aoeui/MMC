package test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import markov.AggregateMachine;
import markov.AggregateNet;
import markov.DoubleProbability;
import markov.MonteCarlo;
import markov.Net;
import markov.ResultTree;
import markov.SymbolicProbability;
import markov.TransitionMatrix;
import parser.XmlParser;
import util.Stack;

public class TestICU1 {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion6.xml";
  public final static int NUM_PATIENTS = 12;
  public final static int STEPS=10000000;  

  public static void main(String[] args) {
    long begin = System.nanoTime();

    Net<DoubleProbability> net= (new XmlParser(PATIENT_MODEL_FILENAME,NUM_PATIENTS)).net;

    System.out.println(net);
    Pattern arrivePattern = Pattern.compile("p(\\d+)arriving");

    AggregateNet<DoubleProbability> aNetOrig=new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
    AggregateNet<DoubleProbability> aNet=aNetOrig;

    System.out.println(aNet);
  
    for (int i = aNet.size()-1; i >= 1; i--) {
      aNet = aNet.multiply(i-1, i);
      System.out.println("\nAfter multiplying p" + (i-1));
      
      AggregateMachine<?> machine = aNet.getMachine(aNet.size()-1);
      for (Integer val : machine.labels) {
        String name = aNet.dict.getName(val).tail().head();
        if (name.equals("arriving")) continue;
        
        Matcher match = arrivePattern.matcher(name);
        if (match.matches()) {
          int label = Integer.parseInt(match.group(1));
          if (label < i-1) continue;
        }
        System.out.println("Summing out label " + aNet.dict.getName(val));
        aNet = aNet.sum(val);
      }
      aNet = aNet.reduce(i-1);
      
      // System.out.println(aNet);
    }

    AggregateMachine<DoubleProbability> machine = aNet.getMachine(0);
    System.out.println("\n" + AggregateMachine.query(aNet.dict, machine));

    TransitionMatrix<SymbolicProbability<DoubleProbability>> prob = machine.computeTransitionMatrix();

    try {
      FileWriter fOut=new FileWriter("matOut.txt");
      for (int i=0;i<aNet.getMachine(0).getNumStates();i++){
        fOut.write(aNet.getMachine(0).getState(i).getValueStack().head()+"\t");
      }
      fOut.write("\n");
      for (int i = 0; i < prob.N; i++) {
        for (int j = 0; j < prob.N; j++) {
          if (!prob.get(i,j).value.isZero()){
            fOut.write((i+1)+"\t"+(j+1)+"\t"+Double.toString(prob.get(i,j).value.doubleValue())+"\n");
          }
        }
      }
      fOut.close();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    double elapsed = ((double)(System.nanoTime()-begin))/1e9;
    System.out.println("Time taken = " + elapsed);
    System.exit(0);

/*     System.out.println("\nTrying Monte Carlo: ");
    try {
      runMonteCarlo(aNetOrig);
      runMonteCarlo(aNet);
    } catch (Exception e) {
      e.printStackTrace();
    } */
    
  }
  public static void runMonteCarlo(AggregateNet<DoubleProbability> net) throws Exception {
    MonteCarlo mc = new MonteCarlo(net);
    ArrayList<Stack<String>> names = new ArrayList<Stack<String>>();
    names.add(Stack.makeName("Dispatch", "arriving"));
    ResultTree rv = mc.runQuery(STEPS, names);
    System.out.println(rv.toCountString(STEPS));
  }
}
