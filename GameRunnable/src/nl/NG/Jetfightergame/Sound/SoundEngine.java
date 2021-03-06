package nl.NG.Jetfightergame.Sound;

import nl.NG.Jetfightergame.Rendering.GLException;
import nl.NG.Jetfightergame.Settings.ClientSettings;
import nl.NG.Jetfightergame.Settings.ServerSettings;
import nl.NG.Jetfightergame.Tools.Directory;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Toolbox;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static nl.NG.Jetfightergame.Tools.Toolbox.checkALError;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.EXTEfx.ALC_MAX_AUXILIARY_SENDS;

/**
 * @author Geert van Ieperen
 * created on 5-2-2018.
 * This software is using the J-Ogg library available from http://www.j-ogg.de and copyrighted by Tor-Einar Jarnbjo.
 */
public class SoundEngine {

    // default device
    private final long device;

    /**
     * set up openAL environment
     */
    public SoundEngine() {
        // Create a handle for the device capabilities
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL) throw new RuntimeException("Could not find default device");

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

        IntBuffer contextAttribList = BufferUtils.createIntBuffer(16);
        // default attributes
        int[] attributes = new int[]{
                ALC_REFRESH,                60,
                ALC_SYNC,                   ALC_FALSE,
                ALC_MAX_AUXILIARY_SENDS,    2,
                0
        };

        contextAttribList.put(attributes);

        // create the context with the provided attributes
        long newContext = ALC10.alcCreateContext(device, contextAttribList);

        if(!ALC10.alcMakeContextCurrent(newContext)) {
            throw new GLException("Failed to make OpenAL context current");
        }

        final ALCapabilities alCaps = AL.createCapabilities(deviceCaps);
        checkALError();

        if (ServerSettings.DEBUG) {
            if (!alCaps.OpenAL10) System.err.println("Warning: Sound system does not support Open AL 10");
            if (!deviceCaps.OpenALC10) System.err.println("Warning: Sound system does not support Open ALC 10");
//            if (!alCaps.) System.err.println("Warning: Sound system does not support ...");
        }

//        AL10.alDistanceModel(AL_LINEAR_DISTANCE);
        AL10.alListenerf(AL10.AL_GAIN, ClientSettings.MASTER_GAIN);
        AL10.alDopplerFactor(0.2f);
        setListenerPosition(PosVector.zeroVector(), DirVector.zeroVector());
        setListenerOrientation(DirVector.xVector(), DirVector.yVector());

        checkALError();
    }

    /**
     * set the speed of sound to the specified value
     * @param value speed in m/s
     */
    public void speedOfSound(float value){
        AL11.alSpeedOfSound(value);
    }

    /**
     * set the position and velocity of the listener
     * @param pos position
     * @param vel velocity, does not influence position
     */
    public void setListenerPosition(PosVector pos, DirVector vel) {
        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alListener3f(AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
    }

    public void setListenerOrientation(DirVector forward, DirVector up) {
        float[] asArray = {forward.x, forward.y, forward.z, up.x, up.y, up.z};
        AL10.alListenerfv(AL10.AL_ORIENTATION, asArray);
    }

    public void closeDevices() {
        boolean success = ALC10.alcCloseDevice(device);
        if (!success) Logger.WARN.print("Could not close device");
    }

    public static void main(String[] args) {
        SoundEngine soundEngine = new SoundEngine();
        checkALError();

        try {
            AudioSource src;
            src = play("powerfield.ogg", (float) 2);

            Logger.DEBUG.print("Playing sound... Do you hear it?");
            checkALError();
            while (!src.isOverdue()) {
                src.update();
                Toolbox.waitFor(100);
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            checkALError();
            soundEngine.closeDevices();
        }
        System.exit(404);
    }

    private static AudioSource play(String file, float pitch) {
        AudioFile audioData = new AudioFile(Directory.soundEffects,
                file
        );
        audioData.load();
        AudioSource src = new AudioSource(audioData, 1.0f, true);
        src.setPitch(pitch);
        src.play();
        return src;
    }
}
