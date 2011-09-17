package markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Random;

import util.IndexGenerator;
import util.MajorityVote;
import util.Partition;

public class Lumping<T extends Probability<T>> {
  TransitionMatrix<T> matrix;
  final int N;

  /** Array of states. States belonging to the same block are adjacent */
  ArrayList<Integer> elems;

  // State indexed arrays
  int[] stateToIdx;  // Maps states to their position in elems.
  int[] stateToBlock;  // state -> block

  // Number of blocks is not static
  ArrayList<BlockInfo> blocks;

  // Non-static inner class: may access members of enclosing class directly.
  public class BlockInfo implements Iterable<Integer> {
    int start;  // index of first element
    int end;  // index of last element
    int border;  // index of first marked element (if > end, then none)
    boolean isUnprocessed = false;  // used to flag blocks in unprocessed

    public BlockInfo(int start, int end) {
      this.start = start;
      this.end = end;
      this.border = end+1;
    }

    /** Marks the state associated with the given index. */
    public void mark(int idx) {
      if (idx != border-1) {
        int unmarked = elems.get(border-1);
        int toMark = elems.get(idx);

        elems.set(border-1, toMark);
        elems.set(idx, unmarked);
        
        stateToIdx[unmarked] = idx;
        stateToIdx[toMark] = border-1; 
      }
      border--;
    }
    public boolean hasMarked() { return border <= end; }
    public boolean hasUnmarked() { return border > start;}
    public boolean isEmpty() { return end < start; }
    public BlockInfo split() {
      if (!(hasMarked() && hasUnmarked())) return null;
      BlockInfo rv = new BlockInfo(border, end);
      blocks.add(rv);
      for (int i = border; i <= end; i++) {
        stateToBlock[elems.get(i)] = blocks.size()-1;
      }
      end = border-1;
      return rv;
    }

    /** Returns an iterator over the states spanned by this block.  */
    public Iterator<Integer> iterator() {
      class BlockIterator implements Iterator<Integer> {
        int next;
        BlockIterator() { next = start; }
        public boolean hasNext() { return next <= end; }
        public Integer next() {
          if (!hasNext()) throw new NoSuchElementException();
          return elems.get(next++);
        }
        public void remove() { throw new UnsupportedOperationException(); }
      }
      return new BlockIterator();
    }
    public int size() { return 1+end-start; }

    public String toString() {
      Formatter f = new Formatter();
      f.format("Block(start = %d, end = %d, border = %d): ", start, end, border);
      for (int i = start; i <= end; i++) {
        if (i != start) { f.format(", "); }
        if (i == border) { f.format("*"); }
        f.format("%d", elems.get(i));
      }
      return f.toString();
    }
  }

  public boolean verifyStateToIdx() {
	for (int i = 0; i < elems.size(); i++) {
	  if (stateToIdx[elems.get(i)] != i) {
		return false;
	  }
	}
	for (int i = 0; i < blocks.size(); i++) {
	  BlockInfo b = blocks.get(i);
	  for (int j = b.start; j <= b.end; j++) {
	    if (stateToBlock[elems.get(j)] != i) {
	      return false;
	    }
	  }
	}
	return true;
  }

  public Lumping(TransitionMatrix<T> matrix, Partition<Integer> partition) {
    this.matrix = matrix;
    this.N = matrix.N;
    elems = new ArrayList<Integer>(Collections.<Integer>nCopies(N, null));
    stateToIdx = new int[N];
    stateToBlock = new int[N];
    blocks = new ArrayList<BlockInfo>();
    int idx = 0;
    for (int i = 0; i < partition.getNumBlocks(); i++) {
      Partition<Integer>.Block block = partition.getBlock(i);
      blocks.add(new BlockInfo(idx, idx+block.size()-1));
      for (int j = 0; j < block.size(); j++) {
        int state = block.get(j);
        elems.set(idx, state);
        stateToIdx[state] = idx;
        stateToBlock[state] = i;
        idx++;
      }
    }
    assert(idx == N);
  }

  public void runLumping(Random rng) {
    final ArrayList<T> w = new ArrayList<T>();  // weights indexed by state
    for (int i = 0; i < N; i++) { w.add(null); }

    ArrayList<BlockInfo> unprocessed = new ArrayList<BlockInfo>(blocks);
    for (BlockInfo b : unprocessed) { b.isUnprocessed = true; }
    LinkedList<BlockInfo> touchedBlocks = new LinkedList<BlockInfo>();

    while (unprocessed.size() > 0) {
      assert(verifyStateToIdx());
      BlockInfo next = unprocessed.remove(rng.nextInt(unprocessed.size()));
      next.isUnprocessed = false;
      
      LinkedList<Integer> touchedStates = new LinkedList<Integer>();

      // Compute probability of reaching block 'next' from each state s.
      for (int i = next.start; i <= next.end; i++) {
        int dest = elems.get(i);
        for (int s = 0; s < N; s++) {
          T prob = matrix.get(s, dest);
          if (prob == null || prob.isZero()) continue;
          T curr = w.get(s);
          if (curr == null) {
            touchedStates.add(s);
            w.set(s, prob);
          } else {
            w.set(s, curr.sum(prob));
          }
        }
      }
      assert(verifyStateToIdx());

      // Add blocks reaching next to touchedBlocks and mark the states.
      for (int s : touchedStates) {
        if (w.get(s).isZero()) continue;  // can't be null since s is touched
        BlockInfo b = blocks.get(stateToBlock[s]);
        if (!b.hasMarked()) { touchedBlocks.add(b); }
        b.mark(stateToIdx[s]);
        assert(verifyStateToIdx());
      }

      while (!touchedBlocks.isEmpty()) {
        BlockInfo b = touchedBlocks.pop();
        BlockInfo b1 = null;
        if (b.hasUnmarked()) {  // b.hasMarked() implied by being in touched
          b1 = b.split();
        } else {
          b1 = b;
          b1.border = b1.end+1;
        }
        assert(verifyStateToIdx());
        // do possible majority candidate on w[s] for s in B1
        T candidate = MajorityVote.vote(new IndexGenerator<T>(w, b1));
        // Split B1 to B1 + B2 based on candidate
        for (int i = b1.start; i < b1.border; i++) {
          int state = elems.get(i);
          if (w.get(state).compareTo(candidate) != 0) {
            boolean swapped = (i != b1.border-1);
            b1.mark(i);
            if (swapped) { i--; }  // need to repeat if a swap was performed
          }
        }
        assert(verifyStateToIdx());
        if (b1.hasMarked()) {  // b1 certainly has unmarked because of pmc
          BlockInfo b2 = b1.split();
          // sort and partition B2 based on w[s]
          Collections.sort(elems.subList(b2.start, b2.end),
              new Comparator<Integer>() {
                public int compare(Integer v1, Integer v2) {
                  return w.get(v1).compareTo(w.get(v2));
                }});
          // Since elems rearranged by sorting, need to reindex
          for (int i = b2.start; i <= b2.end; i++) {
            stateToIdx[elems.get(i)] = i;
          }
          assert(verifyStateToIdx());
          LinkedList<BlockInfo> partition = new LinkedList<BlockInfo>();
          recurseSplitByWeight(partition, w, b2);
          assert(verifyStateToIdx());
          if (b.isUnprocessed) {
            if (b1 != b) { partition.add(b1); }
            for (BlockInfo bi : partition) {
              unprocessed.add(bi);
              bi.isUnprocessed = true;
            }
          } else {
            if (b1 != b) { partition.add(b); }
            partition.add(b1);
            int maxIdx = 0;
            int maxSize = partition.get(0).size();
            // find max size block
            for (int i = 1; i < partition.size(); i++) {
              int sizeI = partition.get(i).size();
              if (maxSize < sizeI) {
                maxIdx = i;
                maxSize = sizeI;
              }
            }
            // add all but max size block to unprocessed
            for (int i = 0; i < partition.size(); i++) {
              if (i == maxIdx) continue;
              BlockInfo add = partition.get(i);
              add.isUnprocessed = true;
              unprocessed.add(add);
            }
          }
        } else { // only one weight to next occurs in b1
          if (b1 != b) {
            BlockInfo add = (b.isUnprocessed || b1.size() <= b.size())? b1 : b;
            add.isUnprocessed = true;
            unprocessed.add(add);
          }
        }
      }
      for (int s : touchedStates) { w.set(s, null); }
    }
  }

  public void recurseSplitByWeight(LinkedList<BlockInfo> accu,
      ArrayList<T> w, BlockInfo current) {
    if (current.isEmpty()) return;

    T weight = w.get(elems.get(current.start));
    int next = current.start+1;
    boolean found = false;
    while (next <= current.end && !found) {
      if (w.get(elems.get(next)).compareTo(weight) == 0) {
        next++;
      } else {
        found = true;
      }
    }
    if (found) {
      current.border = next;
      BlockInfo nextBlock = current.split();
      accu.add(current);
      recurseSplitByWeight(accu, w, nextBlock);  // tail recursion?
    } else {
      accu.add(current);
    }
  }

  public void runLumping() {
    runLumping(new Random(0));
  }
  
  public Partition<Integer> getPartition() {
    final int[] category = new int[elems.size()];
    for (int blockNum = 0; blockNum < blocks.size(); blockNum++) {
      BlockInfo b = blocks.get(blockNum);
      for (int i = b.start; i <= b.end; i++) {
        category[elems.get(i)] = blockNum;
      }
    }
    return Partition.createFromCategories(category);
  }

  public String toString() {
    Formatter f = new Formatter();
    boolean isFirst = true;
    for (BlockInfo b : blocks) {
      if (isFirst) {isFirst = false;}
      else { f.format(", "); }
      f.format("{");
      for (int i = b.start; i <= b.end; i++) {
        if (i != b.start) f.format(", ");
        if (i == b.border) f.format("*");
        f.format("%d", elems.get(i));
      }
      f.format("}");
    }
    return f.toString();
  }
}
