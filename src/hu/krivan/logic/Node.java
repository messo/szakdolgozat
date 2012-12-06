package hu.krivan.logic;

/**
 *
 * @author Balint
 */
public class Node implements Comparable<Node> {

    public enum Type {

        CONTROL_STATE, STATE
    }
    private String name;
    private Type type = Type.CONTROL_STATE;
    private boolean disabled = false;

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Node o) {
        return name.compareTo(o.getName());
    }
}
