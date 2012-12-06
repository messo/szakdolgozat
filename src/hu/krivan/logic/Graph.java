package hu.krivan.logic;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import java.util.*;

/**
 *
 * @author Balint
 */
public class Graph extends DirectedSparseMultigraph<Node, Edge> {

    private Map<String, Node> nodesByName = new HashMap<String, Node>();
    private Map<Node, Map<Node, List<Edge>>> spanningTreesByNode = new HashMap<Node, Map<Node, List<Edge>>>();

    public enum Type {

        INPUT, PRED, LEFT, RIGHT
    }

    @Override
    public boolean addVertex(Node vertex) {
        boolean added = super.addVertex(vertex);
        if (added) {
            nodesByName.put(vertex.getName(), vertex);
        }
        return added;
    }

    public Node getNodeByName(String name) {
        return nodesByName.get(name);
    }

    /**
     * Útkeresés from-ból to-ba, szélességi bejárással
     *
     * @param from
     * @param to
     * @return
     */
    public List<Edge> findPath(Node from, Node to) {
        if (from.isDisabled() || to.isDisabled()) {
            return null; // tutira nincs út
        }

        if (from.equals(to)) {
            // üres út
            return new ArrayList<Edge>();
        }

        //System.out.printf("## sima keresés: %s -> %s \n", from, to);
        Queue<Node> nodeQueue = new LinkedList<Node>();
        nodeQueue.add(from);

        // melyik csúcsba, melyik élen jutottunk el
        Map<Node, Edge> map = new HashMap<Node, Edge>();
        Map<Node, Integer> visited = new HashMap<Node, Integer>(nodeQueue.size());
        for (Node n : getVertices()) {
            visited.put(n, 0);
        }

        while (!nodeQueue.isEmpty()) {
            Node n = nodeQueue.poll();
            if (visited.get(n) == 2) {
                continue;
            } else {
                visited.put(n, 2);
            }

            if (n.equals(to)) {
                Node daddy = n;
                List<Edge> reversePath = new ArrayList<Edge>();
                while (daddy != from) {
                    Edge e = map.get(daddy);
                    reversePath.add(e);
                    daddy = getSource(e);
                }

                // megfordítjuk
                List<Edge> path = new ArrayList<Edge>(reversePath.size());
                for (int i = reversePath.size() - 1; i >= 0; i--) {
                    path.add(reversePath.get(i));
                }
                return path;
            }

            Collection<Edge> outEdges = getOutEdges(n);
            for (Edge e : outEdges) {
                if (e.isDisabled()) {
                    continue; // ha az él nem engedélyezett, akkor nem megyünk arra.
                }
                Node node = getDest(e);
                if (!node.isDisabled() && visited.get(node) == 0) {
                    map.put(node, e);
                    nodeQueue.add(node);
                    visited.put(node, 1);
                }
            }
        }

        return null;
    }

    /**
     * Út keresése v1-ből v2-be, adott élek érintésével
     *
     * @param v1 honnan
     * @param v2 hova
     * @param edges élek amiket érinteni kell
     */
    public List<Edge> findPath(Node v1, Node v2, List<Edge> edgesToTraverse) {
        if (v1.isDisabled() || v2.isDisabled()) {
            return null; // tutira nincs út
        }

        System.out.printf("# Útkeresés [%s]-ből [%s]-be\n", v1, v2);

        if (edgesToTraverse.isEmpty()) {
            // egyszerűsített esetre fallback
            return findPath(v1, v2);
        }

        List<Edge> path = new ArrayList<Edge>();
        path.addAll(findPath(v1, getSource(edgesToTraverse.get(0))));

        // az edgesLeft sort kell elfogyasztani. Kiveszünk belőle egy élt
        // és megpróbálunk eljutni egy olyan élbe, ahonnan szintén megy ki még él.
        for (int i = 0; i < edgesToTraverse.size(); i++) {
            Edge e = edgesToTraverse.get(i); // ezen kell átmennünk!
            Edge next;
            if (edgesToTraverse.size() == i + 1) {
                next = null;
            } else {
                next = edgesToTraverse.get(i + 1);
            }
            Node to = (next != null) ? getSource(next) : v2;

            path.add(e);
            path.addAll(findPath(getDest(e), to));
        }

        return path;
    }

    public Collection<Edge> getEdgesOnVariableHasGivenType(String variable, Type type) {
        Collection<Edge> result = new HashSet<Edge>();

        for (Edge e : getEdges()) {
            switch (type) {
                case INPUT:
                    if (e.hasInput()) {
                        if (variable.equals(e.getInput())) {
                            result.add(e);
                        }
                    }
                    break;
                case PRED:
                    if (e.hasPredicate()) {
                        for (Predicate pred : e.getPredicates()) {
                            if (variable.equals(pred.getLeft()) || variable.equals(pred.getRight())) {
                                result.add(e);
                                break;
                            }
                        }
                    }
                    break;
                case LEFT:
                    if (e.hasAction()) {
                        for (Action action : e.getActions()) {
                            if (variable.equals(action.getKey())) {
                                result.add(e);
                                break;
                            }
                        }
                    }
                    break;
                case RIGHT:
                    if (e.hasAction()) {
                        for (Action action : e.getActions()) {
                            if (action.getValue().contains(variable)) {
                                result.add(e);
                                break;
                            }
                        }
                    }
                    break;
            }
        }

        return result;
    }

    private Set<Edge> getEdgesWhereItCanBeChanged(String variable) {
        Set<Edge> edges = new HashSet<Edge>();

        for (Edge e : getEdges()) {
            if (e.hasInput()) {
                if (variable.equals(e.getInput())) {
                    edges.add(e);
                    break;
                }
            }
            if (e.hasAction()) {
                for (Action action : e.getActions()) {
                    if (variable.equals(action.getKey())) {
                        edges.add(e);
                        break;
                    }
                }
            }
        }

        return edges;
    }

    /**
     * Hurkokat keres, adott vezérlőállapotból, adott változó állításához.
     *
     * @param start
     * @param variable
     * @return
     */
    public List<List<Edge>> findLoops(Node start, String variable) {
        List<List<Edge>> result = new LinkedList<List<Edge>>();

        Set<Edge> _edges = getEdgesWhereItCanBeChanged(variable);
        Edge[] edges = _edges.toArray(new Edge[_edges.size()]);
        for (int i = 0; i < edges.length; i++) {
            Node edgeSrc = getSource(edges[i]);
            // út start-ból edgeSrc-ba
            List<Edge> path1 = findSpanningTree(start).get(edgeSrc);

            Node edgeDst = getDest(edges[i]);
            // út edgeDst-ből start-ba
            List<Edge> path2 = findSpanningTree(edgeDst).get(start);

            List<Edge> loop = new LinkedList<Edge>();
            loop.addAll(path1);
            loop.add(edges[i]);
            loop.addAll(path2);
            result.add(loop);
        }

        return result;
    }

    public Map<Node, List<Edge>> findSpanningTree(Node root) {
        if (spanningTreesByNode.containsKey(root)) {
            return spanningTreesByNode.get(root);
        }

        Map<Node, List<Edge>> paths = new HashMap<Node, List<Edge>>();
        spanningTreesByNode.put(root, paths);

        paths.put(root, Collections.<Edge>emptyList());
        Deque<Node> nodes = new ArrayDeque<Node>(getVertexCount());
        nodes.add(root);

        List<Node> seen = new LinkedList<Node>();

        while (!nodes.isEmpty()) {
            Node node = nodes.pop();
            seen.add(node);

            for (Edge edge : getOutEdges(node)) {
                Node dest = getDest(edge);
                if (!seen.contains(dest) && !nodes.contains(dest)) {
                    // őt még nem láttuk és nincs a sorban.
                    nodes.add(dest);
                    List<Edge> pathSofar = paths.get(node);
                    List<Edge> path = new LinkedList<Edge>();
                    for (Edge e : pathSofar) {
                        path.add(e);
                    }
                    path.add(edge);
                    paths.put(dest, path);
                }
            }
        }

        return paths;
    }
}
