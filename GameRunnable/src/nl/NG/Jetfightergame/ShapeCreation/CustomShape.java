package nl.NG.Jetfightergame.ShapeCreation;

import nl.NG.Jetfightergame.Primitives.Plane;
import nl.NG.Jetfightergame.Primitives.Triangle;
import nl.NG.Jetfightergame.Tools.DataStructures.Pair;
import nl.NG.Jetfightergame.Tools.Logger;
import nl.NG.Jetfightergame.Tools.Tracked.TrackedObject;
import nl.NG.Jetfightergame.Tools.Tracked.TrackedVector;
import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

/**
 * defines a custom, static object shape
 *
 * Created by Geert van Ieperen on 1-3-2017.
 */
public class CustomShape {

    private PosVector middle;
    private final boolean invertMiddle;

    private final Map<PosVector, Integer> points;
    private final List<DirVector> normals;
    private final List<Mesh.Face> faces;

    /**
     * custom shape with middle on (0, 0, 0) and non-inverted
     * @see #CustomShape(PosVector, boolean)
     */
    public CustomShape() {
        this(PosVector.zeroVector());
    }

    /**
     * @param middle the middle of this object.
     * @see #CustomShape(PosVector, boolean)
     */
    public CustomShape(PosVector middle) {
        this(middle, false);
    }

    /**
     * A shape that may be defined by the client code using methods of this class. When the shape is finished, call
     * {@link #wrapUp(boolean)} to load it into the GPU. The returned shape should be re-used as a static mesh for any
     * future calls to such shape.
     * @param middle the middle of this object. More specifically, from this point, all normal vectors point outward
     *               except maybe for those that have their normal explicitly defined.
     */
    public CustomShape(PosVector middle, boolean invertMiddle) {
        this.middle = middle;
        this.faces = new ArrayList<>();
        this.points = new Hashtable<>();
        this.normals = new ArrayList<>();
        this.invertMiddle = invertMiddle;
    }

    /**
     * defines a quad in rotational order. The vectors do not have to be given clockwise
     * @param A      (0, 0)
     * @param B      (0, 1)
     * @param C      (1, 1)
     * @param D      (1, 0)
     * @param normal the direction of the normal of this plane
     * @throws NullPointerException if any of the vectors is null
     */
    public void addQuad(PosVector A, PosVector B, PosVector C, PosVector D, DirVector normal) {
        DirVector currentNormal = Triangle.getNormalVector(A, B, C);

        if (currentNormal.dot(normal) >= 0) {
            addFinalQuad(A, B, C, D, currentNormal);
        } else {
            currentNormal.negate();
            addFinalQuad(D, C, B, A, currentNormal);
        }
    }

    /** a quad in rotational, counterclockwise order */
    private void addFinalQuad(PosVector A, PosVector B, PosVector C, PosVector D, DirVector normal) {
        addFinalTriangle(A, C, B, normal);
        addFinalTriangle(A, D, C, normal);
    }

    /**
     * defines a quad that is mirrored over the xz-plane
     * @see CustomShape#addFinalQuad(PosVector, PosVector, PosVector, PosVector, DirVector)
     */
    public void addQuad(PosVector A, PosVector B) {
        addQuad(A, B, B.mirrorY(new PosVector()), A.mirrorY(new PosVector()));
    }

    /**
     * @see CustomShape#addQuad(PosVector, PosVector, PosVector, PosVector, DirVector)
     */
    public void addQuad(PosVector A, PosVector B, PosVector C, PosVector D) {
        DirVector normal = Plane.getNormalVector(A, B, C);

        final DirVector direction = middle.to(B, new DirVector());

        if ((normal.dot(direction) >= 0) != invertMiddle) {
            addFinalQuad(A, B, C, D, normal);
        } else {
            normal.negate();
            addFinalQuad(D, C, B, A, normal);
        }
    }

    /**
     * Adds a quad which is mirrored in the XZ-plane
     * @see #addQuad(PosVector, PosVector, PosVector, PosVector, DirVector)
     */
    public void addMirrorQuad(PosVector A, PosVector B, PosVector C, PosVector D) {
        addQuad(A, B, C, D);
        addQuad(
                A.mirrorY(new PosVector()),
                B.mirrorY(new PosVector()),
                C.mirrorY(new PosVector()),
                D.mirrorY(new PosVector())
        );
    }

    /**
     * @see CustomShape#addFinalTriangle(PosVector, PosVector, PosVector, DirVector)
     */
    public void addTriangle(PosVector A, PosVector B, PosVector C) {
        DirVector normal = Plane.getNormalVector(A, B, C);
        final DirVector direction = middle.to(B, new DirVector());

        if ((normal.dot(direction) >= 0) != invertMiddle) {
            addFinalTriangle(A, B, C, normal);
        } else {
            normal.negate();
            addFinalTriangle(C, B, A, normal);
        }
    }


    public void addTriangle(PosVector A, PosVector B, PosVector C, DirVector normal) {
        DirVector currentNormal = Triangle.getNormalVector(A, B, C);

        if (currentNormal.dot(normal) >= 0) {
            addFinalTriangle(A, B, C, currentNormal);
        } else {
            currentNormal.negate();
            addFinalTriangle(C, B, A, currentNormal);
        }
    }

    /**
     * defines a triangle with the given points in counterclockwise ordering
     * @see CustomShape#addQuad(PosVector, PosVector, PosVector, PosVector)
     */
    private void addFinalTriangle(PosVector A, PosVector B, PosVector C, DirVector normal) {
        int aInd = addHitpoint(A);
        int bInd = addHitpoint(B);
        int cInd = addHitpoint(C);
        int nInd = addNormal(normal);
        faces.add(new Mesh.Face(new int[]{aInd, bInd, cInd}, nInd));
    }

    private int addNormal(DirVector normal) {
        if ((normal == null) || normal.equals(DirVector.zeroVector()))
            throw new IllegalArgumentException("Customshape.addNormal(DirVector): invalid normal: " + normal);

        normals.add(normal);
        return normals.size() - 1;
    }

    /**
     * stores a vector in the collection, and returns its resulting position
     * @param vector
     * @return index of the vector
     */
    private int addHitpoint(PosVector vector) {
        points.putIfAbsent(vector, points.size());
        return points.get(vector);
    }

    /**
     * Adds a triangle which is mirrored in the XZ-plane
     */
    public void addMirrorTriangle(PosVector A, PosVector B, PosVector C) {
        addTriangle(A, B, C);
        addTriangle(A.mirrorY(new PosVector()), B.mirrorY(new PosVector()), C.mirrorY(new PosVector()));
    }

    /**
     * Adds a triangle which is mirrored in the XZ-plane, where the defined triangle has a normal in the given
     * direction
     */
    public void addMirrorTriangle(PosVector A, PosVector B, PosVector C, DirVector normal) {
        addTriangle(A, B, C, normal);
        DirVector otherNormal = normal.negate(new DirVector());
        addTriangle(A.mirrorY(new PosVector()), B.mirrorY(new PosVector()), C.mirrorY(new PosVector()), otherNormal);
    }

    /**
     * Adds a strip defined by a beziér curve the 1-vectors are the curve of one size, the 2-vectors are the curve of
     * the other side
     * @param slices number of fractions of the curve
     * @return either side of the strip, with left the row starting with A1
     */
    public Pair<List<PosVector>, List<PosVector>> addBezierStrip(PosVector A1, PosVector A2, PosVector B1, PosVector B2,
                                                                 PosVector C1, PosVector C2, PosVector D1, PosVector D2,
                                                                 double slices) {

        DirVector startNormal = Plane.bezierDerivative(A2, B2, C2, D2, 0);
        if ((startNormal.dot(A2.to(middle, new DirVector())) > 0) != invertMiddle) {
            startNormal = startNormal.scale(-1, new DirVector());
        }
        TrackedObject<DirVector> normal = new TrackedVector<>(startNormal);

        List<PosVector> leftVertices = new ArrayList<>();
        List<PosVector> rightVertices = new ArrayList<>();

        // initialize the considered vertices by their starting point
        TrackedObject<PosVector> left = new TrackedObject<>(Plane.bezierPoint(A1, B1, C1, D1, 0).toPosVector());
        TrackedObject<PosVector> right = new TrackedObject<>(Plane.bezierPoint(A2, B2, C2, D2, 0).toPosVector());

        // add vertices for every part of the slice, and combine these into a quad
        for (int i = 1; i <= slices; i++) {
            left.update(Plane.bezierPoint(A1, B1, C1, D1, i / slices).toPosVector());
            leftVertices.add(left.current());

            right.update(Plane.bezierPoint(A2, B2, C2, D2, i / slices).toPosVector());
            rightVertices.add(right.current());

            DirVector newNormal = Plane.bezierDerivative(A2, B2, C2, D2, i / slices);
            newNormal = (newNormal.dot(normal.previous()) > 0) ? newNormal : newNormal.scale(-1, new DirVector());
            normal.update(newNormal);

            addFinalQuad(left.previous(), right.previous(), right.current(), left.current(), normal.current());
        }

        return new Pair<>(leftVertices, rightVertices);
    }

    /**
     * adds a simple beziér curve, mirrored over the xz plane
     * @param start the starting point of the curve
     * @param M     a point indicating the direction of the curve (NOT the middle control point, but the direction
     *              coefficients DO point to M)
     * @param end   the endpoint of the curve
     * @return this strip defined as two lists of points each defining one side of the strip
     * @see CustomShape#addBezierStrip
     */
    public Pair<List<PosVector>, List<PosVector>> addBezierStrip(PosVector start, PosVector M, PosVector end, int slices) {
        PosVector B = start.middleTo(M);
        PosVector C = end.middleTo(M);
        return addBezierStrip(start, start.mirrorY(new PosVector()), B, B.mirrorY(new PosVector()), C, C.mirrorY(new PosVector()), end, end.mirrorY(new PosVector()), slices);
    }

    /**
     * creates a plane connecting an existing beziér curve to a point
     * @param point    the point where the curve must be connected to
     * @param strip    the id returned upon creation of the specific curve
     * @param takeLeft {@code true} if the first vectors should be accounted {@code false} if the second vectors should
     *                 be accounted
     */
    public void addPlaneToBezierStrip(PosVector point, Pair<List<PosVector>, List<PosVector>> strip, boolean takeLeft) {
        Iterator<PosVector> positions = (takeLeft ? strip.left : strip.right).iterator();

        TrackedObject<PosVector> targets = new TrackedVector<>(positions.next());
        while (positions.hasNext()) {
            targets.update(positions.next());
            DirVector normal = Plane.getNormalVector(point, targets.previous(), targets.current());
            addFinalTriangle(targets.previous(), targets.current(), point, normal);
        }
    }

    /**
     * adds a strip as separate quad objects
     * @param quads an array of 2n+4 vertices defining quads as {@link #addQuad(PosVector, PosVector, PosVector,
     *              PosVector)} for every natural number n.
     */
    public void addStrip(PosVector... quads) {
        final int inputSize = quads.length;
        if (((inputSize % 2) != 0) || (inputSize < 4)) {
            throw new IllegalArgumentException(
                    "input arguments can not be of odd length or less than 4 (length is " + inputSize + ")");
        }

        for (int i = 4; i < inputSize; i += 2) {
            // create quad as [1, 2, 4, 3], as rotational order is required
            addQuad(quads[i - 4], quads[i - 3], quads[i - 1], quads[i - 2]);
        }
    }

    /**
     * convert this object to a Shape
     * @return a shape with hardware-accelerated graphics using the {@link BasicShape} object
     * @param loadMesh
     */
    public Shape wrapUp(boolean loadMesh) {
        return new BasicShape(getSortedVertices(), normals, faces, loadMesh, GL_TRIANGLES);
    }

    /**
     * convert this object into a Mesh
     * @return a hardware-accelerated Mesh object
     */
    public Mesh asMesh() {
        return new Mesh(getSortedVertices(), normals, faces, GL_TRIANGLES);
    }

    private List<PosVector> getSortedVertices() {
        // this is the most clear, structured way of the duplicate-vector problem. maybe not the most efficient.
        PosVector[] sortedVertices = new PosVector[points.size()];
        points.forEach((v, i) -> sortedVertices[i] = v);

        return Arrays.asList(sortedVertices);
    }

    /**
     * writes an object to the given filename
     * @throws IOException if any problem occurs while creating the file
     * @param filename
     */
    public void writeOBJFile(String filename) throws IOException {
        if (!filename.endsWith(".obj")) {
            filename += ".obj";
        }
        PrintWriter writer = new PrintWriter(filename, "UTF-8");

        writer.println("# created using a simple obj writer by Geert van Ieperen");
        writer.println("# calling method: " + Logger.getCallingMethod(2));

        PosVector[] sortedVertices = new PosVector[points.size()];
        points.forEach((v, i) -> sortedVertices[i] = v);

        for (PosVector vec : sortedVertices) {
            writer.println(String.format(Locale.US, "v %1.09f %1.09f %1.09f", vec.x(), vec.z(), vec.y()));
        }

        for (DirVector vec : normals) {
            writer.println(String.format(Locale.US, "vn %1.09f %1.09f %1.09f", vec.x(), vec.z(), vec.y()));
        }

        writer.println("usemtl None");
        writer.println("s off");
        writer.println("");

        for (Mesh.Face face : faces) {
            writer.print("f ");
            for (int i = 0; i < face.vert.length; i++) {
                writer.print(" " + readVertex(face.vert[i], face.norm[i]));
            }
            writer.println();
        }

        writer.close();

        Logger.DEBUG.print("Successfully created obj file: " + filename);
    }

    private static String readVertex(int vertex, int normal) {
        return String.format("%d//%d", vertex + 1, normal + 1);
    }

    public void setMiddle(PosVector middle) {
        this.middle = middle;
    }

    @Override
    public String toString() {
        return getSortedVertices().toString();
    }

    /**
     * Adds an arbitrary polygon to the object. For correct rendering, the plane should be flat
     * @param normal the direction of the normal of this plane. When null, it is calculated using the middle
     * @param edges  the edges of this plane
     */
    public void addPlane(DirVector normal, PosVector... edges) {
        switch (edges.length) {
            case 3:
                if (normal == null) addTriangle(edges[0], edges[1], edges[2]);
                else addTriangle(edges[0], edges[1], edges[2], normal);
                return;
            case 4:
                if (normal == null) addQuad(edges[0], edges[1], edges[2], edges[3]);
                else addQuad(edges[0], edges[1], edges[2], edges[3], normal);
                return;
        }
        for (int i = 1; i < (edges.length - 2); i++) {
            if (normal == null) addTriangle(edges[i], edges[i + 1], edges[i + 2]);
            else addTriangle(edges[i], edges[i + 1], edges[i + 2], normal);
        }
    }
}
