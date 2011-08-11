package markov;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
* This class provides transition matrices with states indexed
* from 0 to N-1.
*/
public class TransitionMatrix<T extends Probability<T>> {
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

  public static class BuildException extends RuntimeException {
	private static final long serialVersionUID = 6822878477446637499L;

	public BuildException(String str) { super(str); }
  }

  public static class Builder<T extends Probability<T>> {
    final int n;
    ArrayList<ArrayList<T>> rows;

    public Builder(int n) {
      this.n = n;
      rows = new ArrayList<ArrayList<T>>();
    }

    public void addRow(T... row) {
      if (rows.size() >= n) {
        throw new BuildException(
            "Attempted to add too many rows to TransitionMatrix.");
      }
      if (row.length != n) {
        throw new BuildException("Wrong number of elements in row.");
      }
      rows.add(new ArrayList<T>(Arrays.asList(row)));
    }

    public void addRow(List<T> row) {
      if (rows.size() >= n) {
        throw new BuildException(
            "Attempted to add too many rows to TransitionMatrix.");
      }
      if (row.size() != n) {
        throw new BuildException("Wrong number of elements in row.");
      }
      rows.add(new ArrayList<T>(row));
    }

    /** Returns the current number of rows in the builder. */
    public int getRows() { return rows.size(); }

    public TransitionMatrix<T> build() {
      if (rows.size() != n) {
        throw new BuildException("Wrong number of rows n = "
            + n + " != " + rows.size());
      }
      for (int i = 0; i < n; i++) {
        ArrayList<T> row = rows.get(i);
        if (row.size() != n) {
          throw new BuildException("Row " + i + " has size "
              + row.size() + " != " + n);
        }
      }
      ArrayList<T> data = new ArrayList<T>();
      for (ArrayList<T> row : rows) {
        data.addAll(row);
      }
      return new TransitionMatrix<T>(n, data);
    }
  }
}
