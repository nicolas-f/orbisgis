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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import org.apache.log4j.Logger;

/**
 * Root class of all bundle actions.
 * @author Nicolas Fortin
 */
public class ActionBundle extends AbstractAction {
    protected static Logger LOGGER = Logger.getLogger(ActionBundle.class);
    private ProgressLayerUI progressLayerUI;
    private ActionListener action;

    public ActionBundle(String label, String toolTipText,Icon icon, ProgressLayerUI progressLayerUI) {
        super(label);
        this.progressLayerUI = progressLayerUI;
        putValue(SHORT_DESCRIPTION,toolTipText);
        putValue(SMALL_ICON, icon);
    }

    /**
     * Call this listener when the command is executed.
     * @param action
     * @return this
     */
    public ActionBundle setActionListener(ActionListener action) {
        this.action = action;
        return this;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        setEnabled(false);
        progressLayerUI.setMessage((String)getValue(SHORT_DESCRIPTION));
        progressLayerUI.start();
        ActionSwingWorker actionSwingWorker = new ActionSwingWorker(this, actionEvent,action, progressLayerUI);
        actionSwingWorker.execute();
    }

    private static class ActionSwingWorker extends SwingWorker {
        private ActionEvent actionEvent;
        private ProgressLayerUI progressLayerUI;
        private Action action;
        private ActionListener actionListener;

        private ActionSwingWorker(Action action, ActionEvent actionEvent, ActionListener actionListener, ProgressLayerUI progressLayerUI) {
            this.action = action;
            this.actionEvent = actionEvent;
            this.actionListener = actionListener;
            this.progressLayerUI = progressLayerUI;
        }

        @Override
        protected Object doInBackground() throws Exception {
            actionListener.actionPerformed(actionEvent);
            return 0;
        }

        @Override
        protected void done() {
            progressLayerUI.stop();
            action.setEnabled(true);
        }
    }
}
