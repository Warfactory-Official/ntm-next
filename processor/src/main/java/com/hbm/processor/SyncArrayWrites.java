// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;

final class SyncArrayWrites extends TreeTranslator {
    private record Input(Symbol.VarSymbol field, JCTree.JCExpression owner) {}

    private final TreeMaker make;
    private final Names names;
    private final Types types;
    private final Trees trees;
    private final Map<Symbol, Input> aliases = new IdentityHashMap<>();
    private JCTree.JCCompilationUnit unit;
    private Symbol.ClassSymbol currentClass;
    private Symbol.MethodSymbol currentMethod;
    private int temporary;

    SyncArrayWrites(Context context, Trees trees) {
        make = TreeMaker.instance(context);
        names = Names.instance(context);
        types = Types.instance(context);
        this.trees = trees;
    }

    void rewrite(JCTree.JCClassDecl type, JCTree.JCCompilationUnit unit) {
        this.unit = unit;
        translate(type);
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        var previous = currentClass;
        currentClass = tree.sym;
        super.visitClassDef(tree);
        currentClass = previous;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        var previous = currentMethod;
        currentMethod = tree.sym;
        if (!tree.name.toString().startsWith("hbm$sync")) super.visitMethodDef(tree);
        else result = tree;
        currentMethod = previous;
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        super.visitVarDef(tree);
        if (tree.sym == null || tree.init == null || tree.sym.owner instanceof Symbol.ClassSymbol)
            return;
        Input input = input(tree.init);
        if (input != null) {
            aliases.put(tree.sym, input);
        } else if (tree.sym.type instanceof Type.ArrayType) {
            new TreeScanner() {
                @Override
                public void visitConditional(JCTree.JCConditional conditional) {
                    if (input(conditional.truepart) != null
                            || input(conditional.falsepart) != null) {
                        error(
                                conditional,
                                "A conditional sync array alias must retain its declared owner");
                    }
                    super.visitConditional(conditional);
                }
            }.scan(tree.init);
        }
    }

    @Override
    public void visitAssign(JCTree.JCAssign tree) {
        super.visitAssign(tree);
        Symbol assigned = TreeInfo.symbol(tree.lhs);
        if (assigned != null
                && assigned.owner instanceof Symbol.MethodSymbol
                && (aliases.containsKey(assigned) || input(tree.rhs) != null)) {
            error(
                    tree,
                    "A sync array alias must be initialized once; keep reassignment at the declared field");
        }
        if (!(tree.lhs instanceof JCTree.JCArrayAccess access)) return;
        Input input = input(access.indexed);
        if (input != null) {
            make.at(tree.pos);
            var definitions = new ListBuffer<JCTree.JCStatement>();
            var owner = local(input.owner, definitions);
            var array = local(capturedArray(access.indexed, input, owner), definitions);
            var write =
                    call(
                            new Input(input.field, owner),
                            array,
                            access.index,
                            tree.rhs,
                            tree.type,
                            tree.pos);
            result = make.LetExpr(definitions.toList(), write).setType(tree.type);
        }
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp tree) {
        super.visitAssignop(tree);
        if (!(tree.lhs instanceof JCTree.JCArrayAccess access)) return;
        Input input = input(access.indexed);
        if (input != null)
            result =
                    update(
                            input,
                            access,
                            tree.rhs,
                            tree.operator,
                            tree.getTag().noAssignOp(),
                            tree.type,
                            false,
                            tree.pos);
    }

    @Override
    public void visitUnary(JCTree.JCUnary tree) {
        super.visitUnary(tree);
        boolean increment =
                tree.getTag() == JCTree.Tag.PREINC || tree.getTag() == JCTree.Tag.POSTINC;
        boolean decrement =
                tree.getTag() == JCTree.Tag.PREDEC || tree.getTag() == JCTree.Tag.POSTDEC;
        if ((!increment && !decrement) || !(tree.arg instanceof JCTree.JCArrayAccess access))
            return;
        Input input = input(access.indexed);
        if (input == null) return;
        error(
                tree,
                "Use a compound assignment for a sync array increment so its value remains explicit");
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation tree) {
        super.visitApply(tree);
        Symbol symbol = TreeInfo.symbol(tree.meth);
        if (!(symbol instanceof Symbol.MethodSymbol method)) return;
        String owner = method.owner.toString(), name = method.name.toString();
        int target =
                owner.equals("java.lang.System") && name.equals("arraycopy")
                        ? 2
                        : owner.equals("java.util.Arrays")
                                        && Set.of(
                                                        "fill",
                                                        "sort",
                                                        "parallelSort",
                                                        "setAll",
                                                        "parallelSetAll",
                                                        "parallelPrefix")
                                                .contains(name)
                                ? 0
                                : -1;
        if (target < 0 || tree.args.size() <= target) return;
        Input input = input(tree.args.get(target));
        if (input == null) return;
        if (currentMethod != null
                && currentMethod.isConstructor()
                && input.owner instanceof JCTree.JCIdent ident
                && ident.name == names._this) return;
        error(tree, "Bulk sync array writes must use the owning mutation API");
    }

    private JCTree.JCExpression update(
            Input input,
            JCTree.JCArrayAccess access,
            JCTree.JCExpression rhs,
            Symbol.OperatorSymbol operator,
            JCTree.Tag operation,
            Type type,
            boolean oldResult,
            int pos) {
        make.at(pos);
        var definitions = new ListBuffer<JCTree.JCStatement>();
        var owner = local(input.owner, definitions);
        var array = local(capturedArray(access.indexed, input, owner), definitions);
        var index = local(access.index, definitions);
        var previous = make.Indexed(array, index).setType(type);
        var value = make.Binary(operation, previous, rhs);
        value.operator = operator;
        value.type = operator.type.getReturnType();
        JCTree.JCExpression narrowed = make.TypeCast(make.Type(type), value).setType(type);
        var write = call(new Input(input.field, owner), array, index, narrowed, type, pos);
        return make.LetExpr(definitions.toList(), write).setType(type);
    }

    private JCTree.JCExpression capturedArray(
            JCTree.JCExpression original, Input input, JCTree.JCExpression owner) {
        if (aliases.containsKey(TreeInfo.symbol(TreeInfo.skipParens(original)))) {
            if (!(input.owner instanceof JCTree.JCIdent ident && ident.name == names._this)) {
                error(original, "An aliased sync array write must retain its owner explicitly");
            }
            return original;
        }
        return make.Select(owner, input.field).setType(original.type);
    }

    private JCTree.JCIdent local(
            JCTree.JCExpression value, ListBuffer<JCTree.JCStatement> definitions) {
        var symbol =
                new Symbol.VarSymbol(
                        Flags.SYNTHETIC | Flags.FINAL,
                        names.fromString("hbm$syncArray$" + temporary++),
                        value.type,
                        currentMethod);
        definitions.append(make.VarDef(symbol, value));
        return make.Ident(symbol);
    }

    private JCTree.JCExpression call(
            Input input,
            JCTree.JCExpression array,
            JCTree.JCExpression index,
            JCTree.JCExpression value,
            Type type,
            int pos) {
        if (aliases.containsKey(TreeInfo.symbol(array))
                && !(input.owner instanceof JCTree.JCIdent ident && ident.name == names._this)) {
            error(array, "An aliased sync array write must retain its owner explicitly");
            return value;
        }
        if (!(input.owner instanceof JCTree.JCIdent)) {
            error(
                    array,
                    "A sync array write must use a local owner, without evaluating a receiver twice");
            return value;
        }
        var name =
                names.fromString(
                        MutationSync.ARRAY_PREFIX
                                + input.field.owner.name
                                + "$"
                                + input.field.name);
        var symbols = input.field.owner.members().getSymbolsByName(name).iterator();
        if (!symbols.hasNext()) {
            error(
                    array,
                    "Missing generated sync array writer for "
                            + input.field.owner
                            + "."
                            + input.field);
            return value;
        }
        Symbol.MethodSymbol method = (Symbol.MethodSymbol) symbols.next();
        var select = make.at(pos).Select(input.owner, method);
        select.type = types.memberType(input.owner.type, method);
        return make.Apply(List.nil(), select, List.of(array, index, value)).setType(type);
    }

    private Input input(JCTree.JCExpression expression) {
        expression = TreeInfo.skipParens(expression);
        Symbol symbol = TreeInfo.symbol(expression);
        Input alias = aliases.get(symbol);
        if (alias != null) return alias;
        if (!(symbol instanceof Symbol.VarSymbol field)
                || !(field.owner instanceof Symbol.ClassSymbol)) return null;
        boolean tracked =
                field.getAnnotationMirrors().stream()
                        .anyMatch(
                                annotation ->
                                        annotation
                                                .getAnnotationType()
                                                .toString()
                                                .equals("com.hbm.packet.SyncField"));
        if (!tracked || !(field.type instanceof Type.ArrayType)) return null;
        JCTree.JCExpression owner =
                expression instanceof JCTree.JCFieldAccess selected
                        ? selected.selected
                        : make.This(currentClass.type);
        return new Input(field, owner);
    }

    private void error(JCTree tree, String message) {
        trees.printMessage(Diagnostic.Kind.ERROR, message, tree, unit);
    }
}
