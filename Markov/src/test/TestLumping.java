package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import markov.TransitionMatrix;
import markov.Lumping;
import markov.DoubleProbability;

import static markov.DoubleProbability.ZERO;
import static markov.DoubleProbability.ONE;
import static markov.DoubleProbability.HALF;

public class TestLumping {
  public static void main(String[] args) {

    TransitionMatrix.Builder<DoubleProbability> builder =
        TransitionMatrix.<DoubleProbability>create(8);

    builder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, HALF, HALF);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE);

    TransitionMatrix<DoubleProbability> matrix = builder.build();

    ArrayList<List<Integer>> lumps = new ArrayList<List<Integer>>();
    lumps.add(Arrays.asList(0,1,2,3,4,5,6,7));

    Lumping<DoubleProbability> lumper =
        new Lumping<DoubleProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);

    lumps.clear();
    lumps.add(Arrays.asList(1,4));
    lumps.add(Arrays.asList(0,2,5,7));
    lumps.add(Arrays.asList(3));
    lumps.add(Arrays.asList(6));

    lumper = new Lumping<DoubleProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);
  }
}
