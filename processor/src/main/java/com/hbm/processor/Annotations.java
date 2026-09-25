// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

final class Annotations {

    private Annotations() {}

    static JCTree.JCAnnotation on(List<JCTree.JCAnnotation> annotations, Name simple) {
        if (annotations.isEmpty()) return null;
        for (JCTree.JCAnnotation annotation : annotations) {
            if (named(annotation.annotationType, simple)) return annotation;
        }
        return null;
    }

    private static boolean named(JCTree type, Name simple) {
        if (type instanceof JCTree.JCIdent ident) return ident.name == simple;
        if (type instanceof JCTree.JCFieldAccess access) return access.name == simple;
        return false;
    }
}
