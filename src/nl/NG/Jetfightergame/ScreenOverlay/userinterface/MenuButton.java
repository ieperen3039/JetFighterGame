package nl.NG.Jetfightergame.ScreenOverlay.userinterface;

import nl.NG.Jetfightergame.ScreenOverlay.MenuStyleSettings;
import nl.NG.Jetfightergame.ScreenOverlay.ScreenOverlay;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

/**
 * @author Geert van Ieperen
 * a button with set shape and text
 */
public class MenuButton extends MenuClickable {

    private final String text;

    private Runnable click;

    public MenuButton(String text, Runnable click) {
        this(text, MenuStyleSettings.BUTTON_WIDTH, MenuStyleSettings.BUTTON_HEIGHT, click);
    }

    /**
     * create a button that executes a click
     * @param text the text displayed on the button, will also be used to name in case of error
     * @param width
     * @param height
     * @param click
     */
    public MenuButton(String text, int width, int height, Runnable click) {
        super(width, height);
        this.text = text;
        this.click = click;
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.roundedRectangle(x, y, width, height, MenuStyleSettings.INDENT);
        hud.setFillColor(MenuStyleSettings.FILL_COLOR);
        hud.setStrokeColor(MenuStyleSettings.STROKE_COLOR);
        hud.setStrokeWidth(MenuStyleSettings.STROKE_WIDTH);

        ScreenOverlay.Font font = ScreenOverlay.Font.ORBITRON_MEDIUM;
        hud.text(x + width /2, y + MenuStyleSettings.TEXT_SIZE_LARGE + 10, MenuStyleSettings.TEXT_SIZE_LARGE, font, NVG_ALIGN_CENTER, text);
    }

    @Override
    public void onClick(int x, int y) {
        try {
            click.run();
        } catch (Exception ex){
            throw new RuntimeException("Error occurred in button \"" + text + "\"", ex);
        }
    }
}
