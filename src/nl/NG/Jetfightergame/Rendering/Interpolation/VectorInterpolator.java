package nl.NG.Jetfightergame.Rendering.Interpolation;

import nl.NG.Jetfightergame.Tools.Vectors.Vector;

/**
 * @author Geert van Ieperen
 * created on 15-12-2017.
 */
public class VectorInterpolator extends LinearInterpolator<Vector> {

    public VectorInterpolator(int capacity, Vector initialValue) {
        super(capacity, initialValue);
    }

    protected Vector interpolate(Vector firstElt, Vector secondElt, float fraction) {
        Vector dest = firstElt.interpolateTo(secondElt, fraction);

        if (!dest.isScalable()) dest.set(firstElt);
        return dest;
    }
}
