grammar DecisionTree;

tokens {
	AND 	= '&' ;
	OR		= '|' ;
	IMPLIES	= '>' ;
	NEG		= '-' ;
	IF		= 'if';
	ELSE	= 'else';
	LEFTSTACHE	= '{';
	RIGHTSTACHE	= '}';
	LEFT = '(';
	RIGHT = ')';
}

/*------------------------------------------------------------------
 * PARSER RULES
 *------------------------------------------------------------------*/

tree		: branch | terminal ;
branch  	: IF expression LEFTSTACHE tree RIGHTSTACHE ELSE LEFTSTACHE tree RIGHTSTACHE ;
terminal	: NUMBER ;
expression  : (LEFT predicate RIGHT) | predicate ;
predicate	: ( negation | conjunction | disjunction | implication ) ;
negation	: NEG expression;
conjunction	: expression ( AND expression )+;
disjunction : expression ( OR expression)+;
implies		: expression IMPLIES expression;

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

STR		: (CAPALPHA|LOWALPHA|DIGIT|PERMITTED_SYMBOLS)+;

NUMBER	: (DIGIT)+ ;

WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 	{ $channel = HIDDEN; } ;

PERMITTED_SYMBOLS : ( '_' | '.' | '=' );

fragment CAPALPHA : 'A'..'Z';
fragment LOWALPHA : 'a'..'z';
fragment DIGIT	: '0'..'9' ;