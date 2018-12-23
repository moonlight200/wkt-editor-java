package wkteditor.ui;

/**
 * The display options define how the wkt elements are drawn within the editor.
 */
public class DisplayOptions {
    private int pointRadius;
    private float lineWidth;

    private double translateX;
    private double translateY;
    private double zoom;

    public DisplayOptions() {
        reset();
    }

    /**
     * Resets all display options.
     */
    public void reset() {
        resetView();
        resetGraphics();
    }

    /**
     * Resets the display options used with the graphics of an element.
     */
    public void resetGraphics() {
        pointRadius = 4;
        lineWidth = 2.0f;
    }

    /**
     * Resets the display options related to the view (transformation) of
     * elements.
     */
    public void resetView() {
        resetTranslation();
        resetZoom();
    }

    /**
     * Resets the zoom.
     */
    public void resetZoom() {
        zoom = 1.0;
    }

    /**
     * Resets the translation.
     */
    public void resetTranslation() {
        translateX = 0;
        translateY = 0;
    }

    /**
     * Gets the radius of a point.
     *
     * @return The radius of a point.
     */
    public int getPointRadius() {
        return pointRadius;
    }

    /**
     * Gets the diameter of a point.
     *
     * @return The diameter of a point.
     */
    public int getPointDiameter() {
        return pointRadius * 2;
    }

    /**
     * Sets the radius of a point.
     *
     * @param pointRadius The new radius.
     */
    public void setPointRadius(int pointRadius) {
        this.pointRadius = pointRadius;
    }

    /**
     * Sets the width of a line.
     *
     * @param lineWidth The new line width.
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Gets the width of a line.
     *
     * @return The new line width.
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the zoom factor.
     *
     * @param zoom The new zoom factor.
     */
    void setZoom(double zoom) {
        this.zoom = zoom;
    }

    double getZoom() {
        return zoom;
    }

    /**
     * Sets the translation.
     *
     * @param x The new translation in x direction.
     * @param y The new translation in y direction.
     */
    void setTranslation(double x, double y) {
        translateX = x;
        translateY = y;
    }

    /**
     * Adds the given translation to the current translation.
     *
     * @param x The difference in translation in x direction.
     * @param y The difference in translation in y direction.
     */
    void setTranslationRelative(double x, double y) {
        translateX += x;
        translateY += y;
    }

    /**
     * Builds a new transform object with the current display options.
     * Subsequent changes to the display options will not affect the transform.
     *
     * @return A new transform object.
     */
    public Transform getTransform() {
        return new Transform(translateX, translateY, zoom);
    }
}
