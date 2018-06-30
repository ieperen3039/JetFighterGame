package nl.NG.Jetfightergame.GameState;

import nl.NG.Jetfightergame.AbstractEntities.Spawn;
import nl.NG.Jetfightergame.Engine.GameTimer;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.util.Collection;

/**
 * entities generated by other entities can leave them with objects implementing this interface.
 * These classes should make sure that all required actions are taken to implement the new entity into the game
 * @author Geert van Ieperen
 * created on 5-2-2018.
 */
public interface SpawnReceiver {

    /**
     * adds an moving entity to the game's collision detection and rendering
     * @param spawn the new entity
     */
    void addSpawn(Spawn spawn);

    /**
     * add multiple new entities
     * @see #addSpawn(Spawn)
     */
    default void addSpawns(Collection<Spawn> spawn) {
        for (Spawn entity : spawn) {
            addSpawn(entity);
        }
    }

    GameTimer getTimer();

    /**
     * adds an explosion of given magnitude to the game's rendering
     * @see nl.NG.Jetfightergame.Rendering.Particles.Particles#explosion(PosVector, DirVector, Color4f, Color4f, float, int)
     */
    void addExplosion(PosVector position, DirVector direction, Color4f color1, Color4f color2, float power, int density);
}
