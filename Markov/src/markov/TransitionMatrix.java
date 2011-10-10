package markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
* This class provides transition matrices with states indexed
* from 0 to N-1.
*/
public class TransitionMatrix<T extends Probability<T>> {
  public final static String SEPARATOR = ", ";
  public final int N;
  private final ArrayList<HashMap<Integer,T>> data;
  
  public final T zeroInstance;

  TransitionMatrix(int n, List<HashMap<Integer,T>> input, T zero) {
    this.N = n;
    this.data = new ArrayList<HashMap<Integer,T>>(input);
    this.zeroInstance = zero;
  }

  public T get(int row, int col) {
    T rv = data.get(row).get(col);
    return rv == null ? zeroInstance : rv;
  }
  
  public static <T extends Probability<T>> Builder<T> create(int n, T zeroInstance) {
    return new Builder<T>(n, zeroInstance);
  }
  
  public String toString() {
    int[] widths = new int[N];
    // Two passes needed. First one calculates the column widths.
    for (int row = 0; row < N; row++) {
      for (int col = 0; col < N; col++) {
        widths[col] = Math.max(widths[col], get(row,col).toString().length());
      }
    }
    StringBuilder builder = new StringBuilder();
    for (int row = 0; row < N; row++) {
      if (row != 0) builder.append('\n');
      builder.append("[ ");
      for (int col = 0; col < N; col++) {
        String str = get(row,col).toString();
        builder.append(str);
        if (col < N-1) {
          builder.append(SEPARATOR);
        }
        for (int i = 0; i < (widths[col] - str.length()); i++) {
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
    List<HashMap<Integer,T>> rows;
    final T zeroInstance;

    public AbstractBuilder(int n, T zeroInstance) {
      this.N = n;
      rows = Collections.synchronizedList(new ArrayList<HashMap<Integer,T>>());
      this.zeroInstance = zeroInstance;
    }

    public TransitionMatrix<T> build() {
      if (rows.size() != N) {
        throw new BuildException("Wrong number of rows n = " + N + " != " + rows.size());
      }
      for (int i = 0; i < N; i++) {
        HashMap<Integer,T> row = rows.get(i);
        // Check that the row elements sum to 1.
        T accu = zeroInstance;
        for (T prob : row.values()) {
          accu = accu.sum(prob);
        }
        if (!accu.isOne()) throw new BuildException("Probabilities of row " + i + " don't sum to 1: " + accu);
      }
      return new TransitionMatrix<T>(N, rows, zeroInstance);
    }
    
    public TransitionMatrix<T> buildNoCheck() {
      if (rows.size() != N) {
        throw new BuildException("Wrong number of rows n = " + N + " != " + rows.size());
      }
      return new TransitionMatrix<T>(N, rows, zeroInstance);
    }
  }
  
  // This is multithread safe.
  public static class RandomBuilder<T extends Probability<T>> extends AbstractBuilder<T> {
    public RandomBuilder(int n, T zeroInstance) {
      super(n, zeroInstance);
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
      HashMap<Integer, T> newRow = new HashMap<Integer,T>();
      for (int i = 0; i < row.length; i++) {
        if (row[i].isZero()) continue;
        newRow.put(i, row[i]);
      }
      rows.set(rowNum, newRow);
    }

    public void set(int rowNum, List<T> row){
      if (rowNum >= N) {
        throw new BuildException("Out of bounds row number.");
      }
      if (row.size() != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      HashMap<Integer, T> newRow = new HashMap<Integer,T>();
      for (int i = 0; i < row.size(); i++) {
        if (row.get(i).isZero()) continue;
        newRow.put(i, row.get(i));
      }
      rows.set(rowNum, newRow);
    }
  }

  public static class Builder<T extends Probability<T>> extends AbstractBuilder<T> {
    public Builder(int n, T zeroInstance) {
      super(n, zeroInstance);
    }
    
    public void addRow(T... row) {
      if (rows.size() >= N) {
        throw new BuildException(
            "Attempted to add too many rows to TransitionMatrix.");
      }
      if (row.length != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      HashMap<Integer, T> newRow = new HashMap<Integer,T>();
      for (int i = 0; i < row.length; i++) {
        if (row[i].isZero()) continue;
        newRow.put(i, row[i]);
      }
      rows.add(newRow);
    }

    public void addRow(List<T> row) {
      if (rows.size() >= N) {
        throw new BuildException(
            "Attempted to add too many rows to TransitionMatrix.");
      }
      if (row.size() != N) {
        throw new BuildException("Wrong number of elements in row.");
      }
      HashMap<Integer, T> newRow = new HashMap<Integer,T>();
      for (int i = 0; i < row.size(); i++) {
        if (row.get(i).isZero()) continue;
        newRow.put(i, row.get(i));
      }
      rows.add(newRow);
    }

    /** Returns the current number of rows in the builder. */
    public int getRows() { return rows.size(); }
  }
}
