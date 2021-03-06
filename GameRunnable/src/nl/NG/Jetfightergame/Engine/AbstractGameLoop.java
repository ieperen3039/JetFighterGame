package nl.NG.Jetfightergame.Engine;

import nl.NG.Jetfightergame.Settings.ServerSettings;
import nl.NG.Jetfightergame.Tools.DataStructures.AveragingQueue;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Timer;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;


/**
 * @author Geert van Ieperen
 * recreated on 29-10-2019
 *
 * a general-purpose game loop
 * also usable for rendering
 */
public abstract class AbstractGameLoop extends Thread {
    private Float targetDeltaMillis;
    private CountDownLatch pauseBlock = new CountDownLatch(0);
    private boolean shouldStop;
    private boolean isPaused = true;
    private final boolean notifyDelay;

    private AveragingQueue avgTPS;
    private AveragingQueue avgPoss;
    private final Supplier<String> tickCounter;
    private final Supplier<String> possessionCounter;

    /**
     * creates a new, paused gameloop
     * @param name the name as displayed in {@link #toString()}
     * @param targetTps the target number of executions of {@link #update(float)} per second
     * @param notifyDelay if true, an error message will be printed whenever the update method has encountered delay.
     */
    public AbstractGameLoop(String name, int targetTps, boolean notifyDelay) {
        super(name);
        if (targetTps == 0) pauseBlock = new CountDownLatch(1);
        this.targetDeltaMillis = 1000f/targetTps;
        this.notifyDelay = false;

        avgTPS = new AveragingQueue(targetTps/2);
        avgPoss = new AveragingQueue(targetTps/10);

        tickCounter = () -> String.format("%s TPS: %1.01f", name, avgTPS.average());
        possessionCounter = () -> String.format("%s POSS: %3d%%", name, (int) (100* avgPoss.average()));
    }

    /**
     * invoked (targetTps) times per second
     * @param deltaTime real-time difference since last loop
     */
    protected abstract void update(float deltaTime) throws Exception;

    /**
     * commands the engine to finish the current loop, and then quit
     */
    public void stopLoop(){
        shouldStop = true;
        pauseBlock.countDown();
    }

    /**
     * is always called when this gameloop terminates
     */
    protected abstract void cleanup();

    /**
     * start the loop, running until {@link #stopLoop()} is called.
     */
    public void run() {
        if (ServerSettings.DEBUG) Logger.DEBUG.print(this + " enabled");
        float deltaTime = 0;

        Logger.printOnline(tickCounter);
        Logger.printOnline(possessionCounter);

        try {
            pauseBlock.await();
            Timer loopTimer = new Timer();
            isPaused = false;

            while (!shouldStop || Thread.interrupted()) {
                // start measuring how long a gameloop takes
                loopTimer.updateLoopTime();

                // do stuff
                update(deltaTime);

                if (Thread.interrupted()) break;

                // number of milliseconds remaining in this loop
                float remainingTime = targetDeltaMillis - loopTimer.getTimeSinceLastUpdate();
                if (ServerSettings.DEBUG && notifyDelay && (remainingTime < 0))
                    Logger.WARN.printf("%s can't keep up! Running %d milliseconds behind%n", this, (int) -remainingTime);

                // sleep at least one millisecond
                long correctedTime = (long) Math.max(remainingTime, 1f);
                Thread.sleep(correctedTime);

                // store the duration and set this as length of next update
                loopTimer.updateLoopTime();
                deltaTime = loopTimer.getElapsedSeconds();

                // update Ticks per Second
                float realTPS = 1000f / loopTimer.getElapsedTime();
                avgTPS.add(realTPS);
                avgPoss.add((targetDeltaMillis - remainingTime) / targetDeltaMillis);

                // wait if the game is paused
                isPaused = true;
                pauseBlock.await();
                isPaused = false;
            }

        } catch (Exception ex) {
            Logger.ERROR.print(this + " has Crashed! Blame Menno.");
            exceptionHandler(ex);

        } finally {
            Logger.removeOnlineUpdate(tickCounter);
            Logger.removeOnlineUpdate(possessionCounter);
            cleanup();
        }

        // terminate engine
        Logger.DEBUG.print(this + " is stopped");
    }

    /**
     * is executed after printing the stacktrace
     * @param ex the exception that caused the crash
     */
    protected void exceptionHandler(Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public String toString() {
        return getName();
    }

    public void unPause(){
        pauseBlock.countDown();
        Logger.DEBUG.print("unpaused " + this);
    }

    public void pause(){
        pauseBlock = new CountDownLatch(1);
        Logger.DEBUG.print("paused " + this);
    }

    /**
     * @return true if this loop is not executing its loop.
     * This method returns false if {@link #pause()} is called, but the loop is still finishing its loop
     * @see #unPause()
     */
    public boolean isPaused() {
        return isPaused && (pauseBlock.getCount() > 0);
    }

    public void setTPS(int TPS) {
        this.targetDeltaMillis = 1000f/TPS;
    }
}
