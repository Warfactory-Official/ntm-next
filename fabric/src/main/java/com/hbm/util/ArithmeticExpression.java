/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.NoSuchElementException;

public final class ArithmeticExpression {

    private static final int MAX_DEPTH = 256;
    private static final double[] FACTORIALS = factorials();

    private static final int OPEN = '(';
    private static final int LETTER = 1;
    private static final int UNKNOWN = 2;
    private static final int SQRT = 3,
            SIN = 4,
            COS = 5,
            TAN = 6,
            ASIN = 7,
            ACOS = 8,
            ATAN = 9,
            LOG = 10,
            LN = 11,
            CEIL = 12,
            FLOOR = 13,
            ROUND = 14;
    private static final String[] FUNCTION_NAMES = {
        "sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "ceil", "floor", "round",
    };

    private ArithmeticExpression() {}

    public static double evaluate(String text) {
        return evaluate(text, 0);
    }

    private static double evaluate(String text, int depth) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("Expression nests too deeply");
        return infix(foldPowers(text, depth));
    }

    private static String foldPowers(String text, int depth) {
        for (int caret = text.lastIndexOf('^'); caret >= 0; caret = text.lastIndexOf('^')) {
            int start = baseStart(text, caret);
            int end = exponentEnd(text, caret);
            double base = evaluate(text.substring(start, caret), depth + 1);
            double exponent = evaluate(text.substring(caret + 1, end), depth + 1);
            String power =
                    new BigDecimal(Math.pow(base, exponent), MathContext.DECIMAL64).toPlainString();
            text = text.substring(0, start) + power + text.substring(end);
        }
        return text;
    }

    private static int baseStart(String text, int caret) {
        int i = caret - 1;
        if (text.charAt(i) != ')') {
            while (i >= 0 && !spanDelimiter(text.charAt(i))) i--;
            return i + 1;
        }
        int depth = 1;
        for (i--; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                if (depth == 0) break;
                depth--;
            } else if (depth == 0 && operator(c)) {
                break;
            }
        }
        if (depth > 0) throw new IllegalArgumentException("Incomplete parentheses");
        return i + 1;
    }

    private static int exponentEnd(String text, int caret) {
        int length = text.length();
        int i = caret + 1;
        if (text.charAt(i) != '(') {
            while (i < length && !spanDelimiter(text.charAt(i))) i++;
            return i;
        }
        int depth = 1;
        for (i++; i < length; i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) break;
                depth--;
            } else if (depth == 0 && operator(c)) {
                break;
            }
        }
        if (depth > 0) throw new IllegalArgumentException("Incomplete parentheses");
        return i;
    }

    private static boolean operator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private static boolean spanDelimiter(char c) {
        return c == '(' || c == ')' || operator(c);
    }

    private static boolean signPrefix(char c) {
        return c == '%' || c == '(' || operator(c);
    }

    private static boolean asciiLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static double infix(String text) {
        int length = text.length();
        var operands = new Operands(length);
        var pending = new int[length];
        int top = 0;
        for (int i = 0; i < length; ) {
            char c = text.charAt(i);
            if (Character.isDigit(c)
                    || c == '.'
                    || c == '-' && (i == 0 || signPrefix(text.charAt(i - 1)))) {
                int end = i + 1;
                while (end < length
                        && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '.')) end++;
                operands.push(Double.parseDouble(text.substring(i, end)));
                i = end;
                continue;
            }
            if (asciiLetter(c)) {
                int end = i + 1;
                while (end < length && asciiLetter(text.charAt(end))) end++;
                int span = end - i;
                if (span == 2 && text.regionMatches(true, i, "pi", 0, 2)) {
                    operands.push(Math.PI);
                } else if (span == 1 && (c == 'e' || c == 'E')) {
                    operands.push(Math.E);
                } else {
                    pending[top++] = span == 1 ? LETTER : functionId(text, i, span);
                }
                i = end;
                continue;
            }
            switch (c) {
                case '+', '-', '*', '/', '%' -> {
                    boolean additive = c == '+' || c == '-';
                    while (top > 0
                            && operatorLike(pending[top - 1])
                            && (additive || pending[top - 1] != '+' && pending[top - 1] != '-')) {
                        operands.apply(pending[--top]);
                    }
                    pending[top++] = c;
                }
                case '(' -> pending[top++] = OPEN;
                case ')' -> {
                    while (top > 0 && pending[top - 1] != OPEN) operands.apply(pending[--top]);
                    if (top == 0) throw new NoSuchElementException();
                    top--;
                    if (top > 0 && named(pending[top - 1])) {
                        operands.replaceTop(function(pending[--top], operands.top()));
                    }
                }
                case '!' -> operands.replaceTop(factorial(operands.top()));
                default -> {}
            }
            i++;
        }
        while (top > 0) operands.apply(pending[--top]);
        return operands.top();
    }

    private static boolean operatorLike(int entry) {
        return entry == '+'
                || entry == '-'
                || entry == '*'
                || entry == '/'
                || entry == '%'
                || entry == LETTER;
    }

    private static boolean named(int entry) {
        return entry >= UNKNOWN && entry <= ROUND;
    }

    private static int functionId(String text, int start, int span) {
        for (int i = 0; i < FUNCTION_NAMES.length; i++) {
            String name = FUNCTION_NAMES[i];
            if (name.length() == span && text.regionMatches(true, start, name, 0, span))
                return SQRT + i;
        }
        return UNKNOWN;
    }

    private static double function(int id, double x) {
        return switch (id) {
            case SQRT -> Math.sqrt(x);
            case SIN -> Math.sin(x);
            case COS -> Math.cos(x);
            case TAN -> Math.tan(x);
            case ASIN -> Math.asin(x);
            case ACOS -> Math.acos(x);
            case ATAN -> Math.atan(x);
            case LOG -> Math.log10(x);
            case LN -> Math.log(x);
            case CEIL -> Math.ceil(x);
            case FLOOR -> Math.floor(x);
            case ROUND -> Math.round(x);
            default -> 0;
        };
    }

    private static double factorial(double value) {
        long n = Math.round(value);
        if (n < 0) throw new IllegalArgumentException("Factorial needs n >= 0");
        return n < FACTORIALS.length ? FACTORIALS[(int) n] : Double.POSITIVE_INFINITY;
    }

    private static double[] factorials() {
        var values = new double[171];
        var exact = BigInteger.ONE;
        values[0] = 1;
        for (int n = 1; n < values.length; n++) {
            exact = exact.multiply(BigInteger.valueOf(n));
            values[n] = exact.doubleValue();
        }
        return values;
    }

    private static final class Operands {
        private final double[] values;
        private int size;

        private Operands(int capacity) {
            values = new double[capacity];
        }

        private void push(double value) {
            values[size++] = value;
        }

        private double top() {
            if (size == 0) throw new NoSuchElementException();
            return values[size - 1];
        }

        private void replaceTop(double value) {
            values[size - 1] = value;
        }

        private void apply(int entry) {
            if (size < 2) throw new NoSuchElementException();
            double right = values[--size];
            double left = values[size - 1];
            values[size - 1] =
                    switch (entry) {
                        case '+' -> left + right;
                        case '-' -> left - right;
                        case '*' -> left * right;
                        case '/' -> left / right;
                        case '%' -> left % right;
                        default -> 0;
                    };
        }
    }
}
