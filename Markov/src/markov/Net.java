package markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import dsl.NetLexer;
import dsl.NetParser;

import util.Indenter;
import util.TerminatedIterator;
import util.UnmodifiableIterator;
import util.Stack;

public class Net<T extends Probability<T>> implements Iterable<Machine<T>> {
  public final int N;

  private LinkedHashMap<String,Machine<T>> machineTable;

  public final Dictionary dictionary;

  private ArrayList<String> idxToMachine;
  private LinkedHashMap<String,Integer> machineToIdx;  // giving a machine an index
  
  private ArrayList<ArrayList<Boolean>> connectivity;  // describes machine topology
  
  private Net(Collection<Machine<T>> machines) {
    this.machineTable = new LinkedHashMap<String,Machine<T>>();
    
    Dictionary.Builder dBuilder = new Dictionary.Builder();
    for (Machine<T> m : machines) {
      machineTable.put(m.name, m);
      State<T> protoState = m.iterator().next();
      for (Iterator<String> labelIt = protoState.labelNameIterator(); labelIt.hasNext(); ) {
        Alphabet.Builder alphaBuilder = new Alphabet.Builder(m, labelIt.next());
        dBuilder.add(alphaBuilder.build());
      }
    }
    this.dictionary = dBuilder.build();

    this.N = machineTable.size();
    idxToMachine = new ArrayList<String>(machineTable.keySet());
    Collections.sort(idxToMachine);
    machineToIdx = new LinkedHashMap<String,Integer>();
    for (int i = 0; i < N; i++) {
      machineToIdx.put(idxToMachine.get(i), i);
    }
    connectivity = new ArrayList<ArrayList<Boolean>>();
    for (String machineName : idxToMachine) {
      Machine<T> machine = machineTable.get(machineName);
      HashSet<String> names = new HashSet<String>();
      findReferencedMachineNames(machine, names);
      ArrayList<Boolean> row = new ArrayList<Boolean>();
      for (String ref : idxToMachine) {
        row.add(names.contains(ref));
      }
      connectivity.add(row);
    }
  }
  
  private void findReferencedMachineNames(Machine<T> machine, Set<String> accu) {
    for (State<T> state : machine) {
      TerminatedIterator<Predicate.Atom> it = state.getTransitionFunction().atomIterator();
      while (it.hasNext()) {
        accu.add(it.next().name.head());
      }
    }
  }
  
  public boolean isConnected(String m1, String m2) {
    if (m1.equals(m2)) return true;
    
    return connectivity.get(machineToIdx.get(m1)).get(machineToIdx.get(m2));
  }
  
  public Machine<T> getMachine(String name) {
    return machineTable.get(name);
  }
  
  public TreeSet<String> getNeighbors(String str) {
    ArrayList<Boolean> row = connectivity.get(machineToIdx.get(str));
    TreeSet<String> neighbors = new TreeSet<String>();
    for (int i = 0; i < N; i++) {
      if (row.get(i)) {
        neighbors.add(idxToMachine.get(i));
      }
    }
    return neighbors;
  }

  public Iterator<Machine<T>> iterator() {
    return new UnmodifiableIterator<Machine<T>>(machineTable.values().iterator());
  }
  
  public static class Builder<T extends Probability<T>> {
    LinkedHashMap<String, Machine<T>> machines;
    
    public Builder() {
      machines = new LinkedHashMap<String,Machine<T>>();
    }
    
    public void addMachine(Machine<T> m) {
      machines.put(m.name, m);
    }
    
    public Net<T> build() {
      return new Net<T>(machines.values());
    }
  }
  
  public static Net<DoubleProbability> parse(String filename) throws IOException, RecognitionException {
    return partialParse(filename).build();
  }

  public static Net.Builder<DoubleProbability> partialParse(String filename) throws IOException, RecognitionException {
    NetLexer lex = new NetLexer(new ANTLRFileStream(filename));
    CommonTokenStream tokens = new CommonTokenStream(lex);
    NetParser parser = new NetParser(tokens);
  
    return parser.net();
  }
  
  public String toString() {
    Indenter indenter = new Indenter();
    indent(indenter);
    return indenter.toString();
  }
  public void indent(Indenter indenter) {
    boolean isFirstMachine = true;
    for (String name : machineTable.keySet()) {
      if (isFirstMachine) isFirstMachine = false;
      else indenter.println();
      indenter.print(machineTable.get(name));
      indenter.print("// dictionary: ").indent().println(dictionary.print(Stack.makeName(name))).deindent();
      indenter.print("// neighbors = [");
      boolean isFirst = true;
      for (String neighbor : getNeighbors(name)) {
        if (isFirst) isFirst = false;
        else indenter.print(", ");
        indenter.print(neighbor);
      }
      indenter.println("]");
    }
  }
}