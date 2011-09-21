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

import util.Coroutine;
import util.Stack;

public class TestICU {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion4.xml";
  public final static int NUM_PATIENTS = 3;
  
  public static DecisionTree<TransitionVector<DoubleProbability>> computeTree(BitSet set, int nextBit) {
    if (nextBit >= NUM_PATIENTS) return nextState(set);
    
    if (set.get(nextBit)) {
      Predicate pred = new Predicate.Atom("p" + nextBit, "condition", "NA");
      BitSet cSet = (BitSet)set.clone();
      cSet.clear(nextBit);
      return new DecisionTree.Branch<TransitionVector<DoubleProbability>>(pred, computeTree(cSet, nextBit+1), computeTree(set, nextBit+1));
    } else {
      return computeTree(set, nextBit+1);
    }
  }
  
  public static String stringOf(BitSet set) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < NUM_PATIENTS; i++) {
      builder.append(set.get(i) ? "1" : "0");
    }
    return builder.toString();
  }
  
  public static DecisionTree<TransitionVector<DoubleProbability>> nextState(BitSet set) {
    Predicate pred = new Predicate.Atom("arrival", "arriving", "1");
    DecisionTree<TransitionVector<DoubleProbability>> cons = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(arrivalVector(set));
    DecisionTree<TransitionVector<DoubleProbability>> alt = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(TransitionVector.<DoubleProbability>getSingle("disptach", stringOf(set),DoubleProbability.ONE));
    return new DecisionTree.Branch<TransitionVector<DoubleProbability>>(pred, cons, alt);
  }
  
  // Takes the input bitset and generates arrivals to the unoccupied slots
  public static TransitionVector<DoubleProbability> arrivalVector(BitSet set) {
    TransitionVector.Builder<DoubleProbability> builder = new TransitionVector.Builder<DoubleProbability>("dispatch");
    if (set.cardinality() == NUM_PATIENTS) {
      builder.setProbability(stringOf(set), DoubleProbability.ONE);
    } else {
      DoubleProbability prob = new DoubleProbability(1./(double)(NUM_PATIENTS - set.cardinality()));
      for (int i = 0; i < NUM_PATIENTS; i++) {
        if (set.get(i)) continue;
        set.set(i);
        builder.setProbability(stringOf(set), prob);
        set.clear(i);
      }
    }
    return builder.build();
  }
  
  public static State<DoubleProbability> computeState(BitSet set) {
    State.Builder<DoubleProbability> builder = new State.Builder<DoubleProbability>("dispatch", stringOf(set), computeTree(set, 0));
    for (int i = 0; i < NUM_PATIENTS; i++) {
      builder.setLabel("p" + i, set.get(i) ? "occupied" : "empty");
    }
    builder.setLabel("count", Integer.toString(set.cardinality()));
    return builder.build();
  }
  
  public static class BitSetPermuter extends Coroutine<BitSet> {
    public void init() {
      recurse(new BitSet(), 0);
    }
    public void recurse(BitSet set, int nextBit) {
      if (nextBit >= NUM_PATIENTS) {
        yield((BitSet)set.clone());
        return;
      }
      BitSet setted = (BitSet)set.clone();
      setted.set(nextBit);
      recurse(setted, nextBit+1);
      recurse(set, nextBit+1);
    }
  }
  
  public static Machine<DoubleProbability> computeDispatch() {
    Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>("Dispatch");
    for (BitSet set : new BitSetPermuter()) {
      builder.addState(computeState(set));
    }
    return builder.build();
  }

  public static void main(String[] args) throws Exception {
    Net.Builder<DoubleProbability> netBuilder = Net.partialParse("Patient");
    netBuilder.addMachine(computeDispatch());
    
    Net<DoubleProbability> net = netBuilder.build();
    System.out.println(net);
    AggregateNet<DoubleProbability> origNet = new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
    System.out.println(origNet);
    AggregateNet<DoubleProbability> aNet = origNet;
    aNet = aNet.multiply(5, 4).sum("p2Lung","Condition").sum("p2Lung","Cost").reduce(4);
    aNet = aNet.multiply(3, 2).sum("p1Lung","Condition").sum("p1Lung","Cost").reduce(2);
    aNet = aNet.multiply(1, 0).sum("p0Lung","Condition").sum("p0Lung","Cost").reduce(0);
    System.out.println("\nAfter multiplying lung\n" + aNet);

    aNet = aNet.multiply(3, 4).sum("arrival","arriving").reduce(3);
    System.out.println("\nAfter multiplying arrival\n" + aNet);
    aNet = aNet.multiply(3, 2).sum("p2","ICP").sum("p2","condition").sum("Dispatch","p2").sum("p2","Cost").reduce(2);
    // System.out.println("After first multiplication\n" + aNet);
    aNet = aNet.multiply(2, 1).sum("p1","ICP").sum("p1","condition").sum("Dispatch","p1").sum("p1","Cost").reduce(1);
    // System.out.println("After second multiplication\n" + aNet);
    aNet = aNet.multiply(1, 0).sum("p0","ICP").sum("p0","condition").sum("Dispatch","p0").sum("p0","Cost").reduce(0);
    // System.out.println("After second multiplication\n" + aNet);
    
    System.out.println(AggregateMachine.query(aNet.dict, aNet.getMachine(0)));
    
    MonteCarlo mc = new MonteCarlo(origNet);
    ArrayList<Stack<String>> names = new ArrayList<Stack<String>>();
    names.add(Stack.makeName("Dispatch", "count"));
    ResultTree rv = mc.runQuery(10000000, names);
    System.out.println(rv.toCountString(10000000));

    
    System.exit(0);
  }
}
