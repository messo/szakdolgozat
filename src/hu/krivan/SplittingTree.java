package hu.krivan;

import hu.krivan.logic.Edge;
import hu.krivan.logic.Graph;
import hu.krivan.logic.Node;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Balint
 */
class SplittingTree {

    private static class Data {

        private Collection<Node> nodes;
        private String input;
        private String output;

        public Data(Collection<Node> nodes) {
            this.nodes = nodes;
        }

        public Data(Collection<Node> nodes, String input, String output) {
            this.nodes = nodes;
            this.input = input;
            this.output = output;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public Collection<Node> getNodes() {
            return nodes;
        }
    }

    private static class TreeNode<T> {

        private T data;
        private List<TreeNode<T>> children;
        private TreeNode<T> parent;

        public TreeNode(T data) {
            this.data = data;
            this.children = new ArrayList<TreeNode<T>>();
        }

        public void addChild(TreeNode<T> child) {
            children.add(child);
            child.parent = this;
        }

        public T getData() {
            return data;
        }
    }
    private final Graph graph;
    private PrintStream out;
    private Map<String, Collection<Edge>> edgesByInput;

    public SplittingTree(Graph graph) {
        this.graph = graph;
        this.out = System.out;
    }

    public void output() {
        Collection<Node> nodes = graph.getVertices();
        edgesByInput = new HashMap<String, Collection<Edge>>();

        for (Edge e : graph.getEdges()) {
            if (!edgesByInput.containsKey(e.getInput())) {
                edgesByInput.put(e.getInput(), new ArrayList<Edge>());
            }
            edgesByInput.get(e.getInput()).add(e);
        }

        for (String input : edgesByInput.keySet()) {
            System.out.println(input);
            for (Edge e : edgesByInput.get(input)) {
                System.out.print("  ");
                System.out.println(e);
            }
        }


        List<Node> controlStates = new ArrayList<Node>();
        controlStates.add(graph.getNodeByName("DISCONNECTED_0"));
        controlStates.add(graph.getNodeByName("AWAITING_CONN_1"));
        controlStates.add(graph.getNodeByName("AWAITING_DISC_2"));
        controlStates.add(graph.getNodeByName("DATA_TRANSFER_3"));
        controlStates.add(graph.getNodeByName("FRAME_REJECT_4"));

        TreeNode<Data> tree = new TreeNode<Data>(new Data(controlStates));

        if (buildTree(tree)) {
            System.out.println("OK!");
        } else {
            System.out.println("NEM JÓÓÓÓÓÓÓ!");
        }

        printTree(tree, 0);
    }

    private void printTree(TreeNode<Data> tree, int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }

        System.out.println(tree.getData().getInput() + ": " + tree.getData().getNodes() + " - " + tree.getData().getOutput());
        for (TreeNode<Data> child : tree.children) {
            printTree(child, indent + 1);
        }
    }

    private boolean buildTree(TreeNode<Data> tree) {
        Collection<Node> nodes = tree.getData().getNodes();

        // nodes-ot kellene darabolni, bemenet/kimenet alapján.

        // minden lehetséges bemenetre, megnézzük, hogy eltérőek-e a kimenetek.
        boolean ok = false;

//        if("PRED_NR_OK".equals(tree.getData().getInput())) {
//            System.out.println("magic!");
//        }

        for (String input : edgesByInput.keySet()) {
            Set<Node> outputNodes = new HashSet<Node>();
            Map<String, Collection<Node>> nodesByOutput = new HashMap<String, Collection<Node>>();

            boolean handled = true;
            boolean badInput = false;
            for (Node node : nodes) {
                boolean noEdgeForThisInput = true;
                badInput = false;

                // nézzük meg, hogy szerepel-e ez bemenetként.
                for (Edge e : graph.getOutEdges(node)) {
                    if (input.equals(e.getInput())) {
                        if (!outputNodes.contains(graph.getDest(e))) {

                            // még nincs ilyen, akkor rendben van
                            outputNodes.add(graph.getDest(e));
                            String output = e.hasOutput() ? implode(e.getOutputs()) : null;

                            if (!nodesByOutput.containsKey(output)) {
                                nodesByOutput.put(output, new ArrayList<Node>());
                            }
                            nodesByOutput.get(output).add(node);
                            noEdgeForThisInput = false;
                        } else {
                            badInput = true;
                        }
                        break;
                    }
                }

                if (badInput) {
                    break;
                }

                if (noEdgeForThisInput) {
                    // nincs ilyen él -- implicit átmenet!
                    if (outputNodes.contains(node)) {
                        // implicit él, önmagába mennénk, ha van iylen nem jó!
                        badInput = true;
                        break;
                    }

                    // implicit él, önmagába megyünk, kimenet epszilon.
                    outputNodes.add(node);

                    String output = "epszilon";
                    if (!nodesByOutput.containsKey(output)) {
                        nodesByOutput.put(output, new ArrayList<Node>());
                    }
                    nodesByOutput.get(output).add(node);
                } else {
                    // ez a bemenet nem használható!
                    handled = false;
                }
            }

            if (!badInput) {
                // darabolunk!
                Collection<Collection<Node>> partitions = nodesByOutput.values();
                if (partitions.size() != 1) {
                    ok = true;
                    System.out.println("darabolunk!");
                    // csak akkor jó input, ha több partíciót csinál!
                    tree.children = new ArrayList<TreeNode<Data>>(); // töröljük a már meglévő gyerekeket, ha van!
                    boolean subTreeNotOk = false;
                    for (String output : nodesByOutput.keySet()) {
                        Collection<Node> partition = nodesByOutput.get(output);
                        TreeNode<Data> child = new TreeNode<Data>(new Data(partition, input, output));
                        tree.addChild(child);

                        if (child.getData().getNodes().size() > 1) {
                            if (!buildTree(child)) {
                                subTreeNotOk = true;
                                break;
                            }
                        }
                    }
                    if (subTreeNotOk) {
                        // nem jó az algráf, új input alapján kell darabolni!.
//                        System.out.println("Elakadtunk itt: " + input);
//                        if(input.equals("DISC")) {
//                            System.out.println("");
//                        }
                        ok = false;
                        continue;
                    } else {
                        break;
                    }
                }
            }
        }

        if (!ok) {
            return false;
        } else {
            return true;
        }
    }

    private String implode(String[] ary) {
        String out = "";
        for (int i = 0; i < ary.length; i++) {
            if (i != 0) {
                out += "; ";
            }
            out += ary[i];
        }
        return out;
    }
}
