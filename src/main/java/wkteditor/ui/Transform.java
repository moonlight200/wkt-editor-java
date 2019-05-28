package wkteditor.ui;

/**
 * Calculates the translation and zoom.
 */
public class Transform {
    private final double translateX;
    private final double translateY;
    private final double zoom;

    Transform(double translateX, double translateY, double zoom) {
        this.translateX = translateX;
        this.translateY = translateY;
        this.zoom = zoom;
    }

    /**
     * Transforms the given x-coordinate according to the current settings.
     *
     * @param x The x-coordinate to transform.
     * @return The transformed coordinate.
     */
    public int transformX(int x) {
        return (int) ((x + translateX) * zoom);
    }

    /**
     * Transforms the given y-coordinate according to the current settings.
     *
     * @param y The y-coordinate to transform.
     * @return The transformed coordinate.
     */
    public int transformY(int y) {
        return (int) ((y + translateY) * zoom);
    }

    /**
     * Zooms the given value.
     *
     * @param i The value to zoom.
     * @return The zoomed value.
     */
    public int zoom(int i) {
        return (int) (i * zoom);
    }

    /**
     * Reverts any changes done to i.
     *
     * @param i The value to reverse zoom.
     * @return The normal i.
     */
    public int reverseZoom(int i) {
        return (int) (i / zoom);
    }

    /**
     * Reverts any transformations done to x.
     *
     * @param x The x-coordinate to reverse transform.
     * @return The normal x-coordinate.
     */
    int reverseTransformX(int x) {
        return (int) ((((double) x) / zoom) - translateX);
    }

    /**
     * Reverts any transformations done to y.
     *
     * @param y The y-coordinate to reverse transform.
     * @return The normal y-coordinate.
     */
    int reverseTransformY(int y) {
        return (int) ((((double) y) / zoom) - translateY);
    }
}
