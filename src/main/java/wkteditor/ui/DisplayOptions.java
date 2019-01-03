package wkteditor.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The display options define how the wkt elements are drawn within the editor.
 */
public class DisplayOptions {
    private static final int DEFAULT_POINT_RADIUS = 4;
    private static final float DEFAULT_LINE_WIDTH = 2.0f;
    private static final Color DEFAULT_COLOR_SELECT = new Color(255, 95, 74);
    private static final Color DEFAULT_COLOR_HIGHLIGHT = new Color(255, 166, 154);
    private static final double DEFAULT_TRANSLATE_X = 0.0;
    private static final double DEFAULT_TRANSLATE_Y = 0.0;
    private static final double DEFAULT_ZOOM = 1.0;

    private int pointRadius;
    private float lineWidth;
    private Color colorSelect;
    private Color colorHighlight;

    private double translateX;
    private double translateY;
    private double zoom;

    private List<ChangeListener> changeListeners;

    public DisplayOptions() {
        changeListeners = new ArrayList<>();

        pointRadius = DEFAULT_POINT_RADIUS;
        lineWidth = DEFAULT_LINE_WIDTH;
        colorSelect = DEFAULT_COLOR_SELECT;
        colorHighlight = DEFAULT_COLOR_HIGHLIGHT;
        translateX = DEFAULT_TRANSLATE_X;
        translateY = DEFAULT_TRANSLATE_Y;
        zoom = DEFAULT_ZOOM;
    }

    /**
     * Add a listener, that will be notified whenever one of the values changes.
     *
     * @param listener The listener to be notified.
     */
    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes the listener, that is being notified whenever one of the values
     * changes.
     *
     * @param listener The listener to remove.
     */
    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
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
        setPointRadius(DEFAULT_POINT_RADIUS);
        setLineWidth(DEFAULT_LINE_WIDTH);
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
        setZoom(DEFAULT_ZOOM);
    }

    /**
     * Resets the translation.
     */
    public void resetTranslation() {
        setTranslation(DEFAULT_TRANSLATE_X, DEFAULT_TRANSLATE_Y);
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
     * Gets the color used for a highlighted element, such as when the cursor
     * hovers over an element.
     *
     * @return The color for highlighted elements.
     */
    public Color getHighlightColor() {
        return colorHighlight;
    }

    /**
     * Gets the color used for the selected element.
     *
     * @return The color for the selected element.
     */
    public Color getSelectColor() {
        return colorSelect;
    }

    /**
     * Sets the radius of a point.
     *
     * @param radius The new radius.
     */
    public void setPointRadius(int radius) {
        final int old = pointRadius;
        pointRadius = radius;

        notifyPointRadiusChange(old, pointRadius);
    }

    /**
     * Sets the width of a line.
     *
     * @param width The new line width.
     */
    public void setLineWidth(float width) {
        final float old = lineWidth;
        lineWidth = width;

        notifyLineWidthChange(old, lineWidth);
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
        final double old = this.zoom;
        this.zoom = zoom;

        notifyZoomChange(old, this.zoom);
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
        final double oldX = translateX;
        final double oldY = translateY;
        translateX = x;
        translateY = y;

        notifyTranslationChange(oldX, oldY, translateX, translateY);
    }

    /**
     * Adds the given translation to the current translation.
     *
     * @param x The difference in translation in x direction.
     * @param y The difference in translation in y direction.
     */
    void setTranslationRelative(double x, double y) {
        final double oldX = translateX;
        final double oldY = translateY;
        translateX += x;
        translateY += y;

        notifyTranslationChange(oldX, oldY, translateX, translateY);
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

    /**
     * Notifies all listeners, that the point radius value has changed.
     *
     * @param oldRadius The old point radius.
     * @param newRadius The new point radius.
     */
    private void notifyPointRadiusChange(final int oldRadius, final int newRadius) {
        if (oldRadius == newRadius) {
            // Nothing changed, no need to notify
            return;
        }

        for (ChangeListener listener : changeListeners) {
            listener.pointRadiusChanged(oldRadius, newRadius);
        }
    }

    /**
     * Notifies all listeners, that the line width value has changed.
     *
     * @param oldWidth The old line width.
     * @param newWidth The new line width.
     */
    private void notifyLineWidthChange(final float oldWidth, final float newWidth) {
        if (oldWidth == newWidth) {
            // Nothing changed, no need to notify
            return;
        }

        for (ChangeListener listener : changeListeners) {
            listener.lineWidthChanged(oldWidth, newWidth);
        }
    }

    /**
     * Notifies all listeners, that the translation values have changed.
     *
     * @param oldX The old translation in horizontal direction.
     * @param oldY The old translation in vertical direction.
     * @param newX The new translation in horizontal direction.
     * @param newY The new translation in vertical direction.
     */
    private void notifyTranslationChange(final double oldX, final double oldY,
                                         final double newX, final double newY) {
        if (oldX == newX && oldY == newY) {
            // Nothing changed, no need to notify
            return;
        }

        for (ChangeListener listener : changeListeners) {
            listener.translationChanged(oldX, oldY, newX, newY);
        }
    }

    /**
     * Notifies all listeners, that the zoom value has changed.
     *
     * @param oldZoom The old zoom.
     * @param newZoom The new zoom.
     */
    private void notifyZoomChange(final double oldZoom, final double newZoom) {
        if (oldZoom == newZoom) {
            // Nothing changed, no need to notify
            return;
        }

        for (ChangeListener listener : changeListeners) {
            listener.zoomChanged(oldZoom, newZoom);
        }
    }

    /**
     * Will be notified whenever a variable changes.
     */
    public interface ChangeListener {
        /**
         * Called when the value of the point radius changes.
         *
         * @param oldRadius The old point radius.
         * @param newRadius The new point radius.
         */
        void pointRadiusChanged(int oldRadius, int newRadius);

        /**
         * Called when the value of the line width changes.
         *
         * @param oldWidth The old line width.
         * @param newWidth The new line width.
         */
        void lineWidthChanged(float oldWidth, float newWidth);

        /**
         * Called when the translation values change.
         *
         * @param oldX The old translation in horizontal direction.
         * @param oldY The old translation in vertical direction.
         * @param newX The new translation in horizontal direction.
         * @param newY The new translation in vertical direction.
         */
        void translationChanged(double oldX, double oldY, double newX, double newY);

        /**
         * Called when the value of the zoom changes.
         *
         * @param oldZoom The old zoom.
         * @param newZoom The new zoom.
         */
        void zoomChanged(double oldZoom, double newZoom);
    }
}
