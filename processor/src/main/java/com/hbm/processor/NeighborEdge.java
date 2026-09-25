// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

final class NeighborEdge {

    static final String ANNOTATION = "RefreshesNeighborState";

    private static final String FOLDED_FQN = "com.hbm.blocks.multiblock.BlockMultiblockCore";

    private static final int MAX_CHAIN = 32;

    private static final String FOLDED_BASE = "BlockMultiblockCore";
    private static final String BLOCK_ENTITY = "net.minecraft.world.level.block.entity.BlockEntity";
    private static final String BLOCK_POS = "net.minecraft.core.BlockPos";
    private static final String BLOCK_STATE = "net.minecraft.world.level.block.state.BlockState";
    private static final String SERVER_LEVEL = "net.minecraft.server.level.ServerLevel";
    private static final String LEVEL = "net.minecraft.world.level.Level";
    private static final String BLOCK = "net.minecraft.world.level.block.Block";
    private static final String ORIENTATION = "net.minecraft.world.level.redstone.Orientation";

    private static final java.util.List<String> WRITES =
            java.util.List.of(
                    "wantsNeighborUpdates", "cellNeighborChanged", "neighborChanged", "onPlace");

    private record Pending(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {}

    private record SuperRef(JCTree.JCExpression type, JCTree.JCCompilationUnit cu) {}

    private final TreeMaker make;
    private final Names names;
    private final Trees trees;
    private final Elements elements;
    private final Types types;
    private final ClassFinder classes;
    private final Symtab symbols;

    private final Name marker;
    private final Name foldedBase;

    private final Map<Name, SuperRef> supers = new HashMap<>();
    private final ArrayList<Pending> pending = new ArrayList<>();
    private final Map<JCTree.JCClassDecl, Boolean> written = new IdentityHashMap<>();
    private boolean flushed;
    private TypeMirror foldedType;
    private boolean foldedTypeResolved;

    NeighborEdge(
            Context context,
            TreeMaker make,
            Names names,
            Trees trees,
            Elements elements,
            Types types) {
        this.make = make;
        this.names = names;
        this.trees = trees;
        this.elements = elements;
        this.types = types;
        this.classes = ClassFinder.instance(context);
        this.symbols = Symtab.instance(context);
        this.marker = names.fromString(ANNOTATION);
        this.foldedBase = names.fromString(FOLDED_BASE);
    }

    void scan(JCTree.JCCompilationUnit cu) {
        flushed = false;
        for (JCTree def : cu.defs) {
            if (def instanceof JCTree.JCClassDecl clazz) scan(clazz, cu);
        }
    }

    private void scan(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        for (JCTree member : clazz.defs) {
            if (member instanceof JCTree.JCClassDecl nested) scan(nested, cu);
        }
        if (clazz.extending != null) supers.put(clazz.name, new SuperRef(clazz.extending, cu));
        if (annotationOn(clazz) != null) pending.add(new Pending(clazz, cu));
    }

    void flush() {
        if (flushed) return;
        flushed = true;
        for (Pending held : pending) emit(held.clazz(), held.cu());
        pending.clear();
    }

    private void emit(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        JCTree.JCAnnotation annotation = annotationOn(clazz);
        if (annotation == null) return;

        String be = beName(annotation);
        ArrayList<String> calls = calls(annotation);
        if (be == null || calls.isEmpty()) {
            trees.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@"
                            + ANNOTATION
                            + " on '"
                            + clazz.name
                            + "' needs be = <BlockEntity>.class and at least one calling = \"method\"",
                    clazz,
                    cu);
            return;
        }
        for (String method : WRITES) {
            if (!declares(clazz, method)) continue;
            trees.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@"
                            + ANNOTATION
                            + " writes "
                            + method
                            + "(), and '"
                            + clazz.name
                            + "' already declares it; delete the hand-written forward, or"
                            + " drop the annotation where the block means a narrower law",
                    clazz,
                    cu);
            return;
        }

        make.at(clazz.pos);
        boolean folded = reachesFoldedBase(clazz, cu);
        written.put(clazz, folded);
        if (folded) {
            clazz.defs =
                    clazz.defs
                            .append(wantsNeighborUpdates())
                            .append(cellNeighborChanged(be, calls));
        } else {
            clazz.defs =
                    clazz.defs
                            .append(plainNeighborChanged(be, calls))
                            .append(plainOnPlace(be, calls));
        }
    }

    private JCTree.JCMethodDecl wantsNeighborUpdates() {
        return make.MethodDef(
                overriding(Flags.PUBLIC),
                names.fromString("wantsNeighborUpdates"),
                make.TypeIdent(TypeTag.BOOLEAN),
                List.nil(),
                List.nil(),
                List.nil(),
                make.Block(0, List.of(make.Return(make.Literal(true)))),
                null);
    }

    private JCTree.JCMethodDecl cellNeighborChanged(String be, ArrayList<String> calls) {
        JCTree.JCVariableDecl level = param("level", qualified(SERVER_LEVEL));
        JCTree.JCVariableDecl core = param("core", qualified(BLOCK_POS));
        JCTree.JCVariableDecl cell = param("cell", qualified(BLOCK_POS));
        return make.MethodDef(
                overriding(Flags.PUBLIC),
                names.fromString("cellNeighborChanged"),
                make.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.of(level, core, cell),
                List.nil(),
                make.Block(0, dispatch(be, calls, make.Ident(level.name), make.Ident(core.name))),
                null);
    }

    private JCTree.JCMethodDecl plainNeighborChanged(String be, ArrayList<String> calls) {
        JCTree.JCVariableDecl state = param("state", qualified(BLOCK_STATE));
        JCTree.JCVariableDecl level = param("level", qualified(LEVEL));
        JCTree.JCVariableDecl pos = param("pos", qualified(BLOCK_POS));
        JCTree.JCVariableDecl block = param("neighborBlock", qualified(BLOCK));
        JCTree.JCVariableDecl orientation = param("orientation", qualified(ORIENTATION));
        JCTree.JCVariableDecl moved = param("movedByPiston", make.TypeIdent(TypeTag.BOOLEAN));

        ListBuffer<JCTree.JCStatement> body = new ListBuffer<>();
        body.append(
                make.Exec(
                        superCall(
                                "neighborChanged",
                                make.Ident(state.name),
                                make.Ident(level.name),
                                make.Ident(pos.name),
                                make.Ident(block.name),
                                make.Ident(orientation.name),
                                make.Ident(moved.name))));
        body.appendList(dispatch(be, calls, make.Ident(level.name), make.Ident(pos.name)));

        return make.MethodDef(
                overriding(Flags.PROTECTED),
                names.fromString("neighborChanged"),
                make.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.of(state, level, pos, block, orientation, moved),
                List.nil(),
                make.Block(0, body.toList()),
                null);
    }

    private JCTree.JCMethodDecl plainOnPlace(String be, ArrayList<String> calls) {
        JCTree.JCVariableDecl state = param("state", qualified(BLOCK_STATE));
        JCTree.JCVariableDecl level = param("level", qualified(LEVEL));
        JCTree.JCVariableDecl pos = param("pos", qualified(BLOCK_POS));
        JCTree.JCVariableDecl oldState = param("oldState", qualified(BLOCK_STATE));
        JCTree.JCVariableDecl moved = param("movedByPiston", make.TypeIdent(TypeTag.BOOLEAN));

        ListBuffer<JCTree.JCStatement> body = new ListBuffer<>();
        body.append(
                make.Exec(
                        superCall(
                                "onPlace",
                                make.Ident(state.name),
                                make.Ident(level.name),
                                make.Ident(pos.name),
                                make.Ident(oldState.name),
                                make.Ident(moved.name))));
        body.appendList(dispatch(be, calls, make.Ident(level.name), make.Ident(pos.name)));

        return make.MethodDef(
                overriding(Flags.PROTECTED),
                names.fromString("onPlace"),
                make.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.of(state, level, pos, oldState, moved),
                List.nil(),
                make.Block(0, body.toList()),
                null);
    }

    private List<JCTree.JCStatement> dispatch(
            String be,
            ArrayList<String> calls,
            JCTree.JCExpression level,
            JCTree.JCExpression pos) {
        Name local = names.fromString("hbm$be");
        JCTree.JCVariableDecl held =
                make.VarDef(
                        make.Modifiers(0),
                        local,
                        qualified(BLOCK_ENTITY),
                        make.Apply(
                                List.nil(),
                                make.Select(level, names.fromString("getBlockEntity")),
                                List.of(pos)));

        ListBuffer<JCTree.JCStatement> body = new ListBuffer<>();
        for (String call : calls) {
            body.append(
                    make.Exec(
                            make.Apply(
                                    List.nil(),
                                    make.Select(
                                            make.TypeCast(qualified(be), make.Ident(local)),
                                            names.fromString(call)),
                                    List.nil())));
        }
        JCTree.JCStatement guarded = body.size() == 1 ? body.first() : make.Block(0, body.toList());
        return List.of(
                held, make.If(make.TypeTest(make.Ident(local), qualified(be)), guarded, null));
    }

    private JCTree.JCExpression superCall(String method, JCTree.JCExpression... args) {
        return make.Apply(
                List.nil(),
                make.Select(make.Ident(names._super), names.fromString(method)),
                List.from(args));
    }

    private JCTree.JCModifiers overriding(long access) {
        return make.Modifiers(
                access, List.of(make.Annotation(qualified("java.lang.Override"), List.nil())));
    }

    private JCTree.JCVariableDecl param(String name, JCTree.JCExpression type) {
        return make.VarDef(make.Modifiers(Flags.PARAMETER), names.fromString(name), type, null);
    }

    private String beName(JCTree.JCAnnotation annotation) {
        JCTree.JCExpression value = argument(annotation, "be");
        if (!(value instanceof JCTree.JCFieldAccess access)) return null;
        if (!access.name.contentEquals("class")) return null;
        String selected = access.selected.toString();
        return selected.substring(selected.lastIndexOf('.') + 1);
    }

    private ArrayList<String> calls(JCTree.JCAnnotation annotation) {
        ArrayList<String> out = new ArrayList<>();
        literals(argument(annotation, "calling"), out);
        return out;
    }

    private JCTree.JCExpression argument(JCTree.JCAnnotation annotation, String name) {
        for (JCTree.JCExpression argument : annotation.args) {
            if (argument instanceof JCTree.JCAssign assign && assign.lhs.toString().equals(name)) {
                return assign.rhs;
            }
        }
        return null;
    }

    private void literals(JCTree.JCExpression value, ArrayList<String> out) {
        if (value instanceof JCTree.JCNewArray array) {
            for (JCTree.JCExpression element : array.elems) literals(element, out);
        } else if (value instanceof JCTree.JCLiteral literal
                && literal.value instanceof String text) {
            out.add(text);
        }
    }

    private boolean reachesFoldedBase(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        SuperRef current = new SuperRef(clazz.extending, cu);
        for (int hop = 0; current.type() != null && hop < MAX_CHAIN; hop++) {
            Name name = superName(current.type());
            if (name == null) return false;
            if (name == foldedBase) return true;
            SuperRef parsed = supers.get(name);
            if (parsed != null) {
                current = parsed;
                continue;
            }
            TypeElement compiled = resolveSuper(current);
            return compiled != null && reachesFoldedBase(compiled, MAX_CHAIN - hop - 1);
        }
        return false;
    }

    private boolean reachesFoldedBase(TypeElement type, int remaining) {
        for (int hop = 0; type != null && hop < remaining; hop++) {
            if (type.getQualifiedName().contentEquals(FOLDED_FQN)) return true;
            var parent = types.asElement(type.getSuperclass());
            type = parent instanceof TypeElement element ? element : null;
        }
        return false;
    }

    private TypeElement resolveSuper(SuperRef ref) {
        JCTree.JCExpression raw =
                ref.type() instanceof JCTree.JCTypeApply apply ? apply.clazz : ref.type();
        String name = raw.toString();
        TypeElement found = loadClass(name);
        if (found != null) return found;

        String packageName =
                ref.cu().getPackageName() == null ? "" : ref.cu().getPackageName().toString();
        if (!packageName.isEmpty()) {
            found = loadClass(packageName + "." + name);
            if (found != null) return found;
        }

        int dot = name.indexOf('.');
        String head = dot < 0 ? name : name.substring(0, dot);
        for (JCTree def : ref.cu().defs) {
            if (!(def instanceof JCTree.JCImport imported) || imported.isStatic()) continue;
            String qualified = imported.getQualifiedIdentifier().toString();
            if (qualified.endsWith("." + head)) {
                found = loadClass(qualified + name.substring(head.length()));
            } else if (qualified.endsWith(".*")) {
                found = loadClass(qualified.substring(0, qualified.length() - 1) + name);
            }
            if (found != null) return found;
        }
        return loadClass("java.lang." + name);
    }

    private TypeElement loadClass(String name) {
        try {
            Symbol.ClassSymbol found =
                    classes.loadClass(symbols.unnamedModule, names.fromString(name));
            return found == symbols.errSymbol ? null : found;
        } catch (Symbol.CompletionFailure ignored) {
            return null;
        }
    }

    private Name superName(JCTree.JCExpression extending) {
        JCTree.JCExpression type =
                extending instanceof JCTree.JCTypeApply apply ? apply.clazz : extending;
        if (type instanceof JCTree.JCIdent ident) return ident.name;
        if (type instanceof JCTree.JCFieldAccess access) return access.name;
        return null;
    }

    void prove(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        Boolean chose = written.remove(clazz);
        if (chose == null || clazz.sym == null) return;
        TypeMirror base = foldedBaseType();
        if (base == null) return;
        boolean folded = types.isSubtype(types.erasure(clazz.sym.asType()), base);
        if (folded == chose) return;
        trees.printMessage(
                Diagnostic.Kind.ERROR,
                "@"
                        + ANNOTATION
                        + " gave "
                        + clazz.name
                        + " the "
                        + (chose ? "folded" : "plain")
                        + " pair, but its resolved supertypes make it "
                        + (folded ? "folded" : "plain")
                        + "; the written pair forwards nothing",
                clazz,
                cu);
    }

    private TypeMirror foldedBaseType() {
        if (!foldedTypeResolved) {
            foldedTypeResolved = true;
            TypeElement found = elements.getTypeElement(FOLDED_FQN);
            foldedType = found == null ? null : types.erasure(found.asType());
        }
        return foldedType;
    }

    private JCTree.JCAnnotation annotationOn(JCTree.JCClassDecl clazz) {
        return Annotations.on(clazz.mods.annotations, marker);
    }

    private boolean declares(JCTree.JCClassDecl clazz, String method) {
        for (JCTree member : clazz.defs) {
            if (member instanceof JCTree.JCMethodDecl declared
                    && declared.name.contentEquals(method)) {
                return true;
            }
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
}
