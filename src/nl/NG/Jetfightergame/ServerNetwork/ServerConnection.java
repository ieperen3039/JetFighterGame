package nl.NG.Jetfightergame.ServerNetwork;

import nl.NG.Jetfightergame.AbstractEntities.AbstractJet;
import nl.NG.Jetfightergame.AbstractEntities.MovingEntity;
import nl.NG.Jetfightergame.AbstractEntities.Spawn;
import nl.NG.Jetfightergame.GameState.Player;
import nl.NG.Jetfightergame.Tools.DataStructures.Pair;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * can be viewed as a client's personal connection inside the server
 * @author Geert van Ieperen created on 5-5-2018.
 */
public class ServerConnection implements BlockingListener, Player {
    private final DataInputStream clientIn;
    private final DataOutputStream clientOut;
    private final String clientName;
    private final boolean hasAdminCapabilities;

    private final GameServer server;
    private final AbstractJet playerJet;
    private Lock sendOutput = new ReentrantLock();

    private final RemoteControlReceiver controls;

    public ServerConnection(Socket connection, boolean isAdmin, GameServer server, MovingEntity.State playerSpawn) throws IOException {
        this.clientOut = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
        this.clientIn = new DataInputStream(connection.getInputStream());
        this.hasAdminCapabilities = isAdmin;
        this.server = server;
        this.controls = new RemoteControlReceiver();
        // notify client
        clientOut.write(MessageType.CONFIRM_CONNECTION.ordinal());
        clientOut.flush();
        // get player properties
        Pair<String, Spawn> p = JetFighterProtocol.playerSpawnAccept(clientIn, playerSpawn);
        clientName = p.left;
        MovingEntity construct = p.right.construct(server, controls);
        assert construct instanceof AbstractJet : "player tried flying on something that is not a jet.";
        playerJet = (AbstractJet) construct;

        JetFighterProtocol.syncTimerSource(clientIn, clientOut, server.getTimer());
    }

    @Override
    public boolean handleMessage() throws IOException {
        MessageType type = MessageType.get(clientIn.read());

        if (type.isOf(MessageType.adminOnly) && !hasAdminCapabilities) {
            Logger.printError(this + " sent an " + type + " command, while it has no access to it");
            return true;

        } else if (type == MessageType.CONNECTION_CLOSE) {
            clientOut.write(MessageType.CONNECTION_CLOSE.ordinal()); // reflect
            clientOut.close();
            Logger.print(clientName + " connection close");
            return false;
        }

        if (type.isOf(MessageType.controls)) {
            JetFighterProtocol.controlRead(clientIn, controls, type);

        } else switch (type) {
            case PING:
                clientOut.write(MessageType.PONG.ordinal());
                clientOut.flush();
                break;

            case START_GAME:
                server.unPause();
                break;

            case PAUSE_GAME:
                server.pause();
                break;

            case SHUTDOWN_GAME:
                server.shutDown();
                break;

            case ENTITY_SPAWN:
                // actually works for Jets
                Spawn spawn = JetFighterProtocol.spawnRequestRead(clientIn);
                server.addSpawn(spawn);
                break;

            default:
                long bits = clientIn.skip(type.nOfArgs());
                Logger.printError("Message caused an error: " + type, "skipping " + bits + " bits");
        }

        return true;
    }

    /**
     * a passive closing, doesnt actually close the connection yet
     */
    public void close() {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.SHUTDOWN_GAME.ordinal());
            clientOut.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            sendOutput.unlock();
        }
    }

    /** send the previously collected data to the clients */
    public void flush() {
        sendOutput.lock();
        try {
            clientOut.flush();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            sendOutput.unlock();
        }
    }

    /**
     * sends an update to the client of the given entity's position, rotation and velocity
     * @param thing       the object to be updated
     * @param currentTime the time of when this entity is on the said position
     */
    public void sendEntityUpdate(MovingEntity thing, float currentTime) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.ENTITY_UPDATE.ordinal());
            JetFighterProtocol.entityUpdateSend(clientOut, thing, currentTime);

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    /**
     * sends the event of a newly spawned object
     * @param entity the entity to be sent
     * @param id     its unique id, generated by the server
     */
    public void sendEntitySpawn(Spawn entity, int id) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.ENTITY_SPAWN.ordinal());
            JetFighterProtocol.newEntitySend(clientOut, entity, id);

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    public void sendExplosionSpawn(PosVector position, DirVector direction, float spread, Color4f color1, Color4f color2) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.EXPLOSION_SPAWN.ordinal());
            JetFighterProtocol.explosionSend(clientOut, position, direction, spread, color1, color2);

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    public void sendEntityRemove(MovingEntity entity) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.ENTITY_REMOVE.ordinal());
            JetFighterProtocol.entityRemoveSend(clientOut, entity.idNumber());

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    public void sendProgress(String playerName, int checkPointNr, int roundNr) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.RACE_PROGRESS.ordinal());
            JetFighterProtocol.raceProgressSend(clientOut, playerName, checkPointNr, roundNr);

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    public void sendPlayerSpawn(Player player) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.PLAYER_SPAWN.ordinal());
            JetFighterProtocol.playerSpawnSend(clientOut, player.playerName(), player.jet());

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    public void sendWorldSwitch(WorldClass world) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.WORLD_SWITCH.ordinal());
            JetFighterProtocol.worldSwitchSend(clientOut, world);

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }

    @Override
    public String toString() {
        return clientName;
    }

    @Override
    public AbstractJet jet() {
        return playerJet;
    }

    @Override
    public String playerName() {
        return clientName;
    }
}
