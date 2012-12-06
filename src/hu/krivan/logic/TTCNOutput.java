package hu.krivan.logic;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Balint
 */
public class TTCNOutput {

    private final Graph graph;
    private int indent = 0;
    private PrintStream out;

    public TTCNOutput(Graph g) {
        this.graph = g;
        this.out = System.out;
    }

    public void output() {
        List<Node> nodes = new ArrayList<Node>();
        for (Node n : graph.getVertices()) {
            nodes.add(n);
        }

        Collections.sort(nodes, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                String id1 = o1.getName().substring(o1.getName().lastIndexOf('_'));
                String id2 = o2.getName().substring(o2.getName().lastIndexOf('_'));

                return id1.compareTo(id2);
            }
        });

        println("module mini_m");
        println("{");
        indent += 2;
        println("import from mini_d all;");
        println("");
        println("function f_model() runs on Model");
        println("{");
        indent += 2;

        for (Node n : nodes) {
            if (n.getType() == Node.Type.CONTROL_STATE) {
                writeStateBlock(n);
            }
        }

        indent -= 2;
        println("}");
        indent -= 2;
        println("}");
    }

    private void writeStateBlock(Node n) {
        println(String.format("label %s {", n.getName()));
        indent += 2;
        writePorts(n);
        writeAlt(n);
        indent -= 2;
        println(String.format("}"));
        println("");
    }

    private void writePorts(Node n) {
    }

    private void writeAlt(Node n) {
        println("alt {");
        indent += 2;

        String port;
        for (Edge e : graph.getOutEdges(n)) {
            String input = e.getInput();
            if (input.startsWith("U_")) {
                port = "portU";
            } else {
                port = "portP";
            }
            println(String.format("[] %s.receive(%s) %s",
                    port,
                    input.startsWith("U_") ? "\"" + input + "\"" : input,
                    input.startsWith("U_") ? "{" : "-> value frame {"));
            indent += 2;

            if (e.hasPredicate()) {
                println(String.format("if (%s) {", implode(e.getPredicates())));
                indent += 2;
            }

            sendOutputs(e);
            doActions(e);

            Node to = graph.getDest(e);
            if (to.getType() == Node.Type.CONTROL_STATE) {
                println(String.format("goto %s;", to.getName()));
            } else {
                Collection<Edge> outEdges = graph.getOutEdges(to);
                for (Edge outEdge : outEdges) {
                    if (graph.getDest(outEdge).getType() != Node.Type.CONTROL_STATE) {
                        throw new RuntimeException("Rekurzio kezelese szukseges!");
                    }

                    if (outEdge.hasPredicate()) {
                        println(String.format("if (%s) {", implode(outEdge.getPredicates())));
                        indent += 2;
                    }

                    sendOutputs(outEdge);
                    doActions(outEdge);
                    println(String.format("goto %s;", graph.getDest(outEdge).getName()));

                    if (outEdge.hasPredicate()) {
                        indent -= 2;
                        println("}");
                    }
                }
            }

            if (e.hasPredicate()) {
                indent -= 2;
                println("}");
            }

            indent -= 2;
            println("}");
        }

        indent -= 2;
        println("}");
    }

    private void println(String s) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.println(s);
    }

    private String implode(Predicate[] ary) {
        String out = "";
        for (int i = 0; i < ary.length; i++) {
            if (i != 0) {
                out += " and ";
            }
            out += ary[i].toString();
        }
        return out;
    }

    private void sendOutputs(Edge e) {
        if (e.hasOutput()) {
            for (String o : e.getOutputs()) {
                if (o.startsWith("U_")) {
                    println(String.format("portU.send(\"%s\");", o));
                } else {
                    println(String.format("portP.send(%s);", o));
                }
            }
        }
    }

    private void doActions(Edge e) {
        if (e.hasAction()) {
            for (Action action : e.getActions()) {
                println(action.toString());
            }
        }
    }
}
