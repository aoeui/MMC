package util;

public class Joiner {
  public static String join(Iterable<?> gen, String separator) {
    StringBuilder builder = new StringBuilder();
    appendJoin(builder, gen, separator);
    return builder.toString();
  }
  
  public static void appendJoin(StringBuilder builder, Iterable<?> gen, String separator) {
    boolean isFirst = true;
    for (Object obj : gen) {
      if (isFirst) isFirst = false;
      else builder.append(separator);
      builder.append(obj);
    }
  }
}
