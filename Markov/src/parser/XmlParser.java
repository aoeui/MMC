package parser;

import java.util.ArrayList;
import java.util.Iterator;
import markov.Machine;
import markov.Net;
import markov.Predicate;
import markov.FractionProbability;
import markov.State;
import markov.TransitionVector;
import markov.DecisionTree;

import org.w3c.dom.*;
import javax.xml.parsers.*;


public class XmlParser {

      public static void main (String args[]) {
          System.out.println(new XmlParser());
      }
      
      
      public XmlParser() {
        
        Net<FractionProbability> net = XmlInput("xml/umlVersion3.xml");
        System.out.println(net.toString());
       
      }
      
      /****** This method can be used in other class to retrieve machines **********/
      public static Net<FractionProbability> XmlInput(String fileName){
        return XmlInput(fileName,1);
      }
      public static Net<FractionProbability> XmlInput(String fileName, int numOfPatient) {
        try {
          
          Net.Builder<FractionProbability> netBuild=new Net.Builder<FractionProbability>();
          
          for (int patientNum=0;patientNum<numOfPatient;patientNum++){
            //Read in XML file
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.parse(fileName);

            doc.getDocumentElement().normalize();
            
//            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList machineList = doc.getElementsByTagName("machine");
      
            for (int i=0; i<machineList.getLength(); i++){
              
              Element machineXml = (Element) machineList.item(i);
              
//              System.out.println("-----------------------");
//              System.out.println("MachineeName : " + machineXml.getAttribute("name") );
              String machineNames[]=machineXml.getAttribute("name").split(":");
              machineNames[0]=machineNames[0].substring(1);
              String machineName=machineNames[0]+patientNum+":"+machineNames[1];
              Machine.Builder<FractionProbability> machineBuild = new Machine.Builder<FractionProbability>(machineName);
              ArrayList<String> labelName=new ArrayList<String>();
              
              NodeList stateList = machineXml.getElementsByTagName("state");
              
              for (int j=0; j<stateList.getLength(); j++){
                 
                Element stateXml = (Element) stateList.item(j);
//                System.out.println("  StateName : " + stateXml.getAttribute("name") );
                            
                NodeList labels=stateXml.getChildNodes().item(0).getChildNodes();

                
/*                  for (int temp=0; temp<labels.getLength();temp++)
                {
                  if (labels.item(temp).getNodeName().equals("labelPair")) {
                    Element labelVector=(Element) labels.item(temp);
                    System.out.println("    LabelVector: [" + labelVector.getAttribute("name") + ":"+ labelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue()+"]" );
                  }
                }*/

            
                //getDecisionTreeInfo  under state using 3rd under alternative or consequence use 1st of the Elements's childrenNode
                DecisionTree<TransitionVector<FractionProbability>> decisionTree=getDecisionTreeInfo(machineBuild.name, stateXml,patientNum);
            
                State.Builder<FractionProbability> stateBuild=new State.Builder<FractionProbability>(machineName,stateXml.getAttribute("name"),decisionTree);
                
                for (int temp=0; temp<labels.getLength();temp++)
                {
                  if (labels.item(temp).getNodeName().equals("labelPair")) {
                    Element labelVector=(Element) labels.item(temp);
                    stateBuild.setLabel(labelVector.getAttribute("name"), labelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue());
                    labelName.add(labelVector.getAttribute("name"));
                  }
                }
                State<FractionProbability> state=stateBuild.build();
                machineBuild.addState(state);
              }
              Machine<FractionProbability> machine=machineBuild.build();
              netBuild.addMachine(machine);
                                                             
            }
          }
            Net<FractionProbability> net=netBuild.build();
            
            return net;
          
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        return null;

    }
      
/*      public static DecisionTree<String> decisionTreeParser2(String xmlFileName){
        
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
          docBuilder = dbfac.newDocumentBuilder();
          Document doc = docBuilder.parse(xmlFileName);
          doc.getDocumentElement().normalize();
          
//        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
          Element stateXml = (Element)doc.getElementsByTagName("state").item(0);
          
          DecisionTree<String> out=getDecisionTreeInfo2(stateXml);
          return out;
          
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        return null;

      }*/
      
      

/*      private static DecisionTree<String> getDecisionTreeInfo2(Element parent){
        
        Element decisionTreeXml=null;
        //if DecisionTree is under state use the 4th item of childNodes 
        if (parent.getNodeName().equals("state")){
          decisionTreeXml=(Element)parent.getChildNodes().item(1);
//          System.out.println(parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(0).getNodeName()+ ": ");

        }//else if DecisionTree is under consequent or alternative use 2nd item of childNodes
        else if(parent.getNodeName().equals("consequent")||parent.getNodeName().equals("alternative")){
         decisionTreeXml=(Element)parent.getChildNodes().item(0);
//         System.out.println("  "+parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(0).getNodeName()+ ": ");

        }
        
        if (decisionTreeXml==null){
          System.err.println("DecisionTree parent is not valid");
        }
        
        
        
        if (decisionTreeXml.getChildNodes().item(0).getNodeName().equals("branch")){
          Element predicateXml=(Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(0);
//        System.out.println("  "+predicateXml.getNodeName()+": ");
            
          //get atom or ... info from predicate
          Predicate predicate=null;
          predicate=getInfoFromPredicate(predicateXml,predicate);
          
          if (predicate==null){
            System.err.println("!!Predicate not parsed!");
          }
          
          DecisionTree<String> consequent=null;
          DecisionTree<String> alternative=null;         
          Element consequentXml = (Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(1);
          consequent = getDecisionTreeInfo2 (consequentXml);
          if (decisionTreeXml.getChildNodes().item(0).getChildNodes().getLength()<3){
            System.err.println("!Missing consequence or alternative");
          } //no alternative
          else {
            Element alternativeXml = (Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(2);
            alternative=getDecisionTreeInfo2(alternativeXml);
          }
          if (consequent==null || alternative==null)
            System.err.println("!Consequence or alternative not parsed!");
          
          DecisionTree.Branch<String> branch=new DecisionTree.Branch<String>(predicate,consequent,alternative);
          
          return branch;  
        }
        else if(decisionTreeXml.getChildNodes().item(0).getNodeName().equals("probability")){  
          Element probabilityXml = (Element)decisionTreeXml.getChildNodes().item(0);
          String temp = probabilityXml.getNodeValue();
          DecisionTree.Terminal<String> terminal= new DecisionTree.Terminal<String>(temp);
          return terminal;
          
        }else{
          return null;
        }
        
       
      }*/
      
      
      private static DecisionTree<TransitionVector<FractionProbability>> getDecisionTreeInfo(String machineName, Element parent, int patientNum){
        
        Element decisionTreeXml=null;
        //if DecisionTree is under state use the 4th item of childNodes 
        if (parent.getNodeName().equals("state")){
          decisionTreeXml=(Element)parent.getChildNodes().item(1);
//          System.out.println(parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(0).getNodeName()+ ": ");

        }//else if DecisionTree is under consequent or alternative use 2nd item of childNodes
        else if(parent.getNodeName().equals("consequent")||parent.getNodeName().equals("alternative")){
         decisionTreeXml=(Element)parent.getChildNodes().item(0);
//         System.out.println("  "+parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(0).getNodeName()+ ": ");

        }
        
        if (decisionTreeXml==null){
          System.err.println("DecisionTree parent is not valid");
        }
        
        
        
        if (decisionTreeXml.getChildNodes().item(0).getNodeName().equals("branch")){
          Element predicateXml=(Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(0);
//        System.out.println("  "+predicateXml.getNodeName()+": ");
            
          //get atom or ... info from predicate
          Predicate predicate=null;
          predicate=getInfoFromPredicate(predicateXml,predicate,patientNum);
          
          if (predicate==null){
            System.err.println("!!Predicate not parsed!");
          }
          
          DecisionTree<TransitionVector<FractionProbability>> consequent=null;
          DecisionTree<TransitionVector<FractionProbability>> alternative=null;         
          Element consequentXml = (Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(1);
          consequent = getDecisionTreeInfo (machineName, consequentXml,patientNum);
          if (decisionTreeXml.getChildNodes().item(0).getChildNodes().getLength()<3){
            System.err.println("!Missing consequence or alternative");
          } //no alternative
          else {
            Element alternativeXml = (Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(2);
            alternative=getDecisionTreeInfo(machineName, alternativeXml,patientNum);
          }
          if (consequent==null || alternative==null)
            System.err.println("!Consequence or alternative not parsed!");
          
          DecisionTree.Branch<TransitionVector<FractionProbability>> branch=new DecisionTree.Branch<TransitionVector<FractionProbability>>(predicate,consequent,alternative);
          
          return branch;  
        }
        else if(decisionTreeXml.getChildNodes().item(0).getNodeName().equals("probability")){  
          Element probabilityXml = (Element)decisionTreeXml.getChildNodes().item(0);
          NodeList stateNameXml = probabilityXml.getElementsByTagName("stateName");
          NodeList pValueXml = probabilityXml.getElementsByTagName("pValue");
          TransitionVector.Builder<FractionProbability> b=new TransitionVector.Builder<FractionProbability>(machineName);

          for (int temp=0; temp<stateNameXml.getLength();temp++){
//           System.out.println("  stateName:"+stateNameXml.item(temp).getChildNodes().item(0).getNodeValue()+",pValue:"+pValueXml.item(temp).getChildNodes().item(0).getNodeValue());
            String[] pValue=pValueXml.item(temp).getChildNodes().item(0).getNodeValue().split(",");
            String stateName=stateNameXml.item(temp).getChildNodes().item(0).getNodeValue();

            if (pValue.length!=2)
              System.err.println("pValue input error! Should have format <num>,<den>");
            Long num=Long.parseLong(pValue[0]);
            Long den=Long.parseLong(pValue[1]);
            FractionProbability probability=new FractionProbability(num,den);
            b.setProbability(stateName, probability);

          }
        
          TransitionVector<FractionProbability> transitionVector=b.build();
          DecisionTree.Terminal<TransitionVector<FractionProbability>> terminal= new DecisionTree.Terminal<TransitionVector<FractionProbability>>(transitionVector);
          return terminal;
          
        }else{
          return null;
        }
        
       
      }
      
      private static Predicate getInfoFromPredicate(Element predicate, Predicate Input, int patientNum ){
        //get atom info
        if (predicate.getChildNodes().item(0).getNodeName().equals("atom")){
          Element atomXml=(Element) predicate.getChildNodes().item(0);
//          System.out.println("    " + atomXml.getNodeName()+": machineName: "+atomXml.getAttribute("machineName"));   
          Element atomLabelVector=(Element) atomXml.getChildNodes().item(0);
//          System.out.println("      atom labelVector: [" + atomLabelVector.getAttribute("name")+":"+atomLabelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue()+"]");
          // create atom
          String machineName[]=atomXml.getAttribute("machineName").split(":");
          machineName[0]=machineName[0].substring(1);
          Predicate.Atom atom=new Predicate.Atom(machineName[0]+patientNum+":"+machineName[1], atomLabelVector.getAttribute("name"), atomLabelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue());
          Predicate output=(Predicate)atom;

          return output;
        }
        
        else if (predicate.getChildNodes().item(0).getNodeName().equals("or")||predicate.getChildNodes().item(0).getNodeName().equals("and")){
          Element orAnd=(Element) predicate.getChildNodes().item(0);
          ArrayList<Element> predicates=new ArrayList<Element>();

          for (int temp=0; temp<orAnd.getChildNodes().getLength();temp++){
            if (orAnd.getChildNodes().item(temp).getNodeName().equals("predicate")){
              predicates.add((Element)orAnd.getChildNodes().item(temp));
            }
          }
//          System.out.println("  "+orAnd.getNodeName()+": predicates #: "+predicates.size());

          Predicate.CollectionBuilder cBuild=null;
          if (predicate.getChildNodes().item(0).getNodeName().equals("or"))
            cBuild=new Predicate.CollectionBuilder(Predicate.CollectionType.OR);
          else if (predicate.getChildNodes().item(0).getNodeName().equals("and"))
            cBuild=new Predicate.CollectionBuilder(Predicate.CollectionType.AND);
          else
            System.err.println("Error cBuild!");
          
          // call itself to retrieve the predicate info
          Iterator<Element> itr = predicates.iterator();
          while(itr.hasNext()){
            Element pred=itr.next();
            Predicate temp=getInfoFromPredicate(pred,Input,patientNum);
            cBuild.add(temp);
          }
          Predicate output=cBuild.build();

          return output;
        }
        else if (predicate.getChildNodes().item(0).getNodeName().equals("neg")){
          Element neg=(Element) predicate.getChildNodes().item(0);
          Element pred=(Element) neg.getChildNodes().item(0);
          // call itself to retrieve the predicate info
          Predicate temp=getInfoFromPredicate(pred,Input,patientNum);
          Predicate output=new Predicate.Neg(temp);

          return output;

        }else if (predicate.getChildNodes().item(0).getNodeName().equals("implies")){
          Element implies=(Element) predicate.getChildNodes().item(0);
          Element antecedentXml=(Element) implies.getChildNodes().item(0);
          Element consequentXml=(Element) implies.getChildNodes().item(1);
          // call itself to retrieve the predicate info
          Predicate antecedent=getInfoFromPredicate(antecedentXml,Input,patientNum);
          Predicate consequent=getInfoFromPredicate(consequentXml,Input,patientNum);
          Predicate output=new Predicate.Implies(antecedent,consequent);
          return output;
        }else{
          return null;
        }
       
      }



}
