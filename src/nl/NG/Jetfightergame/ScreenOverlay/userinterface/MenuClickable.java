package nl.NG.Jetfightergame.ScreenOverlay.userinterface;

import nl.NG.Jetfightergame.ScreenOverlay.UIElement;

/**
 * @author Jorren Hendriks.
 */
public abstract class MenuClickable extends UIElement {

    public static final int BUTTON_WIDTH = 400;
    public static final int BUTTON_HEIGHT = 80;

    public MenuClickable(int width, int height) {
        super(width, height);
    }

    /**
     * is called when this element is clicked on. coordinates are screen coordinates, NOT relative
     * @param x screen x coordinate
     * @param y screen y coordinate
     */
    public abstract void onClick(int x, int y);
}
