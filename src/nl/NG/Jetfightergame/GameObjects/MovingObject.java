package nl.NG.Jetfightergame.GameObjects;

import nl.NG.Jetfightergame.Engine.GLMatrix.GL2;
import nl.NG.Jetfightergame.Engine.GLMatrix.MatrixStack;
import nl.NG.Jetfightergame.ShapeCreators.Shape;
import nl.NG.Jetfightergame.Vectors.DirVector;
import nl.NG.Jetfightergame.Vectors.PosVector;

import java.util.function.Consumer;

/**
 * {@author Geert van Ieperen
 *         created on 7-11-2017.
 * an object that moves, and may be influenced by other objects.
 * These methods may not influence the state of this object, this should be done with methods in
 * {@link nl.NG.Jetfightergame.Engine.Updatable}
 */
public interface MovingObject extends Touchable {

    /**
     * moves the reference frame from local space to each shape, executing {@code action} on every shape.
     * every create call should preserve the matrix stack.
     * @param ms reference frame to perform transformations on
     * @param action actions to execute for every Shape
     * @param extrapolate false if the actions are done on the (stable) current points,
     *                   true if the actions are done on the (unstable) extrapolated points
     */
    void create(MatrixStack ms, Consumer<Shape> action, boolean extrapolate);

    default void create(MatrixStack ms, Consumer<Shape> action){
        create(ms, action, false);
    }

    /**
     * moves the reference frame from global space to this object and executes action.
     * every create call should preserve the matrix stack.
     * @param ms reference frame to perform transformations on
     * @param action action to perform one in local space
     * @param extrapolate true if estimations may be used (e.g. the not-rendered part)
     *                   false if the actions must be performed on parameters that no longer change
     */
    void toLocalSpace(MatrixStack ms, Runnable action, boolean extrapolate);

    default void toLocalSpace(MatrixStack ms, Runnable action){
        toLocalSpace(ms, action, false);
    }

    /**
     * calculate expected position and rotation, but does not change the current state of the object.
     * This means that {@code rotation} and {@code position} are not updated
     * @param deltaTime time since last frame
     */
    void preUpdate(float deltaTime);

    /**
     * calculate effect of collision, but does not apply new position
     * <p>The procedure for computing the post-collision linear velocities {@code v'_i} and angular velocities {@code  w'_i} is as follows:</p>
     * (1) Compute the reaction impulse magnitude {@code j_r}
     * (2) Compute the reaction impulse vector {@code J_r} in terms of its magnitude {@code j_r} and contact normal {@code |n|}  using {@code  J_r = j_r * |n|}.
     * (3) Compute new linear velocities {@code  v'_i} in terms of old velocities {@code v_i}, masses {@code m_i} and reaction impulse vector {@code j_r}
     * (4) Compute new angular velocities {@code  w'_i} in terms of old angular velocities {@code  w_i}, inertia tensors {@code I_i} and reaction impulse {@code j_r}
     */
    void applyCollision();

    /**
     * checks the movement of the hitpoints of this object against the planes of 'other'.
     * @param other an object that may hit this object
     * @return true if there was a collision
     */
    boolean checkCollisionWith(Touchable other);

    /**
     * draws this object, interpolated for the given timestamp.
     * Every call to currentTime must be larger than the one before (this should be checked)
     * @param currentTime time since some t_0 in seconds
     * @see #getPosition(float)
     */
    void draw(GL2 gl, float currentTime);

    /**
     * Every call to currentTime must be larger than the one before (this should be checked)
     * @return position of the center of mass of this object, interpolated for the given timestamp.
     * @param currentTime time since some t_0 in seconds
     * @throws IllegalArgumentException if the currentTime is less than the previous call to this method
     */
    PosVector getPosition(float currentTime);

    /**
     * @return movement of the center of mass of this object in world-space
     */
    DirVector getVelocity();
}

