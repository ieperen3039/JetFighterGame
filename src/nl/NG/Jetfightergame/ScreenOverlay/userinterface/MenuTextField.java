package nl.NG.Jetfightergame.ScreenOverlay.userinterface;

import nl.NG.Jetfightergame.ScreenOverlay.ScreenOverlay;
import nl.NG.Jetfightergame.ScreenOverlay.UIElement;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP;

/**
 * @author Geert van Ieperen
 * a field with the make-up of a menubutton, automatically including title and back-button
 */
public class MenuTextField extends UIElement {

    public static final int MARGIN = 20; //TODO adapt with textsize?
    public static final int TEXT_SIZE_SMALL = 30;
    public static final int INTERTEXT_MARGIN = MenuPositioner.STD_MARGIN;
    private final String title;
    private final String[] content;

    /**
     * Creates a textfield
     * @param title The title of the field
     * @param content the content this textbox has to display
     */
    public MenuTextField(String title, String[] content, int width) {
        super(width, (TEXT_SIZE_SMALL + INTERTEXT_MARGIN) * content.length + TEXT_SIZE_LARGE + INTERTEXT_MARGIN + 2*MARGIN);

        this.title = title;
        this.content = content;
    }

    @Override
    public void draw(ScreenOverlay.Painter hud) {
        hud.roundedRectangle(x, y, width, height, INDENT);
        hud.fill(MENU_FILL_COLOR);
        hud.stroke(MENU_STROKE_WIDTH, MENU_STROKE_COLOR);

        final int middle = this.x + width / 2;
        hud.text(middle, y + MARGIN, TEXT_SIZE_LARGE, ScreenOverlay.Font.MEDIUM,
                NVG_ALIGN_CENTER | NVG_ALIGN_TOP, title, TEXT_COLOR);

        int textYPosition = y + MARGIN*2 + TEXT_SIZE_LARGE;
        for (String line : content) {
            hud.text(middle, textYPosition, TEXT_SIZE_SMALL, ScreenOverlay.Font.MEDIUM, NVG_ALIGN_CENTER | NVG_ALIGN_TOP, line, TEXT_COLOR);
            textYPosition += TEXT_SIZE_SMALL + INTERTEXT_MARGIN;
        }
    }
}
