package test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import markov.Alphabet;
import markov.DecisionTree;
import markov.Dictionary;
import markov.Domain;
import markov.Predicate;
import markov.Predicate.CollectionType;
import markov.Romdd;

import util.Pair;
import util.Stack;

public class Sudoku {
  public final int N;
  public final int root;

  private int[] puzzle;
  
  private Sudoku(int N, int root, int[] puzzle) {
    this.N = N;
    this.root = root;
    this.puzzle = new int[N*N];
    System.arraycopy(puzzle, 0, this.puzzle, 0, N*N);
  }
  
  public int get(int row, int col) {
    if (row < 0 || row >= N || col < 0 || col >= N) throw new ArrayIndexOutOfBoundsException();
    return puzzle[row*N+col];
  }
  
  public int get(Pair<Integer> pair) {
    return get(pair.first, pair.second);
  }
  
  public int get(int idx) {
    return puzzle[idx];
  }
  
  public List<Pair<Integer>> getNeighbors(int row, int col) {
    ArrayList<Pair<Integer>> neighbors = new ArrayList<Pair<Integer>>();
    Pair<Integer> blockInfo = fromIdxToBlockIdx(N, root, row, col);
    for (int i = 0; i < N; i++) {
      if (i != row) neighbors.add(new Pair<Integer>(i, col));
      if (i != col) neighbors.add(new Pair<Integer>(row, i));
      if (i != blockInfo.second) neighbors.add(fromBlockIdxToIdx(N, root, blockInfo.first, i));
    }
    return neighbors;
  }
  
  /** Blocks are indexed by block number, offset. Example:
   *  1 | 2  There are sqrt(N) x sqrt(N) blocks labeled thusly.
   * -------  
   *  3 | 4  Each block is internally structured just like overall block structure.
   * */
  public static Pair<Integer> fromIdxToBlockIdx(int N, int root, int row, int col) {
    int blockRow = row / root;
    int blockRowOffset = row % root;
    int blockCol = col / root;
    int blockColOffset = col % root;

    int blockNum = blockRow*root + blockCol;
    int blockOffset = blockRowOffset * root + blockColOffset;
    
    return new Pair<Integer>(blockNum, blockOffset);
  }
  
  public static Pair<Integer> fromBlockIdxToIdx(int N, int root, int blockNum, int offset) {
    int blockRow = blockNum / root;
    int blockCol = blockNum % root;
    int offRow = offset / root;
    int offCol = offset % root;
    
    return new Pair<Integer>(blockRow * root + offRow, blockCol*root + offCol);
  }
  
  public static class Builder {
    public final int N;
    public final int root;
    private int[] puzzle;

    public Builder(int N) {
      this.N = N;
      this.root = (int)Math.sqrt(N);
      if (root*root != N) {
        throw new RuntimeException("Puzzle size must be a perfect square.");
      }
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
          Pair<Integer> pair = fromBlockIdxToIdx(N, root, row, col);
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
      return new Sudoku(N, root, puzzle);
    }
  }
  
  public static class ValidationException extends RuntimeException {
    private static final long serialVersionUID = -4291372421542506125L;}
  
  public static class AndOp implements Romdd.Op<Boolean> {
    public final static AndOp INSTANCE = new AndOp();
    
    private AndOp() {}

    public Boolean apply(Boolean v1, Boolean v2) { return v1 && v2; }

    public boolean isDominant(Boolean value) { return !value; }
  }
  public static class OrOp implements Romdd.Op<Boolean> {
    public final static OrOp INSTANCE = new OrOp();
    
    private OrOp() {}

    public Boolean apply(Boolean v1, Boolean v2) { return v1 || v2; }

    public boolean isDominant(Boolean value) { return value; }
  }

  public static class Solver {
    public final static DecisionTree<Boolean> TRUE = new DecisionTree.Terminal<Boolean>(true);
    public final static DecisionTree<Boolean> FALSE = new DecisionTree.Terminal<Boolean>(false);
    
    public final int N;
    public final Sudoku instance;
    public final Dictionary dict;

    boolean[] marked;
    int markCount;
    Stack<Romdd<Boolean>> stack;
    
    public Solver(Sudoku instance) {
      this.N = instance.N;
      this.instance = instance;
      this.dict = buildDictionary();
      marked = new boolean[instance.N*instance.N];
      initialize();
      stack = Stack.<Romdd<Boolean>>emptyInstance().push(TRUE.toRomdd(dict));
    }
    
    public void solve() {
      while (markCount < N*N-1) {  // we should be able to read the value of the last node
        Integer fixed = findDetermined();
        if (fixed != null) {
          eliminate(fixed / N, fixed % N);
        } else {
          Pair<Integer> opt = findBestCandidate();
          eliminate(opt.first, opt.second);
        }
      }
    }
    
    // Instantiates all neighboring constraints and then eliminates the given variable.
    public void eliminate(int row, int col) {
      if (isMarked(row, col)) throw new RuntimeException();
      
      Predicate.CollectionBuilder builder = new Predicate.CollectionBuilder(CollectionType.AND);
      for (Pair<Integer> neighbor : instance.getNeighbors(row, col)) {
        int assigned = instance.get(neighbor);
        if (assigned == 0) {
          // conventional handling of constraints
          for (int i = 1; i <= N; i++) {
            Predicate.CollectionBuilder termBuilder = new Predicate.CollectionBuilder(CollectionType.AND);
            termBuilder.add(new Predicate.Atom(Integer.toString(row), Integer.toString(col), Integer.toString(i)));
            termBuilder.add(new Predicate.Atom(neighbor.first.toString(), neighbor.second.toString(), Integer.toString(i)));
            builder.add(new Predicate.Neg(termBuilder.build()));
          }
        } else {
          // instantiated constraints
          builder.add(new Predicate.Neg(new Predicate.Atom(Integer.toString(row), Integer.toString(col), Integer.toString(assigned))));
        }
      }
      Romdd<Boolean> newConstraints = (new DecisionTree.Branch<Boolean>(builder.build(), TRUE, FALSE)).toRomdd(dict);
      stack = stack.push(Romdd.<Boolean>apply(AndOp.INSTANCE, newConstraints, stack.head()).sum(OrOp.INSTANCE, row + "." + col));
    }
    
    public Pair<Integer> findBestCandidate() {
      Pair<Integer> rv = null;
      int minMarkedNeighbors = Integer.MAX_VALUE;
      
      for (int row = 0; row < N; row++) {
        for (int col = 0; col < N; col++) {
          int count = 0;
          for (Pair<Integer> neighbor : instance.getNeighbors(row, col)) {
            if (!isMarked(neighbor)) {
              count++;
            }
          }
          if (count < minMarkedNeighbors) {
            rv = new Pair<Integer>(row, col);
          }
        }
      }
      return rv;
    }

    private Integer findDetermined() {
      for (int row = 0; row < N; row++) {
        for (int col = 0; col < N; col++) {
          if (isMarked(row,col)) continue;
          
          if (countPossibleValues(row, col, stack.head()) == 1) {
            return N*row+col;
          }
        }
      }
      return null;
    }
    
    // this could be an expensive operation
    private int countPossibleValues(int row, int col, Romdd<Boolean> tree) {
      return tree.findChildrenReaching(row + "." + col, true).size();
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
    
    void initialize() {  // initializes the markings
      for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
          int val = instance.get(i,j);
          if (val == 0) continue;
          setMarked(i,j);
        }
      }
    }
    
    /** We treat rows machines and cols as domains here. */
    private Dictionary buildDictionary() {
      Dictionary.Builder builder = new Dictionary.Builder();
      for (int row = 0; row < N; row++) {
        Domain.AltBuilder domBuilder = new Domain.AltBuilder(Integer.toString(row));
        for (int col = 0; col < N; col++) {
          Alphabet.AltBuilder alphaBuilder = new Alphabet.AltBuilder(Integer.toString(row), Integer.toString(col));
          for (int i = 1; i <= N; i++) {
            alphaBuilder.addCharacter(Integer.toString(i));
          }
          domBuilder.add(alphaBuilder.build());
        }
        builder.add(domBuilder.build());
      }
      return builder.build();
    }
  }
}
