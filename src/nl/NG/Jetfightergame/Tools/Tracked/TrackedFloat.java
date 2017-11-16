package nl.NG.Jetfightergame.Tools.Tracked;

/**
 * Created by Geert van Ieperen on 4-5-2017.
 */
public class TrackedFloat extends TrackedDifferable<Float> {

    public TrackedFloat(Float current, Float previous) {
        super(current, previous);
    }

    public TrackedFloat(Float initial) {
        super(initial);
    }

    @Override
    public void addUpdate(Float addition) {
        update(current() + addition);
    }

    /**
     * @return the increase of the last updatePosition, defined as (current - previous)
     */
    public Float difference() {
        return current() - previous();
    }
}
