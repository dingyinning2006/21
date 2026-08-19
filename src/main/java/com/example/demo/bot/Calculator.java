package com.example.demo.bot;

/**
 * 数学表达式求值器：支持 + - * / ( ) ^（幂）、小数、负数、括号嵌套。
 * 用递归下降解析，不依赖任何库；表达式非法时抛 IllegalArgumentException。
 */
public final class Calculator {

    private Calculator() {
    }

    public static double evaluate(String expression) {
        return new Parser(expression).parse();
    }

    /** 递归下降解析器：表达式 -> 项 -> 幂 -> 一元 -> 原子，按优先级逐层下降 */
    private static final class Parser {
        private final String s;
        private int pos = 0;

        Parser(String s) {
            this.s = s;
        }

        double parse() {
            double v = expression();
            skipSpace();
            if (pos < s.length()) {
                throw new IllegalArgumentException("无法解析的表达式，出错位置：" + pos);
            }
            return v;
        }

        /** 加减（最低优先级） */
        private double expression() {
            double v = term();
            while (true) {
                skipSpace();
                if (peek('+')) { pos++; v += term(); }
                else if (peek('-')) { pos++; v -= term(); }
                else { return v; }
            }
        }

        /** 乘除 */
        private double term() {
            double v = power();
            while (true) {
                skipSpace();
                if (peek('*')) { pos++; v *= power(); }
                else if (peek('/')) {
                    pos++;
                    double d = power();
                    if (d == 0) {
                        throw new IllegalArgumentException("不能除以 0");
                    }
                    v /= d;
                } else { return v; }
            }
        }

        /** 幂（右结合：2^3^2 = 2^(3^2)） */
        private double power() {
            double v = unary();
            skipSpace();
            if (peek('^')) { pos++; v = Math.pow(v, power()); }
            return v;
        }

        /** 正负号 */
        private double unary() {
            skipSpace();
            if (peek('-')) { pos++; return -unary(); }
            if (peek('+')) { pos++; return unary(); }
            return primary();
        }

        /** 数字或括号 */
        private double primary() {
            skipSpace();
            if (peek('(')) {
                pos++;
                double v = expression();
                skipSpace();
                if (!peek(')')) {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                return v;
            }
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("表达式有误：" + s);
            }
            return Double.parseDouble(s.substring(start, pos));
        }

        private void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private boolean peek(char c) {
            return pos < s.length() && s.charAt(pos) == c;
        }
    }
}
