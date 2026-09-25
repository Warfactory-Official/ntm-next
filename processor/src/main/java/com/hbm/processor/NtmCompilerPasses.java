// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileManager;
import mov.movblock.tenon.core.TypeRecorder;

final class NtmCompilerPasses implements TaskListener {
    private final Rotator rotator;
    private final NeighborEdge neighborEdge;
    private final DerivedState derivedState;
    private final FoldedLifecycle foldedLifecycle;
    private final MutationSync mutationSync;
    private final SyncArrayWrites syncArrayWrites;
    private final SyncIndexer syncIndexer;
    private final List<TypeRecorder> recorders = new ArrayList<>();
    private final Log log;

    NtmCompilerPasses(JavacTask task, String syncResource) {
        Context context = ((BasicJavacTask) task).getContext();
        JavaFileManager files = context.get(JavaFileManager.class);
        TreeMaker make = TreeMaker.instance(context);
        Names names = Names.instance(context);
        Trees trees = Trees.instance(task);
        log = Log.instance(context);
        rotator = new Rotator(make, names, trees);
        neighborEdge =
                new NeighborEdge(context, make, names, trees, task.getElements(), task.getTypes());
        derivedState = new DerivedState(make, names, trees);
        foldedLifecycle = new FoldedLifecycle(make, names, trees);
        mutationSync = new MutationSync(context, task);
        syncArrayWrites = new SyncArrayWrites(context, trees);
        syncIndexer =
                syncResource == null
                        ? null
                        : new SyncIndexer(task.getElements(), trees, files, syncResource);
        if (syncIndexer != null) recorders.add(syncIndexer);
    }

    byte[] rewriteSync(byte[] original) {
        return mutationSync.rewrite(original);
    }

    @Override
    public void started(TaskEvent event) {
        if (event.getKind() == TaskEvent.Kind.ENTER) neighborEdge.flush();
    }

    @Override
    public void finished(TaskEvent event) {
        if (event.getKind() == TaskEvent.Kind.COMPILATION) {
            if (log.nerrors != 0) return;
            if (syncIndexer != null) syncIndexer.write();
            return;
        }
        if (!(event.getCompilationUnit() instanceof JCTree.JCCompilationUnit unit)) return;
        switch (event.getKind()) {
            case PARSE -> {
                rotator.augment(unit);
                neighborEdge.scan(unit);
                derivedState.augment(unit);
                foldedLifecycle.augment(unit);
                mutationSync.augment(unit);
            }
            case ANALYZE -> analyze(event.getTypeElement(), unit);
            default -> {}
        }
    }

    private void analyze(TypeElement element, JCTree.JCCompilationUnit unit) {
        Symbol.ClassSymbol top = element instanceof Symbol.ClassSymbol symbol ? symbol : null;
        JCTree.JCClassDecl body = null;
        for (JCTree def : unit.defs) {
            if (def instanceof JCTree.JCClassDecl type && top != null && type.sym == top)
                body = type;
        }
        if (element != null && !recorders.isEmpty()) TypeRecorder.walk(recorders, element);
        if (body == null) return;
        neighborEdge.prove(body, unit);
        syncArrayWrites.rewrite(body, unit);
    }
}
