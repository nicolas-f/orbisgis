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
package org.orbisgis.tocapi;

import org.orbisgis.coremap.layerModel.ILayer;
import org.orbisgis.sif.components.actions.DefaultAction;
import org.orbisgis.sif.components.resourceTree.TreeSelectionIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Layer related Actions.
 * @author Nicolas Fortin
 */
public class LayerAction extends DefaultAction {
    private TocExt toc;
    private static Logger LOGGER = LoggerFactory.getLogger(LayerAction.class);
    private boolean singleSelection = false;
    private boolean onRealLayerOnly = false;
    private boolean onLayerWithRowSelection = false;
    private boolean onLayerGroup = false;
    private boolean onEmptySelection = false;
    private boolean onVectorSourceOnly = false;

    /**
     * @param onLayerWithRowSelection If true this action is shown only if one of the selected layer contain a row selection.
     * @return this
     */
    public LayerAction setOnLayerWithRowSelection(boolean onLayerWithRowSelection) {
        this.onLayerWithRowSelection = onLayerWithRowSelection;
        return this;
    }

    /**
     * @param onVectorSourceOnly If true, do not show this action if the layer source is not a vector
     * @return this
     */
    public LayerAction setOnVectorSourceOnly(boolean onVectorSourceOnly) {
        this.onVectorSourceOnly = onVectorSourceOnly;
        return this;
    }

    /**
     * @param onEmptySelection If true this Action will be visible without selection, default is false.
     * @return this
     */
    public LayerAction setOnEmptySelection(boolean onEmptySelection) {
        this.onEmptySelection = onEmptySelection;
        return this;
    }

    /**
     * @param singleSelection If true this action is shown only if the user select at most one layer,
     *                        default is false
     * @return this
     */
    public LayerAction setSingleSelection(boolean singleSelection) {
        this.singleSelection = singleSelection;
        return this;
    }

    /**
     * @param onLayerGroup Show only if there is at least one layer group selected
     * @return this
     */
    public LayerAction setOnLayerGroup(boolean onLayerGroup) {
        this.onLayerGroup = onLayerGroup;
        return this;
    }

    /**
     * @param onRealLayerOnly If true this action is not shown if selected item is a layer collection.
     * @return this
     */
    public LayerAction setOnRealLayerOnly(boolean onRealLayerOnly) {
        this.onRealLayerOnly = onRealLayerOnly;
        return this;
    }

    /**
     * Constructor
     * @param toc Toc instance
     * @param actionId Action identifier, should be unique for ActionCommands
     * @param actionLabel I18N label short label
     * @param actionToolTip I18N tool tip text
     * @param icon Icon
     * @param actionListener Fire the event to this listener
     * @param keyStroke ShortCut for this action
     */
    public LayerAction(TocExt toc,String actionId, String actionLabel, String actionToolTip, Icon icon, ActionListener actionListener, KeyStroke keyStroke) {
        super(actionId, actionLabel, actionToolTip, icon, actionListener, keyStroke);
        this.toc = toc;
    }

    /**
     * Constructor
     * @param actionId Action identifier, should be unique for ActionCommands
     * @param actionLabel I18N label short label
     * @param icon Icon
     * @param toc Toc instance
     */
    public LayerAction(TocExt toc, String actionId, String actionLabel, Icon icon) {
        super(actionId, actionLabel, icon);
        this.toc = toc;
    }

    @Override
    public boolean isEnabled() {
        // Create the layer iterator
        TreeSelectionIterable<TocTreeNodeLayer> layerIterator =
                new TreeSelectionIterable<>(toc.getTree().getSelectionPaths(),TocTreeNodeLayer.class);
        int selectedLayersCount = 0;
        boolean rowSelection = false;
        boolean hasRealLayer = false;
        boolean hasNonVectorSource = false;
        for(TocTreeNodeLayer layerNode : layerIterator) {
            selectedLayersCount++;
            ILayer layer = layerNode.getLayer();
            if(onLayerWithRowSelection && !rowSelection) {
                rowSelection = !layer.getSelection().isEmpty();
            }
            hasRealLayer = !layerNode.getLayer().acceptsChilds();
            if(hasRealLayer && onVectorSourceOnly && !hasNonVectorSource) {
                hasNonVectorSource = layer.getTableReference().isEmpty();
            }
        }
        return (!onLayerWithRowSelection || rowSelection) &&
                (!singleSelection || selectedLayersCount<=1) &&
                (!onRealLayerOnly || hasRealLayer) &&
                (!onLayerGroup || !hasRealLayer) &&
                (!onVectorSourceOnly || !hasNonVectorSource) &&
                ((onEmptySelection && toc.getTree().getSelectionCount()==0) || selectedLayersCount>=1) && super.isEnabled();
    }
}
