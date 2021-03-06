package nl.NG.Jetfightergame.Assets.Entities;

import nl.NG.Jetfightergame.Assets.Entities.FighterJets.AbstractJet;
import nl.NG.Jetfightergame.Assets.Shapes.GeneralShapes;
import nl.NG.Jetfightergame.Engine.GameTimer;
import nl.NG.Jetfightergame.EntityGeneral.EntityMapping;
import nl.NG.Jetfightergame.EntityGeneral.EntityState;
import nl.NG.Jetfightergame.EntityGeneral.Factory.EntityClass;
import nl.NG.Jetfightergame.EntityGeneral.Factory.EntityFactory;
import nl.NG.Jetfightergame.EntityGeneral.MovingEntity;
import nl.NG.Jetfightergame.EntityGeneral.Touchable;
import nl.NG.Jetfightergame.GameState.SpawnReceiver;
import nl.NG.Jetfightergame.Rendering.Material;
import nl.NG.Jetfightergame.Rendering.MatrixStack.GL2;
import nl.NG.Jetfightergame.Rendering.MatrixStack.MatrixStack;
import nl.NG.Jetfightergame.Rendering.Particles.ParticleCloud;
import nl.NG.Jetfightergame.Rendering.Particles.Particles;
import nl.NG.Jetfightergame.Settings.ClientSettings;
import nl.NG.Jetfightergame.ShapeCreation.Shape;
import nl.NG.Jetfightergame.Sound.AudioSource;
import nl.NG.Jetfightergame.Sound.MovingAudioSource;
import nl.NG.Jetfightergame.Sound.Sounds;
import nl.NG.Jetfightergame.Tools.Toolbox;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import org.joml.Quaternionf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author Geert van Ieperen. Created on 12-8-2018.
 */
public class DeathIcosahedron extends AbstractProjectile {
    private static final float SCALE = 3f;
    private static final float FIRE_SPEED = 30f;
    private static final float SEEKER_LAUNCH_SPEED = 20f;
    private static final Color4f COLOR = new Color4f(0.8f, 0.3f, 0);
    private static final float SEEKER_COOLDOWN_INCREASE = 0.03f;

    private final int NOF_PARTICLES = (int) (1000 * ClientSettings.PARTICLE_MODIFIER);
    private final float SPARK_COOLDOWN = 0.005f / ClientSettings.PARTICLE_MODIFIER;
    private final EntityMapping entities;

    private float seekerCooldown = 0.2f;
    private float sparkTimeRemain;
    private float seekerTimeRemain;

    private DeathIcosahedron(
            int id, PosVector position, DirVector velocity,
            SpawnReceiver particleDeposit, GameTimer gameTimer, AbstractJet sourceEntity, EntityMapping entities
    ) {
        super(
                id, position, Toolbox.xTo(velocity), velocity, 1f,
                0f, 60f, 0f, 0f, 0f, 0f,
                particleDeposit, gameTimer, sourceEntity
        );
        this.entities = entities;
        sparkTimeRemain = 0;
        seekerTimeRemain = 1f;
        if (!particleDeposit.isHeadless()) {
            particleDeposit.addExplosion(position, velocity, COLOR, Color4f.RED, 20f, 30, 2f, 2f);
            particleDeposit.add(new MovingAudioSource(Sounds.jet_fire, this, 0.2f, 0.5f, true));
        }
    }

    @Override
    protected void collideWithOther(Touchable other) {
        other.impact(1.5f, 10);
    }

    @Override
    public EntityFactory getFactory() {
        return new Factory(this);
    }

    @Override
    public void preDraw(GL2 gl) {
        gl.setMaterial(Material.GLOWING, COLOR);

        sparkTimeRemain -= gameTimer.getRenderTime().difference();
        if (sparkTimeRemain >= 0) return;

        ParticleCloud cloud = new ParticleCloud();
        do {
            PosVector pos = positionInterpolator.getInterpolated(renderTime() - sparkTimeRemain).toPosVector();
            DirVector move = positionInterpolator.getDerivative();
            move.add(DirVector.randomOrb().reducedTo(SCALE * 1.5f, new DirVector()));

            cloud.addParticle(pos, move, 0, 1, Color4f.RED, 2f);

            sparkTimeRemain += SPARK_COOLDOWN;
        } while (sparkTimeRemain < 0);

        entityDeposit.add(cloud);
    }

    @Override
    public ParticleCloud explode() {
        timeToLive = 0;
        PosVector pos = getPosition();
        entityDeposit.add(new AudioSource(Sounds.explosion2, pos, 0.5f, 2f));
        return Particles.explosion(
                position, DirVector.zeroVector(), Color4f.YELLOW, Color4f.RED,
                50, NOF_PARTICLES, 2, 6f
        );
    }

    @Override
    protected void updateShape(float deltaTime) {
        super.updateShape(deltaTime);

        seekerTimeRemain -= deltaTime;
        if (seekerTimeRemain >= 0) return;

        do {
            float timeFraction = seekerTimeRemain / deltaTime;
            DirVector move = getVelocity();
            DirVector randDirection = DirVector.randomOrb();
            move.add(randDirection.mul(SEEKER_LAUNCH_SPEED));

            EntityState state = new EntityState(position, randDirection, move);

            target = sourceJet.getTarget(randDirection, getPosition(), entities);
            entityDeposit.add(new Seeker.Factory(state, timeFraction, sourceJet, target));

            seekerTimeRemain += seekerCooldown;
            seekerCooldown += SEEKER_COOLDOWN_INCREASE;
        } while (seekerTimeRemain < 0);
    }

    @Override
    public void create(MatrixStack ms, Consumer<Shape> action) {
        ms.pushMatrix();
        {
            ms.rotate(renderTime(), 0, -1, 0);
            ms.scale(SCALE);
            action.accept(GeneralShapes.ICOSAHEDRON);
        }
        ms.popMatrix();
    }

    public static class Factory extends EntityFactory {
        private int sourceID;

        public Factory() {
        }

        public Factory(DeathIcosahedron e) {
            super(EntityClass.DEATHICOSAHEDRON, e);
            sourceID = e.sourceJet.idNumber();
        }

        public Factory(AbstractJet jet) {
            super(EntityClass.DEATHICOSAHEDRON, jet.getPosition(), new Quaternionf(), getVelocity(jet));
            sourceID = jet.idNumber();
        }

        public static DirVector getVelocity(AbstractJet jet) {
            DirVector vel = jet.getVelocity();
            DirVector launch = jet.getForward().scale(FIRE_SPEED);
            vel.add(launch);
            return vel;
        }

        @Override
        public MovingEntity construct(SpawnReceiver game, EntityMapping entities) {
            AbstractJet entity = (AbstractJet) entities.getEntity(sourceID);
            return new DeathIcosahedron(id, position, velocity, game, game.getTimer(), entity, entities);
        }

        @Override
        protected void writeInternal(DataOutput out) throws IOException {
            super.writeInternal(out);
            out.writeInt(sourceID);
        }

        @Override
        protected void readInternal(DataInput in) throws IOException {
            super.readInternal(in);
            sourceID = in.readInt();
        }
    }
}
