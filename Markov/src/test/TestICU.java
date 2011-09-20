package test;

import java.util.Iterator;

import markov.DoubleProbability;
import markov.Machine;
import markov.Net;
import markov.Simulation;
import parser.XmlParser;

public class TestICU {
  public final static String PATIENT_MODEL_FILENAME = "xml/umlVersion4.xml";
  public final static int NUM_PATIENTS = 5;
  

  public static void main(String[] args) {
    Net<DoubleProbability> net=XmlParser.XmlInput(PATIENT_MODEL_FILENAME,NUM_PATIENTS);
    System.out.println(net);

    Net.Builder<DoubleProbability> netBuild=new Net.Builder<DoubleProbability>();
    Machine<DoubleProbability> dischargeModel=Simulation.constructDischargeModel(NUM_PATIENTS,net);
    Machine<DoubleProbability> arrivalModel=Simulation.constructArrivalModel(NUM_PATIENTS, new DoubleProbability(1,4));

    Iterator<Machine<DoubleProbability>> tempItr=net.iterator();
    while(tempItr.hasNext()){
      netBuild.addMachine(tempItr.next());
    }
    netBuild.addMachine(dischargeModel);
    netBuild.addMachine(arrivalModel);
    net=netBuild.build();
    
    System.out.println(net);
  }
}
