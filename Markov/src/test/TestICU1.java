package test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import markov.AggregateMachine;
import markov.AggregateNet;
import markov.DoubleProbability;
import markov.MonteCarlo;
import markov.Net;
import markov.ResultTree;
import markov.SymbolicProbability;
import markov.TransitionMatrix;
import parser.XmlParser;
import util.Stack;

public class TestICU1 {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion6.xml";
  public final static int NUM_PATIENTS = 2;
  public final static int STEPS=10000000;
  

  public static void main(String[] args) {
    Net<DoubleProbability> net= (new XmlParser(PATIENT_MODEL_FILENAME,NUM_PATIENTS)).net;

    System.out.println(net);
    final int nextLabel = net.dictionary.getId(Stack.makeName("Dispatch", "next"));

    AggregateNet<DoubleProbability> aNetOrig=new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);
    AggregateNet<DoubleProbability> aNet=aNetOrig;

    System.out.println(aNet);
  
    for (int i = aNet.size()-1; i >= 1; i--) {
      aNet = aNet.multiply(i-1, i);
      System.out.println("\nAfter multiplying p" + (i-1));
      
      AggregateMachine<?> machine = aNet.getMachine(aNet.size()-1);
      for (Integer val : machine.labels) {
        if (val != nextLabel) {
          aNet = aNet.sum(val);
          System.out.println("Summing out label " + aNet.dict.getName(val));
        }
      }
      /* if (i == 1) {
        HashMap<String,String> relabel = new HashMap<String,String>();
        for (int patientNum = 0; patientNum < NUM_PATIENTS; patientNum++) {
          relabel.put(Integer.toString(patientNum), "0");
        }
        aNet = aNet.relabel(nextLabel, relabel);
      } */
      aNet = aNet.reduce(i-1);
      
      System.out.println(aNet);
    }

    AggregateMachine<DoubleProbability> machine = aNet.getMachine(0);
    TransitionMatrix<SymbolicProbability<DoubleProbability>> prob = machine.computeTransitionMatrix();

    try {
      FileWriter fOut=new FileWriter("matOut.txt");
      for (int i=0;i<aNet.getMachine(0).getNumStates();i++){
        fOut.write(aNet.getMachine(0).getState(i).getValueStack().head()+"\t");
      }
      fOut.write("\n");
      for (int i = 0; i < prob.N; i++) {
        for (int j = 0; j < prob.N; j++) {
          if (!prob.get(i,j).value.isZero()){
            fOut.write((i+1)+"\t"+(j+1)+"\t"+Double.toString(prob.get(i,j).value.p)+"\n");
          }
        }
      }
      fOut.close();
    } catch (IOException e1) {
      e1.printStackTrace();
    }


/*    MLDouble mldouble=new MLDouble("pTransition",sparseMatrix,1);
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
      new MatFileWriter( "mat_file.mat", list);
    } catch (IOException e) {
      e.printStackTrace();
    }*/
    
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
      runMonteCarlo(aNetOrig);
      runMonteCarlo(aNet);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }
  public static void runMonteCarlo(AggregateNet<DoubleProbability> net) throws Exception {
    MonteCarlo mc = new MonteCarlo(net);
    ArrayList<Stack<String>> names = new ArrayList<Stack<String>>();
    names.add(Stack.makeName("Dispatch", "next"));
    ResultTree rv = mc.runQuery(STEPS, names);
    System.out.println(rv.toCountString(STEPS));
  }
}
