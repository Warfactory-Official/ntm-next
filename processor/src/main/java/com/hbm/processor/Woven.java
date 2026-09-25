// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import javax.tools.Diagnostic;

final class Woven {

    private Woven() {}

    static boolean rejectsAppend(
            Trees trees,
            JCTree.JCClassDecl clazz,
            JCTree.JCCompilationUnit cu,
            JCTree.JCMethodDecl declared,
            String woven) {
        if (!returns(declared.body)) return false;
        trees.printMessage(
                Diagnostic.Kind.ERROR,
                woven
                        + " is appended to the end of '"
                        + clazz.name
                        + "."
                        + declared.name
                        + "()', which returns before it; the woven call would not run. Restructure"
                        + " the body to fall through, or state the half by hand.",
                clazz,
                cu);
        return true;
    }

    private static boolean returns(JCTree.JCBlock body) {
        boolean[] found = new boolean[1];
        new TreeScanner() {
            @Override
            public void visitReturn(JCTree.JCReturn tree) {
                found[0] = true;
            }

            @Override
            public void visitLambda(JCTree.JCLambda tree) {}

            @Override
            public void visitClassDef(JCTree.JCClassDecl tree) {}
        }.scan(body);
        return found[0];
    }
}
