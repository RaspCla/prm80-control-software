/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package f4fez.prm80x0.gui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.plaf.UIResource;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/**
 * Zoom of the whole user interface.
 * <p>
 * Fonts and the standard Swing components are zoomed by FlatLaf.
 * This class additionally zooms everything the NetBeans GUI builder
 * writes as fixed pixel values:
 * <ul>
 * <li>positions and sizes in an {@link AbsoluteLayout}
 *     (the layout is replaced by a {@link ZoomAbsoluteLayout})</li>
 * <li>preferred / minimum / maximum sizes set with setPreferredSize() etc.</li>
 * <li>fonts set explicitly (e.g. bold labels, LCD font)</li>
 * </ul>
 * Usage: call {@code Zoom.install(this)} in the constructor of every
 * window / dialog after {@code initComponents()}. The forms stay unchanged,
 * so they can still be edited with the GUI builder.
 */
public final class Zoom {

    public static final float MIN = 0.75f;
    public static final float MAX = 4f;
    private static final float[] STEPS = {0.75f, 0.9f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f};

    // client properties holding the unzoomed values
    private static final String BASE_PREF = "zoom.basePreferredSize";
    private static final String BASE_MIN = "zoom.baseMinimumSize";
    private static final String BASE_MAX = "zoom.baseMaximumSize";
    private static final String BASE_FONT = "zoom.baseFontSize";
    private static final String BASE_VIEWPORT = "zoom.baseViewportSize";

    // for windows (they are no JComponents and have no client properties)
    private static final Map<Window, Dimension> windowBaseMin = new java.util.WeakHashMap<Window, Dimension>();

    private Zoom() {
    }

    /** Current zoom factor, 1 = 100 %. */
    public static float get() {
        return UIScale.getZoomFactor();
    }

    /** Limit a zoom factor to the allowed range and round it to 1 %. */
    public static float clamp(float zoom) {
        zoom = Math.max(MIN, Math.min(MAX, zoom));
        return Math.round(zoom * 100f) / 100f;
    }

    /** Next greater zoom step. */
    public static float stepUp(float zoom) {
        for (float s : STEPS) {
            if (s > zoom + 0.001f) {
                return s;
            }
        }
        return MAX;
    }

    /** Next smaller zoom step. */
    public static float stepDown(float zoom) {
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (STEPS[i] < zoom - 0.001f) {
                return STEPS[i];
            }
        }
        return MIN;
    }

    /**
     * Change the zoom of all open windows.
     * @return true if the zoom factor has changed
     */
    public static boolean set(float zoom) {
        zoom = clamp(zoom);
        if (!UIScale.setZoomFactor(zoom)) {
            return false;
        }
        FlatLaf.updateUI();
        for (Window w : Window.getWindows()) {
            apply(w);
            w.validate();
            w.repaint();
        }
        return true;
    }

    /**
     * Prepare a newly created window for zooming and zoom it to the current
     * factor. Call after initComponents().
     */
    public static void install(Window window) {
        apply(window);
        window.pack();
    }

    /** Zoom a component and all its children to the current zoom factor. */
    public static void apply(Component c) {
        float zoom = get();
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            scaleSize(jc, zoom);
            scaleFont(jc, zoom);
            if (jc instanceof JTable) {
                // tables have a fixed default size of 450 x 400 pixels
                JTable table = (JTable) jc;
                Dimension base = (Dimension) table.getClientProperty(BASE_VIEWPORT);
                if (base == null) {
                    base = table.getPreferredScrollableViewportSize();
                    table.putClientProperty(BASE_VIEWPORT, base);
                }
                table.setPreferredScrollableViewportSize(scale(base, zoom));
            }
        } else if (c instanceof Window) {
            Window w = (Window) c;
            if (w.isMinimumSizeSet() && !windowBaseMin.containsKey(w)) {
                windowBaseMin.put(w, w.getMinimumSize());
            }
            Dimension base = windowBaseMin.get(w);
            if (base != null) {
                w.setMinimumSize(scale(base, zoom));
            }
        }
        if (c instanceof Container) {
            Container ct = (Container) c;
            LayoutManager lm = ct.getLayout();
            if (lm instanceof AbsoluteLayout && !(lm instanceof ZoomAbsoluteLayout)) {
                ct.setLayout(new ZoomAbsoluteLayout((AbsoluteLayout) lm));
            }
            for (Component child : ct.getComponents()) {
                apply(child);
            }
            ct.invalidate();
        }
    }

    private static void scaleSize(JComponent c, float zoom) {
        // remember the unzoomed sizes the first time
        if (c.isPreferredSizeSet() && c.getClientProperty(BASE_PREF) == null) {
            c.putClientProperty(BASE_PREF, c.getPreferredSize());
        }
        if (c.isMinimumSizeSet() && c.getClientProperty(BASE_MIN) == null) {
            c.putClientProperty(BASE_MIN, c.getMinimumSize());
        }
        if (c.isMaximumSizeSet() && c.getClientProperty(BASE_MAX) == null) {
            c.putClientProperty(BASE_MAX, c.getMaximumSize());
        }
        Dimension d = (Dimension) c.getClientProperty(BASE_PREF);
        if (d != null) {
            c.setPreferredSize(scale(d, zoom));
        }
        d = (Dimension) c.getClientProperty(BASE_MIN);
        if (d != null) {
            c.setMinimumSize(scale(d, zoom));
        }
        d = (Dimension) c.getClientProperty(BASE_MAX);
        if (d != null) {
            c.setMaximumSize(scale(d, zoom));
        }
    }

    /**
     * Fonts set by the program (not by the look and feel) are not zoomed by
     * FlatLaf. They are created from the zoomed default font (GUI builder) or
     * multiplied by the zoom factor (see PRM80X0View), so their unzoomed size
     * is size / zoom.
     */
    private static void scaleFont(JComponent c, float zoom) {
        Font f = c.getFont();
        if (f == null || f instanceof UIResource) {
            return;
        }
        Float base = (Float) c.getClientProperty(BASE_FONT);
        if (base == null) {
            base = f.getSize2D() / zoom;
            c.putClientProperty(BASE_FONT, base);
        }
        float size = base * zoom;
        if (Math.abs(size - f.getSize2D()) > 0.01f) {
            c.setFont(f.deriveFont(size));
        }
    }

    static Dimension scale(Dimension d, float zoom) {
        return new Dimension(scale(d.width, zoom), scale(d.height, zoom));
    }

    static int scale(int value, float zoom) {
        // keep "unlimited" values (e.g. Short.MAX_VALUE as maximum size)
        if (value >= Short.MAX_VALUE) {
            return value;
        }
        return Math.round(value * zoom);
    }

    /**
     * AbsoluteLayout of the NetBeans GUI builder, but positions and fixed
     * sizes are multiplied by the current zoom factor.
     */
    public static class ZoomAbsoluteLayout extends AbsoluteLayout {
        private static final long serialVersionUID = 1L;

        /** Take over the components and constraints of an existing AbsoluteLayout. */
        @SuppressWarnings("unchecked")
        ZoomAbsoluteLayout(AbsoluteLayout original) {
            try {
                Field f = AbsoluteLayout.class.getDeclaredField("constraints");
                f.setAccessible(true);
                this.constraints = new Hashtable<Object, Object>((Hashtable<Object, Object>) f.get(original));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                Logger.getLogger(Zoom.class.getName()).log(Level.SEVERE, "Cannot read AbsoluteLayout constraints", ex);
            }
        }

        private Dimension size(Component c, AbsoluteConstraints ac, float zoom, boolean preferred) {
            Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
            int w = ac.width == -1 ? d.width : scale(ac.width, zoom);
            int h = ac.height == -1 ? d.height : scale(ac.height, zoom);
            return new Dimension(w, h);
        }

        private Dimension layoutSize(Container parent, boolean preferred) {
            float zoom = get();
            int maxX = 0;
            int maxY = 0;
            for (Component c : parent.getComponents()) {
                AbsoluteConstraints ac = (AbsoluteConstraints) this.constraints.get(c);
                if (ac == null || !c.isVisible()) {
                    continue;
                }
                Dimension d = size(c, ac, zoom, preferred);
                maxX = Math.max(maxX, scale(ac.x, zoom) + d.width);
                maxY = Math.max(maxY, scale(ac.y, zoom) + d.height);
            }
            Insets in = parent.getInsets();
            return new Dimension(maxX + in.left + in.right, maxY + in.top + in.bottom);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return layoutSize(parent, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return layoutSize(parent, false);
        }

        /** Center the panel horizontally, e.g. in a BoxLayout (AbsoluteLayout: left). */
        @Override
        public float getLayoutAlignmentX(Container parent) {
            return 0.5f;
        }

        /** Do not stretch the panel (e.g. in a BoxLayout), keep its zoomed size. */
        @Override
        public Dimension maximumLayoutSize(Container parent) {
            return parent.getPreferredSize();
        }

        @Override
        public void layoutContainer(Container parent) {
            float zoom = get();
            Insets in = parent.getInsets();
            for (Component c : parent.getComponents()) {
                AbsoluteConstraints ac = (AbsoluteConstraints) this.constraints.get(c);
                if (ac == null) {
                    continue;
                }
                Dimension d = size(c, ac, zoom, true);
                c.setBounds(in.left + scale(ac.x, zoom), in.top + scale(ac.y, zoom), d.width, d.height);
            }
        }
    }
}
