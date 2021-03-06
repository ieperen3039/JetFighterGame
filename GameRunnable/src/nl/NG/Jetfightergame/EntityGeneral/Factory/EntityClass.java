package nl.NG.Jetfightergame.EntityGeneral.Factory;

import nl.NG.Jetfightergame.Assets.Entities.*;
import nl.NG.Jetfightergame.Assets.Entities.FighterJets.JetBasic;
import nl.NG.Jetfightergame.Assets.Entities.FighterJets.JetNightHawk;
import nl.NG.Jetfightergame.Assets.Entities.FighterJets.JetSpitsy;
import nl.NG.Jetfightergame.Camera.CameraFocusMovable;
import nl.NG.Jetfightergame.EntityGeneral.InvisibleEntity;
import nl.NG.Jetfightergame.EntityGeneral.Powerups.PowerupEntity;

import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 10-5-2018.
 * @see EntityFactory for how to create entities
 */
public enum EntityClass {
    SPECTATOR_CAMERA(CameraFocusMovable.Factory::new),
    JET_BASIC(JetBasic.Factory::new),
    JET_SPITZ(JetSpitsy.Factory::new),
    JET_NIGHT_HAWK(JetNightHawk.Factory::new),

    FALLING_CUBE(FallingCube.Factory::new),
    SIMPLE_BULLET(SimpleBullet.Factory::new),
    INVISIBLE_ENTITY(InvisibleEntity.Factory::new),
    POWERUP(PowerupEntity.Factory::new),

    SIMPLE_ROCKET(SimpleRocket.Factory::new),
    SEEKER(Seeker.Factory::new),
    DEATHICOSAHEDRON(DeathIcosahedron.Factory::new),
    BLACK_HOLE(BlackHole.Factory::new),
    CLUSTER_ROCKET(ClusterRocket.Factory::new),
    ONEHIT_SHIELD(OneHitShield::newFactory),
    REFLECTOR_SHIELD(ReflectorShield::newFactory),
    GRAPPLING_HOOK(GrapplingHook.Factory::new);

    private static final EntityClass[] VALUES = values();
    private static final EntityClass[] jets = new EntityClass[]{JET_BASIC, JET_SPITZ, JET_NIGHT_HAWK};

    private final Supplier<EntityFactory> constructor;

    EntityClass(Supplier<EntityFactory> constructor) {
        this.constructor = constructor;
    }

    /**
     * @param id a number n corresponing to an enum ordinal
     * @return the enum e such that {@code e.ordinal() == n}
     * @throws IllegalArgumentException if the id does not correspond to a valid message
     */
    public static EntityClass get(int id) {
        if (id >= VALUES.length) throw new IllegalArgumentException("Invalid entityclass identifier " + id);
        else return VALUES[id];
    }

    public static String asString(int id) {
        return id < VALUES.length ? get(id).toString() : id + " (Invalid entity id)";
    }

    public static EntityClass[] getJets() {
        return jets.clone();
    }

    EntityFactory getFactory() {
        EntityFactory f = constructor.get();
        f.type = this;
        return f;
    }
}
