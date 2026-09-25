// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

final class FoldedLifecycle {
    private final TreeMaker make;
    private final Names names;
    private final Trees trees;

    FoldedLifecycle(TreeMaker make, Names names, Trees trees) {
        this.make = make;
        this.names = names;
        this.trees = trees;
    }

    void augment(JCTree.JCCompilationUnit unit) {
        for (JCTree def : unit.defs) {
            if (def instanceof JCTree.JCClassDecl type) augment(type, unit);
        }
    }

    private void augment(JCTree.JCClassDecl type, JCTree.JCCompilationUnit unit) {
        if ((type.mods.flags & Flags.INTERFACE) != 0) return;
        for (JCTree member : type.defs) {
            if (member instanceof JCTree.JCClassDecl nested) augment(nested, unit);
        }
        boolean resident = false;
        for (JCTree.JCExpression face : type.implementing) {
            if (face instanceof JCTree.JCIdent ident
                    && ident.name.contentEquals("FoldedCoreResident")) resident = true;
            if (face instanceof JCTree.JCFieldAccess access
                    && access.name.contentEquals("FoldedCoreResident")) resident = true;
        }
        if (!resident) return;
        append(type, unit, "clearRemoved", "onLoad");
        append(type, unit, "setRemoved", "onRemove");
    }

    private void append(
            JCTree.JCClassDecl type, JCTree.JCCompilationUnit unit, String lifecycle, String call) {
        make.at(type.pos);
        Name name = names.fromString(lifecycle);
        JCTree.JCStatement action =
                make.Exec(
                        make.Apply(
                                List.nil(),
                                make.Select(
                                        make.Ident(names.fromString("FoldedCoreResident")),
                                        names.fromString(call)),
                                List.of(make.Ident(names._this))));
        for (JCTree member : type.defs) {
            if (member instanceof JCTree.JCMethodDecl declared
                    && declared.name == name
                    && declared.params.isEmpty()
                    && declared.body != null) {
                if (Woven.rejectsAppend(
                        trees, type, unit, declared, "FoldedCoreResident." + call + "()")) return;
                declared.body.stats = declared.body.stats.append(action);
                return;
            }
        }
        type.defs =
                type.defs.append(
                        make.MethodDef(
                                make.Modifiers(
                                        Flags.PUBLIC,
                                        List.of(
                                                make.Annotation(
                                                        make.Select(
                                                                make.Select(
                                                                        make.Ident(
                                                                                names.fromString(
                                                                                        "java")),
                                                                        names.fromString("lang")),
                                                                names.fromString("Override")),
                                                        List.nil()))),
                                name,
                                make.TypeIdent(TypeTag.VOID),
                                List.nil(),
                                List.nil(),
                                List.nil(),
                                make.Block(
                                        0,
                                        List.of(
                                                make.Exec(
                                                        make.Apply(
                                                                List.nil(),
                                                                make.Select(
                                                                        make.Ident(names._super),
                                                                        name),
                                                                List.nil())),
                                                action)),
                                null));
    }
}
