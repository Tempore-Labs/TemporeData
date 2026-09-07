package org.temporedata.modules.dev.workflow.runner;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A tiny, safe expression evaluator for conditional edges (R3).
 *
 * Supported syntax:
 *   - operands: node.&lt;nodeId&gt;.rows | node.&lt;nodeId&gt;.success | node.&lt;nodeId&gt;.status,
 *     numbers, single/double-quoted strings, true/false
 *   - operators: == != &gt; &gt;= &lt; &lt;= (numeric/string compare), &amp;&amp; || ! , + -
 *   - parentheses
 *
 * No arbitrary code is executed; only upstream node results are read.
 */
@Component
public class ConditionEvaluator {

    public boolean evaluate(String expr, Map<String, NodeRunResult> upstream) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        List<Token> tokens = tokenize(expr);
        Parser parser = new Parser(tokens, upstream);
        Object value = parser.parseOr();
        return toBoolean(value);
    }

    // ---- Types ----

    private enum Kind { NUMBER, STRING, IDENT, OP, LPAREN, RPAREN }

    private static final class Token {
        final Kind kind;
        final String text;
        final double num;

        Token(Kind kind, String text, double num) {
            this.kind = kind;
            this.text = text;
            this.num = num;
        }
    }

    private static final class EvalException extends RuntimeException {
        EvalException(String m) { super(m); }
    }

    // ---- Tokenizer ----

    private List<Token> tokenize(String expr) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = expr.length();
        while (i < n) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { out.add(new Token(Kind.LPAREN, "(", 0)); i++; continue; }
            if (c == ')') { out.add(new Token(Kind.RPAREN, ")", 0)); i++; continue; }
            if (c == '"' || c == '\'') {
                int end = expr.indexOf(c, i + 1);
                if (end < 0) throw new EvalException("unterminated string");
                out.add(new Token(Kind.STRING, expr.substring(i + 1, end), 0));
                i = end + 1;
                continue;
            }
            if (Character.isDigit(c)) {
                int j = i;
                while (j < n && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                out.add(new Token(Kind.NUMBER, expr.substring(i, j), Double.parseDouble(expr.substring(i, j))));
                i = j;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(expr.charAt(j)) || expr.charAt(j) == '_' || expr.charAt(j) == '.')) j++;
                out.add(new Token(Kind.IDENT, expr.substring(i, j), 0));
                i = j;
                continue;
            }
            // operators
            String two = i + 1 < n ? expr.substring(i, i + 2) : "x";
            switch (two) {
                case "==": case "!=": case ">=": case "<=": case "&&": case "||":
                    out.add(new Token(Kind.OP, two, 0)); i += 2; continue;
                default:
                    break;
            }
            if (c == '>' || c == '<' || c == '&' || c == '|' || c == '!' || c == '+' || c == '-') {
                out.add(new Token(Kind.OP, String.valueOf(c), 0)); i++; continue;
            }
            throw new EvalException("unexpected char: " + c);
        }
        return out;
    }

    // ---- Parser ----

    private static boolean toBoolean(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Double) return (Double) v != 0;
        if (v instanceof Integer) return ((Integer) v) != 0;
        if (v instanceof Long) return ((Long) v) != 0;
        if (v instanceof String) return !((String) v).isBlank();
        return false;
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final Map<String, NodeRunResult> upstream;
        private int pos;

        Parser(List<Token> tokens, Map<String, NodeRunResult> upstream) {
            this.tokens = tokens;
            this.upstream = upstream;
        }

        private Token peek() {
            return pos < tokens.size() ? tokens.get(pos) : new Token(Kind.RPAREN, "$", 0);
        }

        private Token next() {
            return tokens.get(pos++);
        }

        private boolean accept(String op) {
            Token t = peek();
            if (t.kind == Kind.OP && t.text.equals(op)) { pos++; return true; }
            return false;
        }

        Object parseOr() {
            Object l = parseAnd();
            while (accept("||")) {
                boolean a = toBoolean(l);
                boolean b = toBoolean(parseAnd());
                l = a || b;
            }
            return l;
        }

        Object parseAnd() {
            Object l = parseNot();
            while (accept("&&")) {
                boolean a = toBoolean(l);
                boolean b = toBoolean(parseNot());
                l = a && b;
            }
            return l;
        }

        Object parseNot() {
            if (accept("!")) {
                return !toBoolean(parseNot());
            }
            return parseCmp();
        }

        Object parseCmp() {
            Object l = parseAdd();
            Token t = peek();
            if (t.kind == Kind.OP) {
                String op = t.text;
                if (op.equals("==") || op.equals("!=") || op.equals(">") || op.equals(">=") || op.equals("<") || op.equals("<=")) {
                    pos++;
                    Object r = parseAdd();
                    return compare(l, r, op);
                }
            }
            return l;
        }

        Object parseAdd() {
            Object l = parsePrimary();
            while (true) {
                Token t = peek();
                if (t.kind == Kind.OP && t.text.equals("+")) { pos++; l = toNumeric(l) + toNumeric(parsePrimary()); }
                else if (t.kind == Kind.OP && t.text.equals("-")) { pos++; l = toNumeric(l) - toNumeric(parsePrimary()); }
                else break;
            }
            return l;
        }

        Object parsePrimary() {
            Token head = peek();
            if (head.kind == Kind.LPAREN) {
                next();
                Object v = parseOr();
                if (peek().kind == Kind.RPAREN) next();
                else throw new EvalException("missing )");
                return v;
            }
            Token t = next();
            if (t.kind == Kind.NUMBER) return t.num;
            if (t.kind == Kind.STRING) return t.text;
            if (t.kind == Kind.IDENT) return resolveIdent(t.text);
            throw new EvalException("unexpected token: " + t.text);
        }

        private Object resolveIdent(String ident) {
            if (ident.equals("true")) return true;
            if (ident.equals("false")) return false;
            if (ident.startsWith("node.")) {
                String[] parts = ident.split("\\.");
                if (parts.length != 3) throw new EvalException("bad node ref: " + ident);
                NodeRunResult r = upstream.get(parts[1]);
                if (r == null) throw new EvalException("no upstream result for node " + parts[1]);
                switch (parts[2].toLowerCase(Locale.ROOT)) {
                    case "rows": return (double) r.getAffectedRows();
                    case "success": return r.isSuccess();
                    case "status": return r.isSuccess() ? "SUCCESS" : "FAILED";
                    default: throw new EvalException("unknown property: " + parts[2]);
                }
            }
            throw new EvalException("unknown identifier: " + ident);
        }

        private static double toNumeric(Object v) {
            if (v instanceof Double) return (Double) v;
            if (v instanceof Integer) return ((Integer) v).doubleValue();
            if (v instanceof Long) return ((Long) v).doubleValue();
            if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
            if (v instanceof String) {
                try { return Double.parseDouble(((String) v).trim()); } catch (Exception ignored) { return 0; }
            }
            return 0;
        }

        private static Object compare(Object l, Object r, String op) {
            // numeric compare
            if (isNumeric(l) && isNumeric(r)) {
                double a = toNumeric(l), b = toNumeric(r);
                if ("==".equals(op)) return Math.abs(a - b) < 1e-9;
                if ("!=".equals(op)) return Math.abs(a - b) >= 1e-9;
                if (">".equals(op)) return a > b;
                if (">=".equals(op)) return a >= b;
                if ("<".equals(op)) return a < b;
                if ("<=".equals(op)) return a <= b;
                return false;
            }
            if (l instanceof Boolean && r instanceof Boolean) {
                boolean lb = (Boolean) l, rb = (Boolean) r;
                if ("==".equals(op)) return lb == rb;
                if ("!=".equals(op)) return lb != rb;
                return false;
            }
            String a = String.valueOf(l), b = String.valueOf(r);
            int c = a.compareTo(b);
            if ("==".equals(op)) return c == 0;
            if ("!=".equals(op)) return c != 0;
            if (">".equals(op)) return c > 0;
            if (">=".equals(op)) return c >= 0;
            if ("<".equals(op)) return c < 0;
            if ("<=".equals(op)) return c <= 0;
            return false;
        }

        private static boolean isNumeric(Object v) {
            return v instanceof Double || v instanceof Integer || v instanceof Long;
        }
    }
}