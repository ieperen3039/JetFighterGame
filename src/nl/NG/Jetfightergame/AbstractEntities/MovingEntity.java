package nl.NG.Jetfightergame.AbstractEntities;

import nl.NG.Jetfightergame.AbstractEntities.Hitbox.RigidBody;
import nl.NG.Jetfightergame.Rendering.MatrixStack.MatrixStack;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import org.joml.Quaternionf;

/**
 * {@author Geert van Ieperen created on 7-11-2017. an object that moves, and
 * may be influenced by other objects.
 */
public interface MovingEntity extends Touchable {

    /**
     * moves the reference frame from global space to this object and executes
     * action. every create call should preserve the matrix stack.
     * @param ms          reference frame to perform transformations on
     * @param action      action to perform one in local space
     * @param extrapolate true if estimations may be used (e.g. the not-rendered
     *                    part) false if the actions must be performed on
     *                    parameters that no longer change
     */
    void toLocalSpace(MatrixStack ms, Runnable action, boolean extrapolate);

    default void toLocalSpace(MatrixStack ms, Runnable action) {
        toLocalSpace(ms, action, false);
    }

    /**
     * calculate expected position and rotation, but does not change the current
     * state of the object. This means that {@code rotation} and {@code
     * position} are not updated
     * @param deltaTime time since last frame
     * @param netForce
     */
    void preUpdate(float deltaTime, DirVector netForce);

    /**
     * update the state of this object, may not be called by any method from
     * another interface. Other operations on position and rotation should be
     * synchronized
     * @param currentTime seconds between some starttime t0 and the begin of the
     *                    current gameloop
     */
    void update(float currentTime);

    /**
     * applies the final collision after it has been processed to this object
     * @param newState
     * @param deltaTime
     * @param currentTime
     */
    void applyCollision(RigidBody newState, float deltaTime, float currentTime);

    /**
     * checks the movement of the hitpoints of this object against the planes of
     * 'other'. the method implementing this must be thread-safe
     * @param other     an object that may hit this object
     * @param deltaTime
     * @return true if there was a collision. This also means that the other has
     *         a collision as well
     */
    boolean checkCollisionWith(Touchable other, float deltaTime);

    /**
     * @return a copy of the position of the center of mass of this object,
     *         interpolated for a predefined timeStamp
     * @implNote The entity should take care of defining a valid
     *         timestamp method to ensure correct interpolation
     */
    PosVector interpolatedPosition();

    /**
     * @return a copy of the rotation of this object, interpolated for a
     *         predefined timeStamp
     * @implNote The entity should take care of defining a valid
     *         timestamp method to ensure correct interpolation
     */
    Quaternionf interpolatedRotation();

    /**
     * @return a copy of the movement of the center of mass of this object in
     *         world-space
     */
    DirVector getVelocity();

    /**
     * @return the mass of this object in kilograms.
     */
    float getMass();

    /**
     * @return a copy of the position of the center of mass of this object in
     *         wolrd-space
     */
    PosVector getPosition();

    /**
     * distance of the farthest vertex of this object
     * @return the vertex P for which P.to(getPosition()) is maximized in
     *         world-space
     */
    float getRange();

    /**
     * @return the position of this object in the next timestamp. This value is
     *         bound to change after calls to
     *         {@link nl.NG.Jetfightergame.Engine.GameState.CollisionDetection}
     */
    PosVector getExpectedPosition();

    /**
     * applies a change in velocity by applying the given momentum to the velocity.
     * This may only be applied between calls to
     * {@link #preUpdate(float, DirVector)} and {@link #update(float)}
     * @param momentum the momentum to be applied to the gravity center of this
     *                 entity
     */
    void applyMoment(DirVector momentum);
}

