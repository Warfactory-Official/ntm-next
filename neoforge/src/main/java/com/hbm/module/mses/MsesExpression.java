// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class MsesExpression {
    sealed interface Node permits Constant, Variable, Binary, Function {}

    record Constant(double value) implements Node {}

    record Variable(int index, boolean negate) implements Node {}

    record Binary(char operator, Node left, Node right) implements Node {}

    record Function(String name, Node argument) implements Node {}

    record Input(String name, boolean allowNegative) {}

    record Plan(Node root, List<Input> inputs) {}

    private static final Set<String> FUNCTIONS =
            Set.of(
                    "sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "ceil",
                    "floor", "round");
    private final String text;
    private final List<Input> inputs = new ArrayList<>();
    private int pos, nodes, depth;

    private MsesExpression(String text) {
        this.text = text;
    }

    static Plan tryParse(String text) {

        if (text.length() > 2048 || text.indexOf('^') >= 0 || text.indexOf('!') >= 0) return null;
        try {
            MsesExpression p = new MsesExpression(text);
            Node root = p.expression();
            p.spaces();
            if (p.pos != text.length()) return null;
            return new Plan(root, List.copyOf(p.inputs));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Node count(Node n) {
        if (++nodes > 128) throw new IllegalArgumentException();
        return n;
    }

    private void spaces() {
        while (pos < text.length() && text.charAt(pos) == ' ') pos++;
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private Node expression() {
        if (++depth > 48) throw new IllegalArgumentException();
        Node left = term();
        while (true) {
            spaces();
            char c = peek();
            if (c != '+' && c != '-') break;
            pos++;
            left = count(new Binary(c, left, term()));
        }
        depth--;
        return left;
    }

    private Node term() {
        Node left = atom();
        while (true) {
            spaces();
            char c = peek();
            if (c != '*' && c != '/') return left;
            pos++;
            left = count(new Binary(c, left, atom()));
        }
    }

    private boolean unaryPosition(int p) {
        return p == 0 || "+-*/(".indexOf(text.charAt(p - 1)) >= 0;
    }

    private Node atom() {
        spaces();
        int start = pos;
        boolean minus = false;
        if (peek() == '-') {

            if (!unaryPosition(pos)) throw new IllegalArgumentException();
            minus = true;
            pos++;
        }
        char c = peek();
        if (c == '$') {
            int a = pos++, b = text.indexOf('$', pos);
            if (b < 0) throw new IllegalArgumentException();
            int index = inputs.size();
            inputs.add(new Input(text.substring(pos, b), !minus && unaryPosition(a)));
            pos = b + 1;
            return count(new Variable(index, minus));
        }
        if ((c >= '0' && c <= '9') || c == '.') {
            while ((peek() >= '0' && peek() <= '9') || peek() == '.') pos++;
            return count(new Constant(Double.parseDouble(text.substring(start, pos))));
        }
        if (minus) throw new IllegalArgumentException();
        if (c == '(') {
            pos++;
            Node node = expression();
            spaces();
            if (peek() != ')') throw new IllegalArgumentException();
            pos++;
            return node;
        }
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            while ((peek() >= 'a' && peek() <= 'z') || (peek() >= 'A' && peek() <= 'Z')) pos++;
            String name = text.substring(start, pos).toLowerCase(Locale.ROOT);
            if (name.equals("pi")) return count(new Constant(Math.PI));
            if (name.equals("e")) return count(new Constant(Math.E));
            if (!FUNCTIONS.contains(name)) throw new IllegalArgumentException();
            spaces();
            if (peek() != '(') throw new IllegalArgumentException();
            pos++;
            Node arg = expression();
            spaces();
            if (peek() != ')') throw new IllegalArgumentException();
            pos++;
            return count(new Function(name, arg));
        }
        throw new IllegalArgumentException();
    }
}
