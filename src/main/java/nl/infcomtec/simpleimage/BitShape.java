package nl.infcomtec.simpleimage;

import java.awt.geom.Point2D;
import java.util.BitSet;

/**
 * Tiny class to hold pixel membership. Incomparably faster then Area.
 *
 * @author Walter Stroebel
 */
public class BitShape {

    private final BitSet bs = new BitSet();
    private final int W;

    public BitShape(int W) {
        this.W = W;
    }

    /**
     * @param p The point, java.awt.Point is also a Point2D.
     * @return true if p is inside the shape.
     */
    public final boolean contains(Point2D p) {
        int x = (int) (Math.round(p.getX()));
        int y = (int) (Math.round(p.getY()));
        return contains(x, y);
    }

    /**
     * @param x
     * @param y
     * @return true if (x,y) is inside the shape.
     */
    public final boolean contains(int x, int y) {
        return bs.get(y * W + x);
    }

    /**
     * Just for semantics == contains().
     *
     * @param p The point, java.awt.Point is also a Point2D.
     * @return true if p is inside the shape.
     */
    public final boolean get(Point2D p) {
        return contains(p);
    }

    /**
     * Add a point.
     *
     * @param x
     * @param y
     */
    public final void set(int x, int y) {
        bs.set(y * W + x);
    }

    /**
     * Remove a point.
     *
     * @param x
     * @param y
     */
    public final void reset(int x, int y) {
        bs.clear(y * W + x);
    }

    /**
     * Add a point.
     *
     * @param p The point, java.awt.Point is also a Point2D.
     */
    public final void set(Point2D p) {
        int x = (int) (Math.round(p.getX()));
        int y = (int) (Math.round(p.getY()));
        bs.set(y * W + x);
    }

    /**
     * Remove a point.
     *
     * @param p The point, java.awt.Point is also a Point2D.
     */
    public final void reset(Point2D p) {
        int x = (int) (Math.round(p.getX()));
        int y = (int) (Math.round(p.getY()));
        bs.clear(y * W + x);
    }

    /**
     * Just for semantics == reset().
     *
     * @param p The point, java.awt.Point is also a Point2D.
     */
    public final void clear(Point2D p) {
        reset(p);
    }
}
