/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.krivan.logic;

import org.apache.commons.collections15.KeyValue;

/**
 *
 * @author Balint
 */
public class Action implements KeyValue<String, String> {

    private String key;
    private String value;

    public Action(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + " := " + value + ";";
    }
}
