package nl.NG.Jetfightergame.ScreenOverlay;

import nl.NG.Jetfightergame.Controllers.InputHandling.MouseTracker;
import nl.NG.Jetfightergame.Controllers.InputHandling.TrackerClickListener;
import nl.NG.Jetfightergame.ScreenOverlay.Userinterface.MenuClickable;
import nl.NG.Jetfightergame.ScreenOverlay.Userinterface.MenuPositioner;
import nl.NG.Jetfightergame.ScreenOverlay.Userinterface.MenuPositionerCenter;
import nl.NG.Jetfightergame.Sound.AudioSource;
import nl.NG.Jetfightergame.Sound.Sounds;
import nl.NG.Jetfightergame.Tools.Toolbox;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Jorren Hendriks
 * @author Geert van Ieperen
 */

public abstract class HudMenu implements TrackerClickListener, Consumer<ScreenOverlay.Painter> {
    private final Supplier<Integer> width;
    private final Supplier<Integer> height;
    private UIElement[] activeElements;
    private BooleanSupplier menuMode;
    private AudioSource currentSound = AudioSource.empty;

    public HudMenu(Supplier<Integer> width, Supplier<Integer> height, BooleanSupplier isMenuMode) {
        this.width = width;
        this.height = height;
        this.menuMode = isMenuMode;
        MouseTracker.getInstance().addClickListener(this, false);
    }

    @Override
    public void accept(ScreenOverlay.Painter hud) {
        Toolbox.checkALError();
        for (UIElement element : activeElements) {
            element.draw(hud);
        }
    }

    /**
     * set the active elements to the defined elements
     * @param newElements new elements of the menu
     */
    public void switchContentTo(UIElement[] newElements) {
        activeElements = newElements.clone();

        // set positions
        MenuPositioner caret = new MenuPositionerCenter(width.get());
        for (UIElement element : activeElements) {
            caret.place(element);
        }
    }

    // note that these can only fire when mouse is not in capture mode
    @Override
    public void clickEvent(int x, int y) {
        if (!menuMode.getAsBoolean()) return;

        Arrays.stream(activeElements)
                // take all clickable elements
                .filter(element -> element instanceof MenuClickable)
                // identify
                .map(element -> (MenuClickable) element)
                // take the button that is clicked
                .filter(button -> button.contains(x, y))
                .findAny()
                // execute buttonpress
                .ifPresent(button -> {
                    Toolbox.checkALError();
                    currentSound.dispose();
                    currentSound = new AudioSource(Sounds.button.get(), 1.0f, false);
                    button.onClick(x, y);
                });
    }

    @Override
    public void cleanUp() {
        currentSound.dispose();
        MouseTracker.getInstance().removeClickListener(this, false);
    }

}
