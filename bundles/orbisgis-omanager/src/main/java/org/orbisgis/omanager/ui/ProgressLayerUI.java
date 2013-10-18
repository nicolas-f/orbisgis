/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information.
 *
 * OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.omanager.ui;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;

/**
 * This layer UI show an ImageIcon with a text message below.
 * This ImageIcon has to be animated in order to show Fade in and Fade out.
 * @author Nicolas Fortin
 */
public class ProgressLayerUI extends LayerUI<JPanel> implements ImageObserver {
    private int interpolationCount;
    private static final int INTERPOLATION_MAX = 8;
    private int interpolationDrawn;
    private boolean running;
    private boolean blackInterpolating;
    private boolean keepRunning;
    private ImageIcon icon;
    private static final float LAYER_OPACITY = 0.5f;
    private String message;
    private Font messageFont;


    /**
     * @param icon Icon
     */
    public void setWaitIcon(ImageIcon icon) {
        this.icon = icon;
        icon.setImageObserver(this);
    }

    @Override
    public boolean imageUpdate(Image image, int i, int i2, int i3, int i4, int i5) {
        if (running) {
            firePropertyChange("interpolationCount", null, interpolationCount);
            if (blackInterpolating && interpolationDrawn == interpolationCount) {
                if (--interpolationCount <= 0) {
                    running = false;
                }
            }
            else if (interpolationCount < INTERPOLATION_MAX) {
                if(interpolationDrawn == interpolationCount) {
                    interpolationCount++;
                }
            } else if(interpolationCount == INTERPOLATION_MAX && !keepRunning) {
                keepRunning = false;
                blackInterpolating = true;
            }
        }
        return running;
    }

    @Override
    public void applyPropertyChange(PropertyChangeEvent pce, JLayer l) {
        if ("interpolationCount".equals(pce.getPropertyName())) {
            l.repaint();
        }
    }

    @Override
    public void paint (Graphics g, JComponent c) {
        int w = c.getWidth();
        int h = c.getHeight();
        int iconHeight = icon.getIconHeight();
        int iconWidth = icon.getIconWidth();
        // Paint the view.
        super.paint (g, c);

        if (!running) {
            return;
        }
        Graphics2D g2 = (Graphics2D)g.create();
        float fade = Math.max(0, Math.min(1, (float) interpolationCount / (float) INTERPOLATION_MAX));
        Composite urComposite = g2.getComposite();
        // Set alpha
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LAYER_OPACITY * fade));
        // Set black background
        g2.fillRect(0, 0, w, h);
        // Draw gif
        g2.drawImage(icon.getImage(), (int)(w / 2.f - (iconWidth / 2.f)), (int)(h / 2.f - (iconHeight / 2.f)), this);
        // Draw message
        g2.setFont(messageFont);
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D textSize = fm.getStringBounds(message, g2);
        g2.setColor(Color.WHITE);
        g2.drawString(message, (int) ((w / 2.f) - (textSize.getWidth() / 2.f)), (int) (h / 2 + iconHeight / 2 + textSize.getHeight()));
        g2.setComposite(urComposite);
        g2.dispose();
        interpolationDrawn = interpolationCount;
    }

    /**
     * @return True if this component is active
     */
    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) {
            return;
        }
        // Run a thread for animation.
        running = true;
        blackInterpolating = false;
        interpolationCount = 0;
        keepRunning = true;
        firePropertyChange("interpolationCount", null, interpolationCount);
    }

    public void stop() {
        keepRunning = false;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JLayer<?> l = (JLayer<?>) c;
        // this LayerUI will receive mouse/motion events
        l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        messageFont = new JLabel().getFont().deriveFont(Font.BOLD);
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        // JLayer must be returned to its initial state
        JLayer<?> l = (JLayer<?>) c;
        l.setLayerEventMask(0);
    }

    /**
     * @param message Shown message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}