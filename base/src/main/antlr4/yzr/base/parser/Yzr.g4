/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

grammar Yzr;

yzrUnit: goal* EOF;

goal: Identifier goalBody;

goalBody: LBRACE goalLine* RBRACE;

goalLine: Identifier SEMI;

Identifier: JavaLetter JavaLetterOrDigit*;

fragment JavaLetter: [a-zA-Z$_]
	| ~[\u0000-\u007F\uD800-\uDBFF] {
		Character.isJavaIdentifierStart(_input.LA(-1))
	}? | [\uD800-\uDBFF] [\uDC00-\uDFFF] {
		Character.isJavaIdentifierStart(
			Character.toCodePoint(
				(char)_input.LA(-2), (char)_input.LA(-1)
			)
		)
	}? ;

fragment JavaLetterOrDigit: [a-zA-Z0-9$_]
	| ~[\u0000-\u007F\uD800-\uDBFF] {
		Character.isJavaIdentifierPart(_input.LA(-1))
	}? | [\uD800-\uDBFF] [\uDC00-\uDFFF] {
		Character.isJavaIdentifierPart(
			Character.toCodePoint(
				(char)_input.LA(-2), (char)_input.LA(-1)
			)
		)
	}?;

//LPAREN: '(';
//RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
//LBRACK: '[';
//RBRACK: ']';
SEMI: ';';
//COMMA: ',';
//DOT: '.';
//ELLIPSIS: '...';
//AT: '@';
//COLONCOLON: '::';

WS: [ \t\r\n\u000C]+ -> skip;

COMMENT: '/*' .*? '*/' -> channel(HIDDEN);

LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
