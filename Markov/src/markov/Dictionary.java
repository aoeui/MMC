package markov;

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import util.Stack;

public class Dictionary {
  private TreeMap<Stack<String>, Tuple> data = new TreeMap<Stack<String>, Tuple>(Stack.STRING_COMP);
  private Alphabet[] alphabetIdx;
  
  private Dictionary(TreeSet<Alphabet> list) {
    alphabetIdx = list.toArray(new Alphabet[0]);
    for (int i = 0; i < alphabetIdx.length; i++) {
      Alphabet alpha = alphabetIdx[i];
      data.put(alpha.name, new Tuple(alpha, i));
    }
  }
  
  public Stack<String> getName(int id) {
    return alphabetIdx[id].name;
  }
  
  public Alphabet getAlpha(int id) {
    return alphabetIdx[id];
  }
  
  public Alphabet getAlpha(Stack<String> name) {
    return data.get(name).alpha;
  }
  
  public int getId(Stack<String> name) {
    return data.get(name).id;
  }

  public boolean isCompleteSet(Stack<String> name, SortedSet<String> characters) {
    return getAlpha(name).isCharSetEqual(characters); 
  }
  
  public static class Builder {
    TreeSet<Alphabet> list;
    public Builder() {
      this.list = new TreeSet<Alphabet>();
    }
    
    public void add(Alphabet alpha) {
      list.add(alpha);
    }
    
    public Dictionary build() {
      return new Dictionary(list);
    }
  }
  
  private static class Tuple {
    public final Alphabet alpha;
    public final int id;
    
    public Tuple(Alphabet alpha, int id) {
      this.alpha = alpha;
      this.id = id;
    }
  }
}
