package nl.NG.Jetfightergame.ShapeCreators;

import nl.NG.Jetfightergame.Engine.GLMatrix.GL2;
import nl.NG.Jetfightergame.Primitives.Surfaces.Plane;
import nl.NG.Jetfightergame.Primitives.Surfaces.Triangle;
import nl.NG.Jetfightergame.Tools.Pair;
import nl.NG.Jetfightergame.Tools.Toolbox;
import nl.NG.Jetfightergame.Vectors.DirVector;
import nl.NG.Jetfightergame.Vectors.PosVector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geert van Ieperen
 *         created on 11-11-2017.
 */
public class ShapeFromMesh implements Shape {

    public static final ShapeFromMesh BASIC = new ShapeFromMesh("res/models/ConceptBlueprint.obj");

    /** an arrow along the Z-axis, 1 long */
    public static final ShapeFromMesh ARROW = new ShapeFromMesh("res/models/arrow.obj");

    /**
     * every point of this mesh exactly once
     */
    private final List<PosVector> vertices;
    /**
     * every plane on this mesh (triangles)
     */
    private final List<Triangle> triangles;

    private final Mesh mesh;

    /**
     * a void method that allows pre-initialisation
     */
    public static void initAll(){}

    /**
     * @param fileName path to the .obj file
     *
     */
    private ShapeFromMesh(String fileName) {
        this(fileName, PosVector.O, new int[]{3, 1, 2}, 1f);
    }

    /**
     * @param fileName path to the .obj file
     * @param offSet   offset of the gravity middle in this mesh as {@code GM * -1}
     * @param XYZ      determines the definition of the axes. maps {forward, right, up} for X=1, Y=2, Z=3.
     * @param scale    the scale standard applied to this object, to let it correspond to its contract
     */
    private ShapeFromMesh(String fileName, PosVector offSet, int[] XYZ, float scale) {
        this(new MeshParameters(fileName, offSet, XYZ, scale), fileName);
    }

    private ShapeFromMesh(MeshParameters par, String name) {
        this(par.vertices, par.normals, par.faces);
        Toolbox.print("loaded mesh " + name + ": [Faces: " + par.faces.size() + ", vertices: " + par.vertices.size() + "]");
    }

    /**
     * create a mesh, and bind it to the GPU. the list of vertices is contained in this object and should not
     * externally be modified
     *
     * @param vertices a list of vertices, where preferably every vertex occurs only once.
     * @param normals  a list of normals, where preferably every normal occurs only once
     * @param faces    a collection of indices mapping to 3 vertices and corresponding normals
     */
    public ShapeFromMesh(List<PosVector> vertices, List<DirVector> normals, List<Mesh.Face> faces) {
        this.vertices = vertices;
        triangles = faces.stream()
                .map(f -> toTriangle(f, vertices, normals))
                .collect(Collectors.toCollection(LinkedList::new));

        mesh = new Mesh(vertices, normals, faces);
    }

    @Override
    public Stream<? extends Plane> getPlanes() {
        return triangles.stream();
    }

    @Override
    public Collection<PosVector> getPoints() {
        return vertices;
    }

    @Override
    public void render(GL2.Painter lock) {
        mesh.render(lock);
    }

    /**
     * creates a triangle object, using the indices on the given lists
     *
     * @param posVectors a list where the vertex indices of A, B and C refer to
     * @param normals    a list where the normal indices of A, B and C refer to
     * @return a triangle whose normal is the average of those of A, B and C, in Shape-space
     */
    public Triangle toTriangle(Mesh.Face face, List<PosVector> posVectors, List<DirVector> normals) {
        final PosVector alpha = posVectors.get(face.A.left);
        final PosVector beta = posVectors.get(face.B.left);
        final PosVector gamma = posVectors.get(face.C.left);

        // take average normal as normal of plane, or use default method if none are registered
        DirVector normal = fetchDir(normals, face.A.right)
                .add(fetchDir(normals, face.B.right))
                .add(fetchDir(normals, face.C.right));
        if (!normal.isScalable()) normal = Plane.getNormalVector(alpha, beta, gamma, PosVector.O);

        return new Triangle(alpha, beta, gamma, normal);
    }

    private static DirVector fetchDir(List<DirVector> normals, int index) {
        return index < 0 ? DirVector.O : normals.get(index);
    }

    private static class MeshParameters {
        private List<PosVector> vertices;
        private List<DirVector> normals;
        private List<Mesh.Face> faces;

        private MeshParameters(String fileName, PosVector offSet, int[] XYZ, float scale) {
            vertices = new ArrayList<>();
            normals = new ArrayList<>();
            faces = new LinkedList<>();
            List<String> lines = openMesh(fileName);

            for (String line : lines) {
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v":
                        // Geometric vertex
                        PosVector vec3f = new PosVector(
                                Float.parseFloat(tokens[XYZ[0]]),
                                Float.parseFloat(tokens[XYZ[1]]),
                                Float.parseFloat(tokens[XYZ[2]]))
                                .scale(scale)
                                .add(offSet)
                                .toPosVector();
                        vertices.add(vec3f);
                        break;
                    case "vn":
                        // Vertex normal
                        DirVector vec3fNorm = new DirVector(
                                Float.parseFloat(tokens[XYZ[0]]),
                                Float.parseFloat(tokens[XYZ[1]]),
                                Float.parseFloat(tokens[XYZ[2]]));
                        normals.add(vec3fNorm);
                        break;
                    case "f":
                        faces.add(makeFace(tokens[1], tokens[2], tokens[3]));
                        break;
                    default:
                        // Ignore other lines
                        break;
                }
            }
        }

        private List<String> openMesh(String fileName) {
            try {
                return Files.readAllLines(Paths.get(fileName));
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Could not read mesh! Continuing game without model...");
                return new LinkedList<>();
            }
        }

        /**
         * for storage of vertex-indices
         * face == plane
         */
        public Mesh.Face makeFace(String v1, String v2, String v3) {
            Pair<Integer, Integer> a = (parseVertex(v1));
            Pair<Integer, Integer> b = (parseVertex(v2));
            Pair<Integer, Integer> c = (parseVertex(v3));
            return new Mesh.Face(a, b, c);
        }

        /**
         * parse and store the references of a single vertex
         */
        private static Pair<Integer, Integer> parseVertex(String line) {
            String[] lineTokens = line.split("/");
            int vertex = Integer.parseInt(lineTokens[0]) - 1;

            if (lineTokens.length > 2) {
                return new Pair<>(vertex, Integer.parseInt(lineTokens[2]) - 1);
            }
            return new Pair<>(vertex, -1);
        }
    }
}