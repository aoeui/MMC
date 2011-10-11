package markov;

import java.util.List;
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

  public ResultTree runQuery(int runLength, List<Stack<String>> names) {
    Stack<Alphabet> alphas = Stack.<Alphabet>emptyInstance();
    for (int i = names.size()-1; i >= 0 ; i--) {
      alphas = alphas.push(net.dict.getAlpha(names.get(i)));
    }
    Stack<Var> varStack = Stack.<Var>emptyInstance();
    for (int i = 0; i < names.size(); i++) {
      int varId = net.dict.getId(names.get(i));      
      varStack = varStack.push(new Var(net.machineProviding(varId), varId));
    }
    ResultTree rv = ResultTree.create(alphas);
    int[] state = new int[net.size()];
    for (int i = 0; i < state.length; i++) {
      state[i] = rng.nextInt(net.getMachine(i).getNumStates());
    }
    for (int step = 0; step < runLength; step++) {
      state = updateState(state);
      // update the run result
      Stack<String> result = Stack.<String>emptyInstance();
      for (Var var : varStack) {
        result = result.push(net.getMachine(var.machineNum).getState(state[var.machineNum]).getValue(var.varId));
      }
      rv.increment(result, (double)1/(double)runLength);
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
      sum += vector.get(count).doubleValue();
      if (sum < samp) {
        count++;
      } else {
        found = true;
      }
    } while (!found);
    return count;
  }
}
