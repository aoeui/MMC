package test;

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
        TransitionMatrix.<DoubleProbability>create(8, DoubleProbability.ZERO);

    mBuilder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, HALF, HALF);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO);
    mBuilder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE);

    TransitionMatrix<DoubleProbability> matrix = mBuilder.build();

    Partition<Integer> lumps = Partition.createFromCategories(0,0,0,0,0,0,0,0);

    Lumping<DoubleProbability> lumper = new Lumping<DoubleProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);
    System.out.println(lumper.getPartition());

    lumps = Partition.createFromCategories(1,0,1,2,0,1,3,1);

    lumper = new Lumping<DoubleProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);
    System.out.println(lumper.getPartition());
  }
}
