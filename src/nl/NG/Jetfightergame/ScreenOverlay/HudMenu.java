package nl.NG.Jetfightergame.ScreenOverlay;

import nl.NG.Jetfightergame.Controllers.InputHandling.MouseTracker;
import nl.NG.Jetfightergame.Controllers.InputHandling.TrackerClickListener;
import nl.NG.Jetfightergame.ScreenOverlay.userinterface.MenuClickable;
import nl.NG.Jetfightergame.ScreenOverlay.userinterface.MenuPositioner;
import nl.NG.Jetfightergame.ScreenOverlay.userinterface.MenuPositionerCenter;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * @author Jorren Hendriks
 * @author Geert van Ieperen
 */

public abstract class HudMenu implements TrackerClickListener {
    private final Supplier<Integer> width;
    private final Supplier<Integer> height;
    private UIElement[] activeElements;
    private ScreenOverlay hud;

    public HudMenu(Supplier<Integer> width, Supplier<Integer> height, ScreenOverlay hud) {
        this.width = width;
        this.height = height;
        this.hud = hud;
        MouseTracker.getInstance().addClickListener(this, false);
    }

    /**
     * set the active elements to the defined elements
     * @param newElements new elements of the menu
     */
    public void switchContentTo(UIElement[] newElements) {
        activeElements = newElements.clone();

        // destroy the current entries of the hud
        hud.removeMenuItem();

        // correct positions of buttons
        MenuPositioner caret = new MenuPositionerCenter(width.get());
        for (UIElement element : activeElements) {
            caret.place(element);
            hud.addMenuItem(element::draw);
        }

    }

    // note that these can only fire when mouse is not in capture mode
    @Override
    public void clickEvent(int x, int y) {
        if (!hud.isMenuMode()) return;

        Arrays.stream(activeElements)
                // take all clickable elements
                .filter(element -> element instanceof MenuClickable)
                // identify
                .map(element -> (MenuClickable) element)
                // take the button that is clicked
                .filter(button -> button.contains(x, y))
                // execute buttonpress
                .forEach(button -> button.onClick(x, y));
    }

    @Override
    public void cleanUp() {
        MouseTracker.getInstance().removeClickListener(this, false);
    }
}
