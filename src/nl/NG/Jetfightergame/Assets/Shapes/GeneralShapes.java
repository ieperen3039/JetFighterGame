package nl.NG.Jetfightergame.Assets.Shapes;

import nl.NG.Jetfightergame.Assets.WorldObjects.CheckpointRing;
import nl.NG.Jetfightergame.Rendering.MatrixStack.GL2;
import nl.NG.Jetfightergame.Settings.ServerSettings;
import nl.NG.Jetfightergame.ShapeCreation.BasicShape;
import nl.NG.Jetfightergame.ShapeCreation.CustomShape;
import nl.NG.Jetfightergame.ShapeCreation.Shape;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.util.List;

/**
 * @author Geert van Ieperen created on 7-11-2017.
 */
public final class GeneralShapes {
    private static final float CONTAINER_SIZE = 200f;
    private static boolean isLoaded = false;

    /** an arrow along the Z-axis, 1 long */
    public static Shape ARROW;
    /** a 2*2*2 cube with center on (0, 0, 0) */
    public static Shape CUBE;

    public static Shape ICOSAHEDRON;

    public static Shape LAB_CUBE;
    public static Shape INVERSE_CUBE;
    public static CheckpointRing CHECKPOINTRING;

    public static List<Shape> ISLAND1;

    /**
     * loads the shapes into memory. This method may be split into several selections of models
     * @param doLoadMesh whether the meshes should be loaded. If this is false, calling {@link
     *                   Shape#render(GL2.Painter)} will result in a {@link NullPointerException}
     */
    public static void init(boolean doLoadMesh) {
        ServerSettings.RENDER_ENABLED = doLoadMesh;
        if (isLoaded) {
            Logger.ERROR.print("Tried loading shapes while they where already loaded");
            return;
        }
        isLoaded = true;

        ARROW = new BasicShape("arrow.obj", doLoadMesh);
        INVERSE_CUBE = makeInverseCube(0, doLoadMesh);
        LAB_CUBE = makeInverseCube(3, doLoadMesh);
        CUBE = makeCube(doLoadMesh);
        ISLAND1 = BasicShape.loadSplit("maps/GlitchIsland.obj", doLoadMesh, CONTAINER_SIZE, 50f);
//        ISLAND1 = Collections.singletonList(new BasicShape("maps/Map1WIP_Triangle.obj", loadMesh));
        CHECKPOINTRING = new CheckpointRing(10, 0.03f, doLoadMesh);

        ICOSAHEDRON = makeIcosahedron(doLoadMesh);
    }

    private static Shape makeIcosahedron(boolean loadMesh) {
        CustomShape frame = new CustomShape();

        float A = 0.5257311f;
        float B = 0.8506508f;
        PosVector[] vdata = new PosVector[]{
                new PosVector(-A, 0, B), new PosVector(A, 0, B), new PosVector(-A, 0, -B), new PosVector(A, 0, -B),
                new PosVector(0, B, A), new PosVector(0, B, -A), new PosVector(0, -B, A), new PosVector(0, -B, -A),
                new PosVector(B, A, 0), new PosVector(-B, A, 0), new PosVector(B, -A, 0), new PosVector(-B, -A, 0),
        };

        int[][] indices = {
                {0, 4, 1}, {0, 9, 4}, {9, 5, 4}, {4, 5, 8}, {4, 8, 1},
                {8, 10, 1}, {8, 3, 10}, {5, 3, 8}, {5, 2, 3}, {2, 7, 3},
                {7, 10, 3}, {7, 6, 10}, {7, 11, 6}, {11, 0, 6}, {0, 1, 6},
                {6, 1, 10}, {9, 0, 11}, {9, 11, 2}, {9, 2, 5}, {7, 2, 11}
        };

        for (int[] index : indices) {
            frame.addTriangle(
                    vdata[index[0]],
                    vdata[index[1]],
                    vdata[index[2]]
            );
        }

        return frame.wrapUp(loadMesh);
    }

    private static Shape makeCube(boolean loadMesh) {
        CustomShape frame = new CustomShape();

        PosVector PPP = new PosVector(1, 1, 1);
        PosVector PPN = new PosVector(1, 1, -1);
        PosVector NPP = new PosVector(-1, 1, 1);
        PosVector NPN = new PosVector(-1, 1, -1);

        frame.addQuad(PPP, PPN);
        frame.addQuad(PPN, NPN);
        frame.addQuad(NPN, NPP); // -x plane
        frame.addQuad(NPP, PPP);
        frame.addMirrorQuad(PPP, PPN, NPN, NPP);

        return frame.wrapUp(loadMesh);
    }

    /**
     * create a new inverse cube
     * @param splits   number of splits on each side.
     * @param loadMesh
     * @return a cube with normals pointing inside, made out of {@code 6 * 2 ^ splits} quads
     */
    public static Shape makeInverseCube(int splits, boolean loadMesh) {
        CustomShape frame = new CustomShape();

        PosVector PPP = new PosVector(1, 1, 1);
        PosVector PPN = new PosVector(1, 1, -1);
        PosVector PNP = new PosVector(1, -1, 1);
        PosVector PNN = new PosVector(1, -1, -1);
        PosVector NPP = new PosVector(-1, 1, 1);
        PosVector NPN = new PosVector(-1, 1, -1);
        PosVector NNP = new PosVector(-1, -1, 1);
        PosVector NNN = new PosVector(-1, -1, -1);

        recursiveQuad(frame, NPN, NPP, NNP, NNN, new DirVector(1, 0, 0), splits);
        recursiveQuad(frame, PNP, PNN, NNN, NNP, new DirVector(0, 1, 0), splits);
        recursiveQuad(frame, PPP, PPN, PNN, PNP, new DirVector(-1, 0, 0), splits);
        recursiveQuad(frame, PPN, NPN, NNN, PNN, new DirVector(0, 0, 1), splits);
        recursiveQuad(frame, PPP, PPN, NPN, NPP, new DirVector(0, -1, 0), splits);
        recursiveQuad(frame, NPP, PPP, PNP, NNP, new DirVector(0, 0, -1), splits);

        return frame.wrapUp(loadMesh);
    }

    /**
     * recursively split the given quad, and add all tiny components to frame. This results in {@code 2^splits} quads
     * @param normal the shared normal of the resulting quads
     * @param splits the number of splits to be made, with 0 is no splits
     */
    private static void recursiveQuad(CustomShape frame, PosVector A, PosVector B, PosVector C, PosVector D, DirVector normal, int splits) {
        if (splits == 0) {
            frame.addQuad(A, B, C, D, normal);

        } else {
            PosVector AB = A.middleTo(B);
            PosVector BC = B.middleTo(C);
            PosVector CD = C.middleTo(D);
            PosVector AD = A.middleTo(D);
            PosVector MID = AB.middleTo(CD);

            recursiveQuad(frame, A, AB, MID, AD, normal, splits - 1);
            recursiveQuad(frame, AB, B, BC, MID, normal, splits - 1);
            recursiveQuad(frame, MID, BC, C, CD, normal, splits - 1);
            recursiveQuad(frame, AD, MID, CD, D, normal, splits - 1);
        }
    }
}
