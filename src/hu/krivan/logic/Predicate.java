/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.krivan.logic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Balint
 */
public class Predicate {

    private String left;
    private String operator;
    private String right;
    private static final Pattern pattern = Pattern.compile("(.*?)\\s*(<=|>=|<|>|==|!=)\\s*(.*?)");

    public Predicate(String left, String operator, String right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public Predicate(String fullString) {
        Matcher m = pattern.matcher(fullString);
        if (m.matches()) {
            left = m.group(1);
            operator = m.group(2);
            right = m.group(3);
        } else {
            left = fullString;
        }
    }

    public String getLeft() {
        return left;
    }

    public String getOperator() {
        return operator;
    }

    public String getRight() {
        return right;
    }

    @Override
    public String toString() {
        if(operator != null) {
            return left + " " + operator + " " + right;
        } else {
            return left;
        }
    }

    public String toHTML() {
        return toString().replace(">", "&gt;").replace("<", "&lt;");
    }
}
