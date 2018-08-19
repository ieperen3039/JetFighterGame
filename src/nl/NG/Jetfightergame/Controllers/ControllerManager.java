package nl.NG.Jetfightergame.Controllers;

import nl.NG.Jetfightergame.ArtificalIntelligence.HunterAI;
import nl.NG.Jetfightergame.Assets.Entities.FighterJets.AbstractJet;
import nl.NG.Jetfightergame.ScreenOverlay.ScreenOverlay;
import nl.NG.Jetfightergame.ServerNetwork.ClientConnection;
import nl.NG.Jetfightergame.Tools.Manager;

import java.util.function.Consumer;

import static nl.NG.Jetfightergame.Controllers.ControllerManager.ControllerImpl.*;
import static nl.NG.Jetfightergame.Controllers.ControllerManager.ControllerImpl.XBoxController;

/**
 * a controller decorator that manages the current controller for the player, implementing overriding control.
 * @author Geert van Ieperen
 * created on 22-12-2017.
 */
public class ControllerManager implements Controller, Manager<ControllerManager.ControllerImpl> {

    private static final ControllerImpl[] SELECTABLE_CONTROLLERS = {MouseAbsolute, MouseAbsoluteActive, MouseRelative, XBoxController};
    private final ClientConnection controlReceiver;
    private ScreenOverlay hud;
    private Controller instance;

    public ControllerManager(ScreenOverlay hud, ClientConnection controlReceiver) {
        this.controlReceiver = controlReceiver;
        instance = new EmptyController();
        this.hud = hud;
    }

    /**
     * all control types available for the player. This logically excludes AI.
     */
    public enum ControllerImpl {
        MouseAbsolute, MouseRelative,
        XBoxController,
        MouseAbsoluteActive,
        EmptyController,
        HunterAI,
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public ControllerImpl[] implementations() {
        return SELECTABLE_CONTROLLERS;
    }

    public void switchTo(ControllerImpl type){
        Controller newInst;

        switch (type){
            case MouseAbsolute:
                newInst = new PassivePCControllerAbsolute();
                break;

            case MouseRelative:
                newInst = new PassivePCControllerRelative();
                break;

            case XBoxController:
                newInst = new XBoxController();
                break;

            case MouseAbsoluteActive:
                newInst = new ActivePCControllerAbsolute(controlReceiver);
                break;

            case EmptyController:
                newInst = new EmptyController();
                break;

            case HunterAI:
                AbstractJet jet = controlReceiver.jet();
                newInst = new HunterAI(jet, jet.getTarget(), controlReceiver.getWorld(), 100);
                break;

            default:
                throw new UnsupportedOperationException("unknown enum: " + type);
        }
        newInst.update();

        // thread-safe swap
        Controller oldInst = instance;
        instance = newInst;

        oldInst.cleanUp();
        if (hud != null) {
            hud.removeHudItem(oldInst.hudElement());
            hud.addHudItem(instance.hudElement());
        }
    }

    public void setDisplay(ScreenOverlay target) {
        if (hud != null) hud.removeHudItem(instance.hudElement());
        hud = target;
        Consumer<ScreenOverlay.Painter> newElement = instance.hudElement();
        if (newElement != null) hud.addHudItem(newElement);
    }

    @Override
    public float throttle() {
        return instance.throttle();
    }

    @Override
    public Consumer<ScreenOverlay.Painter> hudElement() {
        return instance.hudElement();
    }

    @Override
    public float pitch() {
        return instance.pitch();
    }

    @Override
    public float yaw() {
        return instance.yaw();
    }

    @Override
    public float roll() {
        return instance.roll();
    }

    @Override
    public boolean primaryFire() {
        return instance.primaryFire();
    }

    @Override
    public boolean secondaryFire() {
        return instance.secondaryFire();
    }

    @Override
    public void update() {
        instance.update();
    }

    @Override
    public boolean isActiveController() {
        return instance.isActiveController();
    }
}
