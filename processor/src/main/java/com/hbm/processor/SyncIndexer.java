// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import mov.movblock.tenon.core.IndexFile;
import mov.movblock.tenon.core.TypeRecorder;

final class SyncIndexer implements TypeRecorder {

    private static final String ANNOTATION = "com.hbm.inventory.container.ContainerSync";
    private static final Set<String> PACKABLE = Set.of("boolean", "short", "int", "long");

    private final Elements elements;
    private final Trees trees;
    private final IndexFile file;

    private final Set<String> found = new TreeSet<>();
    private final Set<String> compiled = new HashSet<>();

    SyncIndexer(Elements elements, Trees trees, JavaFileManager files, String resource) {
        this.elements = elements;
        this.trees = trees;
        this.file =
                new IndexFile(
                        files,
                        resource,
                        "container-sync index",
                        "so a menu would ship a stale field set",
                        "<owner> <field> <type>");
    }

    @Override
    public void type(TypeElement type) {
        compiled.add(elements.getBinaryName(type).toString());
    }

    @Override
    public void field(TypeElement owner, Element field) {
        if (!annotated(field)) return;
        check(field);
        found.add(
                elements.getBinaryName(owner)
                        + " "
                        + field.getSimpleName()
                        + " "
                        + (isEnum(field.asType()) ? "enum" : field.asType()));
    }

    private boolean annotated(Element member) {
        for (AnnotationMirror a : member.getAnnotationMirrors()) {
            if (a.getAnnotationType().toString().equals(ANNOTATION)) return true;
        }
        return false;
    }

    private boolean isEnum(TypeMirror type) {
        return type instanceof DeclaredType declared
                && declared.asElement().getKind() == ElementKind.ENUM;
    }

    private void check(Element field) {
        String what = null;
        if (field.getModifiers().contains(Modifier.STATIC))
            what = "is static, so it is not per-machine state";
        else if (field.getModifiers().contains(Modifier.FINAL))
            what = "is final, so it never changes to ship";
        else if (!PACKABLE.contains(field.asType().toString()) && !isEnum(field.asType())) {
            what =
                    "is a "
                            + field.asType()
                            + "; a synced field packs into 16-bit slots, so it must be "
                            + PACKABLE
                            + " or an enum";
        }
        if (what != null) {
            trees.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@ContainerSync field '" + field.getSimpleName() + "' " + what,
                    trees.getTree(field),
                    trees.getPath(field).getCompilationUnit());
        }
    }

    void write() {
        file.write(found, compiled, line -> line.substring(0, line.indexOf(' ')));
    }
}
