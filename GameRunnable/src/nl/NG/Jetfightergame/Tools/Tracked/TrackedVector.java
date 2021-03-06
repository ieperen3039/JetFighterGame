package nl.NG.Jetfightergame.Tools.Tracked;

import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.Vector;

/**
 * @author Geert van Ieperen created on 30-10-2017.
 */
public class TrackedVector<V extends Vector> extends TrackedObject<V> implements Differable {
    public TrackedVector(V initial) {
        super(initial);
    }

    public TrackedVector(V previous, V current) {
        super(previous, current);
    }

    @Override
    public DirVector difference() {
        return previous().to(current(), new DirVector());
    }

    /**
     * updates the value by adding the parameter to the current value
     *
     * @param addition the value that is added to the current. actual results may vary
     * @param dest the new value of this vector will be stored in dest. This is mainly for intermediate calculations
     */
    public void addUpdate(DirVector addition, V dest) {
        current().add(addition, dest);
        update(dest);
    }
}
