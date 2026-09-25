// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.tools.Diagnostic;

final class Rotator {

    static final String ANNOTATION = "AutoRotate";

    private static final String BLOCK_STATE = "net.minecraft.world.level.block.state.BlockState";
    private static final Map<String, String> ARGUMENT =
            Map.of(
                    "rotate", "net.minecraft.world.level.block.Rotation",
                    "mirror", "net.minecraft.world.level.block.Mirror");

    private static final List<String> RING = List.of("NORTH", "EAST", "SOUTH", "WEST");
    private static final Map<String, List<String>> CASES =
            Map.of(
                    "rotate", List.of("CLOCKWISE_90", "CLOCKWISE_180", "COUNTERCLOCKWISE_90"),
                    "mirror", List.of("LEFT_RIGHT", "FRONT_BACK"));

    private final TreeMaker make;
    private final Names names;
    private final Trees trees;

    private final Name marker;

    Rotator(TreeMaker make, Names names, Trees trees) {
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
        if (!annotated(clazz)) return;
        for (String method : ARGUMENT.keySet()) {
            if (!declares(clazz, method)) continue;
            trees.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@AutoRotate writes rotate() and mirror(), and '"
                            + clazz.name
                            + "' already declares "
                            + method
                            + "(); delete the hand-written pair, or"
                            + " drop the annotation where the block means its own law",
                    clazz,
                    cu);
            return;
        }
        Map<String, Map<String, String>> rings = rings(clazz, cu);
        if (rings == null) return;
        List<String> facings = facings(clazz, cu);
        if (facings == null) return;
        if (rings.isEmpty() && facings.isEmpty()) {
            trees.printMessage(
                    Diagnostic.Kind.ERROR,
                    "'"
                            + clazz.name
                            + "' carries @AutoRotate and"
                            + " nothing to turn: it declares no connection ring and names no inherited facing."
                            + " Drop the annotation, or name the facing it inherits",
                    clazz,
                    cu);
            return;
        }
        make.at(clazz.pos);
        clazz.defs =
                clazz.defs
                        .append(build("rotate", rings, facings))
                        .append(build("mirror", rings, facings));
    }

    private Map<String, Map<String, String>> rings(
            JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        for (JCTree member : clazz.defs) {
            if (!(member instanceof JCTree.JCVariableDecl field)) continue;
            if ((field.mods.flags & Flags.STATIC) == 0) continue;
            String name = field.name.toString();
            for (String direction : RING) {
                if (!name.equals(direction) && !name.endsWith("_" + direction)) continue;
                out.computeIfAbsent(
                                name.substring(0, name.length() - direction.length()),
                                key -> new LinkedHashMap<>())
                        .put(direction, name);
                break;
            }
        }
        for (Map.Entry<String, Map<String, String>> ring : out.entrySet()) {
            for (String direction : RING) {
                if (ring.getValue().containsKey(direction)) continue;
                trees.printMessage(
                        Diagnostic.Kind.ERROR,
                        "'"
                                + clazz.name
                                + "' carries a partial"
                                + " connection ring: "
                                + ring.getKey()
                                + direction
                                + " is missing, so a turn has"
                                + " nowhere to put that arm",
                        clazz,
                        cu);
                return null;
            }
        }
        return out;
    }

    private List<String> facings(JCTree.JCClassDecl clazz, JCTree.JCCompilationUnit cu) {
        List<String> out = new ArrayList<>();
        for (JCTree member : clazz.defs) {
            if (!(member instanceof JCTree.JCVariableDecl field)) continue;
            if ((field.mods.flags & Flags.STATIC) == 0 || field.vartype == null) continue;
            String type = field.vartype.toString();
            if (!type.contains("Direction") && !type.contains("Property")) continue;
            if (type.contains("Axis")) {
                trees.printMessage(
                        Diagnostic.Kind.ERROR,
                        "'"
                                + clazz.name
                                + "' carries the axis property"
                                + " '"
                                + field.name
                                + "', whose turn is a swap @AutoRotate does not express; state"
                                + " its own rotate() and mirror()",
                        clazz,
                        cu);
                return null;
            }
            if (type.equals("DirectionProperty") || type.contains("<Direction>"))
                out.add(field.name.toString());
        }
        for (JCTree.JCAnnotation annotation : clazz.mods.annotations) {
            String name = annotation.annotationType.toString();
            if (!name.equals(ANNOTATION) && !name.endsWith("." + ANNOTATION)) continue;
            for (JCTree.JCExpression argument : annotation.args) {
                literals(argument instanceof JCTree.JCAssign assign ? assign.rhs : argument, out);
            }
        }
        return out;
    }

    private void literals(JCTree.JCExpression value, List<String> out) {
        if (value instanceof JCTree.JCNewArray array) {
            for (JCTree.JCExpression element : array.elems) literals(element, out);
        } else if (value instanceof JCTree.JCLiteral literal
                && literal.value instanceof String text) {
            out.add(text);
        }
    }

    private JCTree.JCMethodDecl build(
            String method, Map<String, Map<String, String>> rings, List<String> facings) {
        String type = ARGUMENT.get(method);
        JCTree.JCVariableDecl state =
                make.VarDef(
                        make.Modifiers(Flags.PARAMETER),
                        names.fromString("state"),
                        qualified(BLOCK_STATE),
                        null);
        JCTree.JCVariableDecl operation =
                make.VarDef(
                        make.Modifiers(Flags.PARAMETER),
                        names.fromString(
                                type.substring(type.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)),
                        qualified(type),
                        null);

        com.sun.tools.javac.util.List<JCTree.JCStatement> body;
        if (rings.isEmpty()) {

            body =
                    com.sun.tools.javac.util.List.of(
                            make.Return(turned(state, operation, method, rings, facings, null)));
        } else {
            ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
            for (String constant : CASES.get(method)) {
                statements.append(
                        make.If(
                                make.Binary(
                                        JCTree.Tag.EQ,
                                        make.Ident(operation.name),
                                        qualified(type + "." + constant)),
                                make.Return(
                                        turned(state, operation, method, rings, facings, constant)),
                                null));
            }
            statements.append(make.Return(make.Ident(state.name)));
            body = statements.toList();
        }

        JCTree.JCModifiers modifiers =
                make.Modifiers(
                        Flags.PROTECTED,
                        com.sun.tools.javac.util.List.of(
                                make.Annotation(
                                        qualified("java.lang.Override"),
                                        com.sun.tools.javac.util.List.nil())));
        return make.MethodDef(
                modifiers,
                names.fromString(method),
                qualified(BLOCK_STATE),
                com.sun.tools.javac.util.List.nil(),
                com.sun.tools.javac.util.List.of(state, operation),
                com.sun.tools.javac.util.List.nil(),
                make.Block(0, body),
                null);
    }

    private JCTree.JCExpression turned(
            JCTree.JCVariableDecl state,
            JCTree.JCVariableDecl operation,
            String method,
            Map<String, Map<String, String>> rings,
            List<String> facings,
            String constant) {
        JCTree.JCExpression out = make.Ident(state.name);
        for (Map<String, String> ring : rings.values()) {
            for (int target = 0; target < RING.size(); target++) {
                int source = source(constant, target);
                if (source == target) continue;
                out =
                        call(
                                out,
                                "setValue",
                                make.Ident(names.fromString(ring.get(RING.get(target)))),
                                call(
                                        make.Ident(state.name),
                                        "getValue",
                                        make.Ident(names.fromString(ring.get(RING.get(source))))));
            }
        }
        for (String facing : facings) {
            Name field = names.fromString(facing);
            out =
                    call(
                            out,
                            "setValue",
                            make.Ident(field),
                            call(
                                    make.Ident(operation.name),
                                    method,
                                    call(make.Ident(state.name), "getValue", make.Ident(field))));
        }
        return out;
    }

    private int source(String constant, int target) {
        if (constant == null) return target;
        return switch (constant) {
            case "CLOCKWISE_90" -> (target + 3) % 4;
            case "CLOCKWISE_180" -> (target + 2) % 4;
            case "COUNTERCLOCKWISE_90" -> (target + 1) % 4;

            case "LEFT_RIGHT" -> target % 2 == 0 ? (target + 2) % 4 : target;

            case "FRONT_BACK" -> target % 2 == 1 ? (target + 2) % 4 : target;
            default -> target;
        };
    }

    private JCTree.JCExpression call(
            JCTree.JCExpression receiver, String method, JCTree.JCExpression... arguments) {
        return make.Apply(
                com.sun.tools.javac.util.List.nil(),
                make.Select(receiver, names.fromString(method)),
                com.sun.tools.javac.util.List.from(arguments));
    }

    private boolean annotated(JCTree.JCClassDecl clazz) {
        return Annotations.on(clazz.mods.annotations, marker) != null;
    }

    private boolean declares(JCTree.JCClassDecl clazz, String method) {
        for (JCTree member : clazz.defs) {
            if (member instanceof JCTree.JCMethodDecl declared
                    && declared.name.contentEquals(method)
                    && declared.params.size() == 2) {
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
