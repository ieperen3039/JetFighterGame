package nl.NG.Jetfightergame.Rendering;

import nl.NG.Jetfightergame.Tools.Vectors.Color4f;

/**
 * Materials that can be used for the robots.
 */
public enum Material {

    /**
     * standard material for non-metals
     */
    ROUGH(
            new Color4f(0.9f, 0.9f, 0.9f, 1),
            new Color4f(0f, 0f, 0f, 0.1f),
            new Color4f(0.3f, 0.3f, 0.3f, 1),
            1f
    ),

    /**
     * material for glowing materials like light-sources
     */
    GLOWING(
            Color4f.WHITE,
            Color4f.BLACK,
            Color4f.INVISIBLE,
            1f
    ),

    /**
     * Gold material properties.
     */
    GOLD(
            new Color4f(0.82f, 0.76f, 0, 1),
            new Color4f(0.95f, 0.92f, 0.5f, 1),
            Color4f.WHITE,
            75
    ),

    /**
     * Gray material that shines blueish
     */
    SILVER(
            new Color4f(0.75f, 0.75f, 0.75f, 1),
            new Color4f(0.90f, 0.90f, 1.0f, 1),
            Color4f.WHITE,
            50
    ),

    /**
     * regular non-shiny material properties.
     */
    PLASTIC(
            Color4f.WHITE,
            Color4f.GREY,
            Color4f.BLACK,
            2
    ),

    GLASS(
            new Color4f(1, 1, 1, 0f),
            new Color4f(1, 1, 1, 0f),
            Color4f.WHITE,
            20
    ),

    /**
     * Rough, brown material
     */
    WOOD(
            new Color4f(0.5f, 0.27f, 0.14f, 1),
            new Color4f(0.02f, 0.04f, 0.06f, 1),
            new Color4f(0.75f, 0.375f, 0.20f, 1),
            0.5f
    );

    private static final Material[] VALUES = values();

    /**
     * The material natural color.
     */
    public final Color4f diffuse;
    /**
     * The specular reflectance of the material.
     */
    public final Color4f specular;
    /**
     * color of the lines of the material
     */
    public final Color4f lineColor;
    /**
     * The specular exponent of the material.
     */
    public final float shininess;

    Material(Color4f diffuse, Color4f specular, Color4f lineColor, float shininess) {
        this.diffuse = diffuse;
        this.specular = specular;
        this.lineColor = lineColor;
        this.shininess = shininess;
    }

    /**
     * @param id a number n corresponing to an enum ordinal
     * @return the enum e such that {@code e.ordinal() == n}
     * @throws IllegalArgumentException if the id does not correspond to a valid message
     */
    public static Material get(int id) {
        if (id >= VALUES.length) throw new IllegalArgumentException("Invalid material identifier " + id);
        else return VALUES[id];
    }
}
