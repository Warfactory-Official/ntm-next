// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

public class NoteBuilder {

    private String beat = "";

    public static NoteBuilder start() {
        __asm__ {
            new "com/hbm/util/NoteBuilder";
            dup;
            invokespecial "com/hbm/util/NoteBuilder" "<init>" "()V";
            areturn;
        }
    }

    public static Hit[] translate(String beat) {
        __asm__ {
            .local hits "[Ljava/lang/String;";
            .local notes "[Lcom/hbm/util/NoteBuilder$Hit;";
            .local components "[Ljava/lang/String;";
            .local index "I";

            aload beat;
            ldc string "-";
            invokevirtual "java/lang/String" "split" "(Ljava/lang/String;)[Ljava/lang/String;";
            astore hits;
            aload hits;
            arraylength;
            anewarray "com/hbm/util/NoteBuilder$Hit";
            astore notes;
            iconst_0;
            istore index;

        guarded:
            iload index;
            aload hits;
            arraylength;
            if_icmpge complete;

            aload hits;
            iload index;
            aaload;
            ldc string ":";
            invokevirtual "java/lang/String" "split" "(Ljava/lang/String;)[Ljava/lang/String;";
            astore components;

            aload notes;
            iload index;
            new "com/hbm/util/NoteBuilder$Hit";
            dup;

            invokestatic "com/hbm/util/NoteBuilder$Instrument" "values" "()[Lcom/hbm/util/NoteBuilder$Instrument;";
            aload components;
            iconst_0;
            aaload;
            invokestatic "java/lang/Integer" "parseInt" "(Ljava/lang/String;)I";
            aaload;

            invokestatic "com/hbm/util/NoteBuilder$Note" "values" "()[Lcom/hbm/util/NoteBuilder$Note;";
            aload components;
            iconst_1;
            aaload;
            invokestatic "java/lang/Integer" "parseInt" "(Ljava/lang/String;)I";
            aaload;

            invokestatic "com/hbm/util/NoteBuilder$Octave" "values" "()[Lcom/hbm/util/NoteBuilder$Octave;";
            aload components;
            iconst_2;
            aaload;
            invokestatic "java/lang/Integer" "parseInt" "(Ljava/lang/String;)I";
            aaload;

            invokespecial "com/hbm/util/NoteBuilder$Hit" "<init>"
                "(Lcom/hbm/util/NoteBuilder$Instrument;Lcom/hbm/util/NoteBuilder$Note;Lcom/hbm/util/NoteBuilder$Octave;)V";
            aastore;

            iinc index 1;
            goto guarded;

        complete:
            aload notes;
        guardEnd:
            areturn;

        failed:
            pop;
            iconst_0;
            anewarray "com/hbm/util/NoteBuilder$Hit";
            .catch "java/lang/Exception" guarded guardEnd failed;
            areturn;
        }
    }

    public NoteBuilder add(Instrument instrument, Note note, Octave octave) {
        __asm__ {
            aload this;
            getfield "com/hbm/util/NoteBuilder" "beat" "Ljava/lang/String;";
            invokevirtual "java/lang/String" "isEmpty" "()Z";
            ifne append;

            aload this;
            aload this;
            getfield "com/hbm/util/NoteBuilder" "beat" "Ljava/lang/String;";
            ldc string "-";
            invokedynamic "java/lang/invoke/StringConcatFactory" "makeConcat"
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                "concat" "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
            putfield "com/hbm/util/NoteBuilder" "beat" "Ljava/lang/String;";

        append:
            aload this;
            aload this;
            getfield "com/hbm/util/NoteBuilder" "beat" "Ljava/lang/String;";

            aload instrument;
            invokevirtual "com/hbm/util/NoteBuilder$Instrument" "ordinal" "()I";
            ldc string ":";
            aload note;
            invokevirtual "com/hbm/util/NoteBuilder$Note" "ordinal" "()I";
            ldc string ":";
            aload octave;
            invokevirtual "com/hbm/util/NoteBuilder$Octave" "ordinal" "()I";
            invokedynamic "java/lang/invoke/StringConcatFactory" "makeConcat"
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                "concat" "(ILjava/lang/String;ILjava/lang/String;I)Ljava/lang/String;";

            invokedynamic "java/lang/invoke/StringConcatFactory" "makeConcat"
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                "concat" "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
            putfield "com/hbm/util/NoteBuilder" "beat" "Ljava/lang/String;";

            aload this;
            areturn;
        }
    }

    public String end() {
        return __asm__(String) {
            aload this;
            getfield "com/hbm/util/NoteBuilder" "beat" "Ljava/lang/String;";
        };
    }

    public enum Instrument {
        PIANO,
        BASSDRUM,
        SNARE,
        CLICKS,
        BASSGUITAR
    }

    public enum Note {
        F_SHARP,
        G,
        G_SHARP,
        A,
        A_SHARP,
        B,
        C,
        C_SHARP,
        D,
        D_SHARP,
        E,
        F
    }

    public enum Octave {
        LOW,
        MID,
        HIGH
    }

    public record Hit(Instrument instrument, Note note, Octave octave) {}
}
