/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.compiler;

public interface TokenId {
    int ABSTRACT = 300;
    int BOOLEAN = 301;
    int BREAK = 302;
    int BYTE = 303;
    int CASE = 304;
    int CATCH = 305;
    int CHAR = 306;
    int CLASS = 307;
    int CONST = 308;    // reserved keyword
    int CONTINUE = 309;
    int DEFAULT = 310;
    int DO = 311;
    int DOUBLE = 312;
    int ELSE = 313;
    int EXTENDS = 314;
    int FINAL = 315;
    int FINALLY = 316;
    int FLOAT = 317;
    int FOR = 318;
    int GOTO = 319;     // reserved keyword
    int IF = 320;
    int IMPLEMENTS = 321;
    int IMPORT = 322;
    int INSTANCEOF = 323;
    int INT = 324;
    int INTERFACE = 325;
    int LONG = 326;
    int NATIVE = 327;
    int NEW = 328;
    int PACKAGE = 329;
    int PRIVATE = 330;
    int PROTECTED = 331;
    int PUBLIC = 332;
    int RETURN = 333;
    int SHORT = 334;
    int STATIC = 335;
    int SUPER = 336;
    int SWITCH = 337;
    int SYNCHRONIZED = 338;
    int THIS = 339;
    int THROW = 340;
    int THROWS = 341;
    int TRANSIENT = 342;
    int TRY = 343;
    int VOID = 344;
    int VOLATILE = 345;
    int WHILE = 346;
    int STRICT = 347;

    int NEQ = 350;      // !=
    int MOD_E = 351;    // %=
    int AND_E = 352;    // &=
    int MUL_E = 353;    // *=
    int PLUS_E = 354;   // +=
    int MINUS_E = 355;  // -=
    int DIV_E = 356;    // /=
    int LE = 357;               // <=
    int EQ = 358;               // ==
    int GE = 359;               // >=
    int EXOR_E = 360;   // ^=
    int OR_E = 361;     // |=
    int PLUSPLUS = 362; // ++
    int MINUSMINUS = 363;       // --
    int LSHIFT = 364;   // <<
    int LSHIFT_E = 365; // <<=
    int RSHIFT = 366;   // >>
    int RSHIFT_E = 367; // >>=
    int OROR = 368;     // ||
    int ANDAND = 369;   // &&
    int ARSHIFT = 370;  // >>>
    int ARSHIFT_E = 371;        // >>>=

    // operators from NEQ to ARSHIFT_E
    String opNames[] = { "!=", "%=", "&=", "*=", "+=", "-=", "/=",
                       "<=", "==", ">=", "^=", "|=", "++", "--",
                       "<<", "<<=", ">>", ">>=", "||", "&&", ">>>",
                       ">>>=" };

    // operators from MOD_E to ARSHIFT_E
    int assignOps[] = { '%', '&', '*', '+', '-', '/', 0, 0, 0,
                        '^', '|', 0, 0, 0, LSHIFT, 0, RSHIFT, 0, 0, 0,
                        ARSHIFT };

    int Identifier = 400;
    int CharConstant = 401;
    int IntConstant = 402;
    int LongConstant = 403;
    int FloatConstant = 404;
    int DoubleConstant = 405;
    int StringL = 406;

    int TRUE = 410;
    int FALSE = 411;
    int NULL = 412;

    int CALL = 'C';     // method call
    int ARRAY = 'A';    // array access
    int MEMBER = '#';   // static member access

    int EXPR = 'E';     // expression statement
    int LABEL = 'L';    // label statement
    int BLOCK = 'B';    // block statement
    int DECL = 'D';     // declaration statement

    int BadToken = 500;
}
