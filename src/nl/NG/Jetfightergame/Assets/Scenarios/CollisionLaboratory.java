package nl.NG.Jetfightergame.Assets.Scenarios;

import nl.NG.Jetfightergame.AbstractEntities.MovingEntity;
import nl.NG.Jetfightergame.AbstractEntities.StaticObject;
import nl.NG.Jetfightergame.AbstractEntities.Touchable;
import nl.NG.Jetfightergame.Assets.GeneralEntities.FallingCube;
import nl.NG.Jetfightergame.Assets.Shapes.GeneralShapes;
import nl.NG.Jetfightergame.Engine.GameState.GameState;
import nl.NG.Jetfightergame.Engine.GameTimer;
import nl.NG.Jetfightergame.Player;
import nl.NG.Jetfightergame.Rendering.Material;
import nl.NG.Jetfightergame.Settings;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import nl.NG.Jetfightergame.Tools.Vectors.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Geert van Ieperen
 * created on 26-12-2017.
 */
public class CollisionLaboratory extends GameState {

    private static final float CUBESIZE = 1f;
    private static final float CUBEMASS = 10f;
    private static final int LAB_SIZE = 20;
    private static final int NR_OF_CUBES = 4*4*4;

    private final int labSize;
    private final int nrOfCubes;
    private final float speeds;

    public CollisionLaboratory(Player player, GameTimer time) {
        this(LAB_SIZE, NR_OF_CUBES, time, player);
    }

    public CollisionLaboratory(int labSize, int nrOfCubes, GameTimer time, Player player) {
        super(player, time);
        this.labSize = labSize;
        this.nrOfCubes = nrOfCubes;
        this.speeds = labSize / 3f;

        Settings.SPECTATOR_MODE = true;
        time.setGameTimeMultiplier(Settings.TARGET_FPS/60f);
    }

    @Override
    protected Collection<Touchable> createWorld() {
//        lights.add(new Pair<>(PosVector.zeroVector(), Color4f.WHITE.darken(0.3f)));
        return Collections.singletonList(new StaticObject(GeneralShapes.makeInverseCube(0), Material.ROUGH, Color4f.ORANGE, labSize));
    }

    @Override
    protected Collection<MovingEntity> setEntities() {
        int gridSize = (int) Math.ceil(Math.cbrt(nrOfCubes));
        int interSpace = (2 * labSize) / (gridSize + 1);

        int remainingCubes = nrOfCubes;

        Collection<MovingEntity> cubes = new ArrayList<>(nrOfCubes);

        cubing:
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                for (int z = 0; z < gridSize; z++) {
                    cubes.add(makeCube(labSize, speeds, interSpace, x, y, z));

                    if (--remainingCubes <= 0) break cubing;
                }
            }
        }

        return cubes;
    }

    private FallingCube makeCube(int labSize, float speeds, int interSpace, int x, int y, int z) {
        final PosVector pos = new PosVector(
                -labSize + ((x + 1) * interSpace), -labSize + ((y + 1) * interSpace), -labSize + ((z + 1) * interSpace)
        );

        DirVector random = Vector.random();
        random.scale(speeds, random);

        FallingCube cube = new FallingCube(
                Material.SILVER, CUBEMASS, CUBESIZE,
                pos.scale(0.8f, pos).toPosVector(),
                random, new Quaternionf(), getTimer(), this
        );
        cube.addRandomRotation(0.4f);

        return cube;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public DirVector entityNetforce(MovingEntity entity) {

        final int version = 3;

        switch (version){
            case 1:
                // toward middle
                final DirVector middle = DirVector.random();//.scale(labSize, new DirVector());
                entity.getPosition().to(middle, middle);
                return middle;
            case 2:
                // random
                return DirVector.random().scale(labSize, new DirVector());
            case 3:
                // gravity
                final DirVector g = new DirVector(0, 0, -9.81f);
                return g.scale(entity.getMass(), g);
        }
        return DirVector.zeroVector();
    }

    @Override
    public Color4f fogColor(){
        return new Color4f(0.8f, 0.8f, 0.8f, 0f);
    }
}
