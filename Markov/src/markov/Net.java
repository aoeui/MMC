package markov;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import markov.DecisionTree.Branch;
import markov.DecisionTree.Terminal;
import markov.Predicate.Atom;
import markov.Predicate.CollectionPredicate;
import markov.Predicate.Implies;
import markov.Predicate.Neg;
import util.Indenter;
import util.UnmodifiableIterator;

public class Net<T extends Probability<T>> implements Iterable<Machine<T>> {
  public final int N;

  private HashMap<String,Machine<T>> machineTable;

  public final Dictionary dictionary;

  private ArrayList<String> idxToMachine;
  private HashMap<String,Integer> machineToIdx;  // giving a machine an index
  
  private ArrayList<ArrayList<Boolean>> connectivity;  // describes machine topology
  
  private Net(Collection<Machine<T>> machines) {
    this.machineTable = new HashMap<String,Machine<T>>();
    
    Dictionary.Builder dBuilder = new Dictionary.Builder();
    for (Machine<T> m : machines) {
      machineTable.put(m.name, m);
      dBuilder.add(new Domain.Builder(m).build());
    }

    this.dictionary = dBuilder.build();
    this.N = machineTable.size();
    idxToMachine = new ArrayList<String>(machineTable.keySet());
    Collections.sort(idxToMachine);
    machineToIdx = new HashMap<String,Integer>();
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
      recurseTreeFindMachineNames(state.getTransitionFunction(), accu);
    }
  }
  
  private void recurseTreeFindMachineNames(DecisionTree<T> tree, final Set<String> accu) {
    tree.accept(new DecisionTree.Visitor<T>() {
      public void visitTerminal(Terminal<T> t) {}
      public void visitBranch(Branch<T> t) {
        recursePredicateFindMachineNames(t.predicate, accu);
        recurseTreeFindMachineNames(t.consequent, accu);
        recurseTreeFindMachineNames(t.alternative, accu);
      }
    });
  }
  
  private void recursePredicateFindMachineNames(Predicate pred, final Set<String> accu) {
    pred.accept(new Predicate.VisitorAdapter() {
      public void visit(Predicate p) {
        throw new RuntimeException();
      }
      public void visitCollection(CollectionPredicate predicate) {
        for (Predicate p : predicate) {
          recursePredicateFindMachineNames(p, accu);
        }
      }
      public void visitNeg(Neg predicate) {
        recursePredicateFindMachineNames(predicate.subject, accu);
      }
      public void visitImplies(Implies predicate) {
        recursePredicateFindMachineNames(predicate.antecedent, accu);
        recursePredicateFindMachineNames(predicate.consequent, accu);
      }
      public void visitAtom(Atom predicate) {
        accu.add(predicate.machineName);
      }
    });
  }
  
  public boolean isConnected(String m1, String m2) {
    if (m1.equals(m2)) return true;
    
    return connectivity.get(machineToIdx.get(m1)).get(machineToIdx.get(m2));
  }
  
  public Machine<T> getMachine(String name) {
    return machineTable.get(name);
  }
  
  public Domain getDomain(String name) {
    return dictionary.get(name);
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
    HashMap<String, Machine<T>> machines;
    
    public Builder() {
      machines = new HashMap<String,Machine<T>>();
    }
    
    public void addMachine(Machine<T> m) {
      machines.put(m.name, m);
    }
    
    public Net<T> build() {
      return new Net<T>(machines.values());
    }
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
      indenter.println("dictionary:").indent().println(dictionary.get(name)).deindent();
      indenter.print("neighbors = [");
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