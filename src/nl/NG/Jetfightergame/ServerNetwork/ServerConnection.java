package nl.NG.Jetfightergame.ServerNetwork;

import nl.NG.Jetfightergame.AbstractEntities.GameEntity;
import nl.NG.Jetfightergame.AbstractEntities.MovingEntity;
import nl.NG.Jetfightergame.Controllers.Controller;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Vectors.Color4f;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * can be viewed as a client's personal connection inside the server
 * @author Geert van Ieperen created on 5-5-2018.
 */
public class ServerConnection implements BlockingListener {
    private final InputStream clientIn;
    private final BufferedOutputStream clientOut;
    private final String clientName;
    public final boolean hasAdminCapabilities;

    private final GameServer server;
    private final MovingEntity playerJet;
    private Lock sendOutput = new ReentrantLock();

    private final RemoteControlReceiver controls;

    public ServerConnection(Socket connection, boolean isAdmin, GameServer server, GameEntity.State playerSpawn) throws IOException {
        this.clientIn = connection.getInputStream();
        this.clientOut = new BufferedOutputStream(connection.getOutputStream());
        this.hasAdminCapabilities = isAdmin;
        this.server = server;
        this.controls = new RemoteControlReceiver();
        this.clientName = connection.toString();

        JetFighterProtocol.syncTimerSource(clientIn, clientOut, server.getTimer());

        this.playerJet = createPlayer(playerSpawn);
    }

    /**
     * sends an update to the client of the given entity's position, rotation and velocity
     * @param thing the object to be updated
     * @param currentTime the time of when this entity is on the said position
     */
    public void sendEntityUpdate(MovingEntity thing, float currentTime) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.ENTITY_UPDATE.ordinal());
            JetFighterProtocol.entityUpdateSend(clientOut, thing, currentTime);
            // no flush

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
    }
    /**
     * sends the event of a newly spawned object
     * @param entity the entity to be sent
     * @param id its unique id, generated by the server
     */
    public void sendEntitySpawn(MovingEntity.Spawn entity, int id) {
        sendOutput.lock();
        try {
            clientOut.write(MessageType.ENTITY_SPAWN.ordinal());
            JetFighterProtocol.newEntitySend(clientOut, entity, id);
            clientOut.flush();

        } catch (IOException ex) {
            Logger.printError(ex);

        } finally {
            sendOutput.unlock();
        }
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

        } else if (type == MessageType.PING) {
            clientOut.write(MessageType.PONG.ordinal());
            clientOut.flush();

        } else if (type == MessageType.START_GAME) {
            server.unPause();

        } else if (type == MessageType.PAUSE_GAME) {
            server.pause();

        } else if (type == MessageType.SHUTDOWN_GAME){
            server.shutDown();

        } else if (type == MessageType.ENTITY_SPAWN) {
            // actually works for Jets
            MovingEntity.Spawn spawn = JetFighterProtocol.spawnRequestRead(clientIn);
            server.addSpawn(spawn);

        } else {
            long bits = clientIn.skip(type.nOfArgs());
            Logger.printError("Message caused an error: " + type, "skipping " + bits + " bits");
        }

        return true;
    }

    @Override
    public String toString() {
        return clientName;
    }

    /**
     * reads a new playerJet from the client
     * @see ClientConnection#getPlayerJet(Controller)
     */
    private MovingEntity createPlayer(GameEntity.State position) throws IOException {
        // notify client
        clientOut.write(MessageType.CONFIRM_CONNECTION.ordinal());
        clientOut.flush();
        // listen to which plane is desired
        EntityClass type = JetFighterProtocol.playerSpawnAccept(clientIn);
        // create new plane
        MovingEntity.Spawn spawn = new MovingEntity.Spawn(type, position);
        // it sure may be a jet but nobody cares
        MovingEntity player = spawn.construct(server, controls);
        // notify client about his new acquisition
        JetFighterProtocol.newEntitySend(clientOut, spawn, player.idNumber());
        clientOut.flush();

        return player;
    }

    public MovingEntity getPlayerJet() {
        return playerJet;
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
}
