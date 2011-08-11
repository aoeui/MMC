package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import markov.TransitionMatrix;
import markov.Lumping;
import markov.FractionProbability;

import static markov.FractionProbability.ZERO;
import static markov.FractionProbability.ONE;
import static markov.FractionProbability.HALF;

public class TestLumping {
  public static void main(String[] args) {

    TransitionMatrix.Builder<FractionProbability> builder =
        TransitionMatrix.<FractionProbability>create(8);

    builder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ONE, ZERO, ZERO, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, HALF, HALF);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE, ZERO);
    builder.addRow(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ONE);

    TransitionMatrix<FractionProbability> matrix = builder.build();

    ArrayList<List<Integer>> lumps = new ArrayList<List<Integer>>();
    lumps.add(Arrays.asList(0,1,2,3,4,5,6,7));

    Lumping<FractionProbability> lumper =
        new Lumping<FractionProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);

    lumps.clear();
    lumps.add(Arrays.asList(1,4));
    lumps.add(Arrays.asList(0,2,5,7));
    lumps.add(Arrays.asList(3));
    lumps.add(Arrays.asList(6));

    lumper = new Lumping<FractionProbability>(matrix, lumps);
    lumper.runLumping();
    System.out.println(lumper);
  }
}
