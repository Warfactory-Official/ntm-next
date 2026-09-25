// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class Calculator {

    private Calculator() {}

    public static double evaluateExpression(String input) {
        if (input.contains("^")) input = preEvaluatePower(input);

        char[] tokens = input.toCharArray();
        Deque<Double> values = new ArrayDeque<>();
        Deque<String> operators = new ArrayDeque<>();

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == ' ') continue;

            if (tokens[i] >= '0' && tokens[i] <= '9'
                    || tokens[i] == '.'
                    || (tokens[i] == '-'
                            && (i == 0 || "+-*/%^(".contains(String.valueOf(tokens[i - 1]))))) {
                StringBuilder buffer = new StringBuilder();
                if (tokens[i] == '-') {
                    buffer.append('-');
                    i++;
                }
                while (i < tokens.length
                        && (tokens[i] >= '0' && tokens[i] <= '9' || tokens[i] == '.')) {
                    buffer.append(tokens[i++]);
                }
                values.push(Double.parseDouble(buffer.toString()));
                i--;
            } else if (tokens[i] == '(') operators.push(Character.toString(tokens[i]));
            else if (tokens[i] == ')') {
                while (!operators.isEmpty() && operators.peek().charAt(0) != '(') {
                    values.push(
                            evaluateOperator(
                                    operators.pop().charAt(0), values.pop(), values.pop()));
                }
                operators.pop();
                if (!operators.isEmpty() && operators.peek().length() > 1) {
                    values.push(evaluateFunction(operators.pop(), values.pop()));
                }
            } else if (tokens[i] == '+'
                    || tokens[i] == '-'
                    || tokens[i] == '*'
                    || tokens[i] == '/'
                    || tokens[i] == '%'
                    || tokens[i] == '^') {
                while (!operators.isEmpty()
                        && hasPrecedence(String.valueOf(tokens[i]), operators.peek())) {
                    values.push(
                            evaluateOperator(
                                    operators.pop().charAt(0), values.pop(), values.pop()));
                }
                operators.push(Character.toString(tokens[i]));
            } else if (tokens[i] == '!') {
                values.push((double) factorial((int) Math.round(values.pop())));
            } else if (tokens[i] >= 'A' && tokens[i] <= 'Z'
                    || tokens[i] >= 'a' && tokens[i] <= 'z') {
                StringBuilder charBuffer = new StringBuilder();
                while (i < tokens.length
                        && (tokens[i] >= 'A' && tokens[i] <= 'Z'
                                || tokens[i] >= 'a' && tokens[i] <= 'z')) {
                    charBuffer.append(tokens[i++]);
                }
                String string = charBuffer.toString();
                if (string.equalsIgnoreCase("pi")) values.push(Math.PI);
                else if (string.equalsIgnoreCase("e")) values.push(Math.E);
                else operators.push(string.toLowerCase(Locale.ROOT));
                i--;
            }
        }

        while (!operators.isEmpty()) {
            values.push(evaluateOperator(operators.pop().charAt(0), values.pop(), values.pop()));
        }

        return values.pop();
    }

    private static double evaluateOperator(char operator, double x, double y) {
        return switch (operator) {
            case '+' -> y + x;
            case '-' -> y - x;
            case '*' -> y * x;
            case '/' -> y / x;
            case '%' -> y % x;
            case '^' -> Math.pow(y, x);
            default -> 0;
        };
    }

    private static double evaluateFunction(String function, double x) {
        return switch (function) {
            case "sqrt" -> Math.sqrt(x);
            case "sin" -> Math.sin(x);
            case "cos" -> Math.cos(x);
            case "tan" -> Math.tan(x);
            case "asin" -> Math.asin(x);
            case "acos" -> Math.acos(x);
            case "atan" -> Math.atan(x);
            case "log" -> Math.log10(x);
            case "ln" -> Math.log(x);
            case "ceil" -> Math.ceil(x);
            case "floor" -> Math.floor(x);
            case "round" -> Math.round(x);
            default -> 0;
        };
    }

    private static boolean hasPrecedence(String first, String second) {
        if (second.length() > 1) return false;

        char firstChar = first.charAt(0);
        char secondChar = second.charAt(0);

        if (secondChar == '(' || secondChar == ')') return false;
        return (firstChar != '*' && firstChar != '/' && firstChar != '%' && firstChar != '^')
                || (secondChar != '+' && secondChar != '-');
    }

    private static String preEvaluatePower(String input) {
        do {
            int powerOperatorIndex = input.lastIndexOf('^');

            boolean previousTokenIsParentheses = input.charAt(powerOperatorIndex - 1) == ')';
            int parenthesesDepth = previousTokenIsParentheses ? 1 : 0;
            int baseExpressionStart =
                    previousTokenIsParentheses ? powerOperatorIndex - 2 : powerOperatorIndex - 1;
            baseLoop:
            for (; baseExpressionStart >= 0; baseExpressionStart--) {
                switch (input.charAt(baseExpressionStart)) {
                    case ')':
                        if (previousTokenIsParentheses) parenthesesDepth++;
                        else break baseLoop;
                        break;
                    case '(':
                        if (previousTokenIsParentheses && parenthesesDepth > 0) parenthesesDepth--;
                        else break baseLoop;
                        break;
                    case '+', '-', '*', '/', '^':
                        if (parenthesesDepth == 0) break baseLoop;
                }
            }
            baseExpressionStart++;
            if (parenthesesDepth > 0) throw new IllegalArgumentException("Incomplete parentheses");

            boolean nextTokenIsParentheses = input.charAt(powerOperatorIndex + 1) == '(';
            parenthesesDepth = nextTokenIsParentheses ? 1 : 0;
            int exponentExpressionEnd =
                    nextTokenIsParentheses ? powerOperatorIndex + 2 : powerOperatorIndex + 1;
            exponentLoop:
            for (; exponentExpressionEnd < input.length(); exponentExpressionEnd++) {
                switch (input.charAt(exponentExpressionEnd)) {
                    case '(':
                        if (nextTokenIsParentheses) parenthesesDepth++;
                        else break exponentLoop;
                        break;
                    case ')':
                        if (nextTokenIsParentheses && parenthesesDepth > 0) parenthesesDepth--;
                        else break exponentLoop;
                        break;
                    case '+', '-', '*', '/', '^':
                        if (parenthesesDepth == 0) break exponentLoop;
                }
            }
            if (parenthesesDepth > 0) throw new IllegalArgumentException("Incomplete parentheses");

            double base =
                    evaluateExpression(input.substring(baseExpressionStart, powerOperatorIndex));
            double exponent =
                    evaluateExpression(
                            input.substring(powerOperatorIndex + 1, exponentExpressionEnd));
            double result = Math.pow(base, exponent);
            input =
                    input.substring(0, baseExpressionStart)
                            + new BigDecimal(result, MathContext.DECIMAL64).toPlainString()
                            + input.substring(exponentExpressionEnd);
        } while (input.contains("^"));

        return input;
    }

    private static int factorial(int in) {
        if (in < 0) throw new IllegalArgumentException("Factorial needs n >= 0");
        if (in < 2) return 1;
        int p = 1, r = 1;
        int[] currentN = {1};
        int h = 0, shift = 0, high = 1;
        int log2n = log2(in);
        while (h != in) {
            shift += h;
            h = in >> log2n--;
            int len = high;
            high = (h - 1) | 1;
            len = (high - len) / 2;

            if (len > 0) {
                p *= factorialProduct(len, currentN);
                r *= p;
            }
        }

        return r << shift;
    }

    private static int factorialProduct(int in, int[] currentN) {
        int m = in / 2;
        if (m == 0) return currentN[0] += 2;
        if (in == 2) return (currentN[0] += 2) * (currentN[0] += 2);
        return factorialProduct(in - m, currentN) * factorialProduct(m, currentN);
    }

    private static int log2(int in) {
        int log = 0;
        if ((in & 0xffff0000) != 0) {
            in >>>= 16;
            log = 16;
        }
        if (in >= 256) {
            in >>>= 8;
            log += 8;
        }
        if (in >= 16) {
            in >>>= 4;
            log += 4;
        }
        if (in >= 4) {
            in >>>= 2;
            log += 2;
        }
        return log + (in >>> 1);
    }
}
