package test;

import java.util.Comparator;

import markov.TransitionMatrix;
import markov.Lumping;
import markov.DoubleProbability;
import util.Partition;

import static markov.DoubleProbability.ZERO;
import static markov.DoubleProbability.ONE;
import static markov.DoubleProbability.HALF;

public class TestLumping {
  public static void main(String[] args) {

    TransitionMatrix.Builder<DoubleProbability> mBuilder =
        TransitionMatrix.<DoubleProbability>create(8);

    mBuilder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, HALF, HALF);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE);

    TransitionMatrix<DoubleProbability> matrix = mBuilder.build();

    Partition.Builder<Integer> lumpBuilder = new Partition.Builder<Integer>(new Comparator<Integer>() {
      public int compare(Integer v1, Integer v2) { return 0; }
    });
    for (int i = 0; i < 8; i++) {
      lumpBuilder.add(i);
    }
    Partition<Integer> lumps = lumpBuilder.build();

    Lumping<DoubleProbability> lumper = new Lumping<DoubleProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);

    final int[] category = new int[] {1,0,1,2,0,1,3,1};
    lumpBuilder = new Partition.Builder<Integer>(new Comparator<Integer>() {
      public int compare(Integer v1, Integer v2) {
        return category[v1] - category[v2];
      }
    });
    for (int i = 0; i < 8; i++) {
      lumpBuilder.add(i);
    }
    lumps = lumpBuilder.build();

    lumper = new Lumping<DoubleProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);
  }
}
