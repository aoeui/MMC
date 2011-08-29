grammar DecisionTree;

options {
  language = Java;
}

@header {
  package dsl;
  
  import markov.DecisionTree;
  import markov.TransitionVector;
  import markov.Predicate;
  import markov.FractionProbability;
}

@lexer::header {
  package dsl;
}

@members {
  String machineName; 
}

decisionTree returns [DecisionTree<TransitionVector<FractionProbability>> dt]
    : '(' IDENT { machineName = $IDENT.text; } ')' expression EOF { $dt = $expression.dt ;}
    ;

expression returns [DecisionTree<TransitionVector<FractionProbability>> dt]
    : v=probabilityVector { $dt = new DecisionTree.Terminal<TransitionVector<FractionProbability>>($v.tv); }
    | conditional { $dt = $conditional.dt; }
    ;

conditional returns [DecisionTree.Branch<TransitionVector<FractionProbability>> dt]
    : 'if' predicate 'then' cons=expression 'else' alt=expression 'end'
    { $dt = new DecisionTree.Branch<TransitionVector<FractionProbability>>($predicate.pred, $cons.dt, $alt.dt); ;} 
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

probabilityVector returns [TransitionVector<FractionProbability> tv]
    :
    { TransitionVector.Builder<FractionProbability> builder = new TransitionVector.Builder<FractionProbability>(machineName); }
    p1=probability {builder.setProbability($p1.stateName, $p1.fp); } (',' p2=probability { builder.setProbability($p2.stateName, $p2.fp); })*
    { tv = builder.build(); }
    ;

probability returns [String stateName, FractionProbability fp]
    : 'p[' IDENT ']' '=' num=NUMBER '/' den=NUMBER
    { $stateName = $IDENT.text; $fp = new FractionProbability(Long.parseLong($num.text), Long.parseLong($den.text)); }
    ;

IDENT : ALPHA (ALPHA|'0'..'9')* ;
NUMBER : '1'..'9' '0'..'9'* ;

WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\f' )+  { $channel = HIDDEN; } ;

fragment ALPHA : ('a'..'z'|'A'..'Z');
