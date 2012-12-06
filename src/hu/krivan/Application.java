package hu.krivan;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import hu.krivan.logic.*;
import hu.krivan.logic.Graph.Type;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;
import org.apache.commons.collections15.functors.ConstantTransformer;

/**
 *
 * @author Balint
 */
public class Application extends javax.swing.JFrame {

    private final hu.krivan.logic.Graph graph;
    private Collection<Edge> path;
    private final VisualizationViewer<Node, Edge> vv;
    private Iterator<Edge> pathIt;
    private List<Edge> highlighted = new LinkedList<Edge>();
    private List<List<Edge>> loops;
    private int loopIdx = -1;
    private Node lastSelectedStartingForLoop;
    private String lastSelectedVarForLoop;
    private List<Edge> edgesToTraverse = new ArrayList<Edge>();

    /**
     * Creates new form GraphFrameWithJUNG
     */
    public Application() {
        initComponents();
        Parser p = Parser.parse("graph.txt");
        graph = p.getGraph();

        DefaultComboBoxModel cbm1 = new DefaultComboBoxModel();
        for (Node node : graph.getVertices()) {
            cbm1.addElement(node);
        }
        spanningTreeRoot.setModel(cbm1);
        DefaultComboBoxModel cbm2 = new DefaultComboBoxModel();
        for (Node node : graph.getVertices()) {
            cbm2.addElement(node);
        }
        pathFrom.setModel(cbm2);
        DefaultComboBoxModel cbm3 = new DefaultComboBoxModel();
        for (Node node : graph.getVertices()) {
            cbm3.addElement(node);
        }
        pathTo.setModel(cbm3);

        DefaultComboBoxModel cbm4 = new DefaultComboBoxModel();
        for (Node node : graph.getVertices()) {
            if (node.getType() == Node.Type.CONTROL_STATE) {
                cbm4.addElement(node);
            }
        }
        loopStartingNode.setModel(cbm4);

        FRLayout<Node, Edge> l = new FRLayout<Node, Edge>(graph);
        vv = new VisualizationViewer<Node, Edge>(l, new Dimension(1028, 768));

        // graph.visualize(visualGraph, vv);

        vv.setGraphLayout(new StaticLayout<Node, Edge>(graph,
                TransformerUtils.mapTransformer(p.getLocations())));

        final DefaultModalGraphMouse<Node, Edge> gm = new DefaultModalGraphMouse<Node, Edge>();
        gm.setMode(ModalGraphMouse.Mode.PICKING);
        vv.setGraphMouse(gm);

        vv.getRenderContext().setVertexLabelTransformer(new Transformer<Node, String>() {
            @Override
            public String transform(Node n) {
                return n.getName();
            }
        });
        vv.getRenderContext().setEdgeLabelTransformer(new Transformer<Edge, String>() {
            @Override
            public String transform(Edge e) {
                String result = "<html>";
                if (e.hasInput()) {
                    result += "<font color=\"blue\"";
                    if (vv.getPickedEdgeState().getPicked().contains(e)) {
                        result += " bgcolor=\"orange\"";
                    }
                    result += ">";
                    result += e.getInput();
                    result += "</font> ";
                }

                if (e.hasPredicate()) {
                    String color = "<font color=\"red\">";
                    if (vv.getPickedEdgeState().getPicked().contains(e)) {
                        color = "<font color=\"green\">";
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Predicate p : e.getPredicates()) {
                        sb.append(p.toHTML()).append(", ");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    sb.deleteCharAt(sb.length() - 1);
                    result += color + "[" + sb.toString() + "]";
                }

                if (e.hasOutput()) {
                    result += " <font color=\"green\"";
                    if (vv.getPickedEdgeState().getPicked().contains(e)) {
                        result += " bgcolor=\"orange\"";
                    }
                    result += ">";
                    StringBuilder sb = new StringBuilder();
                    for (String o : e.getOutputs()) {
                        sb.append(o).append(", ");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    sb.deleteCharAt(sb.length() - 1);

                    result += sb.toString();
                    result += "</font> ";
                }

                return result;
            }
        });
        vv.setEdgeToolTipTransformer(new Transformer<Edge, String>() {
            @Override
            public String transform(Edge e) {
                StringBuilder sb = new StringBuilder("<html>");
                if (e.hasInput()) {
                    sb.append("<b>INPUT:</b> ").append(e.getInput()).append("<br>");
                    sb.append("<br>");
                }
                if (e.hasPredicate()) {
                    sb.append("<b>PREDICATES:</b><br>");
                    for (Predicate pred : e.getPredicates()) {
                        sb.append("&middot; ").append(pred.toHTML()).append("<br>");
                    }
                    sb.append("<br>");
                }
                if (e.hasAction()) {
                    sb.append("<b>ACTIONS:</b><br>");
                    for (Action action : e.getActions()) {
                        sb.append("&middot; ").append(action).append("<br>");
                    }
                    sb.append("<br>");
                }
                if (e.hasOutput()) {
                    sb.append("<b>OUTPUTS:</b><br>");
                    for (String output : e.getOutputs()) {
                        sb.append("&middot; ").append(output).append("<br>");
                    }
                    sb.append("<br>");
                }
                return sb.toString();
            }
        });

        vv.getRenderContext().setLabelOffset(20);
        vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(.5, .5));
        vv.getRenderContext().setVertexShapeTransformer(new VertexShapeTransformer(vv));
        vv.getRenderContext().setVertexDrawPaintTransformer(new Transformer<Node, Paint>() {
            @Override
            public Paint transform(Node n) {
                if (n.isDisabled()) {
                    return Color.LIGHT_GRAY;
                } else {
                    return Color.BLACK;
                }
            }
        });
        vv.getRenderContext().setVertexFillPaintTransformer(new ConstantTransformer(Color.WHITE));
        vv.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<Edge, Paint>() {
            @Override
            public Paint transform(Edge e) {
                if (highlighted.contains(e)) {
                    return Color.ORANGE;
                } else if (vv.getPickedEdgeState().getPicked().contains(e)) {
                    return Color.GREEN;
                }
                if (e.isDisabled()) {
                    return Color.LIGHT_GRAY;
                } else {
                    return e.getColor();
                }
            }
        });
        vv.getRenderContext().setArrowFillPaintTransformer(new Transformer<Edge, Paint>() {
            @Override
            public Paint transform(Edge e) {
                if (e.isDisabled()) {
                    return Color.LIGHT_GRAY;
                } else {
                    if (highlighted.contains(e)) {
                        return Color.ORANGE;
                    } else if (vv.getPickedEdgeState().getPicked().contains(e)) {
                        return Color.GREEN;
                    } else {
                        return e.getColor();
                    }
                }
            }
        });
        vv.getRenderContext().setArrowDrawPaintTransformer(new Transformer<Edge, Paint>() {
            @Override
            public Paint transform(Edge e) {
                if (e.isDisabled()) {
                    return Color.GRAY;
                } else {
                    return Color.BLACK;
                }
            }
        });
        vv.getRenderer().setVertexLabelRenderer(new VertexLabelRenderer());
        vv.setBackground(Color.white);

        vv.getPickedEdgeState().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    edgesToTraverse.add((Edge) e.getItem());
                } else {
                    edgesToTraverse.remove((Edge) e.getItem());
                }
            }
        });

        graphPanel.add(vv);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        graphPanel = new javax.swing.JPanel();
        getLocations = new javax.swing.JButton();
        findPathBtn = new javax.swing.JButton();
        saveBtn = new javax.swing.JButton();
        variableToHighlight = new javax.swing.JComboBox();
        roleToHighlight = new javax.swing.JComboBox();
        highlightEdgesBtn = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        spanningTreeRoot = new javax.swing.JComboBox();
        findSpanningTreeBtn = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        loopStartingNode = new javax.swing.JComboBox();
        findLoopsBtn = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        pathFrom = new javax.swing.JComboBox();
        pathTo = new javax.swing.JComboBox();
        varForLoop = new javax.swing.JComboBox();
        genTTCNBtn = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Vizualizáció");

        getLocations.setText("Pozíciók kinyerése");
        getLocations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getLocationsActionPerformed(evt);
            }
        });

        findPathBtn.setText("Útkeresés");
        findPathBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findPathBtnActionPerformed(evt);
            }
        });

        saveBtn.setText("Mentés képként");
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBtnActionPerformed(evt);
            }
        });

        variableToHighlight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "va", "vs", "vr" }));

        roleToHighlight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "input", "predicate", "left", "right" }));

        highlightEdgesBtn.setText("Kiemel");
        highlightEdgesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highlightEdgesBtnActionPerformed(evt);
            }
        });

        spanningTreeRoot.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        findSpanningTreeBtn.setText("Feszítőfa");
        findSpanningTreeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findSpanningTreeBtnActionPerformed(evt);
            }
        });

        loopStartingNode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        findLoopsBtn.setText("Hurok keresés");
        findLoopsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findLoopsBtnActionPerformed(evt);
            }
        });

        pathFrom.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        pathTo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        varForLoop.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "va", "vs", "vr" }));

        genTTCNBtn.setText("TTCN-3 kód generálása");
        genTTCNBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genTTCNBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(getLocations)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveBtn)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(variableToHighlight, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(roleToHighlight, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(highlightEdgesBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addComponent(spanningTreeRoot, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(findSpanningTreeBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(findLoopsBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(varForLoop, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator2)
                    .addComponent(jSeparator3)
                    .addComponent(pathFrom, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(genTTCNBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(findPathBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pathTo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator4)
                    .addComponent(loopStartingNode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pathFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pathTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(findPathBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loopStartingNode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(varForLoop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(findLoopsBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spanningTreeRoot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(findSpanningTreeBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(variableToHighlight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(roleToHighlight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(highlightEdgesBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(genTTCNBtn)
                        .addGap(0, 81, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(11, 11, 11)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(getLocations)
                            .addComponent(saveBtn))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void getLocationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getLocationsActionPerformed
        for (Node n : graph.getVertices()) {
            Point2D loc = vv.getGraphLayout().transform(n);
            if (n.getType() == Node.Type.CONTROL_STATE) {
                System.out.printf("%s{{%.0f,%.0f}}\n", n, loc.getX(), loc.getY());
            } else {
                System.out.printf("%s{%.0f,%.0f}\n", n, loc.getX(), loc.getY());
            }
        }
    }//GEN-LAST:event_getLocationsActionPerformed

    private void findPathBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findPathBtnActionPerformed
        // színek alapállapotba
        for (Edge e : graph.getEdges()) {
            e.setColor(Color.BLACK);
        }

        System.out.println(edgesToTraverse);

        path = graph.findPath((Node) pathFrom.getSelectedItem(), (Node) pathTo.getSelectedItem(), edgesToTraverse);

        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nincs ilyen út!", "Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        pathIt = path.iterator();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Edge e = pathIt.next();
                e.setColor(Color.YELLOW);

                vv.repaint();

                if (!pathIt.hasNext()) {
                    cancel();
                }
            }
        }, 0, 400);
    }//GEN-LAST:event_findPathBtnActionPerformed

    private void saveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBtnActionPerformed
        int width = vv.getWidth();
        int height = vv.getHeight();

        BufferedImage bi = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bi.createGraphics();
        vv.paint(graphics);
        graphics.dispose();

        try {
            Calendar cal = Calendar.getInstance();
            ImageIO.write(bi, "png", new File(String.format("graph-%1$ts.png", cal)));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }//GEN-LAST:event_saveBtnActionPerformed

    private void highlightEdgesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highlightEdgesBtnActionPerformed
        String variable = (String) variableToHighlight.getSelectedItem();
        String t = (String) roleToHighlight.getSelectedItem();
        Type type;
        if (t.equals("input")) {
            type = Type.INPUT;
        } else if (t.equals("predicate")) {
            type = Type.PRED;
        } else if (t.equals("left")) {
            type = Type.LEFT;
        } else if (t.equals("right")) {
            type = Type.RIGHT;
        } else {
            type = null;
        }

        highlighted.clear();
        highlighted.addAll(graph.getEdgesOnVariableHasGivenType(variable, type));

        if (highlighted.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nincs ilyen él!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }

        vv.repaint();
    }//GEN-LAST:event_highlightEdgesBtnActionPerformed

    private void findSpanningTreeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findSpanningTreeBtnActionPerformed
        Node node = (Node) spanningTreeRoot.getSelectedItem();
        Map<Node, List<Edge>> edges = graph.findSpanningTree(node);
        System.out.println(String.format("Feszítőfa utak %s-ból.", node));
        for (Node n : edges.keySet()) {
            System.out.println(String.format("Útvonal %s-be:", n));
            for (Edge e : edges.get(n)) {
                System.out.print("  ");
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_findSpanningTreeBtnActionPerformed

    private void findLoopsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findLoopsBtnActionPerformed
        findLoopsBtn.setEnabled(false);

        Node start = (Node) loopStartingNode.getSelectedItem();
        String varForLoopStr = (String) varForLoop.getSelectedItem();

        if (lastSelectedStartingForLoop != start || !varForLoopStr.equals(lastSelectedVarForLoop) || loopIdx == loops.size()) {
            lastSelectedStartingForLoop = start;
            lastSelectedVarForLoop = varForLoopStr;
            loops = graph.findLoops(start, varForLoopStr);
            loopIdx = 0;

            for (Edge e : graph.getEdges()) {
                e.setColor(Color.BLACK);
            }
            vv.repaint();
        }

        pathIt = loops.get(loopIdx++).iterator();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Edge e = pathIt.next();

                if (loopIdx % 3 == 0) {
                    e.setColor(Color.RED);
                } else if (loopIdx % 3 == 1) {
                    e.setColor(Color.ORANGE);
                } else {
                    e.setColor(Color.GREEN);
                }
                vv.repaint();

                if (!pathIt.hasNext()) {
                    cancel();
                }
            }
        }, 0, 400);

        findLoopsBtn.setEnabled(true);
    }//GEN-LAST:event_findLoopsBtnActionPerformed

    private void genTTCNBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genTTCNBtnActionPerformed
        new TTCNOutput(graph).output();
    }//GEN-LAST:event_genTTCNBtnActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Application().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton findLoopsBtn;
    private javax.swing.JButton findPathBtn;
    private javax.swing.JButton findSpanningTreeBtn;
    private javax.swing.JButton genTTCNBtn;
    private javax.swing.JButton getLocations;
    private javax.swing.JPanel graphPanel;
    private javax.swing.JButton highlightEdgesBtn;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JComboBox loopStartingNode;
    private javax.swing.JComboBox pathFrom;
    private javax.swing.JComboBox pathTo;
    private javax.swing.JComboBox roleToHighlight;
    private javax.swing.JButton saveBtn;
    private javax.swing.JComboBox spanningTreeRoot;
    private javax.swing.JComboBox varForLoop;
    private javax.swing.JComboBox variableToHighlight;
    // End of variables declaration//GEN-END:variables
}
