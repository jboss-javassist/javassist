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

package javassist.bytecode;

/**
 * JVM Instruction Set.
 *
 * <p>This interface defines opcodes and
 * array types for the NEWARRAY instruction.
 *
 * @see Mnemonic
 */
public interface Opcode {
    /* Opcodes */

    int AALOAD = 50;
    int AASTORE = 83;
    int ACONST_NULL = 1;
    int ALOAD = 25;
    int ALOAD_0 = 42;
    int ALOAD_1 = 43;
    int ALOAD_2 = 44;
    int ALOAD_3 = 45;
    int ANEWARRAY = 189;
    int ARETURN = 176;
    int ARRAYLENGTH = 190;
    int ASTORE = 58;
    int ASTORE_0 = 75;
    int ASTORE_1 = 76;
    int ASTORE_2 = 77;
    int ASTORE_3 = 78;
    int ATHROW = 191;
    int BALOAD = 51;
    int BASTORE = 84;
    int BIPUSH = 16;
    int CALOAD = 52;
    int CASTORE = 85;
    int CHECKCAST = 192;
    int D2F = 144;
    int D2I = 142;
    int D2L = 143;
    int DADD = 99;
    int DALOAD = 49;
    int DASTORE = 82;
    int DCMPG = 152;
    int DCMPL = 151;
    int DCONST_0 = 14;
    int DCONST_1 = 15;
    int DDIV = 111;
    int DLOAD = 24;
    int DLOAD_0 = 38;
    int DLOAD_1 = 39;
    int DLOAD_2 = 40;
    int DLOAD_3 = 41;
    int DMUL = 107;
    int DNEG = 119;
    int DREM = 115;
    int DRETURN = 175;
    int DSTORE = 57;
    int DSTORE_0 = 71;
    int DSTORE_1 = 72;
    int DSTORE_2 = 73;
    int DSTORE_3 = 74;
    int DSUB = 103;
    int DUP = 89;
    int DUP2 = 92;
    int DUP2_X1 = 93;
    int DUP2_X2 = 94;
    int DUP_X1 = 90;
    int DUP_X2 = 91;
    int F2D = 141;
    int F2I = 139;
    int F2L = 140;
    int FADD = 98;
    int FALOAD = 48;
    int FASTORE = 81;
    int FCMPG = 150;
    int FCMPL = 149;
    int FCONST_0 = 11;
    int FCONST_1 = 12;
    int FCONST_2 = 13;
    int FDIV = 110;
    int FLOAD = 23;
    int FLOAD_0 = 34;
    int FLOAD_1 = 35;
    int FLOAD_2 = 36;
    int FLOAD_3 = 37;
    int FMUL = 106;
    int FNEG = 118;
    int FREM = 114;
    int FRETURN = 174;
    int FSTORE = 56;
    int FSTORE_0 = 67;
    int FSTORE_1 = 68;
    int FSTORE_2 = 69;
    int FSTORE_3 = 70;
    int FSUB = 102;
    int GETFIELD = 180;
    int GETSTATIC = 178;
    int GOTO = 167;
    int GOTO_W = 200;
    int I2B = 145;
    int I2C = 146;
    int I2D = 135;
    int I2F = 134;
    int I2L = 133;
    int I2S = 147;
    int IADD = 96;
    int IALOAD = 46;
    int IAND = 126;
    int IASTORE = 79;
    int ICONST_0 = 3;
    int ICONST_1 = 4;
    int ICONST_2 = 5;
    int ICONST_3 = 6;
    int ICONST_4 = 7;
    int ICONST_5 = 8;
    int ICONST_M1 = 2;
    int IDIV = 108;
    int IFEQ = 153;
    int IFGE = 156;
    int IFGT = 157;
    int IFLE = 158;
    int IFLT = 155;
    int IFNE = 154;
    int IFNONNULL = 199;
    int IFNULL = 198;
    int IF_ACMPEQ = 165;
    int IF_ACMPNE = 166;
    int IF_ICMPEQ = 159;
    int IF_ICMPGE = 162;
    int IF_ICMPGT = 163;
    int IF_ICMPLE = 164;
    int IF_ICMPLT = 161;
    int IF_ICMPNE = 160;
    int IINC = 132;
    int ILOAD = 21;
    int ILOAD_0 = 26;
    int ILOAD_1 = 27;
    int ILOAD_2 = 28;
    int ILOAD_3 = 29;
    int IMUL = 104;
    int INEG = 116;
    int INSTANCEOF = 193;
    int INVOKEINTERFACE = 185;
    int INVOKESPECIAL = 183;
    int INVOKESTATIC = 184;
    int INVOKEVIRTUAL = 182;
    int IOR = 128;
    int IREM = 112;
    int IRETURN = 172;
    int ISHL = 120;
    int ISHR = 122;
    int ISTORE = 54;
    int ISTORE_0 = 59;
    int ISTORE_1 = 60;
    int ISTORE_2 = 61;
    int ISTORE_3 = 62;
    int ISUB = 100;
    int IUSHR = 124;
    int IXOR = 130;
    int JSR = 168;
    int JSR_W = 201;
    int L2D = 138;
    int L2F = 137;
    int L2I = 136;
    int LADD = 97;
    int LALOAD = 47;
    int LAND = 127;
    int LASTORE = 80;
    int LCMP = 148;
    int LCONST_0 = 9;
    int LCONST_1 = 10;
    int LDC = 18;
    int LDC2_W = 20;
    int LDC_W = 19;
    int LDIV = 109;
    int LLOAD = 22;
    int LLOAD_0 = 30;
    int LLOAD_1 = 31;
    int LLOAD_2 = 32;
    int LLOAD_3 = 33;
    int LMUL = 105;
    int LNEG = 117;
    int LOOKUPSWITCH = 171;
    int LOR = 129;
    int LREM = 113;
    int LRETURN = 173;
    int LSHL = 121;
    int LSHR = 123;
    int LSTORE = 55;
    int LSTORE_0 = 63;
    int LSTORE_1 = 64;
    int LSTORE_2 = 65;
    int LSTORE_3 = 66;
    int LSUB = 101;
    int LUSHR = 125;
    int LXOR = 131;
    int MONITORENTER = 194;
    int MONITOREXIT = 195;
    int MULTIANEWARRAY = 197;
    int NEW = 187;
    int NEWARRAY = 188;
    int NOP = 0;
    int POP = 87;
    int POP2 = 88;
    int PUTFIELD = 181;
    int PUTSTATIC = 179;
    int RET = 169;
    int RETURN = 177;
    int SALOAD = 53;
    int SASTORE = 86;
    int SIPUSH = 17;
    int SWAP = 95;
    int TABLESWITCH = 170;
    int WIDE = 196;

    /* array-type code for the newarray instruction */

    int T_BOOLEAN = 4;
    int T_CHAR = 5;
    int T_FLOAT = 6;
    int T_DOUBLE = 7;
    int T_BYTE = 8;
    int T_SHORT = 9;
    int T_INT = 10;
    int T_LONG = 11;

    /* how many values are pushed on the operand stack. */
    int[] STACK_GROW = {
        0, // nop, 0
        1, // aconst_null, 1
        1, // iconst_m1, 2
        1, // iconst_0, 3
        1, // iconst_1, 4
        1, // iconst_2, 5
        1, // iconst_3, 6
        1, // iconst_4, 7
        1, // iconst_5, 8
        2, // lconst_0, 9
        2, // lconst_1, 10
        1, // fconst_0, 11
        1, // fconst_1, 12
        1, // fconst_2, 13
        2, // dconst_0, 14
        2, // dconst_1, 15
        1, // bipush, 16
        1, // sipush, 17
        1, // ldc, 18
        1, // ldc_w, 19
        2, // ldc2_w, 20
        1, // iload, 21
        2, // lload, 22
        1, // fload, 23
        2, // dload, 24
        1, // aload, 25
        1, // iload_0, 26
        1, // iload_1, 27
        1, // iload_2, 28
        1, // iload_3, 29
        2, // lload_0, 30
        2, // lload_1, 31
        2, // lload_2, 32
        2, // lload_3, 33
        1, // fload_0, 34
        1, // fload_1, 35
        1, // fload_2, 36
        1, // fload_3, 37
        2, // dload_0, 38
        2, // dload_1, 39
        2, // dload_2, 40
        2, // dload_3, 41
        1, // aload_0, 42
        1, // aload_1, 43
        1, // aload_2, 44
        1, // aload_3, 45
        -1, // iaload, 46
        0, // laload, 47
        -1, // faload, 48
        0, // daload, 49
        -1, // aaload, 50
        -1, // baload, 51
        -1, // caload, 52
        -1, // saload, 53
        -1, // istore, 54
        -2, // lstore, 55
        -1, // fstore, 56
        -2, // dstore, 57
        -1, // astore, 58
        -1, // istore_0, 59
        -1, // istore_1, 60
        -1, // istore_2, 61
        -1, // istore_3, 62
        -2, // lstore_0, 63
        -2, // lstore_1, 64
        -2, // lstore_2, 65
        -2, // lstore_3, 66
        -1, // fstore_0, 67
        -1, // fstore_1, 68
        -1, // fstore_2, 69
        -1, // fstore_3, 70
        -2, // dstore_0, 71
        -2, // dstore_1, 72
        -2, // dstore_2, 73
        -2, // dstore_3, 74
        -1, // astore_0, 75
        -1, // astore_1, 76
        -1, // astore_2, 77
        -1, // astore_3, 78
        -3, // iastore, 79
        -4, // lastore, 80
        -3, // fastore, 81
        -4, // dastore, 82
        -3, // aastore, 83
        -3, // bastore, 84
        -3, // castore, 85
        -3, // sastore, 86
        -1, // pop, 87
        -2, // pop2, 88
        1, // dup, 89
        1, // dup_x1, 90
        1, // dup_x2, 91
        2, // dup2, 92
        2, // dup2_x1, 93
        2, // dup2_x2, 94
        0, // swap, 95
        -1, // iadd, 96
        -2, // ladd, 97
        -1, // fadd, 98
        -2, // dadd, 99
        -1, // isub, 100
        -2, // lsub, 101
        -1, // fsub, 102
        -2, // dsub, 103
        -1, // imul, 104
        -2, // lmul, 105
        -1, // fmul, 106
        -2, // dmul, 107
        -1, // idiv, 108
        -2, // ldiv, 109
        -1, // fdiv, 110
        -2, // ddiv, 111
        -1, // irem, 112
        -2, // lrem, 113
        -1, // frem, 114
        -2, // drem, 115
        0, // ineg, 116
        0, // lneg, 117
        0, // fneg, 118
        0, // dneg, 119
        -1, // ishl, 120
        -1, // lshl, 121
        -1, // ishr, 122
        -1, // lshr, 123
        -1, // iushr, 124
        -1, // lushr, 125
        -1, // iand, 126
        -2, // land, 127
        -1, // ior, 128
        -2, // lor, 129
        -1, // ixor, 130
        -2, // lxor, 131
        0, // iinc, 132
        1, // i2l, 133
        0, // i2f, 134
        1, // i2d, 135
        -1, // l2i, 136
        -1, // l2f, 137
        0, // l2d, 138
        0, // f2i, 139
        1, // f2l, 140
        1, // f2d, 141
        -1, // d2i, 142
        0, // d2l, 143
        -1, // d2f, 144
        0, // i2b, 145
        0, // i2c, 146
        0, // i2s, 147
        -3, // lcmp, 148
        -1, // fcmpl, 149
        -1, // fcmpg, 150
        -3, // dcmpl, 151
        -3, // dcmpg, 152
        -1, // ifeq, 153
        -1, // ifne, 154
        -1, // iflt, 155
        -1, // ifge, 156
        -1, // ifgt, 157
        -1, // ifle, 158
        -2, // if_icmpeq, 159
        -2, // if_icmpne, 160
        -2, // if_icmplt, 161
        -2, // if_icmpge, 162
        -2, // if_icmpgt, 163
        -2, // if_icmple, 164
        -2, // if_acmpeq, 165
        -2, // if_acmpne, 166
        0, // goto, 167
        1, // jsr, 168
        0, // ret, 169
        -1, // tableswitch, 170
        -1, // lookupswitch, 171
        -1, // ireturn, 172
        -2, // lreturn, 173
        -1, // freturn, 174
        -2, // dreturn, 175
        -1, // areturn, 176
        0, // return, 177
        0, // getstatic, 178            depends on the type
        0, // putstatic, 179            depends on the type
        0, // getfield, 180             depends on the type
        0, // putfield, 181             depends on the type
        0, // invokevirtual, 182        depends on the type
        0, // invokespecial, 183        depends on the type
        0, // invokestatic, 184         depends on the type
        0, // invokeinterface, 185      depends on the type
        0, // undefined, 186
        1, // new, 187
        0, // newarray, 188
        0, // anewarray, 189
        0, // arraylength, 190
        -1, // athrow, 191              stack is cleared
        0, // checkcast, 192
        0, // instanceof, 193
        -1, // monitorenter, 194
        -1, // monitorexit, 195
        0, // wide, 196                 depends on the following opcode
        0, // multianewarray, 197       depends on the dimensions
        -1, // ifnull, 198
        -1, // ifnonnull, 199
        0, // goto_w, 200
        1 // jsr_w, 201
    };
}
