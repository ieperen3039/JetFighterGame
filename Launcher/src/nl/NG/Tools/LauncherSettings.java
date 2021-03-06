package nl.NG.Tools;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 26-4-2018.
 */
@SuppressWarnings("Duplicates")
public final class LauncherSettings {
    /** general settings */
    public static final String GAME_NAME = "Jet Fighter Game"; // laaaame
    public static boolean DEBUG = false;

    /** server settings */
    public static int SERVER_PORT = 3039;
    public static int TARGET_TPS = 30;
    public static int CONNECTION_SEND_FREQUENCY = TARGET_TPS;
    public static boolean MAKE_REPLAY = true;
    public static int NOF_OPPONENTS = 7;
    public static float SPEED_FACTOR = 1.0f;

    /** visual settings */
    public static int TARGET_FPS = 60;
    // rendering is delayed by RENDER_DELAY seconds to smooth out rendering and prevent extrapolation
    public static float RENDER_DELAY = 1f / TARGET_TPS;
    public static float FOV = (float) Math.toRadians(60);

    /** particle settings */
    public static float PARTICLE_MODIFIER = 1f;

    /** miscellaneous */
    public static String JET_TYPE = "JET_SPITZ";
    public static boolean ALLOW_FLYING_TEXT = true;
    public static Color JET_COLOR = Color.WHITE;

    public static File writeSettingsToFile(File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        JsonGenerator gen = new JsonFactory().createGenerator(out);
        gen.useDefaultPrettyPrinter();

        gen.writeStartObject();
        {
            // settings
            /* DO NOT CHANGE STRING NAMES (for backward compatibility) */
            gen.writeNumberField("TARGET_FPS", TARGET_FPS);
            gen.writeNumberField("RENDER_DELAY", RENDER_DELAY);
            gen.writeNumberField("SERVER_PORT", SERVER_PORT);
            gen.writeNumberField("SPEED_FACTOR", SPEED_FACTOR);
            gen.writeBooleanField("MAKE_REPLAY", MAKE_REPLAY);
            gen.writeNumberField("TARGET_TPS", TARGET_TPS);
            gen.writeNumberField("PARTICLE_MODIFIER", PARTICLE_MODIFIER);
            gen.writeNumberField("CONNECTION_SEND_FREQUENCY", CONNECTION_SEND_FREQUENCY);
            gen.writeStringField("JET_TYPE", JET_TYPE);
            gen.writeBooleanField("LOGGER_PRINT_CALLSITES", false);
            gen.writeNumberField("NUMBER_OF_NPCS", NOF_OPPONENTS);
            gen.writeArrayFieldStart("JET_COLOR");
            {
                gen.writeNumber(JET_COLOR.getRed());
                gen.writeNumber(JET_COLOR.getGreen());
                gen.writeNumber(JET_COLOR.getBlue());
            }
            gen.writeEndArray();

            // keybindings
            for (KeyBinding binding : KeyBinding.values()) {
                gen.writeArrayFieldStart(binding.name());
                {
                    gen.writeNumber(binding.getKey());
                    gen.writeBoolean(binding.isMouseAxis());
                    gen.writeNumber(binding.getXBox());
                    gen.writeBoolean(binding.isXBoxAxis());
                }
                gen.writeEndArray();
            }
        }
        gen.writeEndObject();

        gen.close();
        out.close();
        return file;
    }

    public static void readSettingsFromFile(File file) throws IOException {
        if (!file.exists()) return;

        JsonNode src = new ObjectMapper().readTree(file);
        KeyBinding[] keyBindings = KeyBinding.values();

        Iterator<Map.Entry<String, JsonNode>> fields = src.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode result = entry.getValue();

            switch (fieldName) {
                /* DO NOT CHANGE STRING NAMES (for backward compatibility) */
                case "TARGET_FPS":
                    TARGET_FPS = result.intValue();
                    break;
                case "RENDER_DELAY":
                    RENDER_DELAY = result.floatValue();
                    break;
                case "SERVER_PORT":
                    SERVER_PORT = result.intValue();
                    break;
                case "SPEED_FACTOR":
                    SPEED_FACTOR = result.floatValue();
                    break;
                case "MAKE_REPLAY":
                    MAKE_REPLAY = result.booleanValue();
                    break;
                case "TARGET_TPS":
                    TARGET_TPS = result.intValue();
                    break;
                case "PARTICLE_MODIFIER":
                    PARTICLE_MODIFIER = result.floatValue();
                    break;
                case "CONNECTION_SEND_FREQUENCY":
                    CONNECTION_SEND_FREQUENCY = result.intValue();
                    break;
                case "JET_TYPE":
                    JET_TYPE = result.textValue();
                    break;
                case "LOGGER_PRINT_CALLSITES": // always false
                    break;
                case "NUMBER_OF_NPCS":
                    NOF_OPPONENTS = result.intValue();
                    break;
                case "JET_COLOR":
                    assert result.isArray();
                    Iterator<JsonNode> values = result.elements();
                    assert values.hasNext();
                    JET_COLOR = new Color(
                            values.next().intValue(),
                            values.next().intValue(),
                            values.next().intValue()
                    );
                    assert !values.hasNext();
                    break;

                default: // maybe not the fastest, but no exception is thrown when the string is not found
                    for (KeyBinding target : keyBindings) {
                        if (fieldName.equals(target.toString())) {
                            assert result.isArray();
                            Iterator<JsonNode> node = result.elements();
                            assert node.hasNext();
                            target.installNew(node.next().intValue(), node.next().booleanValue(), false);
                            target.installNew(node.next().intValue(), node.next().booleanValue(), true);
                            assert !node.hasNext();

                            break;
                        }
                    }
            }
        }
    }
}
