grammar DecisionTree;

options {
  language = Java;
}

@header {
package dsl;

// import java.util.ArrayList;  // already included by antlr

import markov.DecisionTree;
import markov.DoubleProbability;
import markov.Machine;
import markov.Net;
import markov.Predicate;
import markov.State;
import markov.TransitionVector;

import util.Pair;
}

@lexer::header {
package dsl;
}

@members { }

net returns [Net<DoubleProbability> mNet]
    : {Net.Builder<DoubleProbability> netBuilder = new Net.Builder<DoubleProbability>(); }
    (machine { netBuilder.addMachine($machine.mMachine); } )+
    { mNet = netBuilder.build(); } 
    ;

machine returns [Machine<DoubleProbability> mMachine]
   : 'Machine:' machineName=IDENT { Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>($machineName.text); }
     s1=state[$machineName.text] { builder.addState($s1.mState); } ( s2=state[$machineName.text] { builder.addState($s2.mState); } )*
     { mMachine = builder.build(); }
   ;

state [String machineName] returns [State<DoubleProbability> mState]
    : 'State:' stateName=IDENT lSeq=labelSequence dTree=expression[machineName]
      { State.Builder<DoubleProbability> builder = new State.Builder<DoubleProbability>(machineName, $stateName.text, $dTree.dt);
        for (Pair<String> pair : $lSeq.labels) {
          builder.setLabel(pair.first, pair.second);
        }
        mState = builder.build();
      }
    ;

labelSequence returns [ArrayList<Pair<String>> labels]
    : 'labels:' { labels = new ArrayList<Pair<String>>(); }
      li0=labelInstance { labels.add($li0.labelPair); } (',' lik=labelInstance { labels.add($lik.labelPair); } )*
    ; 

labelInstance returns [Pair<String> labelPair]
    : label=IDENT '=' instance=IDENT { labelPair = new Pair<String>($label.text, $instance.text); }
    ;

expression [String machineName] returns [DecisionTree<TransitionVector<DoubleProbability>> dt]
    : v=probabilityVector[machineName] { $dt = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>($v.tv); }
    | conditional[machineName] { $dt = $conditional.dt; }
    ;

conditional [String machineName] returns [DecisionTree.Branch<TransitionVector<DoubleProbability>> dt]
    : 'if' predicate '{' cons=expression[machineName] '}' 'else' '{' alt=expression[machineName] '}'
    { $dt = new DecisionTree.Branch<TransitionVector<DoubleProbability>>($predicate.pred, $cons.dt, $alt.dt); } 
    ;

term returns [Predicate pred]
    : atom { $pred = $atom.atom; }
    | '(' predicate ')' {$pred = $predicate.pred; }
    ;

negation returns [Predicate pred]
    : { boolean isTrue = true; }
    ('-' { isTrue = !isTrue; })* term { $pred = isTrue ? $term.pred : new Predicate.Neg($term.pred); }
    ;

conjunction returns [Predicate pred]
    : { Predicate.CollectionBuilder builder = new Predicate.CollectionBuilder(Predicate.CollectionType.AND); }
    n1=negation { builder.add($n1.pred); } ('/\\' n2=negation { builder.add($n2.pred); })*
    { $pred = builder.build(); }
    ;

disjunction returns [Predicate pred]
    : { Predicate.CollectionBuilder builder = new Predicate.CollectionBuilder(Predicate.CollectionType.OR); }
    c1=conjunction { builder.add($c1.pred); } ('\\/' c2=conjunction {builder.add($c2.pred); })*
    { $pred = builder.build(); }
    ;

predicate returns [Predicate pred]
    : d1=disjunction { $pred = $d1.pred; } ('->' d2=disjunction { $pred = new Predicate.Implies(pred, $d2.pred); } )?
    ;

atom returns [Predicate.Atom atom]
    : mName=IDENT '::' lName=IDENT '=' label=IDENT { atom = new Predicate.Atom($mName.text, $lName.text, $label.text);  } 
    ;

probabilityVector [String machineName] returns [TransitionVector<DoubleProbability> tv]
    :
    { TransitionVector.Builder<DoubleProbability> builder = new TransitionVector.Builder<DoubleProbability>(machineName); }
    p1=probability {builder.setProbability($p1.stateName, $p1.fp); } (',' p2=probability { builder.setProbability($p2.stateName, $p2.fp); })*
    { tv = builder.build(); }
    ;

probability returns [String stateName, DoubleProbability fp]
    : 'p[' nextState=IDENT ']' { $stateName = $nextState.text; } '='
    // (( num=IDENT '/' den=IDENT { $fp = new DoubleProbability(Long.parseLong($num.text), Long.parseLong($den.text)); } )
    doub=IDENT {$fp = new DoubleProbability(Double.parseDouble($doub.text)); }
    ;

IDENT : (ALPHA|DIGIT|'_'|'.')+;

COMMENT
  : '//' (~('\n'|'\r'))*
    { $channel = HIDDEN; }
  ;

WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\f' )+  { $channel = HIDDEN; } ;

fragment ALPHA : ('a'..'z'|'A'..'Z');
fragment DIGIT : '0'..'9';