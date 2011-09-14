package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Formatter;

import markov.Alphabet;
import markov.Dictionary;
import markov.Predicate;
import markov.Predicate.CollectionType;
import markov.Romdd;

import util.Coroutine;
import util.Pair;
import util.Partition;
import util.Stack;
import util.TerminatedIterator;

public class Sudoku {
  public final int N;
  public final int order;

  private int[] puzzle;

  public static void main(String[] args) throws IOException { 
    long begin = System.nanoTime();
    for (String name : args) {
      solve(name);
    }
    double elapsed = ((double)(System.nanoTime()-begin))/1e9;
    System.out.println("Time taken = " + elapsed);
    System.exit(0);
  }
  
  public static void solve(String fileName) throws IOException {
    Sudoku puzzle = parse(new BufferedReader(new FileReader(fileName)));
    Solver solver = new Solver(puzzle);
    Solver rv = solver.solve();
    if (rv != null) {
      System.out.println(rv);
      System.out.println("Verified = " + rv.verifySolution());
    } else {
      System.out.println("Solver failed, inconsistent problem suspected.");
      System.out.println(puzzle);
    }
  }
  
  public static Sudoku parse(BufferedReader reader) throws IOException {
    String nStr = reader.readLine();
    int order = Integer.parseInt(nStr);
    int N = order*order;
    Builder builder = new Builder(order);
    for (int i = 0; i < N; i++) {
      String[] nums = reader.readLine().split("\\s+");
      for (int j = 0; j < N; j++) {
        try {
          int val = Integer.parseInt(nums[j]);
          if (val != 0) builder.set(i, j, val);
        } catch (Exception e) {}
      }
    }
    return builder.build();
  }
  
  private Sudoku(int order, int[] puzzle) {
    this.N = order*order;
    this.order = order;
    this.puzzle = new int[N*N];
    System.arraycopy(puzzle, 0, this.puzzle, 0, N*N);
  }
  
  public int get(int row, int col) {
    if (row < 0 || row >= N || col < 0 || col >= N) throw new ArrayIndexOutOfBoundsException();
    return puzzle[row*N+col];
  }
  
  public TerminatedIterator<Entry> nonZeroIterator() {
    return new Coroutine<Entry>() {
      public void init() {
        for (int row = 0; row < N; row++) {
          for (int col = 0; col < N; col++) {
            int val = puzzle[N*row + col];
            if (val != 0) yield(new Entry(row, col, val));
          }
        }
      }
    }.iterator();
  }
  
  public int get(Pair<Integer> pair) {
    return get(pair.first, pair.second);
  }
  
  public int get(int idx) {
    return puzzle[idx];
  }
  
  public List<Pair<Integer>> getNeighbors(Pair<Integer> pair) {
    return getNeighbors(pair.first, pair.second);
  }

  public List<Pair<Integer>> getNeighbors(int row, int col) {
    ArrayList<Pair<Integer>> neighbors = new ArrayList<Pair<Integer>>();
    Pair<Integer> blockInfo = fromIdxToBlockIdx(order, row, col);
    for (int i = 0; i < N; i++) {
      if (i != row) neighbors.add(new Pair<Integer>(i, col));
      if (i != col) neighbors.add(new Pair<Integer>(row, i));
      if (i != blockInfo.second) {
        Pair<Integer> candidate = fromBlockIdxToIdx(order, blockInfo.first, i);
        if (candidate.first != row && candidate.second != col) {
          neighbors.add(fromBlockIdxToIdx(order, blockInfo.first, i));
        }
      }
    }
    return neighbors;
  }
  
  /** Blocks are indexed by block number, offset. Example:
   *  1 | 2  There are sqrt(N) x sqrt(N) blocks labeled thusly.
   * -------  
   *  3 | 4  Each block is internally structured just like overall block structure.
   * */
  public static Pair<Integer> fromIdxToBlockIdx(int order, int row, int col) {
    int blockRow = row / order;
    int blockRowOffset = row % order;
    int blockCol = col / order;
    int blockColOffset = col % order;

    int blockNum = blockRow*order + blockCol;
    int blockOffset = blockRowOffset * order + blockColOffset;
    
    return new Pair<Integer>(blockNum, blockOffset);
  }
  
  public static Pair<Integer> fromBlockIdxToIdx(int order, int blockNum, int offset) {
    int blockRow = blockNum / order;
    int blockCol = blockNum % order;
    int offRow = offset / order;
    int offCol = offset % order;
    
    return new Pair<Integer>(blockRow * order + offRow, blockCol*order + offCol);
  }

  public String toString() {
    Formatter format = new Formatter();
    for (int i = 0; i < N; i++) {
      for (int j = 0; j < N; j++) {
        format.format("%4d", puzzle[N*i+j]);
      }
      format.format("\n");
    }
    return format.toString();
  }
  
  public static class Builder {
    public final int N;
    public final int order;
    private int[] puzzle;

    public Builder(int order) {
      this.order = order;
      this.N = order*order;
      puzzle = new int[N*N];
    }

    public void set(int row, int col, int value) {
      if (row < 0 || row >= N || col < 0 || col >= N) throw new ArrayIndexOutOfBoundsException();
      if (value < 1 || value > N) throw new RuntimeException();
      
      puzzle[row*N+col] = value;
    }
    
    public void validate() throws ValidationException {
      BitSet tracker1 = new BitSet(N);  // checks rows
      BitSet tracker2 = new BitSet(N);  // checks cols
      BitSet tracker3 = new BitSet(N);  // checks neighborhoods
      for (int row = 0; row < N; row++) {
        tracker1.clear();
        tracker2.clear();
        tracker3.clear();
        for (int col = 0; col < N; col++) {
          int val = puzzle[row*N+col];
          if (val != 0) {
            if (tracker1.get(val)) throw new RuntimeException("Invalid puzzle.");
            tracker1.set(val);
          }
          val = puzzle[col*N+row];
          if (val != 0) {
            if (tracker2.get(val)) throw new RuntimeException("Invalid puzzle.");
            tracker2.set(val);
          }
          Pair<Integer> pair = fromBlockIdxToIdx(order, row, col);
          val = puzzle[pair.first*N + pair.second];
          if (val != 0) {
            if (tracker3.get(val)) throw new RuntimeException("Invalid puzzle.");
            tracker3.set(val);            
          }
        }
      }
    }

    public Sudoku build() throws ValidationException {
      validate();
      return new Sudoku(order, puzzle);
    }
  }
  
  public static class ValidationException extends RuntimeException {
    private static final long serialVersionUID = -4291372421542506125L;}
    
  public static class SolverException extends RuntimeException {
    private static final long serialVersionUID = -397675822428486153L;
    public SolverException() { }
    public SolverException(String msg) { super(msg); }
  }

  public static class Entry {
    public final String name;
    public final int row;
    public final int col;
    public final int val;
    
    public Entry(int row, int col, int val) {
      this.row = row;
      this.col = col;
      this.val = val;
      this.name = row + "." + col;
    }
    
    public Entry(String name, int val) {
      this.name = name;
      this.val = val;
      String[] names = name.split("\\.");
      row = Integer.parseInt(names[0]);
      col = Integer.parseInt(names[1]);
    }
    
    public Entry(Pair<Integer> pair, int val) {
      this(pair.first, pair.second, val);
    }
    
    public Entry(Pair<Integer> pair, String next) {
      this(pair, Integer.parseInt(next));
    }

    public String toString() {
      return name + " = " + val;
    }
    
    public Stack<String> getId() {
      return Stack.makeName(Integer.toString(row), Integer.toString(col));
    }
  }
  
  public static Pair<Integer> toPair(Stack<String> name) {
    return new Pair<Integer>(Integer.parseInt(name.head()),Integer.parseInt(name.tail().head()));  
  }

  public boolean isNeighbor(Pair<Integer> p1, Pair<Integer> p2) {
    if (p1.first.equals(p2.first) || p1.second.equals(p2.second)) return true;
    if (p1.first / order == p2.first / order && p1.second / order == p2.second) return true; 
    return false;
  }
  
  public static Predicate.Atom buildAtom(Pair<Integer> pair, int val) {
    return buildAtom(pair.first, pair.second, val);
  }
  public static Predicate.Atom buildAtom(Pair<Integer> pair, String val) {
    return new Predicate.Atom(Integer.toString(pair.first), Integer.toString(pair.second), val);
  }
  public static Predicate.Atom buildAtom(int row, int col, int val) {
    return new Predicate.Atom(Integer.toString(row), Integer.toString(col), Integer.toString(val));
  }

  public static class Solver {
    public final int N;
    public final Sudoku instance;
    public final Dictionary dict;

    boolean[] marked;
    int markCount;
    private int[] solution;
    Romdd<Boolean> constraints;
    
    public Solver(Sudoku instance) {
      this.N = instance.N;
      this.instance = instance;
      this.dict = buildDictionary();
      marked = new boolean[N*N];
      initializeMarkings();
      this.solution = new int[N*N];
      System.arraycopy(instance.puzzle, 0, solution, 0, N*N);
      constraints = buildInitialConstraints();
      System.out.println("Initial constraints complete.");
    }
    
    public Solver(Solver toCopy) {
      this.N = toCopy.N;
      this.instance = toCopy.instance;
      this.dict = toCopy.dict;
      this.marked = new boolean[N*N];
      System.arraycopy(toCopy.marked, 0, marked, 0, N*N);
      this.markCount = toCopy.markCount;
      this.solution = new int[N*N];
      System.arraycopy(toCopy.solution, 0, solution, 0, N*N);
      this.constraints = toCopy.constraints;
    }
    
    public void setSolution(int row, int col, int val) {
      if (row < 0 || row >= N || col < 0 || col >= N) throw new ArrayIndexOutOfBoundsException();
      solution[N*row+col] = val;
    }
    public void setSolution(Pair<Integer> coord, int val) {
      setSolution(coord.first, coord.second, val);
    }    
    public void setSolution(Entry e) {
      setSolution(e.row, e.col, e.val);
    }
    public int getSolution(int row, int col) {
      if (row < 0 || row >= N || col < 0 || col >= N) throw new ArrayIndexOutOfBoundsException();
      return solution[N*row+col];
    }    
    public int getSolution(Pair<Integer> coord) {
      return getSolution(coord.first, coord.second);
    }

    public Solver solve() {
      if (deduce()) {
        return constraints.compareTo(Romdd.TRUE) == 0 ? this : null;
      } else {
        Partition<ValueTable> tables = getValueTable(constraints);
        ValueTable table = tables.get(0);
        for (String val : table.values) {
          Solver subSolver = new Solver(this);
          // System.out.println("Speculating");
          subSolver.instantiate(new Entry(table.pair, val));
          Solver rv = subSolver.solve();
          if (rv != null) return rv;
        }
      }
      return null;
    }
    
    public void instantiate(Entry fixed) {
//    System.out.println(fixed);
       setSolution(fixed);
       setMarked(fixed.row, fixed.col);
    
       // System.out.print("restricting " + fixed);
       constraints = constraints.restrict(fixed.getId(), Integer.toString(fixed.val));
       // System.out.println(" complete");
    
       Predicate.CollectionBuilder pBuilder = new Predicate.CollectionBuilder(CollectionType.AND);
       for (Pair<Integer> neighbor : instance.getNeighbors(fixed.row, fixed.col)) {
         if (isMarked(neighbor)) continue;
         pBuilder.add(new Predicate.Neg(buildAtom(neighbor, fixed.val)));
       }
       if (pBuilder.size() >= 1) {
         Predicate prop = pBuilder.build();
         // System.out.println("Propagating constraints " + prop);
         constraints = Romdd.<Boolean>apply(Romdd.AND, constraints, prop.toRomdd(dict));
       }
    }
    
    public boolean deduce() {
      // System.out.println("In deduce().");
      boolean stuck = false;
      while (!stuck && !constraints.isTerminal()) {
        //System.out.println("Computing value tables");
        Partition<ValueTable> tables = getValueTable(constraints);
        //System.out.println("Value tables computed");
        if (tables.get(0).values.size() == 1) {
          // System.out.println("Found " + tables.getBlock(0).size() +  " inferrable " + (tables.getBlock(0).size() > 1 ? "values." : "value."));
          for (ValueTable table : tables.getBlock(0)) {
            instantiate(new Entry(table.pair, table.values.iterator().next()));
          }
        } else {
          // System.out.println("No inferrable values found.");
          stuck = true;
        }
      }
      return !stuck;
    }
    
    private Romdd<Boolean> buildInitialConstraints() {
      Predicate.CollectionBuilder pBuilder = new Predicate.CollectionBuilder(CollectionType.AND);
      TerminatedIterator<Entry> it = instance.nonZeroIterator();
      while (it.hasNext()) {
        Entry entry = it.next();
        for (Pair<Integer> neighbor : instance.getNeighbors(entry.row, entry.col)) {
          if (isMarked(neighbor)) continue;
          pBuilder.add(new Predicate.Neg(buildAtom(neighbor, entry.val)));
        }
      }
      return pBuilder.build().toRomdd(dict);      
    }
    
    private TerminatedIterator<Pair<Integer>> unmarkedIterator() {
      return new Coroutine<Pair<Integer>>() {
        public void init() {
          for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
              if (isMarked(row, col)) continue;
              yield(new Pair<Integer>(row, col));
            }
          }
        }        
      }.iterator();
    }
    
    /*
    private ArrayList<Constraint> createConstraints() {
      ArrayList<Constraint> rv = new ArrayList<Constraint>();
      // create row, col, block constraints
      for (int idx = 0; idx < N; idx++) {
        Romdd<Boolean> rowConstraint = Romdd.TRUE, colConstraint = Romdd.TRUE, blockConstraint = Romdd.TRUE;
        for (int c1 = 0; c1 < N; c1++) {
          for (int c2 = c1+1; c2 < N; c2++) {
            Predicate p = createPairPredicate(new Pair<Integer>(idx, c1), new Pair<Integer>(idx, c2));
            rowConstraint = Romdd.apply(Romdd.AND, rowConstraint, p.toRomdd(dict));
            p = createPairPredicate(new Pair<Integer>(c1, idx), new Pair<Integer>(c2, idx));
            colConstraint = Romdd.apply(Romdd.AND, colConstraint, p.toRomdd(dict));
            p = createPairPredicate(fromBlockIdxToIdx(N, instance.order, idx, c1), fromBlockIdxToIdx(N, instance.order, idx, c2));
            blockConstraint = Romdd.apply(Romdd.AND, blockConstraint, p.toRomdd(dict));
          }
        }
        rv.add(new Constraint(rowConstraint));
        rv.add(new Constraint(colConstraint));
        rv.add(new Constraint(blockConstraint));
      }
      return rv;
    } */
    
    public String getVarString(Romdd<Boolean> romdd) {
      StringBuilder builder = new StringBuilder("[");
      boolean isFirst = true;
      for (Alphabet var : romdd.listVarNames()) {
        if (isFirst) isFirst = false;
        else builder.append(", ");
        builder.append(var.name);
        Pair<Integer> pair = toPair(var.name);
        if (isMarked(pair)) builder.append('*');
      }
      builder.append(']');
      return builder.toString();
    }
    
    private static class ValueTable implements Comparable<ValueTable> {
      public final Pair<Integer> pair;
      public final TreeSet<String> values;
      
      public ValueTable(Pair<Integer> pair, TreeSet<String> values) {
        this.pair = pair;
        this.values = values;
      }
      
      public int compareTo(ValueTable t) {
        return values.size() - t.values.size();
      }
    }
    
    private Partition<ValueTable> getValueTable(Romdd<Boolean> tree) {
      Partition.Builder<ValueTable> builder = Partition.<ValueTable>naturalBuilder();
      for (TerminatedIterator<Pair<Integer>> it = unmarkedIterator(); it.hasNext(); ) {
        Pair<Integer> next = it.next();
        TreeSet<String> values = tree.findChildrenReaching(Stack.makeName(Integer.toString(next.first), Integer.toString(next.second)), true);
        // System.out.println("Possible values for " + next + " = " + values);
        ValueTable table = new ValueTable(next, values);
        builder.add(table);
      }
      return builder.build();
    }

    private void setMarked(int i, int j) {
      setMarked(N*i + j);
    }   
    private void setMarked(int idx) {
      if (!marked[idx]) {
        marked[idx] = true;
        markCount++;
      }
    }
    public boolean isMarked(int i, int j) {
      return marked[N*i + j];
    }
    public boolean isMarked(Pair<Integer> pair) {
      return isMarked(pair.first, pair.second);
    }    
    void initializeMarkings() {  // initializes the markings
      TerminatedIterator<Entry> it = instance.nonZeroIterator();
      while (it.hasNext()) {
        Entry e = it.next();
        setMarked(e.row, e.col);
      }
    }
    
    public boolean verifySolution() {
      for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
          int val = solution[N*i + j];
          for (Pair<Integer> neighbor : instance.getNeighbors(i, j)) {
            if (val == getSolution(neighbor)) return false;
          }
        }
      }
      return true;
    }
    
    public String toString() {
      Formatter format = new Formatter();
      for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
          format.format("%4d", solution[N*i+j]);
        }
        format.format("\n");
      }
      return format.toString();
    }

    /** We treat rows machines and cols as domains here. */
    private Dictionary buildDictionary() {
      Dictionary.Builder builder = new Dictionary.Builder();
      for (int row = 0; row < N; row++) {
        for (int col = 0; col < N; col++) {
          Alphabet.AltBuilder alphaBuilder = new Alphabet.AltBuilder(Integer.toString(row), Integer.toString(col));
          for (int i = 1; i <= N; i++) {
            alphaBuilder.addCharacter(Integer.toString(i));
          }
          builder.add(alphaBuilder.build());
        }
      }
      return builder.build();
    }
  }
}
