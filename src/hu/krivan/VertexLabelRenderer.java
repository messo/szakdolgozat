package hu.krivan;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import hu.krivan.logic.Edge;
import hu.krivan.logic.Node;
import java.awt.Color;

/**
 *
 * @author Balint
 */
public class VertexLabelRenderer extends BasicVertexLabelRenderer<Node, Edge> {

    @Override
    public void labelVertex(RenderContext<Node, Edge> rc, Layout<Node, Edge> layout, Node v, String label) {
        if (v.getType() == Node.Type.CONTROL_STATE) {
            position = Position.CNTR;
        } else {
            position = Position.AUTO;
        }
        Color saved = rc.getScreenDevice().getForeground();
        if (v.isDisabled()) {
            rc.getScreenDevice().setForeground(Color.LIGHT_GRAY);
        }
        super.labelVertex(rc, layout, v, label);
        rc.getScreenDevice().setForeground(saved);
    }
}
