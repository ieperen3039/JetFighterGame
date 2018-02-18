package nl.NG.Jetfightergame.Primitives.Surfaces;

import nl.NG.Jetfightergame.Tools.Vectors.DirVector;
import nl.NG.Jetfightergame.Tools.Vectors.PosVector;
import nl.NG.Jetfightergame.Tools.Vectors.Vector;

/** TODO: allow quads in meshes and optimize CustomShape accordingly
 * @author Geert van Ieperen
 *         created on 29-10-2017.
 */
public class Quad extends Plane {

    /** ABRef, BCRef, CDRef, DARef are four reference vectors for collision detection */
    private PosVector ABRef, BCRef, CDRef, DARef;

    private static PosVector tempAlpha = new PosVector();
    private static PosVector tempBeta = new PosVector();

    /**
     * the vectors must be supplied in counterclockwise ordering
     */
    public Quad(PosVector A, PosVector B, PosVector C, PosVector D, DirVector normal) {
        super(new PosVector[]{A, B, C, D}, normal);

        ABRef = B.sub(A, tempAlpha).cross(D.sub(A, tempBeta), new PosVector());
        BCRef = C.sub(B, tempAlpha).cross(A.sub(B, tempBeta), new PosVector());
        CDRef = D.sub(C, tempAlpha).cross(B.sub(C, tempBeta), new PosVector());
        DARef = A.sub(D, tempAlpha).cross(C.sub(D, tempBeta), new PosVector());
    }

    @Override
    protected boolean encapsules(PosVector hitPos) {
        PosVector A = boundary[0];
        PosVector B = boundary[1];
        PosVector C = boundary[2];
        PosVector D = boundary[3];

        Vector cross = new PosVector();
        B.sub(A, tempAlpha).cross(hitPos.sub(A, tempBeta), cross);

        if (ABRef.dot(cross) >= 0) {
            C.sub(B, tempAlpha).cross(hitPos.sub(B, tempBeta), cross);
            if (BCRef.dot(cross) >= 0) {
                D.sub(C, tempAlpha).cross(hitPos.sub(C, tempBeta), cross);
                if (CDRef.dot(cross) >= 0) {
                    A.sub(D, tempAlpha).cross(hitPos.sub(D, tempBeta), cross);
                    return DARef.dot(cross) >= 0;
                }
            }
        }
        return false;
    }
}
