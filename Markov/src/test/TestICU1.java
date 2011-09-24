package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLDouble;


import markov.AggregateMachine;
import markov.AggregateNet;
import markov.DoubleProbability;
import markov.Machine;
import markov.MonteCarlo;
import markov.Net;
import markov.ResultTree;
import markov.SymbolicProbability;
import markov.TransitionMatrix;
import parser.XmlParser;
import util.Stack;

public class TestICU1 {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion6.xml";
  public final static int NUM_PATIENTS = 4;
  public final static int STEPS=10000000;
  

  public static void main(String[] args) {
    Net<DoubleProbability> net= (new XmlParser(PATIENT_MODEL_FILENAME,NUM_PATIENTS)).net;

    Stack<String> stack= Stack.<String>emptyInstance();
    
    Iterator<Machine<DoubleProbability>> itr=net.iterator();
    while(itr.hasNext()){
      stack=stack.push(itr.next().name);
    }
    AggregateNet<DoubleProbability> aNet=new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);

    System.out.println(aNet);
  
    for (int i = aNet.size()-1; i >= 1; i--) {
      aNet = aNet.multiply(i-1, i);
      System.out.println("\nAfter multiplying p" + i);
      if(stack.head().contains("Dispatch")){
        aNet = aNet.reduce(i-1);
      }else if (stack.head().contains("ICP")){
        aNet = aNet.sum(stack.head(),"ICP").sum(stack.head(),"Patient").sum(stack.head(),"Cost").reduce(i-1);
      }else{
        aNet = aNet.sum(stack.head(),"Cost").reduce(i-1);
      }
      stack=stack.tail();
    }

    AggregateMachine<DoubleProbability> machine = aNet.getMachine(0);
    TransitionMatrix<SymbolicProbability<DoubleProbability>> prob = machine.computeTransitionMatrix();
    double[][] matrix = new double[prob.N][prob.N];
    for (int i = 0; i < prob.N; i++) {
      for (int j = 0; j < prob.N; j++) {
        matrix[i][j]=prob.get(i,j).value.p;
      }
    }

    MLDouble mldouble=new MLDouble("pTransition",matrix);
    String[] heads=new String[aNet.getMachine(0).getNumStates()];
    for (int i=0;i<aNet.getMachine(0).getNumStates();i++){
      heads[i]=aNet.getMachine(0).getState(i).getValueStack().head();
    }
    
    MLCell mlcell=new MLCell("heads", new int[]{heads.length, 1} );
    
    for(int colNum=0;colNum<heads.length;colNum++){
      MLChar temp=new MLChar("text",heads[colNum]);
      mlcell.set(temp, colNum);
    }
    
    //write arrays to file
    ArrayList<MLArray> list = new ArrayList<MLArray>();
    list.add( mldouble );
    list.add( mlcell );
    
    try {
      new MatFileWriter( "mat_file.mat", list );
    } catch (IOException e) {
      e.printStackTrace();
    }
    
/*    Matrix matrix = new Matrix(prob.N, prob.N);
    for (int i = 0; i < prob.N; i++) {
      for (int j = 0; j < prob.N; j++) {
        matrix.set(j, i, prob.get(i,j).value.p);
      }
    }
    EigenvalueDecomposition eig = matrix.eig();
    Matrix eigenVectors = eig.getV();
    double[] stationary = new double[prob.N];
    double sum = 0;
    for (int i = 0; i < prob.N; i++) {
      stationary[i] = eigenVectors.get(i, 0);
      if (i != 0) System.out.print(", ");
      System.out.print(stationary[i]);
      sum += stationary[i];
    }
    for (int i = 0; i < prob.N; i++) {
      stationary[i] /= sum;
    }
    System.out.println("\nsum = " + sum + " eigenvalue = " + eig.getRealEigenvalues()[0] + " + " + eig.getImagEigenvalues()[0] + "i");
    ResultTree rTree = machine.applyStationary(aNet.dict, stationary);
    System.out.println(rTree);

    double[] prod = new double[prob.N];
    for (int i = 0; i < prob.N; i++) {
      double rowSum = 0;
      for (int j = 0; j < prob.N; j++) {
        rowSum += stationary[j] * matrix.get(i, j);
      }
      prod[i] = rowSum;
      if (i != 0) System.out.print(", ");
      System.out.print(prod[i]);
    }*/

    System.out.println("\nTrying Monte Carlo: ");
    try {
      runMonteCarlo(net);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }
  public static void runMonteCarlo(Net<DoubleProbability> netIn) throws Exception {
    AggregateNet<DoubleProbability> net = new AggregateNet<DoubleProbability>(netIn, DoubleProbability.ZERO);
    MonteCarlo mc = new MonteCarlo(net);
    ArrayList<Stack<String>> names = new ArrayList<Stack<String>>();
    names.add(Stack.makeName("Dispatch", "next"));
    ResultTree rv = mc.runQuery(STEPS, names);
    System.out.println(rv.toCountString(STEPS));
  }
}
