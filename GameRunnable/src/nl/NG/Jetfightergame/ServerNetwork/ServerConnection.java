package nl.NG.Jetfightergame.ServerNetwork;

import nl.NG.Jetfightergame.Assets.Entities.FighterJets.AbstractJet;
import nl.NG.Jetfightergame.EntityGeneral.EntityMapping;
import nl.NG.Jetfightergame.EntityGeneral.EntityState;
import nl.NG.Jetfightergame.EntityGeneral.Factory.EntityFactory;
import nl.NG.Jetfightergame.EntityGeneral.MovingEntity;
import nl.NG.Jetfightergame.EntityGeneral.Powerups.PowerupEntity;
import nl.NG.Jetfightergame.EntityGeneral.Powerups.PowerupType;
import nl.NG.Jetfightergame.GameState.Player;
import nl.NG.Jetfightergame.Tools.DataStructures.Pair;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static nl.NG.Jetfightergame.ServerNetwork.MessageType.*;

/**
 * can be viewed as a client's personal connection inside the server
 * @author Geert van Ieperen created on 5-5-2018.
 */
public class ServerConnection implements BlockingListener, Player {
    private final InputStream clientIn;
    private final OutputStream clientOut;
    private final String clientName;
    private final boolean hasAdminCapabilities;

    private final GameServer server;
    private final AbstractJet playerJet;
    private final RemoteControlReceiver controls;
    private final JetFighterProtocol protocol;

    private Lock sendLock = new ReentrantLock();
    protected volatile boolean isClosed;

    /**
     * construct a server-side connection to a player
     * @param inputStream  the incoming communication from the player
     * @param outputStream the outgoing communication to the player
     * @param server       the object that accepts server-commands
     * @param playerSpawn  the place and state of the player at the moment of spawning. this should be an unoccupied
     *                     place in space
     * @param worldType    the current selected world
     * @param entities     an access point for all entities in the world
     * @param isAdmin      if true, allows the player to send commands of host level.
     * @throws IOException if any communication error occurs, as defined by the given Input- and OutputStreams
     */
    public ServerConnection(
            InputStream inputStream, OutputStream outputStream,
            GameServer server, EntityState playerSpawn,
            EnvironmentClass worldType, EntityMapping entities, boolean isAdmin
    ) throws IOException {
        this.clientOut = new BufferedOutputStream(outputStream);
        this.clientIn = inputStream;
        this.hasAdminCapabilities = isAdmin;
        this.server = server;

        this.protocol = new JetFighterProtocol(clientIn, clientOut);
        protocol.syncTimerSource(server.getTimer());
        this.controls = new RemoteControlReceiver();
        protocol.worldSwitchSend(worldType, 0f, 0);
        clientOut.flush();
        Pair<String, AbstractJet> p = protocol.playerSpawnAccept(playerSpawn, server, controls, entities, isAdmin);
        clientName = p.left;
        playerJet = p.right;
    }

    protected ServerConnection(String name, File outputFile, float currentTime) throws IOException {
        this.protocol = new JetFighterProtocol(outputFile, true);
        clientOut = protocol.getOutput();
        clientIn = protocol.getInput();
        new DataOutputStream(clientOut).writeFloat(currentTime);

        clientName = name;
        hasAdminCapabilities = false;

        server = null;
        playerJet = null;
        controls = new RemoteControlReceiver();
    }

    @Override
    public boolean handleMessage() throws IOException {
        MessageType type;
        try {
            type = MessageType.get(clientIn.read());
        } catch (IOException ex) {
            Logger.ERROR.print(ex.getMessage());
            type = MessageType.CONNECTION_CLOSE;
        }


        if (type == CONNECTION_CLOSE) {
            isClosed = true;
            Logger.WARN.print("Connection to " + clientName + " has been lost");
            return false;

        } else if (type.isOf(adminOnly) && !hasAdminCapabilities) {
            Logger.WARN.print(this + " sent a " + type + " command, while it has no access to it");

        } else if (type.isOf(MessageType.controls)) {
            protocol.controlRead(controls, type);

        } else {
            // type is allowed and not a control message
            Logger.DEBUG.printf("[%s @ %.2f] %s", clientName, server.getTimer().time(), type);
            switch (type) {
                case CLOSE_REQUEST:
                    isClosed = true;
                    return false;

                case PING:
                    sendMessage(PONG, clientOut::flush);
                    break;

                case PAUSE_GAME:
                    server.pause();
                    break;

                case UNPAUSE_GAME:
                    server.unPause();
                    break;

                case SYNC_TIMER:
                    sendMessage(SYNC_TIMER, () -> protocol.syncTimerSource(server.getTimer()));
                    break;

                case START_GAME:
                    server.startRace();
                    break;

                case SHUTDOWN_GAME:
                    server.shutDown();
                    break;

                default:
                    long bits = clientIn.skip(type.nOfBits());
                    Logger.ERROR.print("Message caused an error: " + type, "skipping " + bits + " bits");
            }
        }
        return true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    /**
     * sends an update to the client of the given entity's position, rotation and velocity
     * @param entity      the entity to be updated
     * @param currentTime the time of when this entity is on the said position
     */
    public void sendEntityUpdate(MovingEntity entity, float currentTime) {
        sendMessage(ENTITY_UPDATE, () ->
                protocol.entityUpdateSend(entity, currentTime)
        );
    }

    /**
     * sends the event of a newly spawned entity
     * @param entity the entity to be sent
     */
    public void sendEntitySpawn(EntityFactory entity) {
        sendMessage(ENTITY_SPAWN, () ->
                protocol.newEntitySend(entity)
        );
    }

    public void sendExplosionSpawn(PosVector position, DirVector direction, float spread, int density, Color4f color1, Color4f color2, float lingerTime, float particleSize) {
        sendMessage(EXPLOSION_SPAWN, () ->
                protocol.explosionSend(position, direction, spread, density, color1, color2, lingerTime, particleSize)
        );
    }

    public void sendEntityRemove(MovingEntity entity) {
        sendMessage(ENTITY_REMOVE, () ->
                protocol.entityRemoveSend(entity)
        );
    }

    public void sendProgress(int pInd, int checkPointNr, int roundNr) {
        sendMessage(RACE_PROGRESS, () ->
                protocol.raceProgressSend(pInd, checkPointNr, roundNr)
        );
    }

    public void sendPlayerSpawn(Player player, int pInd) {
        if (pInd < 0) throw new IllegalArgumentException("pInd:" + pInd);
        sendMessage(PLAYER_SPAWN, () ->
                protocol.playerSpawnSend(player, pInd)
        );
    }

    public void sendWorldSwitch(EnvironmentClass world, float countDown, int maxRounds) {
        sendMessage(WORLD_SWITCH, () ->
                protocol.worldSwitchSend(world, countDown, maxRounds)
        );
    }

    public void sendPowerupUpdate(PowerupEntity powerup, float collectionTime, boolean isCollected) {
        sendMessage(POWERUP_STATE, () ->
                protocol.powerupUpdateSend(powerup, collectionTime, isCollected)
        );
    }

    public void sendPowerupCollect(PowerupType powerupType) {
        sendMessage(POWERUP_COLLECT, () ->
                protocol.powerupCollectSend(powerupType)
        );
    }

    private void sendMessage(MessageType type, IOAction action) {
        sendLock.lock();
        try {
            clientOut.write(type.ordinal());
            action.run();

        } catch (IOException e) {
            e.printStackTrace();
            isClosed = true;

        } finally {
            sendLock.unlock();
        }
    }

    /** send the previously collected data to the clients */
    public void flush() {
        sendLock.lock();
        try {
            clientOut.flush();

        } catch (IOException e) {
            e.printStackTrace();
            isClosed = true;

        } finally {
            sendLock.unlock();
        }
    }

    public void closeConnection(String message) {
        sendMessage(TEXT_MESSAGE, () -> {
            protocol.sendText(message);
            clientOut.write(CONNECTION_CLOSE.ordinal());
            clientOut.flush();
        });
    }

    @Override
    public String toString() {
        return "player " + playerName();
    }

    @Override
    public AbstractJet jet() {
        return playerJet;
    }

    @Override
    public String playerName() {
        return clientName;
    }

    public void sendBoosterColorChange(AbstractJet jet, Color4f color1, Color4f color2, float duration) {
        sendMessage(BOOSTER_COLOR_CHANGE, () -> protocol.sendBoosterColor(jet, color1, color2, duration));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Player) {
            Player other = (Player) obj;
            return this.playerName().equals(other.playerName());
        }
        return false;
    }

    public void send(MessageType messageType) {
        sendMessage(messageType, clientOut::flush);
    }

    /**
     * closes the output stream, which won't happen by default
     * @throws IOException
     */
    protected void closeOutputStream() throws IOException {
        clientOut.close();
    }

    /** executes the action, which may throw an IOException */
    private interface IOAction {
        void run() throws IOException;
    }
}
