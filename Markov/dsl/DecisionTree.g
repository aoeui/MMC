grammar DecisionTree;

options {
  language = Java;
}

@header {
  package dsl;
  
  import java.util.ArrayList;
  
  import markov.DecisionTree;
  import markov.TransitionVector;
  import markov.Predicate;
  import markov.DoubleProbability;
  import markov.State;
  import markov.Machine;

  import util.Pair;
}

@lexer::header {
  package dsl;
}

@members {
}

machine
    : 'Machine:' machineName=IDENT '{' state[machineName] ( ';' state[machineName] )* '}'
    ;

state [String machineName] returns [State<DoubleProbability> state]
    : '{' seq=labelSequence ';' tree=decisionTree[machineName] '}'
    ;

labelSequence returns [ArrayList<Pair<String>> labels]
    : 'labels:'
      { labels = new ArrayList<Pair<String>>(); }
      li0=labelInstance { labels.add($li0.labelPair); } (',' lik=labelInstance { labels.add($lik.labelPair) } )*
    ; 

labelInstance returns [Pair<String> labelPair]
    : label=IDENT '->' instance=IDENT { labelPair = new Pair<String>($label.text, $instance.text); }
    ;

decisionTree [String machineName] returns [DecisionTree<TransitionVector<DoubleProbability>> dt]
    : expression[machineName] EOF { $dt = $expression.dt ;}
    ;

expression [String machineName] returns [DecisionTree<TransitionVector<DoubleProbability>> dt]
    : v=probabilityVector[machineName] { $dt = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>($v.tv); }
    | conditional[machineName] { $dt = $conditional.dt; }
    ;

conditional [String machineName] returns [DecisionTree.Branch<TransitionVector<DoubleProbability>> dt]
    : 'if' predicate 'then' cons=expression[machineName] 'else' alt=expression[machineName] 'end'
    { $dt = new DecisionTree.Branch<TransitionVector<DoubleProbability>>($predicate.pred, $cons.dt, $alt.dt); ;} 
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
    : mName=IDENT '.' lName=IDENT '=' label=IDENT { atom = new Predicate.Atom($mName.text, $lName.text, $label.text);  } 
    ;

probabilityVector [String machineName] returns [TransitionVector<DoubleProbability> tv]
    :
    { TransitionVector.Builder<DoubleProbability> builder = new TransitionVector.Builder<DoubleProbability>(machineName); }
    p1=probability {builder.setProbability($p1.stateName, $p1.fp); } (',' p2=probability { builder.setProbability($p2.stateName, $p2.fp); })*
    { tv = builder.build(); }
    ;

probability returns [String stateName, DoubleProbability fp]
    : 'p[' IDENT ']' '=' num=NUMBER '/' den=NUMBER
    { $stateName = $IDENT.text; $fp = new DoubleProbability(Long.parseLong($num.text), Long.parseLong($den.text)); }
    ;

IDENT : ALPHA (ALPHA|'0'..'9')* ;
NUMBER : '1'..'9' '0'..'9'* ;

WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\f' )+  { $channel = HIDDEN; } ;

fragment ALPHA : ('a'..'z'|'A'..'Z');
