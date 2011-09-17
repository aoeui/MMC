package markov;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
* This class provides transition matrices with states indexed
* from 0 to N-1.
*/
public class TransitionMatrix<T extends Probability<T>> {
  public final static String SEPARATOR = ", ";
  public final int N;
  private final ArrayList<T> data;

  TransitionMatrix(int n, ArrayList<T> input) {
    this.N = n;
    this.data = input;
  }

  public T get(int row, int col) {
    return data.get(row*N + col);
  }
  
  public static <T extends Probability<T>> Builder<T> create(int n) {
    return new Builder<T>(n);
  }
  
  public String toString() {
    int[] widths = new int[N];
    String[][] strings = new String[N][];
    for (int i = 0; i < N; i++) {
      strings[i] = new String[N];
    }
    // Two passes needed. First one calculates the column widths.
    for (int row = 0; row < N; row++) {
      for (int col = 0; col < N; col++) {
        strings[row][col] = get(row, col).toString();
        widths[col] = Math.max(widths[col], strings[row][col].length());
      }
    }
    StringBuilder builder = new StringBuilder();
    for (int row = 0; row < N; row++) {
      if (row != 0) builder.append('\n');
      builder.append("[ ");
      for (int col = 0; col < N; col++) {
        builder.append(strings[row][col]);
        if (col < N-1) {
          builder.append(SEPARATOR);
        }
        for (int i = 0; i < (widths[col] - strings[row][col].length()); i++) {
          builder.append(' ');
        }
      }
      builder.append(" ]");
    }
    return builder.toString();
  }

  public static class BuildException extends RuntimeException {
	  private static final long serialVersionUID = 6822878477446637499L;

	  public BuildException(String str) { super(str); }
  }

  static class AbstractBuilder<T extends Probability<T>> {
    public final int N;
    ArrayList<ArrayList<T>> rows;

    public AbstractBuilder(int n) {
      this.N = n;
      rows = new ArrayList<ArrayList<T>>();
    }

    public TransitionMatrix<T> build() {
      if (rows.size() != N) {
        throw new BuildException("Wrong number of rows n = " + N + " != " + rows.size());
      }
      for (int i = 0; i < N; i++) {
        ArrayList<T> row = rows.get(i);
        if (row.size() != N) {
          throw new BuildException("Row " + i + " has size " + row.size() + " != " + N);
        }
        // Check that the row elements sum to 1.
        T prob = row.get(0);
        for (int dest = 1; dest < N; dest++) {
          prob = prob.sum(row.get(dest));
        }
        if (!prob.isOne()) throw new BuildException("Probabilities of row " + i + " don't sum to 1.");
      }
      ArrayList<T> data = new ArrayList<T>();
      for (ArrayList<T> row : rows) {
        data.addAll(row);
      }
      return new TransitionMatrix<T>(N, data);
    }
  }
  
  public static class RandomBuilder<T extends Probability<T>> extends AbstractBuilder<T> {
    public RandomBuilder(int n) {
      super(n);
      for (int i = 0; i < N; i++) {
        rows.add(null);
      }
    }

    public void set(int rowNum, T... row){
      if (rowNum >= N) {
        throw new BuildException("Out of bounds row number.");
      }
      if (row.length != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      if (rows.get(rowNum)!=null) rows.remove(rowNum);
      rows.set(rowNum, new ArrayList<T>(Arrays.asList(row)));
      
    }

    public void set(int rowNum, List<T> row){
      if (rowNum >= N) {
        throw new BuildException("Out of bounds row number.");
      }
      if (row.size() != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      if (rows.get(rowNum)!=null) rows.remove(rowNum);
      rows.set(rowNum, new ArrayList<T>(row));
    }
  }

  public static class Builder<T extends Probability<T>> extends AbstractBuilder<T> {
    public Builder(int n) {
      super(n);
    }
    
    public void addRow(T... row) {
      if (rows.size() >= N) {
        throw new BuildException(
            "Attempted to add too many rows to TransitionMatrix.");
      }
      if (row.length != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      rows.add(new ArrayList<T>(Arrays.asList(row)));
    }

    public void addRow(List<T> row) {
      if (rows.size() >= N) {
        throw new BuildException(
            "Attempted to add too many rows to TransitionMatrix.");
      }
      if (row.size() != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      rows.add(new ArrayList<T>(row));
    }

    /** Returns the current number of rows in the builder. */
    public int getRows() { return rows.size(); }
  }
}
