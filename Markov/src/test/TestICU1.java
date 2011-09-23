package test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import markov.AggregateMachine;
import markov.AggregateNet;
import markov.DecisionTree;
import markov.DoubleProbability;
import markov.Machine;
import markov.MonteCarlo;
import markov.Net;
import markov.Predicate;
import markov.ResultTree;
import markov.Simulation;
import markov.State;
import markov.SymbolicProbability;
import markov.TransitionMatrix;
import markov.TransitionVector;
import markov.DecisionTree.Terminal;
import parser.XmlParser;
import util.Stack;

public class TestICU1 {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion4.xml";
  public final static int NUM_PATIENTS = 3;
  public final static int STEPS=10000000;
  

  public static void main(String[] args) {
    Net<DoubleProbability> net=XmlParser.XmlInput(PATIENT_MODEL_FILENAME,NUM_PATIENTS);

    Net.Builder<DoubleProbability> netBuild=new Net.Builder<DoubleProbability>();
    Machine<DoubleProbability> dischargeModel=Simulation.constructDischargeModel(NUM_PATIENTS,net);
    Machine<DoubleProbability> arrivalModel=Simulation.constructArrivalModel(NUM_PATIENTS, new DoubleProbability(1,4));
    
    Stack<String> stack= Stack.<String>emptyInstance();
    
    Iterator<Machine<DoubleProbability>> itr=net.iterator();
    while(itr.hasNext()){
      Machine<DoubleProbability> temp=itr.next();
      stack=stack.push(temp.name);
      if (temp.name.contains("ICP")){
        Iterator<State<DoubleProbability>> states = temp.iterator();
        Machine.Builder<DoubleProbability> machineBuild = new Machine.Builder<DoubleProbability>(temp.name);
        while(states.hasNext()){
          State<DoubleProbability> state = states.next();
          if (state.name.contains("Exit") || state.name.contains("Dead")){

            DecisionTree<TransitionVector<DoubleProbability>> decisionTree = state.getTransitionFunction();
            if (decisionTree.isBranch()) System.err.println("Exit or Dead can only have p[E] p[D]=1");
            else{
              String[] patient=temp.name.split(":");
              String patientNum=patient[0].substring(patient[0].indexOf("Patient")+7);
              Predicate.Atom atom=new Predicate.Atom("DischargeModel",patientNum,"Occupied");
              
              TransitionVector.Builder<DoubleProbability> b=new TransitionVector.Builder<DoubleProbability>(temp.name);
              b.setProbability("InitLow", new DoubleProbability(1, 1));
              TransitionVector<DoubleProbability> initLow=b.build();
              Terminal<TransitionVector<DoubleProbability>> conseq = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(initLow) ;
              b=new TransitionVector.Builder<DoubleProbability>(temp.name);
              b.setProbability(state.name, new DoubleProbability(1, 1));
              Terminal<TransitionVector<DoubleProbability>> alter = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(b.build());

              State.Builder<DoubleProbability> stateBuild=new State.Builder<DoubleProbability>(temp.name,state.name,new DecisionTree.Branch<TransitionVector<DoubleProbability>>(atom, conseq, alter));
              Iterator<Entry<String, String>> labelItr = state.labelIterator();
              while (labelItr.hasNext()){
                Entry<String, String> tempEntry = labelItr.next();
                stateBuild.setLabel(tempEntry.getKey(),tempEntry.getValue());
              }
              machineBuild.addState(stateBuild.build());
            }
          }else{
            machineBuild.addState(state);
          }
        }
        netBuild.addMachine(machineBuild.build());
      }else{
        netBuild.addMachine(temp);
      }

    }
    netBuild.addMachine(dischargeModel);
    stack=stack.push(dischargeModel.name);
    netBuild.addMachine(arrivalModel);

    
    net=netBuild.build();
//    Stack<String> stackCopy=Stack.makeStack(stack.iterator());
    AggregateNet<DoubleProbability> aNet=new AggregateNet<DoubleProbability>(net, DoubleProbability.ZERO);

    System.out.println(aNet);
  
    for (int i = aNet.size()-1; i >= 1; i--) {
      aNet = aNet.multiply(i-1, i);
      System.out.println("\nAfter multiplying p" + i + "\n" + aNet.getMachine(i-1).getNumStates());
      if(stack.head().contains("Discharge")){
        aNet = aNet.sum(stack.head(), "Code").sum(stack.head(),"Cost").sum("ArrivalModel","Arrival").sum("ArrivalModel","Cost").reduce(i-1);
      }else if (stack.head().contains("ICP")){
        String tempStr[]=stack.head().split(":");
        String patientNum=tempStr[0].substring(tempStr[0].lastIndexOf("t")+1);
        aNet = aNet.sum("DischargeModel", patientNum).sum(stack.head(),"ICP").sum(stack.head(),"Patient").sum(stack.head(),"Cost").reduce(i-1);
      }else{
        aNet = aNet.sum(stack.head(),"Cost").reduce(i-1);
      }
      if(i==1){
        for (int j=0;j<NUM_PATIENTS;j++){
          aNet=aNet.sum("Patient"+j+":"+"Lung","Condition");
        }
        aNet=aNet.reduce(0);
      }
      stack=stack.tail();
      System.out.println("\nAfter reducing\n" + aNet.getMachine(i-1).getNumStates());
    }

//    aNet = aNet.multiply(4, 5).multiply(3, 4).multiply(2, 3).multiply(1, 2).multiply(0, 1);
//    aNet = aNet.sum("p5", "using").sum("p4", "using").sum("p3", "using").sum("p2", "using").sum("p1", "using");
//    aNet = aNet.reduce(0);

//     System.out.println(aNet);
    AggregateMachine<DoubleProbability> machine = aNet.getMachine(0);
    TransitionMatrix<SymbolicProbability<DoubleProbability>> prob = machine.computeTransitionMatrix();
    Matrix matrix = new Matrix(prob.N, prob.N);
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
    }

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
    names.add(Stack.makeName("DischargeModel", "Occupancy"));
    ResultTree rv = mc.runQuery(STEPS, names);
    System.out.println(rv.toCountString(STEPS));
  }
}
