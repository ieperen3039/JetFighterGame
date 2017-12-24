package nl.NG.Jetfightergame.Engine.GLMatrix;

import nl.NG.Jetfightergame.Camera.Camera;
import nl.NG.Jetfightergame.Engine.Settings;
import nl.NG.Jetfightergame.Shaders.Material;
import nl.NG.Jetfightergame.Shaders.ShaderProgram;
import nl.NG.Jetfightergame.ShapeCreators.ShapeDefinitions.GeneralShapes;
import nl.NG.Jetfightergame.Vectors.Color4f;
import nl.NG.Jetfightergame.Vectors.DirVector;
import nl.NG.Jetfightergame.Vectors.PosVector;
import nl.NG.Jetfightergame.Vectors.Vector;
import org.joml.*;

import java.util.Stack;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Geert van Ieperen
 *         created on 16-11-2017.
 */
public class ShaderUniformGL implements GL2 {

    private Stack<Matrix4f> matrixStack;

    private Matrix4f modelMatrix;
    private Matrix4f viewProjectionMatrix;
    private Matrix3f normalMatrix = new Matrix3f();

    private ShaderProgram currentShader;
    private int nextLightIndex = 0;

    public ShaderUniformGL(ShaderProgram shader, int windowWidth, int windowHeight, Camera camera) {
        currentShader = shader;

        matrixStack = new Stack<>();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, windowWidth, windowHeight);

        viewProjectionMatrix = getProjection(windowWidth, windowHeight, camera);
        modelMatrix = new Matrix4f();

        viewProjectionMatrix.assumePerspective();
        modelMatrix.assumeAffine();
    }

    private Matrix4f getProjection(float windowWidth, float windowHeight, Camera camera) {
        Matrix4f vpMatrix = new Matrix4f();

        // Set the perspective.
        float aspectRatio = windowWidth / windowHeight;
        vpMatrix.setPerspective(Settings.FOV, aspectRatio, Settings.Z_NEAR, Settings.Z_FAR);

        // Update the view
        vpMatrix.lookAt(
                camera.getEye(),
                camera.getFocus(),
                camera.getUpVector()
        );
        return vpMatrix;
    }

    @Override
    public void draw(Renderable object) {
        currentShader.setProjectionMatrix(viewProjectionMatrix);
        currentShader.setModelMatrix(modelMatrix);
        modelMatrix.normal(normalMatrix);
        currentShader.setNormalMatrix(normalMatrix);
        object.render(new Painter());
    }

    @Override
    public void setLight(DirVector dir, Color4f lightColor){
        this.setLight(dir.toPosVector(), lightColor);
    }

    @Override
    public void setLight(PosVector pos, Color4f lightColor){
        Vector3f mPosition = new Vector3f(pos);
        mPosition.mulPosition(modelMatrix);
        currentShader.setPointLight(nextLightIndex++, mPosition, lightColor);

        setMaterial(Material.GLOWING, lightColor);
        pushMatrix();
        translate(pos);
        scale(0.1f);
        draw(GeneralShapes.INVERSE_CUBE);
        popMatrix();
    }

    @Override
    public void setMaterial(Material material, Color4f color){
        currentShader.setMaterial(material, color);
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        rotate(new AxisAngle4f(angle, x, y, z));
    }

    public void rotate(AxisAngle4f rotation){
        modelMatrix.rotate(rotation);
    }

    @Override
    public void translate(float x, float y, float z) {
        modelMatrix.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        modelMatrix.scale(x, y, z);
    }

    @Override
    public PosVector getPosition(PosVector p) {
        PosVector result = new PosVector();
        p.mulPosition(modelMatrix, result);
        return result;
    }

    @Override
    public DirVector getDirection(DirVector v) {
        DirVector result = new DirVector();
        v.mulDirection(modelMatrix, result);
        return result;
    }

    @Override
    public void pushMatrix() {
        matrixStack.push(new Matrix4f(modelMatrix));
    }

    @Override
    public void popMatrix() {
        modelMatrix = matrixStack.pop();
    }

    @Override
    public void rotate(Quaternionf rotation) {
        modelMatrix.rotate(rotation);
    }

    @Override
    public void translate(Vector v) {
        modelMatrix.translate(v);
    }

    @Override
    public void multiplyAffine(Matrix4f preTransformation) {
        // first apply combinedTransformation, then the viewTransformation
        preTransformation.mul(modelMatrix, modelMatrix);
    }

    @Override
    public void popAll() {
        modelMatrix = new Matrix4f();
        matrixStack = new Stack<>();
    }

    @Override
    public String toString() {
        return "ShaderUniformGL{\n" +
                "modelMatrix=" + modelMatrix +
                ", viewProjectionMatrix=" + viewProjectionMatrix +
                ", normalMatrix=" + normalMatrix +
                ", currentShader=" + currentShader.getClass() +
                ", stackSize=" + matrixStack.size() +
                "\n}";
    }
}
