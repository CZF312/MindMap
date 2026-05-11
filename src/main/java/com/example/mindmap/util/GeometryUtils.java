package com.example.mindmap.util;

import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import javafx.geometry.Point2D;

import java.util.Optional;

public final class GeometryUtils {
    private GeometryUtils() {
    }

    public static boolean geometryChanged(MindMap before, MindMap after) {
        if (before == null || after == null) {
            return false;
        }
        for (MindNode beforeNode : before.allNodes()) {
            Optional<MindNode> afterNode = after.findNodeById(beforeNode.getId());
            if (afterNode.isEmpty()) {
                return true;
            }
            MindNode node = afterNode.get();
            if (Double.compare(beforeNode.getX(), node.getX()) != 0
                    || Double.compare(beforeNode.getY(), node.getY()) != 0
                    || Double.compare(beforeNode.getWidth(), node.getWidth()) != 0
                    || Double.compare(beforeNode.getHeight(), node.getHeight()) != 0
                    || Double.compare(beforeNode.getOffsetX(), node.getOffsetX()) != 0
                    || Double.compare(beforeNode.getOffsetY(), node.getOffsetY()) != 0) {
                return true;
            }
        }
        return before.allNodes().size() != after.allNodes().size();
    }

    public static boolean intersectsNode(MindNode node, double minX, double minY, double maxX, double maxY) {
        return node.getX() <= maxX
                && node.getX() + node.getWidth() >= minX
                && node.getY() <= maxY
                && node.getY() + node.getHeight() >= minY;
    }

    public static boolean intersectsConnection(MindNode child, double minX, double minY, double maxX, double maxY) {
        MindNode parent = child.getParent();
        if (parent == null) {
            return false;
        }
        Point2D parentCenter = new Point2D(parent.getCenterX(), parent.getCenterY());
        Point2D childCenter = new Point2D(child.getCenterX(), child.getCenterY());
        Point2D start = connectionBoundaryPoint(parent, childCenter);
        Point2D end = connectionBoundaryPoint(child, parentCenter);
        if (child.getConnectionStyle().getShape() == ConnectionShape.ELBOW) {
            double midX = (start.getX() + end.getX()) / 2.0;
            Point2D cornerA = new Point2D(midX, start.getY());
            Point2D cornerB = new Point2D(midX, end.getY());
            return segmentIntersectsRect(start, cornerA, minX, minY, maxX, maxY)
                    || segmentIntersectsRect(cornerA, cornerB, minX, minY, maxX, maxY)
                    || segmentIntersectsRect(cornerB, end, minX, minY, maxX, maxY);
        }
        Point2D previous = start;
        for (int i = 1; i <= 24; i++) {
            double t = i / 24.0;
            Point2D current = curvePoint(start, end, t);
            if (segmentIntersectsRect(previous, current, minX, minY, maxX, maxY)) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    static Point2D connectionBoundaryPoint(MindNode node, Point2D toward) {
        double centerX = node.getCenterX();
        double centerY = node.getCenterY();
        double dx = toward.getX() - centerX;
        double dy = toward.getY() - centerY;
        if (Math.abs(dx) < 0.000001 && Math.abs(dy) < 0.000001) {
            return new Point2D(centerX, centerY);
        }
        double scaleX = Math.abs(dx) < 0.000001 ? Double.POSITIVE_INFINITY : (node.getWidth() / 2.0) / Math.abs(dx);
        double scaleY = Math.abs(dy) < 0.000001 ? Double.POSITIVE_INFINITY : (node.getHeight() / 2.0) / Math.abs(dy);
        double scale = Math.min(scaleX, scaleY);
        return new Point2D(centerX + dx * scale, centerY + dy * scale);
    }

    static Point2D curvePoint(Point2D start, Point2D end, double t) {
        double controlOffset = Math.max(80, Math.abs(end.getX() - start.getX()) * 0.5);
        double controlX1 = start.getX() + (end.getX() > start.getX() ? controlOffset : -controlOffset);
        double controlY1 = start.getY();
        double controlX2 = end.getX() + (end.getX() > start.getX() ? -controlOffset : controlOffset);
        double controlY2 = end.getY();
        double inverse = 1 - t;
        double x = Math.pow(inverse, 3) * start.getX()
                + 3 * Math.pow(inverse, 2) * t * controlX1
                + 3 * inverse * Math.pow(t, 2) * controlX2
                + Math.pow(t, 3) * end.getX();
        double y = Math.pow(inverse, 3) * start.getY()
                + 3 * Math.pow(inverse, 2) * t * controlY1
                + 3 * inverse * Math.pow(t, 2) * controlY2
                + Math.pow(t, 3) * end.getY();
        return new Point2D(x, y);
    }

    static boolean segmentIntersectsRect(Point2D a, Point2D b, double minX, double minY, double maxX, double maxY) {
        return pointInRect(a, minX, minY, maxX, maxY)
                || pointInRect(b, minX, minY, maxX, maxY)
                || segmentsIntersect(a, b, new Point2D(minX, minY), new Point2D(maxX, minY))
                || segmentsIntersect(a, b, new Point2D(maxX, minY), new Point2D(maxX, maxY))
                || segmentsIntersect(a, b, new Point2D(maxX, maxY), new Point2D(minX, maxY))
                || segmentsIntersect(a, b, new Point2D(minX, maxY), new Point2D(minX, minY));
    }

    static boolean pointInRect(Point2D point, double minX, double minY, double maxX, double maxY) {
        return point.getX() >= minX && point.getX() <= maxX
                && point.getY() >= minY && point.getY() <= maxY;
    }

    static boolean segmentsIntersect(Point2D a, Point2D b, Point2D c, Point2D d) {
        double epsilon = 0.000001;
        double d1 = direction(c, d, a);
        double d2 = direction(c, d, b);
        double d3 = direction(a, b, c);
        double d4 = direction(a, b, d);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return Math.abs(d1) < epsilon && onSegment(c, d, a)
                || Math.abs(d2) < epsilon && onSegment(c, d, b)
                || Math.abs(d3) < epsilon && onSegment(a, b, c)
                || Math.abs(d4) < epsilon && onSegment(a, b, d);
    }

    private static double direction(Point2D a, Point2D b, Point2D c) {
        return (c.getX() - a.getX()) * (b.getY() - a.getY())
                - (b.getX() - a.getX()) * (c.getY() - a.getY());
    }

    private static boolean onSegment(Point2D a, Point2D b, Point2D c) {
        return Math.min(a.getX(), b.getX()) <= c.getX() && c.getX() <= Math.max(a.getX(), b.getX())
                && Math.min(a.getY(), b.getY()) <= c.getY() && c.getY() <= Math.max(a.getY(), b.getY());
    }
}
