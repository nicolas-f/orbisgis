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
package org.orbisgis.geocatalogtree.impl.nodes;

import org.jooq.QueryPart;
import org.jooq.Schema;
import org.jooq.Table;
import org.orbisgis.geocatalogtree.api.GeoCatalogTreeNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Nicolas Fortin
 */
public class TreeNodeTable extends DefaultMutableTreeNode implements GeoCatalogTreeNode {
    public TreeNodeTable(String name) {
        super(name);
    }

    @Override
    public String getUserObject() {
        return (String)super.getUserObject();
    }

    @Override
    public String getNodeType() {
        return GeoCatalogTreeNode.NODE_TABLE;
    }

    @Override
    public Table getValue(Connection connection) throws SQLException {
        Schema schema = ((TreeNodeSchema)getParent()).getValue(connection);
        return schema != null ? schema.getTable(getUserObject()) : null;
    }

    @Override
    public void loadChildren(Connection connection, DefaultTreeModel model) throws SQLException {

    }

    @Override
    public void check(QueryPart parentValue, DefaultTreeModel defaultTreeModel) throws SQLException {
        Table table = parentValue != null ? ((Schema)parentValue).getTable(getUserObject()) : null;
        if(table == null) {
            defaultTreeModel.removeNodeFromParent(this);
        } else {
            // TODO check children

        }
    }
}