package test;

import java.util.ArrayList;
import java.util.BitSet;

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

public class TestICU {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion4.xml";
  public final static int NUM_PATIENTS = 3;
  public final static double P_ARRIVAL = 0.2;
  
  public static DecisionTree<TransitionVector<DoubleProbability>> computeTree(int state, BitSet bedState, int nextBit) {
    if (nextBit >= NUM_PATIENTS) return nextState(bedState);
    
    if (nextBit == state) {
      BitSet set = (BitSet)bedState.clone();
      set.set(nextBit);
      return computeTree(state, set, nextBit+1);
    }
    Predicate pred = new Predicate.Atom("p" + nextBit, "condition", "NA");
    BitSet unset = (BitSet)bedState.clone();
    unset.clear(nextBit);
    BitSet set = (BitSet)bedState.clone();
    set.set(nextBit);
    return new DecisionTree.Branch<TransitionVector<DoubleProbability>>(pred, computeTree(state, unset, nextBit+1), computeTree(state, set, nextBit+1));
  }
  
  public static DecisionTree<TransitionVector<DoubleProbability>> nextState(BitSet set) {
    TransitionVector.Builder<DoubleProbability> builder = new TransitionVector.Builder<DoubleProbability>("Dispatch");
    if (set.cardinality() < NUM_PATIENTS) {
      builder.setProbability("None", new DoubleProbability(1-P_ARRIVAL));
      for (int i = 0; i < NUM_PATIENTS; i++) {
        if (!set.get(i)) {
          builder.setProbability(Integer.toString(i), new DoubleProbability(P_ARRIVAL/(NUM_PATIENTS-set.cardinality())));
        }
      }
    } else {
      builder.setProbability("None", DoubleProbability.ONE);
    }
    return new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(builder.build());
  }
  
  public static Machine<DoubleProbability> computeDispatch() {
    Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>("Dispatch");
    State.Builder<DoubleProbability> sBuilder = new State.Builder<DoubleProbability>("Dispatch", "None", computeTree(-1, new BitSet(), 0));
    sBuilder.setLabel("next", "none");
    builder.addState(sBuilder.build());
    for (int i = 0; i < NUM_PATIENTS; i++) {
      sBuilder =  new State.Builder<DoubleProbability>("Dispatch", Integer.toString(i), computeTree(i, new BitSet(), 0));
      sBuilder.setLabel("next", Integer.toString(i));
      builder.addState(sBuilder.build());
    }
    return builder.build();
  }

  public static void main(String[] args) throws Exception {
    Net.Builder<DoubleProbability> netBuilder = Net.partialParse("dsl/Patient");
    netBuilder.addMachine(computeDispatch());
    
    Net<DoubleProbability> net = netBuilder.build();
    System.out.println(net);
    AggregateNet<DoubleProbability> origNet = new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
    System.out.println(origNet);

    MonteCarlo mc = new MonteCarlo(origNet);
    ArrayList<Stack<String>> names = new ArrayList<Stack<String>>();
    names.add(Stack.makeName("Dispatch", "next"));
    ResultTree rv = mc.runQuery(10000000, names);
    System.out.println(rv.toCountString(10000000));

    AggregateNet<DoubleProbability> aNet = origNet;
    aNet = aNet.multiply(5, 4).sum("p2Lung","Condition").sum("p2Lung","Cost").reduce(4);
    aNet = aNet.multiply(3, 2).sum("p1Lung","Condition").sum("p1Lung","Cost").reduce(2);
    aNet = aNet.multiply(1, 0).sum("p0Lung","Condition").sum("p0Lung","Cost").reduce(0);
    System.out.println("\nAfter multiplying lung\n" + aNet);

    aNet = aNet.multiply(3, 2).sum("p2","ICP").sum("p2","condition").sum("p2","Cost").reduce(2);
    // System.out.println("After first multiplication\n" + aNet);
    aNet = aNet.multiply(2, 1).sum("p1","ICP").sum("p1","condition").sum("p1","Cost").reduce(1);
    // System.out.println("After second multiplication\n" + aNet);
    aNet = aNet.multiply(1, 0).sum("p0","ICP").sum("p0","condition").sum("p0","Cost").reduce(0);
    // System.out.println("After second multiplication\n" + aNet);
    
    System.out.println(AggregateMachine.query(aNet.dict, aNet.getMachine(0)));
    

    
    System.exit(0);
  }
}
