package wkteditor.ui;

import wkteditor.WKTEditor;
import wkteditor.WKTElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * This pane displays the wkt elements, that are being edited.
 */
public class WKTPane extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener {
    private WKTEditor editor;
    private BufferedImage bgImage;
    private BufferedImage bgImageScaled;

    private int dragX;
    private int dragY;

    public WKTPane(WKTEditor editor) {
        this.editor = editor;
        dragX = -1;
        dragY = -1;

        setPreferredSize(new Dimension(200, 200));
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        editor.getDisplayOptions().addChangeListener(new DisplayOptions.ChangeListener() {
            @Override
            public void pointRadiusChanged(int oldRadius, int newRadius) {

            }

            @Override
            public void lineWidthChanged(float oldWidth, float newWidth) {

            }

            @Override
            public void translationChanged(double oldX, double oldY, double newX, double newY) {

            }

            @Override
            public void zoomChanged(double oldZoom, double newZoom) {
                scaleBackgroundImage(newZoom);
            }
        });
    }

    /**
     * Sets an image that is displayed in the background of the wkt elements.
     *
     * @param image The new image to display, or <code>null</code> to remove the
     *              image.
     */
    public void setBackgroundImage(BufferedImage image) {
        bgImage = image;
        scaleBackgroundImage(editor.getDisplayOptions().getZoom());
        repaint();
    }

    /**
     * Scales the background image and caches the result.
     *
     * @param scale The scaled to apply to the background image.
     */
    private void scaleBackgroundImage(double scale) {
        if (bgImage == null) {
            bgImageScaled = null;
            return;
        }

        final int scaledWidth = (int) (bgImage.getWidth() * scale);
        final int scaledHeight = (int) (bgImage.getHeight() * scale);

        bgImageScaled = new BufferedImage(scaledWidth, scaledHeight, bgImage.getType());
        Graphics2D g2d = bgImageScaled.createGraphics();
        AffineTransform affineTransform = AffineTransform.getScaleInstance(scale, scale);
        g2d.drawRenderedImage(bgImage, affineTransform);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        DisplayOptions dOpt = editor.getDisplayOptions();
        Transform transform = dOpt.getTransform();

        // Background
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (bgImageScaled != null) {
            g2d.drawImage(bgImageScaled, transform.transformX(0), transform.transformY(0),
                    null);
        }

        // Foreground
        g2d.setColor(getForeground());
        for (WKTElement element : editor.getElements()) {
            element.paint(g2d, dOpt);
        }

        WKTElement curElement = editor.getCurrentElement();
        if (curElement != null) {
            g2d.setColor(new Color(255, 95, 74));
            curElement.paint(g2d, dOpt);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1) {
            Transform transform = editor.getDisplayOptions().getTransform();
            editor.addPoint(transform.reverseTransformX(event.getX()),
                    transform.reverseTransformY(event.getY()));
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            dragX = event.getX();
            dragY = event.getY();
        }
    }

    /**
     * Zooms the current view for the specified difference at the center of the WKTPane.
     *
     * @param diff The difference to zoom in (positive for zooming in, negative for zooming out).
     */
    public void zoom(double diff) {
        zoom(diff, getWidth() / 2, getHeight() / 2);
    }

    /**
     * Zooms the current view for the specified difference at the given coordinates.
     *
     * @param diff The difference to zoom in (positive for zooming in, negative for zooming out).
     * @param x    The x-coordinate of the center of the zooming.
     * @param y    The y-coordinate of the center of the zooming.
     */
    public void zoom(double diff, int x, int y) {
        DisplayOptions dOpt = editor.getDisplayOptions();
        Transform tfBeforeZoom = dOpt.getTransform();

        double zoom = dOpt.getZoom();
        zoom += diff * 0.2;
        if (zoom < 0.1) {
            zoom = 0.1;
        }
        if (zoom > 5.0) {
            zoom = 5.0;
        }
        dOpt.setZoom(zoom);

        // Calculate offset caused by zoom
        Transform tfAfterZoom = dOpt.getTransform();
        dOpt.setTranslationRelative(x - tfAfterZoom.transformX(tfBeforeZoom.reverseTransformX(x)),
                y - tfAfterZoom.transformY(tfBeforeZoom.reverseTransformY(y)));

        repaint();
    }

    /**
     * Resets the zoom to the default value.
     */
    public void resetZoom() {
        DisplayOptions dOpt = editor.getDisplayOptions();

        dOpt.resetZoom();
        repaint();
    }

    /**
     * Resets the view to its default values.
     */
    public void resetView() {
        DisplayOptions dOpt = editor.getDisplayOptions();

        dOpt.reset();
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        dragX = -1;
        dragY = -1;
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        // Ignored
    }

    @Override
    public void mouseExited(MouseEvent event) {
        // Ignored
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (event.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK) {
            if (dragX > 0 && dragY > 0) {
                editor.getDisplayOptions()
                        .setTranslationRelative(event.getX() - dragX, event.getY() - dragY);
            }
            dragX = event.getX();
            dragY = event.getY();
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        // Ignored
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        zoom(-event.getPreciseWheelRotation(), event.getX(), event.getY());
    }
}
