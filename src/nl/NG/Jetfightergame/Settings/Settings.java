package nl.NG.Jetfightergame.Settings;

import java.util.Random;

/**
 * a class that harbours the variables that may or may not be changed by the player
 * @see MenuStyleSettings
 * @see KeyBindings
 * @author Geert van Ieperen
 *         created on 2-11-2017.
 */
@SuppressWarnings("ConstantConditions")
public final class Settings {

    /** general settings */
    public static final String GAME_NAME = "Jet Fighter Game"; // laaaame
    public static final short TARGET_TPS = 10;
    public static final short TARGET_FPS = 60;
    public static boolean FIXED_DELTA_TIME = false;

    /** engine settings */
    // rendering is delayed by RENDER_DELAY seconds to smoothen rendering and prevent extrapolation of the gamestate
    public static final float RENDER_DELAY = 5f/TARGET_TPS;
    public static final int INTERPOLATION_QUEUE_SIZE = 10;
    public static float HIGHLIGHT_LINE_WIDTH = 1f;
    public static boolean SAVE_PLAYBACK = false;

    /** collision settings */
    public static final short COLLISION_RESPONSE_LEVEL = 0;
    public static final boolean DO_COLLISION_RESPONSE = COLLISION_RESPONSE_LEVEL >= 0;
    public static final int MAX_COLLISION_ITERATIONS = 250/TARGET_TPS;
    // force factor of how strong two colliding planes bump off
    public static final float BUMP_POWER = 5;

    /** miscellaneous */
    // universal random to be used everywhere
    public static final Random random = new Random();
    public static final boolean DEBUG = true;
    public static final boolean CULL_FACES = true;
    public static final boolean SHOW_LIGHT_POSITIONS = true;
    public static final int PARTICLE_SPLITS = 0;

    /** particles */
    public static final int FIRE_PARTICLE_DENSITY = 1000; // actual particle count of plane explosion
    public static final int SPARK_PARTICLE_DENSITY = 10;
    public static final float FIRE_PARTICLE_SIZE = 0.3f;

    /** sound */
    public static final float SOUND_MASTER_GAIN = 0;
    public static final float MAX_VOLUME = 6f;
    public static final float MIN_VOLUME = -20f;

    /** controller settings; these modifiers are also used to inverse direction */
    public static float PITCH_MODIFIER = -0.05f;
    public static float ROLL_MODIFIER = 0.05f;

    /** visual settings */
    public static float FOV = (float) Math.toRadians(60);
    // absolute size of frustum
    public static float Z_NEAR = 0.05f;
    public static float Z_FAR = 2000.0f;
    public static Boolean INVERT_CAMERA_ROTATION = false;
    public static boolean V_SYNC = true;
    public static int ANTIALIAS = 3;
    public static final int MAX_POINT_LIGHTS = 10;
    public static boolean SPECTATOR_MODE = false;

    public static int entityIDNumber = 0;
}
