// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Locale;
import javax.tools.Diagnostic;

final class DerivedState {

    static final String ANNOTATION = "NeighborDerived";

    private static final String VALUE_INPUT = "net.minecraft.world.level.storage.ValueInput";
    private static final String VALUE_OUTPUT = "net.minecraft.world.level.storage.ValueOutput";

    private final TreeMaker make;
    private final Names names;
    private final Trees trees;

    private final Name marker;

    DerivedState(TreeMaker make, Names names, Trees trees) {
        this.make = make;
        this.names = names;
        this.trees = trees;
        this.marker = names.fromString(ANNOTATION);
    }

    void augment(JCTree.JCCompilationUnit cu) {
        for (JCTree def : cu.defs) {
            if (def instanceof JCTree.JCClassDecl clazz) augment(clazz, cu);
        }
    }

    private void augment(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        for (JCTree member : clazz.defs) {
            if (member instanceof JCTree.JCClassDecl nested) augment(nested, cu);
        }

        ArrayList<Link> links = null;
        for (JCTree member : clazz.defs) {
            if (!(member instanceof JCTree.JCVariableDecl field)) continue;
            JCTree.JCAnnotation annotation = annotationOn(field);
            if (annotation == null) continue;
            if (links == null) links = new ArrayList<>(2);
            if ((field.mods.flags & Flags.STATIC) != 0) {
                trees.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@"
                                + ANNOTATION
                                + " field '"
                                + field.name
                                + "' is static, so it is not per-machine state",
                        clazz,
                        cu);
                return;
            }
            links.add(new Link(field.name, at(annotation)));
        }
        if (links == null) return;

        for (Link link : links) {
            if (!declares(clazz, link.refreshName(names))) continue;
            trees.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@"
                            + ANNOTATION
                            + " writes "
                            + link.refreshName(names)
                            + "(), and '"
                            + clazz.name
                            + "' already declares it; delete the"
                            + " hand-written refresh, or drop the annotation where the machine orders its own",
                    clazz,
                    cu);
            return;
        }

        make.at(clazz.pos);
        for (Link link : links) clazz.defs = clazz.defs.append(refresh(link));

        persist(clazz, cu, links, "loadAdditional", "load", "input", VALUE_INPUT);
        persist(clazz, cu, links, "saveAdditional", "save", "output", VALUE_OUTPUT);
    }

    private JCTree.JCMethodDecl refresh(Link link) {
        JCTree.JCExpression pos = make.Ident(names.fromString("worldPosition"));
        if (!link.at.isEmpty()) {
            pos = make.Apply(List.nil(), make.Select(pos, names.fromString(link.at)), List.nil());
        }
        JCTree.JCStatement body =
                make.Exec(
                        make.Apply(
                                List.nil(),
                                make.Select(make.Ident(link.field), names.fromString("refresh")),
                                List.of(make.Ident(names.fromString("level")), pos)));
        return make.MethodDef(
                make.Modifiers(Flags.PUBLIC),
                link.refreshName(names),
                make.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.nil(),
                List.nil(),
                make.Block(0, List.of(body)),
                null);
    }

    private void persist(
            JCTree.JCClassDecl clazz,
            JCTree.JCCompilationUnit cu,
            ArrayList<Link> links,
            String method,
            String call,
            String fallbackParam,
            String paramType) {
        JCTree.JCMethodDecl declared = find(clazz, method);
        Name param =
                declared != null && declared.params.size() == 1
                        ? declared.params.head.name
                        : names.fromString(fallbackParam);

        ArrayList<JCTree.JCStatement> added = new ArrayList<>();
        for (Link link : links) {
            added.add(
                    make.Exec(
                            make.Apply(
                                    List.nil(),
                                    make.Select(make.Ident(link.field), names.fromString(call)),
                                    List.of(
                                            make.Ident(param),
                                            make.Literal(link.field.toString())))));
        }

        if (declared != null && declared.body != null) {
            if (Woven.rejectsAppend(trees, clazz, cu, declared, "@" + ANNOTATION + " persistence"))
                return;
            for (JCTree.JCStatement statement : added) {
                declared.body.stats = declared.body.stats.append(statement);
            }
            return;
        }

        JCTree.JCVariableDecl argument =
                make.VarDef(make.Modifiers(Flags.PARAMETER), param, qualified(paramType), null);
        ListBuffer<JCTree.JCStatement> body = new ListBuffer<>();
        body.append(
                make.Exec(
                        make.Apply(
                                List.nil(),
                                make.Select(make.Ident(names._super), names.fromString(method)),
                                List.of(make.Ident(param)))));
        for (JCTree.JCStatement statement : added) body.append(statement);

        clazz.defs =
                clazz.defs.append(
                        make.MethodDef(
                                make.Modifiers(
                                        Flags.PROTECTED,
                                        List.of(
                                                make.Annotation(
                                                        qualified("java.lang.Override"),
                                                        List.nil()))),
                                names.fromString(method),
                                make.TypeIdent(TypeTag.VOID),
                                List.nil(),
                                List.of(argument),
                                List.nil(),
                                make.Block(0, body.toList()),
                                null));
    }

    private String at(JCTree.JCAnnotation annotation) {
        for (JCTree.JCExpression argument : annotation.args) {
            if (!(argument instanceof JCTree.JCAssign assign)) continue;
            if (!assign.lhs.toString().equals("at")) continue;
            if (assign.rhs instanceof JCTree.JCLiteral literal
                    && literal.value instanceof String text) {
                return text;
            }
        }
        return "";
    }

    private JCTree.JCAnnotation annotationOn(JCTree.JCVariableDecl field) {
        return Annotations.on(field.mods.annotations, marker);
    }

    private JCTree.JCMethodDecl find(JCTree.JCClassDecl clazz, String method) {
        for (JCTree member : clazz.defs) {
            if (member instanceof JCTree.JCMethodDecl declared
                    && declared.name.contentEquals(method)) {
                return declared;
            }
        }
        return null;
    }

    private boolean declares(JCTree.JCClassDecl clazz, Name method) {
        for (JCTree member : clazz.defs) {
            if (member instanceof JCTree.JCMethodDecl declared && declared.name == method)
                return true;
        }
        return false;
    }

    private JCTree.JCExpression qualified(String dotted) {
        int dot = dotted.indexOf('.');
        JCTree.JCExpression expr =
                make.Ident(names.fromString(dot < 0 ? dotted : dotted.substring(0, dot)));
        while (dot >= 0) {
            int next = dotted.indexOf('.', dot + 1);
            expr =
                    make.Select(
                            expr,
                            names.fromString(
                                    next < 0
                                            ? dotted.substring(dot + 1)
                                            : dotted.substring(dot + 1, next)));
            dot = next;
        }
        return expr;
    }

    private record Link(Name field, String at) {

        Name refreshName(Names names) {
            String raw = field.toString();
            return names.fromString(
                    "refresh" + raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1));
        }
    }
}
