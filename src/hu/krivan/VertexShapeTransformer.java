package hu.krivan;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import hu.krivan.logic.Edge;
import hu.krivan.logic.Node;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import org.apache.commons.collections15.Transformer;

/**
 *
 * @author Balint
 */
public class VertexShapeTransformer implements Transformer<Node, Shape> {

    private static final int PADDING_HORIZ = 10;
    private static final int PADDING_VERT = 5;
    VisualizationViewer<Node, Edge> vv;

    public VertexShapeTransformer(VisualizationViewer<Node, Edge> vv) {
        this.vv = vv;
    }

    @Override
    public Shape transform(Node n) {
        if (n.getType() == Node.Type.CONTROL_STATE) {
            Font f = vv.getRenderContext().getVertexFontTransformer().transform(n);
            FontMetrics fm = vv.getRenderContext().getScreenDevice().getFontMetrics(f);
            int width = fm.stringWidth(n.getName()) + PADDING_HORIZ * 2;
            int height = fm.getHeight() + PADDING_VERT * 2;
            return new Ellipse2D.Float(-width / 2, -height / 2, width, height);
        } else {
            int width = 10;
            int height = 10;
            return new Ellipse2D.Float(-width / 2, -height / 2, width, height);
        }
    }
}
