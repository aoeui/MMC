package util;

public class Indenter {
  public final static String INDENT = "  ";
  final String indent;
  StringBuilder builder;
  int level;
  boolean lineIndented;
  
  public Indenter() {
    this(INDENT);
  }
  
  public Indenter(String indent) {
    this.indent = indent;
    builder = new StringBuilder();
    level = 0;
    lineIndented = false;
  }
  
  public Indenter indent() {
    level++;
    return this;
  }
  public Indenter deindent() {
    if (level <= 0) throw new RuntimeException();
    level--;
    return this;
  }
  
  public Indenter print(Object o) {
    return print(o == null ? "null" : o.toString());
  }
  
  public Indenter print(String str) {
    if (str == null) throw new RuntimeException();

    String[] lines = str.split("\n", -1);

    if (lines[0].length() > 0) {
      if (!lineIndented) {
        addIndent();
        lineIndented = true;
      }
      builder.append(lines[0]);
    }  // it is possible to fall through with lineIndented = false when line[0] has length 0
    for (int i = 1; i < lines.length; i++) {
      builder.append('\n');
      if (lines[i].length() > 0) {
        addIndent();
        builder.append(lines[i]);
      }
      if (i == lines.length-1) {
        lineIndented = (lines[i].length() > 0);
      }
    }
    return this;
  }
  
  public Indenter println(Object o) {
    println(o.toString());
    return this;
  }
  
  public Indenter println(String str) {
    print(str);
    builder.append('\n');
    lineIndented = false;
    return this;
  }
  
  public Indenter println() {
    builder.append('\n');
    lineIndented = false;
    return this;
  }
  
  private void addIndent() {
    for (int i = 0; i < level; i++) {
      builder.append(indent);
    }
  }
  
  public String toString() {
    return builder.toString();
  }
}
