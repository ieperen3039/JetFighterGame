package nl.NG.Jetfightergame.ScreenOverlay.Userinterface;

import nl.NG.Jetfightergame.ScreenOverlay.JFGFonts;
import nl.NG.Jetfightergame.ScreenOverlay.ScreenOverlay;
import nl.NG.Jetfightergame.Settings.MenuStyleSettings;

import java.util.function.Consumer;

import static nl.NG.Jetfightergame.Settings.MenuStyleSettings.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

/**
 * @author Jorren
 */
public class MenuSlider extends MenuClickable {

    private String text;
    private float value;
    private Consumer<Float> handler;

    public MenuSlider(String text, int width, int height, Consumer<Float> handler) {
        super(width, height);
        this.text = text;
        this.value = 1f;
        this.handler = handler;
    }

    public MenuSlider(String text, Consumer<Float> handler) {
        this(text, MenuStyleSettings.BUTTON_WIDTH, MenuStyleSettings.BUTTON_HEIGHT, handler);
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.roundedRectangle(x, y, width, height, INDENT);
        hud.roundedRectangle(x + MENU_STROKE_WIDTH/2, y + MENU_STROKE_WIDTH/2,
                (int) ((width - MENU_STROKE_WIDTH) * Math.max(value, 0.05f)),height - MENU_STROKE_WIDTH, INDENT,
                COLOR_DARK, MENU_STROKE_COLOR, MENU_STROKE_WIDTH);
        hud.text(x + width / 2, y + MenuStyleSettings.TEXT_SIZE_LARGE + 10, MenuStyleSettings.TEXT_SIZE_LARGE, JFGFonts.ORBITRON_MEDIUM, NVG_ALIGN_CENTER,
                MenuStyleSettings.TEXT_COLOR, String.format("%1$s: %2$d%%", text, (int) ((value * 100) >= 1 ? value * 100 : 1)));
    }

    @Override
    public void onClick(int x, int y) {
        value = Math.max((float) (x - this.x) / (float) this.width, 0f);
        try {
            handler.accept(value);
        } catch (Exception ex){
            throw new RuntimeException("Error occurred in slider \"" + text + "\", with value " + value, ex);
        }
    }
}
