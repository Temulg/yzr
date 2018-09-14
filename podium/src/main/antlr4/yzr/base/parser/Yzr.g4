/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

grammar Yzr;


yzrUnit: goal* ;

goal: Id goalBody;

goalBody: '[' SuperId statement* ']';

evalBody: '[-' MethodId statement* ('->' evalBody+)? ']';

statement: evalBody | goalBody | StringLiteral;

StringLiteral: StringTagged | StringInline;

SuperId: Id ('.' Id)*;

MethodId: ('.' Id) | (SuperId ('/' Id)?);

StringTagged: StringQuoted ('.' Id)?;

StringQuoted: '\'' StringCharacters? '\'' ('.' Id)?;

Id: JavaLetter JavaLetterOrDigit*;

StringInline: ~[ \t\r\n\u000C]+;

fragment StringCharacters: StringCharacter+;
fragment StringCharacter: ~['\\\r\n\u000C] | EscapeSequence;

fragment EscapeSequence: '\\' [btnfr'\\] | OctalEscape | UnicodeEscape;

fragment OctalEscape: '\\' OctalDigit
	| '\\' OctalDigit OctalDigit
	| '\\' ZeroToThree OctalDigit OctalDigit;

fragment OctalDigit: [0-7];

fragment ZeroToThree: [0-3];

fragment UnicodeEscape: '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit;

fragment HexDigit: [0-9a-fA-F];

fragment JavaLetter: [a-zA-Z$_]
	| ~[\u0000-\u007F\uD800-\uDBFF] {
		Character.isJavaIdentifierStart(_input.LA(-1))
	}? | [\uD800-\uDBFF] [\uDC00-\uDFFF] {
		Character.isJavaIdentifierStart(
			Character.toCodePoint(
				(char)_input.LA(-2), (char)_input.LA(-1)
			)
		)
	}?;

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

WS: [ \t\r\n\u000C]+ -> skip;

COMMENT: '/*' .*? '*/' -> channel(HIDDEN);

LINE_COMMENT: '//' ~[\r\n\u000C]* -> channel(HIDDEN);
