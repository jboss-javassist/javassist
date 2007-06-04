/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.compiler;

class Token {
    public Token next = null;
    public int tokenId;

    public long longValue;
    public double doubleValue;
    public String textValue;
}

public class Lex implements TokenId {
    private int lastChar;
    private StringBuffer textBuffer;
    private Token currentToken;
    private Token lookAheadTokens;

    private String input;
    private int position, maxlen, lineNumber;

    /**
     * Constructs a lexical analyzer.
     */
    public Lex(String s) {
        lastChar = -1;
        textBuffer = new StringBuffer();
        currentToken = new Token();
        lookAheadTokens = null;

        input = s;
        position = 0;
        maxlen = s.length();
        lineNumber = 0;
    }

    public int get() {
        if (lookAheadTokens == null)
            return get(currentToken);
        else {
            Token t;
            currentToken = t = lookAheadTokens;
            lookAheadTokens = lookAheadTokens.next;
            return t.tokenId;
        }
    }

    /**
     * Looks at the next token.
     */
    public int lookAhead() {
        return lookAhead(0);
    }

    public int lookAhead(int i) {
        Token tk = lookAheadTokens;
        if (tk == null) {
            lookAheadTokens = tk = currentToken;  // reuse an object!
            tk.next = null;
            get(tk);
        }

        for (; i-- > 0; tk = tk.next)
            if (tk.next == null) {
                Token tk2;
                tk.next = tk2 = new Token();
                get(tk2);
            }

        currentToken = tk;
        return tk.tokenId;
    }

    public String getString() {
        return currentToken.textValue;
    }

    public long getLong() {
        return currentToken.longValue;
    }

    public double getDouble() {
        return currentToken.doubleValue;
    }

    private int get(Token token) {
        int t;
        do {
            t = readLine(token);
        } while (t == '\n');
        token.tokenId = t;
        return t;
    }

    private int readLine(Token token) {
        int c = getNextNonWhiteChar();
        if(c < 0)
            return c;
        else if(c == '\n') {
            ++lineNumber;
            return '\n';
        }
        else if (c == '\'')
            return readCharConst(token);
        else if (c == '"')
            return readStringL(token);
        else if ('0' <= c && c <= '9')
            return readNumber(c, token);
        else if(c == '.'){
            c = getc();
            if ('0' <= c && c <= '9') {
                StringBuffer tbuf = textBuffer;
                tbuf.setLength(0);
                tbuf.append('.');
                return readDouble(tbuf, c, token);
            }
            else{
                ungetc(c);
                return readSeparator('.');
            }
        }
        else if (Character.isJavaIdentifierStart((char)c))
            return readIdentifier(c, token);
        else
            return readSeparator(c);
    }

    private int getNextNonWhiteChar() {
        int c;
        do {
            c = getc();
            if (c == '/') {
                c = getc();
                if (c == '/')
                    do {
                        c = getc();
                    } while (c != '\n' && c != '\r' && c != -1);
                else if (c == '*')
                    while (true) {
                        c = getc();
                        if (c == -1)
                            break;
                        else if (c == '*')
                            if ((c = getc()) == '/') {
                                c = ' ';
                                break;
                            }
                            else
                                ungetc(c);
                    }
                else {
                    ungetc(c);
                    c = '/';
                }
            }
        } while(isBlank(c));
        return c;
    }

    private int readCharConst(Token token) {
        int c;
        int value = 0;
        while ((c = getc()) != '\'')
            if (c == '\\')
                value = readEscapeChar();
            else if (c < 0x20) {
                if (c == '\n')
                    ++lineNumber;

                return BadToken;
            }
            else
                value = c;

        token.longValue = value;
        return CharConstant;
    }

    private int readEscapeChar() {
        int c = getc();
        if (c == 'n')
            c = '\n';
        else if (c == 't')
            c = '\t';
        else if (c == 'r')
            c = '\r';
        else if (c == 'f')
            c = '\f';
        else if (c == '\n')
            ++lineNumber;

        return c;
    }

    private int readStringL(Token token) {
        int c;
        StringBuffer tbuf = textBuffer;
        tbuf.setLength(0);
        for (;;) {
            while ((c = getc()) != '"') {
                if (c == '\\')
                    c = readEscapeChar();
                else if (c == '\n' || c < 0) {
                    ++lineNumber;
                    return BadToken;
                }

                tbuf.append((char)c);
            }

            for (;;) {
                c = getc();
                if (c == '\n')
                    ++lineNumber;
                else if (!isBlank(c))
                    break;
            }

            if (c != '"') {
                ungetc(c);
                break;
            }
        }

        token.textValue = tbuf.toString();
        return StringL;
    }

    private int readNumber(int c, Token token) {
        long value = 0;
        int c2 = getc();
        if (c == '0')
            if (c2 == 'X' || c2 == 'x')
                for (;;) {
                    c = getc();
                    if ('0' <= c && c <= '9')
                        value = value * 16 + (long)(c - '0');
                    else if ('A' <= c && c <= 'F')
                        value = value * 16 + (long)(c - 'A' + 10);
                    else if ('a' <= c && c <= 'f')
                        value = value * 16 + (long)(c - 'a' + 10);
                    else {
                        token.longValue = value;
                        if (c == 'L' || c == 'l')
                            return LongConstant;
                        else {
                            ungetc(c);
                            return IntConstant;
                        }
                    }
                }
            else if ('0' <= c2 && c2 <= '7') {
                value = c2 - '0';
                for (;;) {
                    c = getc();
                    if ('0' <= c && c <= '7')
                        value = value * 8 + (long)(c - '0');
                    else {
                        token.longValue = value;
                        if (c == 'L' || c == 'l')
                            return LongConstant;
                        else {
                            ungetc(c);
                            return IntConstant;
                        }
                    }
                }
            }

        value = c - '0';
        while ('0' <= c2 && c2 <= '9') {
            value = value * 10 + c2 - '0';
            c2 = getc();
        }

        token.longValue = value;
        if (c2 == 'F' || c2 == 'f') {
            token.doubleValue = (double)value;
            return FloatConstant;
        }
        else if (c2 == 'E' || c2 == 'e'
                 || c2 == 'D' || c2 == 'd' || c2 == '.') {
            StringBuffer tbuf = textBuffer;
            tbuf.setLength(0);
            tbuf.append(value);
            return readDouble(tbuf, c2, token);
        }
        else if (c2 == 'L' || c2 == 'l')
            return LongConstant;
        else {
            ungetc(c2);
            return IntConstant;
        }
    }

    private int readDouble(StringBuffer sbuf, int c, Token token) {
        if (c != 'E' && c != 'e' && c != 'D' && c != 'd') {
            sbuf.append((char)c);
            for (;;) {
                c = getc();
                if ('0' <= c && c <= '9')
                    sbuf.append((char)c);
                else
                    break;
            }
        }

        if (c == 'E' || c == 'e') {
            sbuf.append((char)c);
            c = getc();
            if (c == '+' || c == '-') {
                sbuf.append((char)c);
                c = getc();
            }

            while ('0' <= c && c <= '9') {
                sbuf.append((char)c);
                c = getc();
            }
        }

        try {
            token.doubleValue = Double.parseDouble(sbuf.toString());
        }
        catch (NumberFormatException e) {
            return BadToken;
        }

        if (c == 'F' || c == 'f')
            return FloatConstant;
        else {
            if (c != 'D' && c != 'd')
                ungetc(c);

            return DoubleConstant;
        }
    }

    // !"#$%&'(    )*+,-./0    12345678    9:;<=>?
    private static final int[] equalOps
        =  { NEQ, 0, 0, 0, MOD_E, AND_E, 0, 0,
             0, MUL_E, PLUS_E, 0, MINUS_E, 0, DIV_E, 0,
             0, 0, 0, 0, 0, 0, 0, 0,
             0, 0, 0, LE, EQ, GE, 0 };

    private int readSeparator(int c) {
        int c2, c3;
        if ('!' <= c && c <= '?') {
            int t = equalOps[c - '!'];
            if (t == 0) 
                return c;
            else {
                c2 = getc();
                if (c == c2)
                    switch (c) {
                    case '=' :
                        return EQ;
                    case '+' :
                        return PLUSPLUS;
                    case '-' :
                        return MINUSMINUS;
                    case '&' :
                        return ANDAND;
                    case '<' :
                        c3 = getc();
                        if (c3 == '=')
                            return LSHIFT_E;
                        else {
                            ungetc(c3);
                            return LSHIFT;
                        }
                    case '>' :
                        c3 = getc();
                        if (c3 == '=')
                            return RSHIFT_E;
                        else if (c3 == '>') {
                            c3 = getc();
                            if (c3 == '=')
                                return ARSHIFT_E;
                            else {
                                ungetc(c3);
                                return ARSHIFT;
                            }
                        }
                        else {
                            ungetc(c3);
                            return RSHIFT;
                        }
                    default :
                        break;
                    }
                else if (c2 == '=')
                    return t;
            }
        }
        else if (c == '^') {
            c2 = getc();
            if (c2 == '=')
                return EXOR_E;
        }
        else if (c == '|') {
            c2 = getc();
            if (c2 == '=')
                return OR_E;
            else if (c2 == '|')
                return OROR;
        }
        else
            return c;

        ungetc(c2);
        return c;
    }

    private int readIdentifier(int c, Token token) {
        StringBuffer tbuf = textBuffer;
        tbuf.setLength(0);

        do {
            tbuf.append((char)c);
            c = getc();
        } while (Character.isJavaIdentifierPart((char)c));

        ungetc(c);

        String name = tbuf.toString();
        int t = ktable.lookup(name);
        if (t >= 0)
            return t;
        else {
            /* tbuf.toString() is executed quickly since it does not
             * need memory copy.  Using a hand-written extensible
             * byte-array class instead of StringBuffer is not a good idea
             * for execution speed.  Converting a byte array to a String
             * object is very slow.  Using an extensible char array
             * might be OK.
             */
            token.textValue = name;
            return Identifier;
        }
    }

    private static final KeywordTable ktable = new KeywordTable();

    static {
        ktable.append("abstract", ABSTRACT);
        ktable.append("boolean", BOOLEAN);
        ktable.append("break", BREAK);
        ktable.append("byte", BYTE);
        ktable.append("case", CASE);
        ktable.append("catch", CATCH);
        ktable.append("char", CHAR);
        ktable.append("class", CLASS);
        ktable.append("const", CONST);
        ktable.append("continue", CONTINUE);
        ktable.append("default", DEFAULT);
        ktable.append("do", DO);
        ktable.append("double", DOUBLE);
        ktable.append("else", ELSE);
        ktable.append("extends", EXTENDS);
        ktable.append("false", FALSE);
        ktable.append("final", FINAL);
        ktable.append("finally", FINALLY);
        ktable.append("float", FLOAT);
        ktable.append("for", FOR);
        ktable.append("goto", GOTO);
        ktable.append("if", IF);
        ktable.append("implements", IMPLEMENTS);
        ktable.append("import", IMPORT);
        ktable.append("instanceof", INSTANCEOF);
        ktable.append("int", INT);
        ktable.append("interface", INTERFACE);
        ktable.append("long", LONG);
        ktable.append("native", NATIVE);
        ktable.append("new", NEW);
        ktable.append("null", NULL);
        ktable.append("package", PACKAGE);
        ktable.append("private", PRIVATE);
        ktable.append("protected", PROTECTED);
        ktable.append("public", PUBLIC);
        ktable.append("return", RETURN);
        ktable.append("short", SHORT);
        ktable.append("static", STATIC);
        ktable.append("strictfp", STRICT);
        ktable.append("super", SUPER);
        ktable.append("switch", SWITCH);
        ktable.append("synchronized", SYNCHRONIZED);
        ktable.append("this", THIS);
        ktable.append("throw", THROW);
        ktable.append("throws", THROWS);
        ktable.append("transient", TRANSIENT);
        ktable.append("true", TRUE);
        ktable.append("try", TRY);
        ktable.append("void", VOID);
        ktable.append("volatile", VOLATILE);
        ktable.append("while", WHILE);
    }

    private static boolean isBlank(int c) {
        return c == ' ' || c == '\t' || c == '\f' || c == '\r'
            || c == '\n';
    }

    private static boolean isDigit(int c) {
        return '0' <= c && c <= '9';
    }

    private void ungetc(int c) {
        lastChar = c;
    }

    public String getTextAround() {
        int begin = position - 10;
        if (begin < 0)
            begin = 0;

        int end = position + 10;
        if (end > maxlen)
            end = maxlen;

        return input.substring(begin, end);
    }

    private int getc() {
        if (lastChar < 0)
            if (position < maxlen)
                return input.charAt(position++);
            else
                return -1;
        else {
            int c = lastChar;
            lastChar = -1;
            return c;
        }
    }
}
