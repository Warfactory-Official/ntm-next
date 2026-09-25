// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

import com.hbm.util.Calculator;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.hbm.module.mses.CompiledMses.*;
import static java.lang.classfile.ClassFile.ACC_FINAL;
import static java.lang.classfile.ClassFile.ACC_PRIVATE;
import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.classfile.ClassFile.ACC_STATIC;
import static java.lang.classfile.ClassFile.ACC_SUPER;
import static java.lang.classfile.ClassFile.JAVA_25_VERSION;
import static java.lang.constant.ConstantDescs.*;

public final class MsesCompiler {
    private MsesCompiler() {}

    private static final int REGION_SIZE = 64;
    private static final int MAX_SOURCE_UNITS = 65536;
    private static final int MAX_LINES = 32768;
    private static final int CACHE_SIZE = 64;
    private static final ClassDesc CTX = desc(MsesState.class);
    private static final ClassDesc HOST = desc(CompiledMses.Host.class);
    private static final ClassDesc PROGRAM = desc(CompiledMses.class);
    private static final ClassDesc RT = desc(MsesRuntime.class);
    private static final ClassDesc MATH = desc(Math.class);
    private static final ClassDesc SELF =
            ClassDesc.of(MsesCompiler.class.getPackageName() + ".Mses$Code");
    private static final MethodTypeDesc RUN = MethodTypeDesc.of(CD_void, CTX, HOST);
    private static final MethodTypeDesc REGION = MethodTypeDesc.of(CD_int, CTX, HOST, CD_int);
    private static final MethodTypeDesc EXPR = MethodTypeDesc.of(CD_int, CTX);
    private static final DirectMethodHandleDesc CONCAT_BSM =
            MethodHandleDesc.ofMethod(
                    DirectMethodHandleDesc.Kind.STATIC,
                    desc(StringConcatFactory.class),
                    "makeConcat",
                    MethodTypeDesc.of(
                            desc(java.lang.invoke.CallSite.class),
                            desc(MethodHandles.Lookup.class),
                            CD_String,
                            desc(MethodType.class)));
    private static final Map<SourceKey, WeakReference<MsesProgram>> CACHE =
            new LinkedHashMap<>(16, .75f, true);

    private static ClassDesc desc(Class<?> c) {
        return ClassDesc.ofDescriptor(c.descriptorString());
    }

    private static MethodTypeDesc type(ClassDesc result, ClassDesc... args) {
        return MethodTypeDesc.of(result, args);
    }

    public static MsesProgram compile(String[] source) {
        SourceKey key = new SourceKey(source.clone());
        synchronized (CACHE) {
            WeakReference<MsesProgram> ref = CACHE.get(key);
            MsesProgram cached = ref == null ? null : ref.get();
            if (cached != null) return cached;
        }
        Unit unit = decode(key.lines);
        MsesProgram result = new MsesProgram(define(bytecode(unit)), unit.slots);
        synchronized (CACHE) {
            CACHE.put(key, new WeakReference<>(result));
            while (CACHE.size() > CACHE_SIZE) CACHE.remove(CACHE.keySet().iterator().next());
        }
        return result;
    }

    public static byte[] bytecode(String[] source) {
        return bytecode(decode(source.clone()));
    }

    private static CompiledMses define(byte[] bytes) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, true);
            return (CompiledMses)
                    lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class))
                            .invoke();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void validate(String[] source) {
        if (source.length > MAX_LINES) throw new IllegalArgumentException("too many MSES lines");
        long units = source.length;
        for (String line : source) {
            if (line == null) continue;
            units += line.length();
            int modifiedUtf8 = 0;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                modifiedUtf8 += c >= 1 && c <= 127 ? 1 : c <= 2047 ? 2 : 3;
            }
            if (modifiedUtf8 > 60000)
                throw new IllegalArgumentException("MSES line too large for the constant pool");
        }
        if (units > MAX_SOURCE_UNITS) throw new IllegalArgumentException("MSES source too large");
    }

    private static final class SourceKey {
        final String[] lines;
        final int hash;

        SourceKey(String[] lines) {
            this.lines = lines;
            hash = Arrays.hashCode(lines);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SourceKey k && Arrays.equals(lines, k.lines);
        }
    }

    private enum Op {
        STATUS,
        NULL_LINE,
        BUFFER,
        BUFFER_PLAIN,
        CONST_DOUBLE,
        CONST_INT,
        LOAD,
        SAVE,
        CLOCK,
        JUMP,
        JUMPIF,
        JUMPNOT,
        EVAL,
        EVAL_BUFFER,
        ROUND,
        CONCAT,
        EQ,
        COMPARE,
        SPLITTER,
        SPLIT,
        SPLITCOUNT,
        PUSH_BUFFER,
        PUSH,
        POP,
        PEEK,
        LENGTH,
        FIRST,
        LAST,
        SEND,
        LISTEN,
        POLL,
        WORLDTIME
    }

    private static final class Insn {
        final Op op;
        final String argument;
        final MsesTemplate template;
        int parameter;
        int target = -1;
        double number;
        MsesExpression.Plan expression;

        Insn(Op op, String argument, int parameter) {
            this.op = op;
            this.argument = argument;
            this.parameter = parameter;
            template = argument == null ? null : MsesTemplate.parse(argument);
        }
    }

    private record Unit(Insn[] code, String[] slots, Map<String, Integer> slotOf) {}

    private static Insn status(int code) {
        return new Insn(Op.STATUS, null, code);
    }

    private static Insn op(Op op) {
        return new Insn(op, null, 0);
    }

    private static Insn arg(Op op, String line, int offset) {
        return line.length() <= offset
                ? status(PARAMETER_ERROR)
                : new Insn(op, line.substring(offset), 0);
    }

    private static Insn integer(Op op, String line, int offset) {
        if (line.length() <= offset) return status(PARAMETER_ERROR);
        try {
            return new Insn(op, null, Integer.parseInt(line.substring(offset)));
        } catch (NumberFormatException ex) {
            return status(PARAMETER_ERROR);
        }
    }

    private static Insn substitutableInteger(Op op, String line, int offset) {
        Insn ins = arg(op, line, offset);
        if (ins.op == Op.STATUS || !ins.template.constant()) return ins;
        try {
            ins.parameter = Integer.parseInt(ins.template.constantValue());
            return ins;
        } catch (NumberFormatException ex) {
            return status(PARAMETER_ERROR);
        }
    }

    private static Unit decode(String[] lines) {
        validate(lines);
        Map<String, Integer> destinations = new HashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];

            if (s != null && s.startsWith("dest ") && s.length() > 5)
                destinations.put(s.substring(5), i);
        }
        Insn[] code = new Insn[lines.length];
        Map<String, Integer> slotOf = new LinkedHashMap<>();
        for (int i = 0; i < lines.length; i++) {
            Insn ins = decode(lines[i], destinations);
            code[i] = ins;
            if (ins.op == Op.LOAD || ins.op == Op.SAVE)
                slotOf.putIfAbsent(ins.argument, slotOf.size());
            else if (ins.template != null) {
                for (MsesTemplate.Part part : ins.template.parts()) {
                    if (part.variable() && !part.text().equals("buffer")) {
                        slotOf.putIfAbsent(part.text(), slotOf.size());
                    }
                }
            }
        }
        return new Unit(code, slotOf.keySet().toArray(String[]::new), slotOf);
    }

    private static Insn decode(String line, Map<String, Integer> destinations) {
        if (line == null) return op(Op.NULL_LINE);
        String s = line.toLowerCase(Locale.US);
        if (line.isEmpty() || s.startsWith("dest ") || s.startsWith("# ")) return status(SKIP);
        Insn exact =
                switch (s) {
                    case "nop" -> status(OK);
                    case "endtick" -> status(END_TICK);
                    case "shutdown" -> status(SHUTDOWN);
                    case "eval" -> op(Op.EVAL_BUFFER);
                    case "evalr" -> new Insn(Op.EVAL_BUFFER, null, 1);
                    case "floor", "rounddown" -> new Insn(Op.ROUND, null, 0);
                    case "ceil", "roundup" -> new Insn(Op.ROUND, null, 1);
                    case "round", "nearest" -> new Insn(Op.ROUND, null, 2);
                    case "splitcount" -> op(Op.SPLITCOUNT);
                    case "push" -> op(Op.PUSH_BUFFER);
                    case "pop" -> op(Op.POP);
                    case "peek" -> op(Op.PEEK);
                    case "length" -> op(Op.LENGTH);
                    case "worldtime" -> op(Op.WORLDTIME);
                    default -> null;
                };
        if (exact != null) return exact;
        if (s.startsWith("clockspeed ")) return integer(Op.CLOCK, line, 11);
        if (s.startsWith("split ")) return substitutableInteger(Op.SPLIT, line, 6);
        if (s.startsWith("first ")) return substitutableInteger(Op.FIRST, line, 6);
        if (s.startsWith("last ")) return substitutableInteger(Op.LAST, line, 5);
        if (s.startsWith("jmp ") || s.startsWith("jmpif ") || s.startsWith("jmpnot ")) {
            Op kind =
                    s.startsWith("jmp ")
                            ? Op.JUMP
                            : s.startsWith("jmpif ") ? Op.JUMPIF : Op.JUMPNOT;
            Insn ins = arg(kind, line, kind == Op.JUMP ? 4 : kind == Op.JUMPIF ? 6 : 7);
            if (ins.template != null && ins.template.constant()) {
                ins.target = destinations.getOrDefault(ins.template.constantValue(), -1);
            }
            return ins;
        }
        if (s.startsWith("eval ") || s.startsWith("evalr ")) {
            boolean rounded = s.startsWith("evalr ");
            Insn ins = arg(Op.EVAL, line, rounded ? 6 : 5);
            if (ins.op != Op.EVAL) return ins;
            ins.parameter = rounded ? 1 : 0;
            String expression =
                    ins.template.constant() ? ins.template.constantValue() : ins.argument;
            ins.expression = MsesExpression.tryParse(expression);

            if (ins.template.constant() && canFoldLiteral(expression)) {
                double value;
                try {
                    value = Calculator.evaluateExpression(expression);
                } catch (RuntimeException ex) {
                    return status(PARAMETER_ERROR);
                }
                if (rounded) return new Insn(Op.CONST_INT, null, (int) Math.round(value));
                Insn folded = new Insn(Op.CONST_DOUBLE, null, 0);
                folded.number = value;
                return folded;
            }
            return ins;
        }
        if (s.startsWith("load ")) return arg(Op.LOAD, line, 5);
        if (s.startsWith("save ")) return arg(Op.SAVE, line, 5);
        if (s.startsWith("buffer ")) {
            Insn ins = arg(Op.BUFFER, line, 7);
            if (ins.op == Op.BUFFER
                    && ins.argument.length() <= MsesState.MAX_BUFFER_LENGTH
                    && MsesRuntime.plainNumber(ins.argument, true)) {
                Insn plain = new Insn(Op.BUFFER_PLAIN, ins.argument, 0);
                plain.number = Double.parseDouble(ins.argument);
                return plain;
            }
            return ins;
        }
        if (s.startsWith("concat ")) return arg(Op.CONCAT, line, 7);
        if (s.startsWith("eq ")) return arg(Op.EQ, line, 3);
        if (s.startsWith("gtb ")
                || s.startsWith("ltb ")
                || s.startsWith("geb ")
                || s.startsWith("leb ")) {
            Insn ins = arg(Op.COMPARE, line, 4);
            if (ins.op != Op.COMPARE) return ins;
            ins.parameter =
                    s.startsWith("gtb ")
                            ? 0
                            : s.startsWith("ltb ") ? 1 : s.startsWith("geb ") ? 2 : 3;
            if (ins.template.constant()) {
                try {
                    ins.number = Double.parseDouble(ins.template.constantValue());
                } catch (NumberFormatException ex) {

                    return status(PARAMETER_ERROR);
                }
            }
            return ins;
        }
        if (s.startsWith("send ")) return arg(Op.SEND, line, 5);
        if (s.startsWith("listen ")) return arg(Op.LISTEN, line, 7);
        if (s.startsWith("poll ")) return arg(Op.POLL, line, 5);
        if (s.startsWith("splitter ")) return arg(Op.SPLITTER, line, 9);
        if (s.startsWith("push ")) return arg(Op.PUSH, line, 5);
        return status(UNRECOGNIZED_COMMAND);
    }

    private static boolean canFoldLiteral(String expression) {
        if (expression.length() > 2048 || expression.indexOf('!') >= 0) return false;
        int depth = 0, powers = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(' && ++depth > 48) return false;
            if (ch == ')') depth--;
            if (ch == '^' && ++powers > 32) return false;
        }
        return true;
    }

    private static byte[] bytecode(Unit unit) {
        Insn[] instructions = unit.code;
        ClassFile cf =
                ClassFile.of(
                        ClassFile.ClassHierarchyResolverOption.of(
                                ClassHierarchyResolver.ofClassLoading(
                                        MsesCompiler.class.getClassLoader())));
        return cf.build(
                SELF,
                cb -> {
                    cb.withVersion(JAVA_25_VERSION, 0)
                            .withFlags(ACC_PUBLIC | ACC_FINAL | ACC_SUPER)
                            .withSuperclass(CD_Object)
                            .withInterfaceSymbols(PROGRAM);
                    cb.withMethodBody(
                            "<init>",
                            type(CD_void),
                            ACC_PUBLIC,
                            b ->
                                    b.aload(0)
                                            .invokespecial(CD_Object, "<init>", type(CD_void))
                                            .return_());
                    cb.withMethodBody(
                            "run",
                            RUN,
                            ACC_PUBLIC | ACC_FINAL,
                            b -> emitRun(b, instructions.length));
                    for (int start = 0; start < instructions.length; start += REGION_SIZE) {
                        int from = start, to = Math.min(start + REGION_SIZE, instructions.length);
                        cb.withMethodBody(
                                "r" + start / REGION_SIZE,
                                REGION,
                                ACC_PRIVATE | ACC_STATIC,
                                b -> emitRegion(b, unit, from, to));
                    }
                    for (int i = 0; i < instructions.length; i++) {
                        if (instructions[i].op == Op.EVAL) {
                            Insn ins = instructions[i];
                            cb.withMethodBody(
                                    "e" + i,
                                    EXPR,
                                    ACC_PRIVATE | ACC_STATIC,
                                    b -> emitExpression(b, unit, ins));
                        }
                    }
                });
    }

    private static void emitRun(CodeBuilder b, int length) {

        Label loop = b.newLabel(), done = b.newLabel(), eof = b.newLabel(), bad = b.newLabel();
        b.ldc(100 << 8).istore(3).labelBinding(loop);
        b.iload(3).iflt(done);
        b.iload(3).ldc(255).iand().aload(1).getfield(CTX, "clockSpeed", CD_int).if_icmpge(done);
        b.iload(3).ldc(8).iushr().ifeq(done);
        b.aload(1).getfield(CTX, "current", CD_int).istore(4);
        b.iload(4).ldc(length).if_icmpeq(eof);
        b.iload(4).iflt(bad);
        b.iload(4).ldc(length).if_icmpge(bad);
        int regions = (length + REGION_SIZE - 1) / REGION_SIZE;
        if (regions == 1) {
            b.aload(1).aload(2).iload(3).invokestatic(SELF, "r0", REGION).istore(3).goto_(loop);
        } else if (regions != 0) {
            List<SwitchCase> cases = new ArrayList<>();
            Label[] labels = new Label[regions];
            for (int i = 0; i < regions; i++) {
                labels[i] = b.newLabel();
                cases.add(SwitchCase.of(i, labels[i]));
            }
            b.iload(4).ldc(REGION_SIZE).idiv().tableswitch(0, regions - 1, bad, cases);
            for (int i = 0; i < regions; i++) {
                b.labelBinding(labels[i])
                        .aload(1)
                        .aload(2)
                        .iload(3)
                        .invokestatic(SELF, "r" + i, REGION)
                        .istore(3)
                        .goto_(loop);
            }
        } else b.goto_(bad);
        MethodTypeDesc endOfProgram = type(CD_void, CD_boolean);
        b.labelBinding(eof)
                .aload(2)
                .iconst_0()
                .invokeinterface(HOST, "msesEndOfProgram", endOfProgram)
                .return_();
        b.labelBinding(bad)
                .aload(2)
                .iconst_1()
                .invokeinterface(HOST, "msesEndOfProgram", endOfProgram)
                .return_();
        b.labelBinding(done).return_();
    }

    private static void emitRegion(CodeBuilder b, Unit unit, int from, int to) {
        Insn[] code = unit.code;

        Label dispatch = b.newLabel(), leave = b.newLabel();
        Label[] labels = new Label[to - from];
        List<SwitchCase> cases = new ArrayList<>();
        for (int i = from; i < to; i++) {
            labels[i - from] = b.newLabel();
            cases.add(SwitchCase.of(i, labels[i - from]));
        }
        b.iload(2).ldc(255).iand().istore(3);
        b.iload(2).ldc(8).iushr().istore(4);
        b.labelBinding(dispatch)
                .aload(0)
                .getfield(CTX, "current", CD_int)
                .tableswitch(from, to - 1, leave, cases);
        for (int i = from; i < to; i++) {
            Insn ins = code[i];
            b.labelBinding(labels[i - from]);

            b.iload(3).aload(0).getfield(CTX, "clockSpeed", CD_int).if_icmpge(leave);
            b.iload(4).ifle(leave).iinc(4, -1);
            Label start = b.newLabel(), end = b.newLabel(), handler = b.newLabel();
            b.labelBinding(start);
            b.aload(0).ldc(i + 1).putfield(CTX, "current", CD_int);
            emitInstruction(b, unit, ins, i);
            b.istore(5)
                    .aload(1)
                    .ldc(i)
                    .iload(5)
                    .invokeinterface(HOST, "msesAfterInstruction", type(CD_void, CD_int, CD_int));
            b.labelBinding(end);

            boolean endsTick = ins.op == Op.STATUS && ins.parameter == END_TICK;
            if (endsTick) {
                b.iconst_m1().ireturn();
            } else if (ins.op == Op.CLOCK) {
                Label charged = b.newLabel();
                b.iload(5).ldc(SKIP).if_icmpeq(charged).iinc(3, 1).labelBinding(charged);
            } else if (!(ins.op == Op.STATUS && ins.parameter == SKIP)) {
                b.iinc(3, 1);
            }
            if (endsTick) {

            } else if (ins.op == Op.JUMP && ins.template.constant() && ins.target >= 0) {
                b.goto_(target(labels, from, to, ins.target, leave));
            } else if ((ins.op == Op.JUMPIF || ins.op == Op.JUMPNOT)
                    && ins.template.constant()
                    && ins.target >= 0) {
                b.aload(0)
                        .getfield(CTX, "current", CD_int)
                        .ldc(ins.target)
                        .if_icmpeq(target(labels, from, to, ins.target, leave));
                b.goto_(target(labels, from, to, i + 1, leave));
            } else if (ins.op == Op.JUMP || ins.op == Op.JUMPIF || ins.op == Op.JUMPNOT) {
                if (ins.template.constant()) b.goto_(target(labels, from, to, i + 1, leave));
                else b.goto_(dispatch);
            } else {
                b.goto_(target(labels, from, to, i + 1, leave));
            }
            b.labelBinding(handler)
                    .pop()
                    .aload(1)
                    .invokeinterface(HOST, "msesEvaluationFailed", type(CD_void));

            b.iinc(3, 1).goto_(dispatch);
            b.exceptionCatch(start, end, handler, desc(Exception.class));
        }
        b.labelBinding(leave).iload(4).ldc(8).ishl().iload(3).ior().ireturn();
    }

    private static Label target(Label[] labels, int from, int to, int index, Label outside) {
        return index >= from && index < to ? labels[index - from] : outside;
    }

    private static void emitInstruction(CodeBuilder b, Unit unit, Insn ins, int sourceIndex) {
        boolean needsBuffer =
                ins.op == Op.CONCAT || ins.op == Op.EQ || ins.op == Op.COMPARE || ins.op == Op.SEND;
        Label empty = needsBuffer ? b.newLabel() : null;
        Label result = needsBuffer ? b.newLabel() : null;
        if (needsBuffer) b.aload(0).invokevirtual(CTX, "bufferEmpty", type(CD_boolean)).ifne(empty);
        switch (ins.op) {
            case STATUS -> b.ldc(ins.parameter);
            case NULL_LINE -> call(b, "nullLine", CD_int);
            case BUFFER -> {
                b.aload(0)
                        .ldc(ins.argument)
                        .invokevirtual(CTX, "writeBuffer", type(CD_boolean, CD_String))
                        .pop();
                b.ldc(OK);
            }
            case BUFFER_PLAIN -> {
                b.aload(0)
                        .ldc(ins.argument)
                        .ldc(ins.number)
                        .invokevirtual(CTX, "writePlain", type(CD_void, CD_String, CD_double));
                b.ldc(OK);
            }
            case CONST_DOUBLE -> {
                b.aload(0)
                        .ldc(ins.number)
                        .ldc(Double.toString(ins.number))
                        .invokevirtual(CTX, "writeDouble", type(CD_void, CD_double, CD_String));
                b.ldc(OK);
            }
            case CONST_INT -> {
                b.aload(0).ldc(ins.parameter).invokevirtual(CTX, "writeInt", type(CD_void, CD_int));
                b.ldc(OK);
            }
            case LOAD, SAVE ->
                    b.aload(0)
                            .ldc(unit.slotOf.get(ins.argument))
                            .invokevirtual(
                                    CTX, ins.op == Op.LOAD ? "load" : "save", type(CD_int, CD_int));
            case CLOCK -> {
                b.aload(0).ldc(ins.parameter);
                call(b, "clockspeed", CD_int, CTX, CD_int);
            }
            case JUMP, JUMPIF, JUMPNOT -> emitJump(b, unit, ins);
            case EVAL -> b.aload(0).invokestatic(SELF, "e" + sourceIndex, EXPR);
            case EVAL_BUFFER -> {
                b.aload(0).ldc(ins.parameter);
                call(b, "evaluateBuffer", CD_int, CTX, CD_boolean);
            }
            case ROUND -> {
                b.aload(0).ldc(ins.parameter);
                call(b, "round", CD_int, CTX, CD_int);
            }
            case CONCAT -> {
                b.aload(0);
                template(b, unit, ins.template, false);
                b.invokevirtual(CTX, "writeBuffer", type(CD_boolean, CD_String)).pop().ldc(OK);
            }
            case EQ, SEND, LISTEN, POLL, SPLITTER, PUSH -> {
                b.aload(0);
                template(b, unit, ins.template, false);
                String name =
                        switch (ins.op) {
                            case EQ -> "eq";
                            case SEND -> "send";
                            case LISTEN -> "listen";
                            case POLL -> "poll";
                            case SPLITTER -> "splitter";
                            default -> "push";
                        };
                call(b, name, CD_int, CTX, CD_String);
            }
            case COMPARE -> {
                Label start = b.newLabel(),
                        end = b.newLabel(),
                        caught = b.newLabel(),
                        done = b.newLabel();
                b.labelBinding(start).aload(0);
                b.aload(0).invokevirtual(CTX, "bufferJava", type(CD_double));
                List<MsesTemplate.Part> parts = ins.template.parts();
                if (ins.template.constant()) b.ldc(ins.number);
                else if (parts.size() == 1 && parts.getFirst().text().equals("buffer")) {
                    b.aload(0).invokevirtual(CTX, "bufferJava", type(CD_double));
                } else if (parts.size() == 1) {
                    b.aload(0)
                            .ldc(unit.slotOf.get(parts.getFirst().text()))
                            .invokevirtual(CTX, "slotJava", type(CD_double, CD_int));
                } else {
                    template(b, unit, ins.template, false);
                    b.invokestatic(CD_Double, "parseDouble", type(CD_double, CD_String));
                }
                b.ldc(ins.parameter);
                call(b, "compare", CD_int, CTX, CD_double, CD_double, CD_int);
                b.labelBinding(end)
                        .goto_(done)
                        .labelBinding(caught)
                        .pop()
                        .ldc(PARAMETER_ERROR)
                        .labelBinding(done);
                b.exceptionCatch(start, end, caught, desc(Exception.class));
            }
            case SPLIT -> {
                b.aload(0);
                if (ins.template.constant()) {
                    b.ldc(ins.parameter);
                    call(b, "split", CD_int, CTX, CD_int);
                } else {
                    template(b, unit, ins.template, true);
                    call(b, "split", CD_int, CTX, CD_String);
                }
            }
            case FIRST, LAST -> {
                b.aload(0);
                if (ins.template.constant()) {
                    b.ldc(ins.parameter).ldc(ins.op == Op.LAST ? 1 : 0);
                    call(b, "substring", CD_int, CTX, CD_int, CD_boolean);
                } else {
                    template(b, unit, ins.template, true);
                    b.ldc(ins.op == Op.LAST ? 1 : 0);
                    call(b, "substring", CD_int, CTX, CD_String, CD_boolean);
                }
            }
            case SPLITCOUNT, PUSH_BUFFER, POP, PEEK, WORLDTIME, LENGTH -> {
                b.aload(0);
                String name =
                        switch (ins.op) {
                            case SPLITCOUNT -> "splitcount";
                            case PUSH_BUFFER -> "pushBuffer";
                            case POP -> "pop";
                            case PEEK -> "peek";
                            case LENGTH -> "length";
                            default -> "worldtime";
                        };
                call(b, name, CD_int, CTX);
            }
        }
        if (needsBuffer)
            b.goto_(result).labelBinding(empty).ldc(PARAMETER_ERROR).labelBinding(result);
    }

    private static void emitJump(CodeBuilder b, Unit unit, Insn ins) {
        Label notTaken = b.newLabel(), done = b.newLabel();
        boolean conditional = ins.op != Op.JUMP;
        if (conditional) {
            b.aload(0).invokevirtual(CTX, "bufferIsTrue", type(CD_boolean));
            if (ins.op == Op.JUMPIF) b.ifeq(notTaken);
            else b.ifne(notTaken);
        }
        if (ins.template.constant()) {
            if (ins.target < 0) b.ldc(PARAMETER_ERROR);
            else b.aload(0).ldc(ins.target).putfield(CTX, "current", CD_int).ldc(OK);
        } else {
            b.aload(0);
            template(b, unit, ins.template, false);
            call(b, "jump", CD_int, CTX, CD_String);
        }
        if (conditional) b.goto_(done).labelBinding(notTaken).ldc(OK).labelBinding(done);
    }

    private static CodeBuilder call(
            CodeBuilder b, String name, ClassDesc result, ClassDesc... args) {
        return b.invokestatic(RT, name, type(result, args));
    }

    private static void template(CodeBuilder b, Unit unit, MsesTemplate t, boolean numeric) {
        if (t.constant()) {
            b.ldc(t.constantValue());
            return;
        }

        if (t.parts().size() > 24) {
            b.aload(0).ldc(t.source()).ldc(numeric ? 1 : 0);
            call(b, "substitute", CD_String, CTX, CD_String, CD_boolean);
            return;
        }
        for (MsesTemplate.Part part : t.parts()) {
            if (!part.variable()) b.ldc(part.text());
            else if (part.text().equals("buffer")) {
                b.aload(0)
                        .ldc(numeric ? 1 : 0)
                        .invokevirtual(CTX, "bufferText", type(CD_String, CD_boolean));
            } else {
                b.aload(0)
                        .ldc(unit.slotOf.get(part.text()))
                        .ldc(numeric ? 1 : 0)
                        .invokevirtual(CTX, "slotText", type(CD_String, CD_int, CD_boolean));
            }
        }
        if (t.parts().size() > 1) {
            ClassDesc[] args = new ClassDesc[t.parts().size()];
            Arrays.fill(args, CD_String);
            b.invokedynamic(DynamicCallSiteDesc.of(CONCAT_BSM, "concat", type(CD_String, args)));
        }
    }

    private static void emitExpression(CodeBuilder b, Unit unit, Insn ins) {
        MsesExpression.Plan plan = ins.expression;
        if (plan != null) {
            Label slow = b.newLabel();

            for (int i = 0; i < plan.inputs().size(); i++) {
                MsesExpression.Input input = plan.inputs().get(i);
                b.aload(0);
                if (input.name().equals("buffer")) {
                    b.ldc(input.allowNegative() ? 1 : 0)
                            .invokevirtual(CTX, "bufferPlain", type(CD_double, CD_boolean));
                } else {
                    b.ldc(unit.slotOf.get(input.name()))
                            .ldc(input.allowNegative() ? 1 : 0)
                            .invokevirtual(CTX, "slotPlain", type(CD_double, CD_int, CD_boolean));
                }
                b.dstore(1 + 2 * i).dload(1 + 2 * i).dload(1 + 2 * i).dcmpl().ifne(slow);
            }
            b.aload(0);
            expressionNode(b, plan.root());
            if (ins.parameter != 0) {
                b.invokestatic(MATH, "round", type(CD_long, CD_double))
                        .l2i()
                        .invokevirtual(CTX, "writeInt", type(CD_void, CD_int));
            } else {
                b.invokevirtual(CTX, "writeDouble", type(CD_void, CD_double));
            }
            b.ldc(OK).ireturn();
            b.labelBinding(slow);
        }
        b.aload(0);
        template(b, unit, ins.template, true);
        b.ldc(ins.parameter);
        call(b, "evaluate", CD_int, CTX, CD_String, CD_boolean).ireturn();
    }

    private static void expressionNode(CodeBuilder b, MsesExpression.Node node) {
        if (node instanceof MsesExpression.Constant n) b.ldc(n.value());
        else if (node instanceof MsesExpression.Variable n) {
            b.dload(1 + 2 * n.index());
            if (n.negate()) b.dneg();
        } else if (node instanceof MsesExpression.Binary n) {
            expressionNode(b, n.left());
            expressionNode(b, n.right());
            switch (n.operator()) {
                case '+' -> b.dadd();
                case '-' -> b.dsub();
                case '*' -> b.dmul();
                case '/' -> b.ddiv();
                default -> throw new AssertionError(n.operator());
            }
        } else if (node instanceof MsesExpression.Function n) {
            expressionNode(b, n.argument());
            if (n.name().equals("round"))
                b.invokestatic(MATH, "round", type(CD_long, CD_double)).l2d();
            else {
                String name =
                        n.name().equals("log") ? "log10" : n.name().equals("ln") ? "log" : n.name();
                b.invokestatic(MATH, name, type(CD_double, CD_double));
            }
        } else throw new AssertionError(node);
    }
}
