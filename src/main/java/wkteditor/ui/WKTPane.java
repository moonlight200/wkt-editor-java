package wkteditor.ui;

import org.jetbrains.annotations.NotNull;
import wkteditor.CursorMode;
import wkteditor.WKTEditor;
import wkteditor.WKTElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;

/**
 * This pane displays the wkt elements, that are being edited.
 */
public class WKTPane extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener {
    private WKTEditor editor;
    private BufferedImage bgImage;
    private BufferedImage bgImageScaled;

    @NotNull
    private WeakReference<WKTElement> hoverElement;

    private int dragX;
    private int dragY;

    private Rectangle select;

    public WKTPane(WKTEditor editor) {
        this.editor = editor;
        dragX = -1;
        dragY = -1;
        select = new Rectangle(-1, -1, 0, 0);
        hoverElement = new WeakReference<>(null);

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
        WKTElement highlightElement = hoverElement.get();
        for (WKTElement element : editor.getElements()) {
            if (element.equals(highlightElement)) {
                g2d.setColor(dOpt.getHighlightColor());
            } else if (editor.isSelected(element)) {
                g2d.setColor(dOpt.getSelectedColor());
            } else {
                g2d.setColor(getForeground());
            }

            element.paint(g2d, dOpt);
        }

        // Selection
        if (select.x >= 0 && select.y >= 0) {
            final int x = select.x + Math.min(select.width, 0);
            final int y = select.y + Math.min(select.height, 0);
            final int w = Math.abs(select.width);
            final int h = Math.abs(select.height);

            g2d.setColor(dOpt.getSelectionColor());
            g2d.fillRect(x, y, w, h);
            g2d.setColor(dOpt.getSelectionBorderColor());
            g2d.drawRect(x, y, w, h);
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
        if (event.getButton() == MouseEvent.BUTTON1) {
            if (editor.getCursorMode() == CursorMode.SELECT) {
                select.x = event.getX();
                select.y = event.getY();
            }
        } else if (event.getButton() == MouseEvent.BUTTON3) {
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
        if (event.getButton() == MouseEvent.BUTTON1) {
            if (editor.getCursorMode() != CursorMode.SELECT || select.x < 0 || select.y < 0 || (select.x == event.getX() && select.y == event.getY())) {
                select.x = -1;
                select.y = -1;
                return;
            }

            Transform transform = editor.getDisplayOptions().getTransform();
            editor.updateSelection(new Rectangle(
                    transform.reverseTransformX(select.x + Math.min(select.width, 0)),
                    transform.reverseTransformY(select.y + Math.min(select.height, 0)),
                    transform.reverseZoom(Math.abs(select.width)),
                    transform.reverseZoom(Math.abs(select.height))));

            select.x = -1;
            select.y = -1;
            repaint();
        } else if (event.getButton() == MouseEvent.BUTTON3) {
            dragX = -1;
            dragY = -1;
        }
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
        if (event.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
            if (select.x < 0 || select.y < 0) {
                return;
            }

            select.width = event.getX() - select.x;
            select.height = event.getY() - select.y;
            repaint();
        } else if (event.getModifiersEx() == MouseEvent.BUTTON3_DOWN_MASK) {
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
        Transform transform = editor.getDisplayOptions().getTransform();

        WKTElement element = editor.getSelectedElement(
                transform.reverseTransformX(event.getX()),
                transform.reverseTransformY(event.getY()));
        WKTElement hover = hoverElement.get();

        if (element == null) {
            if (hover != null) {
                hoverElement = new WeakReference<>(null);
                repaint();
            }
        } else {
            if (!element.equals(hover)) {
                hoverElement = new WeakReference<>(element);
                repaint();
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        zoom(-event.getPreciseWheelRotation(), event.getX(), event.getY());
    }
}
