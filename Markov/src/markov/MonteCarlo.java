package markov;

import java.util.HashMap;
import java.util.Random;

import util.Stack;

public class MonteCarlo {
  private AggregateNet<DoubleProbability> net;
  private Random rng;
  
  public MonteCarlo(AggregateNet<DoubleProbability> net) {
    this(net, new Random());
  }
  
  public MonteCarlo(AggregateNet<DoubleProbability> net, Random rng) {
    this.net = net;
    this.rng = rng;
  }
  
  public static class Var {
    public final int machineNum;
    public final int varId;
    
    public Var(int machineNum, int varId) {
      this.machineNum = machineNum;
      this.varId = varId;
    }
  }

  public RunResult runQuery(int runLength, Stack<String> ... names) {
    Stack<Alphabet> alphas = Stack.<Alphabet>emptyInstance();
    for (int i = names.length-1; i >= 0 ; i--) {
      alphas = alphas.push(net.dict.getAlpha(names[i]));
    }
    Stack<Var> varStack = Stack.<Var>emptyInstance();
    for (int i = 0; i < names.length; i++) {
      int varId = net.dict.getId(names[i]);      
      varStack = varStack.push(new Var(net.machineProviding(varId), varId));
    }
    RunResult rv = RunResult.create(alphas);
    int[] state = new int[net.size()];
    for (int i = 0; i < state.length; i++) {
      state[i] = rng.nextInt(net.getMachine(i).getNumStates());
    }
    for (int step = 0; step < runLength; step++) {
      state = updateState(state);
      // update the run result
      Stack<String> result = Stack.<String>emptyInstance();
      for (Var var : varStack) {
        result.push(net.getMachine(var.machineNum).getState(state[var.machineNum]).getValue(var.varId));
      }
      rv.increment(result);
    }
    return rv;
  }
  
  public int[] updateState(final int[] state) {
    int[] newState = new int[net.size()];
    for (int i = 0; i < state.length; i++) {
      AggregateMachine<DoubleProbability> machine = net.getMachine(i);
      Romdd<AggregateTransitionVector<DoubleProbability>> decision = machine.getState(state[i]).transitionFunction;
      AggregateTransitionVector<DoubleProbability> vector = decision.eval(new Romdd.Query() {
        public int getChoice(int varId) {
          int providerIdx = net.machineProviding(varId);
          AggregateMachine<DoubleProbability> provider = net.getMachine(providerIdx);
          AggregateState<DoubleProbability> providingState = provider.getState(state[providerIdx]);
          return net.dict.getAlpha(varId).indexOf(providingState.getValue(varId));
        }
      });
      newState[i] = sample(vector);      
    }
    return newState;
  }
  
  public int sample(AggregateTransitionVector<DoubleProbability> vector) {
    double samp = rng.nextDouble();
    int count = 0;
    boolean found = false; 
    double sum = 0;
    do {
      sum += vector.get(count).p;
      if (sum < samp) {
        count++;
      } else {
        found = true;
      }
    } while (!found);
    return count;
  }
  
  // This is a tree structure, similar to a Romdd.
  public abstract static class RunResult {
    public static RunResult create(Stack<Alphabet> alphas) {
      return new Node(alphas);
    }
    public abstract void increment(Stack<String> choices);
    public abstract void accept(Visitor v);
    
    public static class Node extends RunResult {
      public final Alphabet alpha;
      HashMap<String, RunResult> children;
      
      public Node(Stack<Alphabet> alphas) {
        if (alphas.isEmpty()) throw new RuntimeException();
        
        alpha = alphas.head();
        Stack<Alphabet> tail = alphas.tail();
        for (int i = 0; i < alpha.size(); i++) {
          children.put(alpha.get(i), tail.isEmpty() ? new Terminal() : new Node(tail));
        }
      }
      
      public void accept(Visitor v) { v.visitNode(this); }
      
      public void increment(Stack<String> choices) {
        // this can throw Null if choices is not according to the initial alphabet stack
        children.get(choices.head()).increment(choices.tail());
      }
    }
    
    public static class Terminal extends RunResult {
      public int count;
      
      public void increment(Stack<String> choices) {
        if (!choices.isEmpty()) throw new RuntimeException();
        count++;
      }
      
      public void accept(Visitor v) { v.visitTerminal(this); }
    }
  }
  
  public static interface Visitor {
    public void visitNode(RunResult.Node node);
    public void visitTerminal(RunResult.Terminal term);
  }
}
