package util;

import java.util.Iterator;

public interface TerminatedIterator<T> extends Iterator<T> {
  public void end();
}
