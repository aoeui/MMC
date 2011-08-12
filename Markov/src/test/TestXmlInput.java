package test;

import java.util.ArrayList;
import java.util.Iterator;

import markov.Machine;
import markov.Predicate;
import markov.FractionProbability;
import markov.State;
import markov.TransitionVector;
import markov.DecisionTree;

import org.w3c.dom.*;
import javax.xml.parsers.*;


public class TestXmlInput {

      /**
       * Our goal is to create a DOM XML tree and then print the XML.
       */
      public static void main (String args[]) {
          new TestXmlInput();
      }

      public TestXmlInput() {
          try {

              //Readin xml file
              DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
              DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
              Document doc = docBuilder.parse("xml/Machine.xml");

              doc.getDocumentElement().normalize();
              
              System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
              NodeList machineList = doc.getElementsByTagName("machine");
              
              for (int i=0; i<machineList.getLength(); i++){
                
                Element machineXml = (Element) machineList.item(i);
                
                System.out.println("-----------------------");
                System.out.println("MachineeName : " + machineXml.getAttribute("name") );
                Machine.Builder<FractionProbability> machineBuild = new Machine.Builder<FractionProbability>(machineXml.getAttribute("name"));
                                
                NodeList stateList = machineXml.getElementsByTagName("state");
                for (int j=0; j<machineList.getLength(); j++){
                  
                  Element stateXml = (Element) stateList.item(j);
                  System.out.println("  StateName : " + stateXml.getAttribute("name") );
                              
                  NodeList labels=stateXml.getChildNodes().item(1).getChildNodes();
                  for (int temp=0; temp<labels.getLength();temp++)
                  {
                    if (labels.item(temp).getNodeName().equals("labelPair")) {
                      Element labelVector=(Element) labels.item(temp);
                      System.out.println("    LabelVector: [" + labelVector.getAttribute("name") + ":"+ labelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue()+"]" );
                    }
                  }
              
                  //getDecisionTreeInfo  under state using 3rd under alternative or consequence use 1st of the Elements's childrenNode
                  DecisionTree<FractionProbability> decisionTree=getDecisionTreeInfo(stateXml);
              
                  State.Builder<FractionProbability> stateBuild=new State.Builder<FractionProbability>(machineXml.getAttribute("name"),stateXml.getAttribute("name"),decisionTree);
                  for (int temp=0; temp<labels.getLength();temp++)
                  {
                    if (labels.item(temp).getNodeName().equals("labelPair")) {
                      Element labelVector=(Element) labels.item(temp);
                      stateBuild.setLabel(labelVector.getAttribute("name"), labelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue());
                    }
                  }
                  State<FractionProbability> state=stateBuild.build();
                  machineBuild.addState(state);
                }
                
                Machine<FractionProbability> machine=machineBuild.build();
                

              }              
          } catch (Exception e) {
            e.printStackTrace();
          }
          

      }
      
      private static DecisionTree<FractionProbability> getDecisionTreeInfo(Element parent){
        
        Element decisionTreeXml=null;
        //if DecisionTree is under state use the 4th item of childNodes 
        if (parent.getNodeName().equals("state")){
          decisionTreeXml=(Element)parent.getChildNodes().item(3);
          System.out.println(parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(1).getNodeName()+ ": ");

        }//else if DecisionTree is under consequent or alternative use 2nd item of childNodes
        else if(parent.getNodeName().equals("consequent")||parent.getNodeName().equals("alternative")){
         decisionTreeXml=(Element)parent.getChildNodes().item(1);
         System.out.println("  "+parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(1).getNodeName()+ ": ");

        }
        
        if (decisionTreeXml==null){
          System.err.println("DecisionTree parent is not valid");
        }
        
        
        
        if (decisionTreeXml.getChildNodes().item(1).getNodeName().equals("branch")){
          Element predicateXml=(Element) decisionTreeXml.getChildNodes().item(1).getChildNodes().item(1);
          System.out.println("  "+predicateXml.getNodeName()+": ");
            
          //get atom or ... info from predicate
          Predicate predicate=null;
          predicate=getInfoFromPredicate(predicateXml,predicate);
          
          if (predicate==null){
            System.err.println("!!Predicate not parsed!");
          }
          
          DecisionTree<FractionProbability> consequent=null;
          DecisionTree<FractionProbability> alternative=null;         
          Element consequentXml = (Element) decisionTreeXml.getChildNodes().item(1).getChildNodes().item(3);
          consequent = getDecisionTreeInfo (consequentXml);
          if (decisionTreeXml.getChildNodes().item(1).getChildNodes().getLength()<6){
            System.err.println("!Missing consequence or alternative");
          } //no alternative
          else {
            Element alternativeXml = (Element) decisionTreeXml.getChildNodes().item(1).getChildNodes().item(5);
            alternative=getDecisionTreeInfo(alternativeXml);
          }
//          System.out.print(consequentXml.getNodeName());
          if (consequent==null || alternative==null)
            System.err.println("!Consequence or alternative not parsed!");
          
          DecisionTree.Branch<FractionProbability> branch=new DecisionTree.Branch<FractionProbability>(predicate,consequent,alternative);
          
          return branch;  
        }
        else if(decisionTreeXml.getChildNodes().item(1).getNodeName().equals("probability")){  
          Element probabilityXml = (Element)decisionTreeXml.getChildNodes().item(1);
          NodeList stateNameXml = probabilityXml.getElementsByTagName("stateName");
          NodeList pValueXml = probabilityXml.getElementsByTagName("pValue");
          TransitionVector.Builder<FractionProbability> b=new TransitionVector.Builder<FractionProbability>();

          for (int temp=0; temp<stateNameXml.getLength();temp++){
            System.out.println("  stateName:"+stateNameXml.item(temp).getChildNodes().item(0).getNodeValue()+",pValue:"+pValueXml.item(temp).getChildNodes().item(0).getNodeValue());
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
          DecisionTree.Terminal<FractionProbability> terminal= new DecisionTree.Terminal<FractionProbability>(transitionVector);
          return terminal;
          
        }else{
          return null;
        }
        
       
      }
      
      private static Predicate getInfoFromPredicate(Element predicate, Predicate Input ){
        //get atom info
        if (predicate.getChildNodes().item(1).getNodeName().equals("atom")){
          Element atomXml=(Element) predicate.getChildNodes().item(1);
          System.out.println("    " + atomXml.getNodeName()+": machineName: "+atomXml.getAttribute("machineName"));   
          Element atomLabelVector=(Element) atomXml.getChildNodes().item(1);
          System.out.println("      atom labelVector: [" + atomLabelVector.getAttribute("name")+":"+atomLabelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue()+"]");
          // create atom
          Predicate.Atom atom=new Predicate.Atom(atomXml.getAttribute("machineName"),atomLabelVector.getAttribute("name"),atomLabelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue());
          Predicate output=(Predicate)atom;
          return output;
        }
        
        else if (predicate.getChildNodes().item(1).getNodeName().equals("or")||predicate.getChildNodes().item(1).getNodeName().equals("and")){
          Element orAnd=(Element) predicate.getChildNodes().item(1);
          int orAndPredicateCounter=0;
          ArrayList<Element> predicates=new ArrayList<Element>();

          for (int temp=0; temp<orAnd.getChildNodes().getLength();temp++){
            if (orAnd.getChildNodes().item(temp).getNodeName().equals("predicate")){
              orAndPredicateCounter++;
              predicates.add((Element)orAnd.getChildNodes().item(temp));
            }
          }
          System.out.println("  "+orAnd.getNodeName()+": predicates #: "+predicates.size());

          Predicate.CollectionBuilder cBuild=null;
          if (predicate.getChildNodes().item(1).getNodeName().equals("or"))
            cBuild=new Predicate.CollectionBuilder(Predicate.CollectionType.OR);
          else cBuild=new Predicate.CollectionBuilder(Predicate.CollectionType.AND);
          
          // call itself to retrieve the predicate info
          Iterator<Element> itr = predicates.iterator();
          while(itr.hasNext()){
            Element pred=itr.next();
            getInfoFromPredicate(pred,Input);
            cBuild.add(Input);
          }
          Predicate output=cBuild.build();
          return output;

        }
        else if (predicate.getChildNodes().item(1).getNodeName().equals("neg")){
          Element neg=(Element) predicate.getChildNodes().item(1);
          Element pred=(Element) neg.getChildNodes().item(1);
          // call itself to retrieve the predicate info
          Predicate temp=getInfoFromPredicate(pred,Input);
          Predicate output=new Predicate.Neg(temp);
          return output;

        }else if (predicate.getChildNodes().item(1).getNodeName().equals("implies")){
          Element antecedentXml=(Element) predicate.getChildNodes().item(1);
          Element consequentXml=(Element) predicate.getChildNodes().item(3);
          // call itself to retrieve the predicate info
          Predicate antecedent=getInfoFromPredicate(antecedentXml,Input);
          Predicate consequent=getInfoFromPredicate(consequentXml,Input);
          Predicate output=new Predicate.Implies(antecedent,consequent);
          return output;
        }else{
          return null;
        }
       
      }



}
