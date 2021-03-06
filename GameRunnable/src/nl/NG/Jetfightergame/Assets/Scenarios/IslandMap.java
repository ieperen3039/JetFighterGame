package nl.NG.Jetfightergame.Assets.Scenarios;

import nl.NG.Jetfightergame.Assets.Shapes.GeneralShapes;
import nl.NG.Jetfightergame.Engine.GameTimer;
import nl.NG.Jetfightergame.EntityGeneral.EntityState;
import nl.NG.Jetfightergame.EntityGeneral.Factory.EntityFactory;
import nl.NG.Jetfightergame.EntityGeneral.MovingEntity;
import nl.NG.Jetfightergame.EntityGeneral.Powerups.PowerupEntity;
import nl.NG.Jetfightergame.EntityGeneral.StaticEntity;
import nl.NG.Jetfightergame.EntityGeneral.Touchable;
import nl.NG.Jetfightergame.GameState.GameState;
import nl.NG.Jetfightergame.GameState.RacePathDescription;
import nl.NG.Jetfightergame.GameState.RaceProgress;
import nl.NG.Jetfightergame.GameState.RaceProgress.Checkpoint;
import nl.NG.Jetfightergame.Rendering.Material;
import nl.NG.Jetfightergame.ShapeCreation.Shape;
import nl.NG.Jetfightergame.Tools.DataStructures.Pair;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Resource;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Geert van Ieperen. Created on 5-7-2018.
 */
public class IslandMap extends GameState {
    private static final int FOG_DIST = 750;
    private static final int WORLD_BOUND = 1500;
    private static final int TILE_SIZE = 100;
    public static final int START_LINE_DIST = 100;
    private PosVector nextSpawnPosition = new PosVector();
    private DirVector nextSpawnOffset = new DirVector();

    private static RacePathDescription racePath = new RacePathDescription(Resource.GLITCHMAP);

    @Override
    protected Collection<Touchable> createWorld(RaceProgress raceProgress, GameTimer timer) {
        List<Touchable> entities = new ArrayList<>();

        // land
        for (Shape s : GeneralShapes.ISLAND1) {
            entities.add(new StaticEntity(s, Material.GLASS, Color4f.BLACK));
        }
        // sea
        int nOfTiles = 2 * WORLD_BOUND / TILE_SIZE;
        for (int x = 0; x < nOfTiles; x++) {
            for (int y = 0; y < nOfTiles; y++) {

                int xCoord = x * TILE_SIZE - WORLD_BOUND;
                int yCoord = y * TILE_SIZE - WORLD_BOUND;
                if ((xCoord < -550 || xCoord > 710) || (yCoord < -730 || yCoord > 510)) {
                    entities.add(getSeaTile(new PosVector(xCoord, yCoord, -50)));
                }

            }
        }

        Pair<PosVector, DirVector> start = racePath.getFirstCheckpoint();
        nextSpawnPosition.set(start.left);
        DirVector offset = DirVector.yVector();
        DirVector backwait = new DirVector(start.right).scale(-START_LINE_DIST);
        offset.cross(start.right).normalize(30).add(backwait);
        nextSpawnOffset = offset;

        for (Checkpoint ch : racePath.getCheckpoints(raceProgress, Color4f.BLUE)) {
            entities.add(ch);
        }

        return entities;
    }

    private StaticEntity getSeaTile(PosVector pos) {
        return new StaticEntity(GeneralShapes.SEA, Material.GLASS, Color4f.BLUE, pos, TILE_SIZE) {
            @Override
            public float getRange() {
                return 0;
            }
        };
    }

    private StaticEntity borderPanel(PosVector offSet, Quaternionf rotation) {
        DirVector normal = DirVector.zVector();
        normal.rotate(rotation);
        Logger.DEBUG.print(offSet, normal);
        return new StaticEntity(GeneralShapes.QUAD, Material.GLOWING, fogColor(), offSet, WORLD_BOUND, rotation);
    }

    @Override
    protected Collection<EntityFactory> getInitialEntities() {
        List<EntityFactory> entities = new ArrayList<>();

        // powerups
        for (PowerupEntity.Factory pop : racePath.getPowerups()) {
            entities.add(pop);
        }

        return entities;

    }

    @Override
    public EntityState getNewSpawnPosition() {
        PosVector pos = new PosVector(nextSpawnPosition);
        nextSpawnOffset.rotateAxis(1.396f, -1, 0, 0); // allows up to 9 players
        pos.add(nextSpawnOffset);

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        return new EntityState(pos, rotation, DirVector.zeroVector());
    }

    @Override
    public Color4f fogColor() {
        return new Color4f(0.7f, 0.7f, 0.8f, 1f / FOG_DIST);
    }

    @Override
    public DirVector entityNetforce(MovingEntity entity) {
        return DirVector.zeroVector();
    }
}
