package wkteditor;

import wkteditor.ui.DisplayOptions;
import wkteditor.ui.Transform;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A wkt line.
 */
public class WKTLineString extends WKTElement {
    private LinkedList<WKTPoint> points;

    public WKTLineString() {
        super();
        points = new LinkedList<>();
    }

    public WKTLineString(int x, int y) {
        super(x, y);
    }

    @Override
    public void add(int x, int y) {
        add(new WKTPoint(x, y));
    }

    public void add(WKTPoint point) {
        points.add(point);
    }

    @Override
    public boolean canAdd() {
        return true;
    }

    @Override
    public String toWKT() {
        return "LINESTRING ("
                + points.stream()
                .map(p -> p.getX() + " " + p.getY())
                .collect(Collectors.joining(", "))
                + ")";
    }

    @Override
    public void paint(Graphics2D g, DisplayOptions opt) {
        Transform transform = opt.getTransform();
        Iterator<WKTPoint> iterator = points.iterator();
        WKTPoint prev = null;

        g.setStroke(new BasicStroke(opt.getLineWidth()));
        while (iterator.hasNext()) {
            WKTPoint cur = iterator.next();
            if (prev != null) {
                g.drawLine(transform.transformX(prev.getX()), transform.transformY(prev.getY()),
                        transform.transformX(cur.getX()), transform.transformY(cur.getY()));
            }
            cur.paint(g, opt);

            prev = cur;
        }
    }

    @Override
    public boolean isOnElement(double x, double y, double maxDistance) {
        final double maxDistanceSq = maxDistance * maxDistance;
        WKTPoint prev = null;

        for (WKTPoint cur : points) {
            // Check if on current point
            if (cur.isOnElement(x, y, maxDistance)) {
                return true;
            }

            // Check line segment
            if (prev != null) {
                double distance = Line2D.ptSegDistSq(prev.getX(), prev.getY(), cur.getX(), cur.getY(), x, y);
                if (distance <= maxDistanceSq) {
                    return true;
                }
            }

            prev = cur;
        }

        return false;
    }

    @Override
    public boolean isContainedBy(Rectangle rect) {
        for (WKTPoint point : points) {
            if (!point.isContainedBy(rect)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Rectangle getContainingRect() {
        if (points.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (WKTPoint point : points) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WKTLineString that = (WKTLineString) o;
        return Objects.equals(points, that.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points);
    }

    @Override
    public String toString() {
        return toWKT();
    }
}
