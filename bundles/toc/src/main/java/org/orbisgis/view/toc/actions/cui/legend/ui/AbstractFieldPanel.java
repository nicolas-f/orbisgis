/**
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information.
 *
 * OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2014 IRSTV (FR CNRS 2488)
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
package org.orbisgis.view.toc.actions.cui.legend.ui;

import org.orbisgis.view.toc.actions.cui.components.CanvasSE;
import org.orbisgis.view.toc.actions.cui.legend.ILegendPanel;

import javax.sql.DataSource;
import javax.swing.*;
import java.awt.*;

/**
 * Root class for UIs in the Simple Style Editor.
 *
 * @author alexis
 * @author Adam Gouge
 */
public abstract class AbstractFieldPanel extends JPanel implements ILegendPanel {

    /**
     * Gets a preview for the fallback value of the symbol.
     * @return The Preview in a CanvasSE.
     */
    public abstract CanvasSE getPreview();

    /**
     * (Re)Build the {@code CanvasSE} instance used to display a preview of
     * the fallback symbol.
     * @param ds Link to sql engine
     */
    public abstract void initPreview();

    // ************************* UIPanel ***************************
    @Override
    public Component getComponent() {
        return this;
    }
    // ********************* ISELegendPanel ************************
    @Override
    public String validateInput() {
        return null;
    }
}
