package util;

import java.util.Iterator;

public class Joiner {
  public final static String DEFAULT_SEPARATOR = ", ";

  public static String join(Iterator<?> iterator) {
    return join(iterator, DEFAULT_SEPARATOR);
  }
  
  public static String join(Iterable<?> gen) {
    return join(gen.iterator());
  }
  
  public static String join(Iterator<?> iterator, String separator) {
    StringBuilder builder = new StringBuilder();
    appendJoin(builder, iterator, separator);
    return builder.toString();    
  }
  
  public static String join(Iterable<?> gen, String separator) {
    return join(gen.iterator(), separator);
  }
  
  public static void appendJoin(StringBuilder builder, Iterable<?> gen) {
    appendJoin(builder, gen, DEFAULT_SEPARATOR);
  }
  
  public static void appendJoin(StringBuilder builder, Iterator<?> iterator) {
    appendJoin(builder, iterator, DEFAULT_SEPARATOR);
  }
  
  public static void appendJoin(StringBuilder builder, Iterator<?> iterator, String separator) {
    boolean isFirst = true; 
    while (iterator.hasNext()) {
      if (isFirst) isFirst = false;
      else builder.append(separator);
      builder.append(iterator.next());
    }    
  }
  
  public static void appendJoin(StringBuilder builder, Iterable<?> gen, String separator) {
    appendJoin(builder, gen.iterator(), separator);
  }
}
