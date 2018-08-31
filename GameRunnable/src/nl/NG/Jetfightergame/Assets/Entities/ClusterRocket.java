package nl.NG.Jetfightergame.Assets.Entities;

import nl.NG.Jetfightergame.ArtificalIntelligence.RocketAI;
import nl.NG.Jetfightergame.Assets.Entities.FighterJets.AbstractJet;
import nl.NG.Jetfightergame.Assets.Shapes.GeneralShapes;
import nl.NG.Jetfightergame.Engine.GameTimer;
import nl.NG.Jetfightergame.EntityGeneral.EntityState;
import nl.NG.Jetfightergame.EntityGeneral.Factory.EntityClass;
import nl.NG.Jetfightergame.EntityGeneral.Factory.EntityFactory;
import nl.NG.Jetfightergame.EntityGeneral.MovingEntity;
import nl.NG.Jetfightergame.EntityGeneral.Touchable;
import nl.NG.Jetfightergame.GameState.SpawnReceiver;
import nl.NG.Jetfightergame.Rendering.Material;
import nl.NG.Jetfightergame.Rendering.MatrixStack.GL2;
import nl.NG.Jetfightergame.Rendering.MatrixStack.MatrixStack;
import nl.NG.Jetfightergame.Rendering.Particles.BoosterLine;
import nl.NG.Jetfightergame.Settings.ClientSettings;
import nl.NG.Jetfightergame.ShapeCreation.Shape;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import org.joml.Quaternionf;

import java.util.function.Consumer;

import static nl.NG.Jetfightergame.Settings.ClientSettings.PARTICLE_MODIFIER;
import static nl.NG.Jetfightergame.Settings.ClientSettings.THRUST_PARTICLE_LINGER_TIME;

/**
 * AKA the BUK rocket
 * @author Geert van Ieperen. Created on 24-7-2018.
 */
public class ClusterRocket extends AbstractProjectile {

    public static final int NOF_PELLETS_LAUNCHED = 25;
    public static final float EXPLOSION_POWER = 12f;
    public static final int EXPLOSION_DENSITY = 200;
    public static final float THRUST_POWER = 500f;
    public static final float TIME_TO_LIVE = 30f;
    public static final float SHOOT_ACCURACY = 0.3f;
    public static final float TURN_ACC = 0.7f;
    public static final float AIR_RESIST = 0.02f;
    public static final float MASS = 5f;
    public static final float THRUST_PARTICLE_PER_SECOND = 200;
    private boolean hasExploded = false;
    private BoosterLine nuzzle;

    /**
     * @param id              unique identifier for this entity
     * @param initialPosition position of spawning (of the origin) in world coordinates
     * @param initialRotation the initial rotation of spawning
     * @param initialVelocity the initial velocity, that is the vector of movement per second in world-space
     * @param entityDeposit   particles are passed here
     * @param gameTimer       the local game timer
     * @param sourceEntity    the entity that launched this projectile
     * @param tgt             the target of this bomb
     */
    private ClusterRocket(
            int id, PosVector initialPosition, Quaternionf initialRotation, DirVector initialVelocity,
            SpawnReceiver entityDeposit, GameTimer gameTimer, AbstractJet sourceEntity, MovingEntity tgt
    ) {
        super(
                id, initialPosition, initialRotation, initialVelocity, MASS,
                AIR_RESIST, TIME_TO_LIVE, TURN_ACC, 0f, THRUST_POWER,
                0.9f, entityDeposit, gameTimer, sourceEntity
        );

        if (tgt != null) {
            this.target = tgt;

            RocketAI con = new RocketAI(this, tgt, 120f, 30f) {
                @Override
                public boolean primaryFire() {
                    float dot = getVelocity().normalize().dot(vecToTarget);
                    return super.primaryFire() && dot > (1 - SHOOT_ACCURACY);
                }
            };
            con.update();
            setController(con);
        }

        nuzzle = new BoosterLine(
                PosVector.zeroVector(), PosVector.zeroVector(), DirVector.zeroVector(),
                THRUST_PARTICLE_PER_SECOND * PARTICLE_MODIFIER, THRUST_PARTICLE_LINGER_TIME,
                Color4f.ORANGE, Color4f.RED, ClientSettings.THRUST_PARTICLE_SIZE,
                gameTimer);
    }

    @Override
    public void create(MatrixStack ms, Consumer<Shape> action) {
        ms.pushMatrix();
        {
            ms.translate(-0.5f, 0, 0);
            ms.rotate((float) Math.toRadians(90), 0f, 1f, 0f);
            action.accept(GeneralShapes.ARROW);
        }
        ms.popMatrix();
    }

    @Override
    public void preDraw(GL2 gl) {
        gl.setMaterial(Material.ROUGH);

        DirVector back = new DirVector();
        forward.scale(controller.throttle() * -THRUST_POWER, back).add(forward);

        toLocalSpace(gl, () -> entityDeposit.add(
                nuzzle.update(gl, DirVector.zeroVector(), 0, THRUST_PARTICLE_PER_SECOND)
        ));
    }

    @Override
    protected void updateShape(float deltaTime) {
        if (!hasExploded && controller.primaryFire()) {
            timeToLive = 0;
            entityDeposit.add(AbstractProjectile.createCloud(
                    getPosition(), getVelocity().scale(2f), NOF_PELLETS_LAUNCHED, EXPLOSION_POWER,
                    SimpleBullet.Factory::new
            ));
            hasExploded = true;
        }
        timeToLive -= deltaTime;
    }

    @Override
    public boolean isOverdue() {
        return hasExploded || super.isOverdue();
    }

    @Override
    public EntityFactory getFactory() {
        return new Factory(this);
    }

    @Override
    public float getRange() {
        return 0;
    }

    @Override
    protected void collideWithOther(Touchable other) {
        other.impact(1.5f, IMPACT_POWER);
    }

    public static class Factory extends RocketFactory {
        public Factory() {
        }

        public Factory(EntityState state, MovingEntity source, MovingEntity target) {
            super(EntityClass.CLUSTER_ROCKET, state, 0, source, target);
        }

        public Factory(ClusterRocket rocket) {
            super(EntityClass.CLUSTER_ROCKET, rocket);
        }

        @Override
        public MovingEntity construct(SpawnReceiver game, AbstractJet src, MovingEntity tgt) {
            return new ClusterRocket(id, position, rotation, velocity, game, game.getTimer(), src, tgt);
        }
    }
}
