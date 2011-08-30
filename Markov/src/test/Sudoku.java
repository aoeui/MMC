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

  public static class Solver {
    public final static DecisionTree<Boolean> TRUE = new DecisionTree.Terminal<Boolean>(true);
    public final static DecisionTree<Boolean> FALSE = new DecisionTree.Terminal<Boolean>(false);
    
    public final Sudoku instance;
    public final Dictionary dict;

    boolean[] marks;
    
    public Solver(Sudoku instance) {
      this.instance = instance;
      this.dict = buildDictionary();
      marks = new boolean[instance.N*instance.N];
    }
    
    public void solve() {
      Romdd<Boolean> tree = initialize();
      
    }
    
    protected void setMarked(int i, int j) {
      marks[instance.N*i + j] = true; 
    }
    
    public boolean isMarked(int i, int j) {
      return marks[instance.N*i + j];
    }
    
    Romdd<Boolean> initialize() {
      Predicate.CollectionBuilder builder = new Predicate.CollectionBuilder(CollectionType.AND);
      for (int i = 0; i < instance.N; i++) {
        for (int j = 0; j < instance.N; j++) {
          int val = instance.get(i,j);
          if (val == 0) continue;
          setMarked(i,j);
          builder.add(new Predicate.Atom(Integer.toString(i), Integer.toString(j), Integer.toString(val)));
        }
      }
      DecisionTree<Boolean> tree = new DecisionTree.Branch<Boolean>(builder.build(), TRUE, FALSE);
      return tree.toRomdd(dict);
    }
    
    /** We treat rows machines and cols as domains here. */
    private Dictionary buildDictionary() {
      Dictionary.Builder builder = new Dictionary.Builder();
      for (int row = 0; row < instance.N; row++) {
        Domain.AltBuilder domBuilder = new Domain.AltBuilder(Integer.toString(row));
        for (int col = 0; col < instance.N; col++) {
          Alphabet.AltBuilder alphaBuilder = new Alphabet.AltBuilder(Integer.toString(row), Integer.toString(col));
          for (int i = 1; i <= instance.N; i++) {
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
