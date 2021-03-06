package nl.NG.Jetfightergame.ShapeCreation;

import nl.NG.Jetfightergame.Primitives.Plane;
import nl.NG.Jetfightergame.Rendering.MatrixStack.GL2;
import nl.NG.Jetfightergame.Rendering.MatrixStack.MatrixStack;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import nl.NG.Jetfightergame.Tools.Vectors.Vector;

/**
 * Created by Geert van Ieperen on 13-3-2017.
 * a {@link Shape} drawn from some point to another point
 */
public class DirectedShapeFrame implements DirectedShape {

    private final Shape object;

    // these vectors are world-positions
    private PosVector source;
    private PosVector target;
    //these vectors are relative to the point of drawing

    public DirectedShapeFrame(Shape shape) {
        this.object = shape;
    }

    @Override
    public void setSource(MatrixStack ms, PosVector source) {
        Vector s = ms.getPosition(source);
        if (s != null) this.source = (PosVector) s;
    }

    @Override
    public void setTarget(MatrixStack ms, PosVector target) {
        Vector t = ms.getPosition(target);
        if (t != null) this.target = (PosVector) t;
    }

    @Override
    public Iterable<? extends Plane> getPlanes() {
        return object.getPlanes();
    }

    @Override
    public Iterable<PosVector> getPoints() {
        return object.getPoints();
    }

    @Override
    public void render(GL2.Painter lock) {
        object.render(lock);
    }

    @Override
    public void dispose() {
        object.dispose();
    }

    /**
     * draws the shape from {@code source} in the direction of {@code target}.
     * the drawObjects method of this class should be called in the worldspace of the ShadowMatrix
     * @throws IllegalStateException if setSource or setTarget is not set
     */
    public void draw(GL2 gl) {
        checkStatus();

//        ToolBox.drawAxisFrame(getGL, glut);
        gl.pushMatrix();
        {
            gl.pointFromTo(source, target);

//            ToolBox.drawAxisFrame(getGL, glut);
            gl.draw(object);
        }
        gl.popMatrix();
    }

    private void checkStatus() {
        if (source == null || target == null) {
            throw new IllegalStateException(
                    String.format("DirectedShape:drawObjects(): drawObjects method was called but (source == %s) and (target == %s)", source, target)
            );
        }
    }

}
