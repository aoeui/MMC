package markov;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import util.Stack;

public class AggregateState<T extends Probability<T>> {
  public final Dictionary dict;
  public final Romdd<TransitionVector<T>> transitionFunction;

  TreeMap<Integer,String> labelVector = new TreeMap<Integer,String>();

  public AggregateState(Dictionary dict, State<T> state1, State<T> state2) {
    this.dict = dict;
    Romdd<TransitionVector<T>> romdd2 = state2.getTransitionFunction().toRomdd(dict);
    for (Iterator<Map.Entry<String, String>> entryIt = state1.labelIterator(); entryIt.hasNext(); ) {
      Map.Entry<String, String> entry = entryIt.next();
      int varId = dict.getId(Stack.makeName(state1.machineName, entry.getKey()));
      labelVector.put(varId, entry.getValue());
      romdd2 = romdd2.restrict(varId, entry.getValue());
    }
    Romdd<TransitionVector<T>> romdd1 = state1.getTransitionFunction().toRomdd(dict);
    for (Iterator<Map.Entry<String, String>> entryIt = state2.labelIterator(); entryIt.hasNext(); ) {
      Map.Entry<String, String> entry = entryIt.next();
      int varId = dict.getId(Stack.makeName(state1.machineName, entry.getKey()));
      labelVector.put(varId, entry.getValue());
      romdd1 = romdd1.restrict(varId, entry.getValue());
    }
    // TODO take the product of two transition functions - need to figure out a way to point to product states
    this.transitionFunction = null;
  }
}
