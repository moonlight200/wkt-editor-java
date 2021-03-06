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
 * A wkt polygon.
 */
public class WKTPolygon extends WKTElement {
    private LinkedList<LinkedList<WKTPoint>> subPolygons;

    public WKTPolygon() {
        super();
        subPolygons = new LinkedList<>();
    }

    @Override
    public void add(int x, int y) {
        add(new WKTPoint(x, y));
    }

    @Override
    public boolean canAdd() {
        return true;
    }

    /**
     * Adds the point to the current sub-polygon. If no sub-polygon exists, a
     * new one will be created.
     *
     * @param p The point to add.
     */
    public void add(WKTPoint p) {
        LinkedList<WKTPoint> subPolygon;
        if (subPolygons.isEmpty()) {
            subPolygon = new LinkedList<>();
            subPolygons.add(subPolygon);
        } else {
            subPolygon = subPolygons.getLast();
        }

        subPolygon.add(p);
    }

    @Override
    public void endSubElement() {
        subPolygons.add(new LinkedList<>());
    }

    @Override
    public String toWKT() {
        return "POLYGON (" +
                subPolygons.stream()
                        .map(subPoly -> {
                            String result = "(";
                            result += subPoly.stream()
                                    .map(p -> p.getX() + " " + p.getY())
                                    .collect(Collectors.joining(", "));
                            if (!subPoly.isEmpty()) {
                                WKTPoint first = subPoly.getFirst();
                                result += ", " + first.getX() + " " + first.getY();
                            }
                            result += ")";
                            return result;
                        }).collect(Collectors.joining(", ")) +
                ")";
    }

    @Override
    public void paint(Graphics2D g, DisplayOptions opt) {
        BasicStroke strokeNormal = new BasicStroke(opt.getLineWidth());
        BasicStroke strokeDashed = new BasicStroke(opt.getLineWidth(),
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f,
                new float[]{opt.getLineWidth() * 2.0f}, 0.0f);
        Transform transform = opt.getTransform();

        Iterator<LinkedList<WKTPoint>> subPolyIterator = subPolygons.iterator();
        while (subPolyIterator.hasNext()) {
            LinkedList<WKTPoint> subPoly = subPolyIterator.next();

            Iterator<WKTPoint> pointIterator = subPoly.iterator();
            WKTPoint prev = null;

            g.setStroke(strokeNormal);
            while (pointIterator.hasNext()) {
                WKTPoint cur = pointIterator.next();
                if (prev != null) {
                    g.drawLine(transform.transformX(prev.getX()), transform.transformY(prev.getY()),
                            transform.transformX(cur.getX()), transform.transformY(cur.getY()));
                }
                cur.paint(g, opt);

                prev = cur;
            }

            if (subPoly.size() > 2) {
                g.setStroke(strokeDashed);
                WKTPoint first = subPoly.getFirst();
                WKTPoint last = subPoly.getLast();

                g.drawLine(transform.transformX(first.getX()), transform.transformY(first.getY()),
                        transform.transformX(last.getX()), transform.transformY(last.getY()));
            }
        }

        g.setStroke(strokeNormal);
    }

    @Override
    public boolean isOnElement(double x, double y, double maxDistance) {
        final double maxDistanceSq = maxDistance * maxDistance;

        for (LinkedList<WKTPoint> subPoly : subPolygons) {
            WKTPoint prev = null;

            for (WKTPoint cur : subPoly) {
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
        }

        return false;
    }

    @Override
    public boolean isContainedBy(Rectangle rect) {
        for (LinkedList<WKTPoint> subPoly : subPolygons) {
            for (WKTPoint point : subPoly) {
                if (!point.isContainedBy(rect)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Rectangle getContainingRect() {
        if (subPolygons.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (LinkedList<WKTPoint> subPoly : subPolygons) {
            for (WKTPoint point : subPoly) {
                minX = Math.min(minX, point.getX());
                minY = Math.min(minY, point.getY());
                maxX = Math.max(maxX, point.getX());
                maxY = Math.max(maxY, point.getY());
            }
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
        WKTPolygon that = (WKTPolygon) o;
        return Objects.equals(subPolygons, that.subPolygons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subPolygons);
    }

    @Override
    public String toString() {
        return toWKT();
    }
}
