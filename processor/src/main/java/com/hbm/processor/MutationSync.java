// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

final class MutationSync {
    private static final String ANNOTATION = "com.hbm.packet.SyncField";
    private static final String PREFIX = "hbm$syncSet$";
    static final String ARRAY_PREFIX = "hbm$syncArraySet$";
    private final ParserFactory parser;
    private final Elements elements;
    private final Types types;
    private final ClassFile classFile;
    private final Map<String, String> setters = new HashMap<>();

    MutationSync(Context context, JavacTask task) {
        parser = ParserFactory.instance(context);
        elements = task.getElements();
        types = task.getTypes();
        ClassHierarchyResolver hierarchy =
                descriptor -> {
                    TypeElement type = type(descriptor);
                    if (type == null)
                        throw new IllegalArgumentException(
                                "Cannot resolve sync output type " + descriptor);
                    if (type.getKind().isInterface())
                        return ClassHierarchyResolver.ClassHierarchyInfo.ofInterface();
                    var parent = type.getSuperclass();
                    return ClassHierarchyResolver.ClassHierarchyInfo.ofClass(
                            parent.getKind() == TypeKind.NONE
                                    ? null
                                    : ClassDesc.of(
                                            elements.getBinaryName(
                                                            (TypeElement) types.asElement(parent))
                                                    .toString()));
                };
        classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(hierarchy.cached()));
    }

    void augment(JCTree.JCCompilationUnit unit) {
        new TreeScanner() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl owner) {
                super.visitClassDef(owner);
                var additions = new ArrayList<JCTree>();
                var inputs = new HashSet<String>();
                for (JCTree member : owner.defs) {
                    if (!(member instanceof JCTree.JCVariableDecl field)) continue;
                    JCTree.JCAnnotation declaration = null;
                    for (var annotation : field.mods.annotations) {
                        String name = annotation.annotationType.toString();
                        if (name.equals(ANNOTATION) || name.equals("SyncField"))
                            declaration = annotation;
                    }
                    if (declaration == null) continue;
                    inputs.add(field.name.toString());
                    if ((field.mods.flags & Flags.STATIC) != 0) {
                        throw new IllegalArgumentException(
                                "Static sync input " + owner.name + "." + field.name);
                    }
                    String mask = "3", units = "0L", callback = "";
                    for (var argument : declaration.args) {
                        if (argument instanceof JCTree.JCAssign assignment) {
                            if (assignment.lhs.toString().equals("value"))
                                mask = assignment.rhs.toString();
                            else if (assignment.lhs.toString().equals("units"))
                                units = assignment.rhs.toString();
                            else if (assignment.lhs.toString().equals("changed"))
                                callback = (String) ((JCTree.JCLiteral) assignment.rhs).value;
                        } else mask = argument.toString();
                    }
                    String name = PREFIX + owner.name + "$" + field.name;
                    String old = "this." + field.name;
                    String different = old + " != next";
                    JCTree.JCExpression declared = field.vartype;
                    while (declared instanceof JCTree.JCAnnotatedType annotated)
                        declared = annotated.underlyingType;
                    boolean reference = !(declared instanceof JCTree.JCPrimitiveTypeTree);
                    boolean string =
                            callback.isEmpty()
                                    && (declared.toString().equals("String")
                                            || declared.toString().equals("java.lang.String"));
                    if (declared.toString().equals("float")) {
                        different =
                                "Float.floatToRawIntBits("
                                        + old
                                        + ") != Float.floatToRawIntBits(next)";
                    } else if (declared.toString().equals("double")) {
                        different =
                                "Double.doubleToRawLongBits("
                                        + old
                                        + ") != Double.doubleToRawLongBits(next)";
                    }
                    String before =
                            reference && !string
                                    ? "boolean bound = syncBound(); if (bound) com.hbm.packet.SyncBindings.unbind(this, "
                                            + old
                                            + "); "
                                    : "";
                    String after =
                            reference && !string
                                    ? "if (bound) bindSyncValue(next, "
                                            + mask
                                            + ", "
                                            + units
                                            + "); "
                                    : "";
                    String notification =
                            declared instanceof JCTree.JCArrayTypeTree
                                    ? "syncArrayChanged(next, -1, " + mask + ", " + units + "); "
                                    : "if (("
                                            + units
                                            + ") == 0L) syncChanged("
                                            + mask
                                            + "); else syncUnitsChanged("
                                            + mask
                                            + ", "
                                            + units
                                            + "); ";
                    String valueCheck =
                            string
                                    ? "boolean changed = !java.util.Objects.equals("
                                            + old
                                            + ", next);"
                                    : "";
                    String source =
                            "class Generated { public final void "
                                    + name
                                    + "("
                                    + field.vartype
                                    + " next) { if ("
                                    + different
                                    + ") { "
                                    + before
                                    + valueCheck
                                    + old
                                    + " = next; "
                                    + after
                                    + (string ? "if (changed) " : "")
                                    + notification
                                    + (callback.isEmpty() ? "" : callback + "(-1);")
                                    + " } } }";
                    if ((field.mods.flags & Flags.FINAL) == 0)
                        additions.add(member(source, field.pos));
                    if (declared instanceof JCTree.JCArrayTypeTree array) {
                        String item = array.elemtype.toString();
                        String comparison = "array[index] != next";
                        if (item.equals("float"))
                            comparison =
                                    "Float.floatToRawIntBits(array[index]) != Float.floatToRawIntBits(next)";
                        if (item.equals("double"))
                            comparison =
                                    "Double.doubleToRawLongBits(array[index]) != Double.doubleToRawLongBits(next)";
                        boolean stringItem =
                                callback.isEmpty()
                                        && (item.equals("String")
                                                || item.equals("java.lang.String"));
                        String children =
                                array.elemtype instanceof JCTree.JCPrimitiveTypeTree || stringItem
                                        ? ""
                                        : "if (array == this."
                                                + field.name
                                                + " && syncBound()) com.hbm.packet.SyncBindings.unbind(this, array[index]);";
                        String body =
                                "class Generated { public final "
                                        + item
                                        + " "
                                        + ARRAY_PREFIX
                                        + owner.name
                                        + "$"
                                        + field.name
                                        + "("
                                        + field.vartype
                                        + " array, int index, "
                                        + item
                                        + " next) { if ("
                                        + comparison
                                        + ") { "
                                        + children
                                        + (stringItem
                                                ? "boolean changed = !java.util.Objects.equals(array[index], next);"
                                                : "")
                                        + " array[index] = next; if (array == this."
                                        + field.name
                                        + (stringItem ? " && changed" : "")
                                        + ") { syncArrayChanged(array, index, "
                                        + mask
                                        + ", "
                                        + units
                                        + "); "
                                        + (callback.isEmpty() ? "" : callback + "(index);")
                                        + " } } return next; } }";
                        additions.add(member(body, field.pos));
                    }
                }
                refuseEarlyWrites(owner, inputs);
                owner.defs = owner.defs.appendList(List.from(additions));
            }
        }.scan(unit);
    }

    private static void refuseEarlyWrites(JCTree.JCClassDecl owner, Set<String> inputs) {
        if (inputs.isEmpty()) return;
        for (JCTree member : owner.defs) {
            if (!(member instanceof JCTree.JCMethodDecl method)
                    || !method.name.contentEquals("<init>")
                    || method.body == null) continue;
            int boundary = -1, index = 0;
            for (var statement : method.body.stats) {
                if (statement instanceof JCTree.JCExpressionStatement expression
                        && expression.expr instanceof JCTree.JCMethodInvocation call
                        && (call.meth.toString().equals("super")
                                || call.meth.toString().equals("this"))) {
                    boundary = index;
                    break;
                }
                index++;
            }
            if (boundary <= 0) continue;
            var locals = new HashSet<String>();
            for (var parameter : method.params) locals.add(parameter.name.toString());
            for (int i = 0; i < boundary; i++) {
                new TreeScanner() {
                    @Override
                    public void visitVarDef(JCTree.JCVariableDecl tree) {
                        locals.add(tree.name.toString());
                        super.visitVarDef(tree);
                    }

                    @Override
                    public void visitAssign(JCTree.JCAssign tree) {
                        String field =
                                tree.lhs instanceof JCTree.JCIdent ident
                                                && !locals.contains(ident.name.toString())
                                        ? ident.name.toString()
                                        : tree.lhs instanceof JCTree.JCFieldAccess select
                                                        && select.selected.toString().equals("this")
                                                ? select.name.toString()
                                                : null;
                        if (inputs.contains(field))
                            throw new IllegalArgumentException(
                                    "Sync input written before constructor delegation: "
                                            + owner.name
                                            + "."
                                            + field);
                        super.visitAssign(tree);
                    }
                }.scan(method.body.stats.get(i));
            }
        }
    }

    private JCTree member(String source, int pos) {
        var generated = parser.newParser(source, false, false, false).parseCompilationUnit();
        JCTree member = ((JCTree.JCClassDecl) generated.defs.head).defs.head;
        new TreeScanner() {
            @Override
            public void scan(JCTree tree) {
                if (tree == null) return;
                tree.pos = pos;
                super.scan(tree);
            }
        }.scan(member);
        return member;
    }

    byte[] rewrite(byte[] original) {
        var model = classFile.parse(original);
        boolean[] changed = {false};
        byte[] result =
                classFile.transformClass(
                        model,
                        (output, element) -> {
                            if (!(element instanceof MethodModel method)
                                    || method.methodName().stringValue().startsWith(PREFIX)) {
                                output.with(element);
                                return;
                            }
                            output.transformMethod(
                                    method,
                                    (methodOutput, member) -> {
                                        if (!(member instanceof CodeModel code)) {
                                            methodOutput.with(member);
                                            return;
                                        }
                                        methodOutput.transformCode(
                                                code,
                                                (instructions, instruction) -> {
                                                    if (instruction
                                                                    instanceof
                                                                    FieldInstruction field
                                                            && field.opcode() == Opcode.PUTFIELD) {
                                                        String setter =
                                                                setter(
                                                                        field.owner().asSymbol(),
                                                                        field.name().stringValue());
                                                        if (setter != null) {
                                                            instructions.invokevirtual(
                                                                    field.owner().asSymbol(),
                                                                    setter,
                                                                    MethodTypeDesc.of(
                                                                            ConstantDescs.CD_void,
                                                                            field.typeSymbol()));
                                                            changed[0] = true;
                                                            return;
                                                        }
                                                    }
                                                    instructions.with(instruction);
                                                });
                                    });
                        });
        return changed[0] ? result : original;
    }

    private String setter(ClassDesc owner, String name) {
        String key = owner.descriptorString() + "." + name;
        if (setters.containsKey(key)) return setters.get(key);
        String result = null;
        for (TypeElement current = type(owner); current != null; ) {
            Element field = null;
            for (Element member : current.getEnclosedElements()) {
                if (member.getKind() == ElementKind.FIELD
                        && member.getSimpleName().contentEquals(name)) {
                    field = member;
                    break;
                }
            }
            if (field != null) {
                if (!field.getModifiers().contains(Modifier.FINAL)) {
                    for (var annotation : field.getAnnotationMirrors()) {
                        if (annotation.getAnnotationType().toString().equals(ANNOTATION)) {
                            result = PREFIX + current.getSimpleName() + "$" + name;
                        }
                    }
                }
                break;
            }
            var parent = current.getSuperclass();
            current =
                    parent.getKind() == TypeKind.NONE
                            ? null
                            : (TypeElement) types.asElement(parent);
        }
        setters.put(key, result);
        return result;
    }

    private TypeElement type(ClassDesc descriptor) {
        return elements.getTypeElement(
                descriptor
                        .descriptorString()
                        .substring(1, descriptor.descriptorString().length() - 1)
                        .replace('/', '.')
                        .replace('$', '.'));
    }
}
