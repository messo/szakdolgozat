package hu.krivan.logic;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Balint
 */
public class Parser {

    private static Pattern linePattern = Pattern.compile("\\s*(.*?)\\s*\\-+\\(\\s*(.*?)\\s*\\)-*>\\s*(.*?)\\s*");
    private static Pattern commentPattern = Pattern.compile("^#(.*?)$");
    private static Pattern positionCSPattern = Pattern.compile("\\s*(.*?)\\s*\\{\\{\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*\\}\\}\\s*");
    private static Pattern positionSPattern = Pattern.compile("\\s*(.*?)\\s*\\{\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*\\}\\s*");
    private BufferedReader br;
    private Map<String, Node> nodes = new HashMap<String, Node>();
    private Graph graph = new Graph();
    private Map<Node, Point2D> locations = new HashMap<Node, Point2D>();

    private Parser(String fileName) throws FileNotFoundException {
        br = new BufferedReader(new FileReader(fileName));
    }

    public Graph getGraph() {
        return graph;
    }

    public Map<Node, Point2D> getLocations() {
        return locations;
    }    

    private Node getOrCreate(String nodeName) {
        Node n = nodes.get(nodeName);
        if (n == null) {
            n = new Node(nodeName);
            nodes.put(nodeName, n);
            graph.addVertex(n);
        }
        return n;
    }

    private void parse() throws IOException {
        String s;
        Node n1, n2;
        while ((s = br.readLine()) != null) {
            Matcher m;
            m = commentPattern.matcher(s);
            if (m.matches()) {
                // comment.
                continue;
            }
            m = linePattern.matcher(s);
            if (m.matches()) {
                n1 = getOrCreate(m.group(1));
                n2 = getOrCreate(m.group(3));
                graph.addEdge(new Edge(s, m.group(2)), n1, n2);
                continue;
            }
            m = positionCSPattern.matcher(s);
            if (m.matches()) {
                n1 = nodes.get(m.group(1));
                int x = Integer.parseInt(m.group(2));
                int y = Integer.parseInt(m.group(3));
                locations.put(n1, new Point2D.Float(x, y));
                continue;
            }
            m = positionSPattern.matcher(s);
            if (m.matches()) {
                n1 = nodes.get(m.group(1));
                int x = Integer.parseInt(m.group(2));
                int y = Integer.parseInt(m.group(3));
                locations.put(n1, new Point2D.Float(x, y));
                n1.setType(Node.Type.STATE);
                continue;
            }
        }
    }

    public static Parser parse(String fn) {
        try {
            Parser p = new Parser(fn);
            p.parse();
            return p;
        } catch (IOException ex) {
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
