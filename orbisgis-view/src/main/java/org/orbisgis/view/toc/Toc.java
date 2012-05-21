/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information. OrbisGIS is
 * distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 * 
 *
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
 *
 * or contact directly:
 * info _at_ orbisgis.org
 */
package org.orbisgis.view.toc;

import java.awt.BorderLayout;
import java.beans.EventHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.apache.log4j.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceListener;
import org.gdms.data.edition.EditionEvent;
import org.gdms.data.edition.EditionListener;
import org.gdms.data.edition.MultipleEditionEvent;
import org.orbisgis.core.DataManager;
import org.orbisgis.core.Services;
import org.orbisgis.core.events.Listener;
import org.orbisgis.core.layerModel.*;
import org.orbisgis.core.renderer.se.Style;
import org.orbisgis.progress.ProgressMonitor;
import org.orbisgis.view.background.BackgroundJob;
import org.orbisgis.view.background.BackgroundManager;
import org.orbisgis.view.docking.DockingPanel;
import org.orbisgis.view.docking.DockingPanelParameters;
import org.orbisgis.view.edition.EditableElement;
import org.orbisgis.view.geocatalog.EditableSource;
import org.orbisgis.view.icons.OrbisGISIcon;
import org.orbisgis.view.map.EditableTransferEvent;
import org.orbisgis.view.map.MapElement;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * @brief The Toc Panel component
 */

public class Toc extends JPanel implements DockingPanel  {
    //The UID must be incremented when the serialization is not compatible with the new version of this class
    private static final long serialVersionUID = 1L; 
    protected final static I18n I18N = I18nFactory.getI18n(Toc.class);
    private final static Logger LOGGER = Logger.getLogger("gui."+Toc.class);
    DockingPanelParameters dockingPanelParameters;
    private MapContext mapContext = null;
    private JTree tree;
    private TocTreeModel treeModel;
    //When this boolean is false, the selection event is not fired
    private AtomicBoolean fireSelectionEvent = new AtomicBoolean(true);
    
    //Listen for map context changes
    private TocMapContextListener tocMapContextListener = new TocMapContextListener();
    //Listen for all layers changes
    private TocLayerListener tocLayerListener = new TocLayerListener();
    //Loaded editable map
    private MapElement mapElement = null;
    /**
     * Constructor
     */
    public Toc() {
        super(new BorderLayout());
        //Set docking parameters
        dockingPanelParameters = new DockingPanelParameters();
        dockingPanelParameters.setName("toc");
        dockingPanelParameters.setTitle(I18N.tr("orbisgis.view.toc.TocTitle"));
        dockingPanelParameters.setTitleIcon(OrbisGISIcon.getIcon("map"));
        
        //Initialise an empty tree
        add(new JScrollPane( makeTree()));

    }
    
    private void setTocSelection(MapContext mapContext) {
        ILayer[] selected = mapContext.getSelectedLayers();
        TreePath[] selectedPaths = new TreePath[selected.length];
        for (int i = 0; i < selectedPaths.length; i++) {
                selectedPaths[i] = new TreePath(selected[i].getLayerPath());
        }

        
        fireSelectionEvent.set(false);
        try {
            tree.setSelectionPaths(selectedPaths);
        } finally {
            fireSelectionEvent.set(true);
        }
    }    
    /**
     * Create the Toc JTree
     * @return the Toc JTree
     */
    private JTree makeTree() {
        tree = new JTree();
        //Items can be selected freely
        tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        TocTransferHandler handler = new TocTransferHandler();
        //Add a drop listener
        handler.getTransferEditableEvent().addListener(this,
                EventHandler.create(Listener.class,this,"onDropEditableElement",""));
        tree.setTransferHandler(handler);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setEditable(true);        
        setEmptyLayerModel(tree);
        tree.setCellRenderer(new TocRenderer(this));
        tree.setCellEditor(new TocEditor(tree));
        //Add a tree selection listener
        tree.getSelectionModel().addTreeSelectionListener(EventHandler.create(TreeSelectionListener.class, this, "onTreeSelectionChange"));
        return tree;
    }
    /**
     * The user drop one or multiple EditableElement into the Toc Tree
     * @param editTransfer 
     * @throws IllegalArgumentException If drop location is not null,Style or ILayer
     */
    public void onDropEditableElement(EditableTransferEvent editTransfer) {
        LOGGER.info("Drop of "+editTransfer.getEditableList().length+" editables on node "+((JTree.DropLocation)editTransfer.getDropLocation()).getPath());
        JTree.DropLocation dropLocation = (JTree.DropLocation) editTransfer.getDropLocation();
        
        ILayer dropNode;
        if(dropLocation.getPath() != null) {            
            Object node = dropLocation.getPath().getLastPathComponent();
            if (node instanceof Style) {
                    dropNode = ((Style) node).getLayer();
            } else if (node instanceof ILayer) {
                    dropNode = (ILayer) node;
            } else {
                throw new IllegalArgumentException("Drop node is not an instance of Style or ILayer");
            }
        }else {
            // By default drop on rootNode
            dropNode = (ILayer) treeModel.getRoot();
        }
        List<EditableSource> sourceToDrop = new ArrayList<EditableSource>();
        for( EditableElement editableElement : editTransfer.getEditableList()) {
            if(editableElement instanceof EditableSource) {
                    sourceToDrop.add((EditableSource)editableElement);
            } else {
                    LOGGER.debug("Drop unknow editable of type :"+ editableElement.getTypeId());
            }
        }
        
        if(!sourceToDrop.isEmpty()) {
            BackgroundManager bm = (BackgroundManager) Services
                            .getService(BackgroundManager.class);
            bm.backgroundOperation(new DropDataSourceListProcess(dropNode,sourceToDrop));
        }
    }

    /**
     * Cast all ILayer elements of provided tree paths
     * @param selectedPaths Internal Tree path
     * @return All ILayer instances of provided path
     */
    private ArrayList<ILayer> getSelectedLayers(TreePath[] selectedPaths) {
            ArrayList<ILayer> layers = new ArrayList<ILayer>();
            for (int i = 0; i < selectedPaths.length; i++) {
                    Object lastPathComponent = selectedPaths[i].getLastPathComponent();
                    if (lastPathComponent instanceof ILayer) {
                            layers.add((ILayer) lastPathComponent);
                    }
            }
            return layers;
    }

    /**
     * Cast all Style elements of provided tree paths
     * @param selectedPaths Internal Tree path
     * @return All Style instances of provided path
     */
    private ArrayList<Style> getSelectedStyles(TreePath[] selectedPaths) {
            ArrayList<Style> rules = new ArrayList<Style>();
            for (int i = 0; i < selectedPaths.length; i++) {
                    Object lastPathComponent = selectedPaths[i].getLastPathComponent();
                    LOGGER.debug("Selection : " + lastPathComponent.toString());
                    if (lastPathComponent instanceof Style) {
                            rules.add(((Style) lastPathComponent));
                    }
            }
            return rules;
    }
    
    /**
     * Copy the selection of the JTree into the selection in the layer model.
     * This method do nothing if the fireSelectionEvent is false
     */
    public void onTreeSelectionChange() {
        if(fireSelectionEvent.get()) {
            fireSelectionEvent.set(false);
            try {
                TreePath[] selectedPaths = tree.getSelectionPaths();
                if(selectedPaths!=null) {
                    ArrayList<Style> styles = getSelectedStyles(selectedPaths);
                    mapContext.setSelectedStyles(styles);
                    if (!styles.isEmpty()) {
                        mapContext.setSelectedLayers(new ILayer[0]);
                    } else {
                        ArrayList<ILayer> layers = getSelectedLayers(selectedPaths);
                        if (!layers.isEmpty()) {
                                mapContext.setSelectedLayers(layers.toArray(new ILayer[0]));
                        }
                    }
                }
            } finally {
                fireSelectionEvent.set(true);
            }            
        }
    }
    
    private void setEmptyLayerModel(JTree jTree) {
        //Add the treeModel
        DataManager dataManager = (DataManager) Services.getService(DataManager.class);
        treeModel = new TocTreeModel(dataManager.createLayerCollection("root"), //$NON-NLS-1$
				jTree);
        jTree.setModel(treeModel);
    }
    public DockingPanelParameters getDockingParameters() {
        return dockingPanelParameters;
    }

        public MapContext getMapContext() {
                return mapContext;
        }

        public void setEditableMap(MapElement newMapElement) {

		// Remove the listeners
		if (this.mapContext != null) {
			this.mapContext.getLayerModel().removeLayerListenerRecursively(tocLayerListener);
			this.mapContext.removeMapContextListener(tocMapContextListener);
		}
                
                
		if (newMapElement != null) {
			this.mapContext = ((MapContext) newMapElement.getObject());
			this.mapElement = newMapElement;
			// Add the listeners to the new MapContext
			this.mapContext.addMapContextListener(tocMapContextListener);
			final ILayer root = this.mapContext.getLayerModel();
			root.addLayerListenerRecursively(tocLayerListener);

			treeModel = new TocTreeModel(root, tree);
			// Apply treeModel and clear the selection        
                        fireSelectionEvent.set(false);
                        try {
                            tree.setModel(treeModel);
                            tree.getSelectionModel().clearSelection();
                        } finally {
                            fireSelectionEvent.set(true);
                        }
                        
			setTocSelection(mapContext);
			//TODO ? repaint 
                }
        }


    boolean isActive(ILayer layer) {
            if (mapContext != null) {
                    return layer == mapContext.getActiveLayer();
            } else {
                    return false;
            }
    }
    public JComponent getComponent() {
        return this;
    }
    
	private class TocLayerListener implements LayerListener, EditionListener,
			DataSourceListener {

                @Override
		public void layerAdded(final LayerCollectionEvent e) {
			for (final ILayer layer : e.getAffected()) {
                                layer.addLayerListenerRecursively(this);
			}
			treeModel.refresh();
		}

                @Override
		public void layerMoved(LayerCollectionEvent e) {
			treeModel.refresh();
		}

		@Override
		public boolean layerRemoving(LayerCollectionEvent e) {
			// Close editors
			for (final ILayer layer : e.getAffected()) {
				ILayer[] layers = new ILayer[]{layer};
				if (layer.acceptsChilds()) {
					layers = layer.getLayersRecursively();
				}
				for (ILayer lyr : layers) {
                                        //TODO Close editors attached to this ILayer
                                        //Or do the job on the Editor (logic)
                                        /*
					EditorManager em = Services.getService(EditorManager.class);
					IEditor[] editors = em.getEditor(new EditableLayer(element,
							lyr));
					for (IEditor editor : editors) {
						if (!em.closeEditor(editor)) {
							return false;
						}
					}
                                        * 
                                        */
				}
			}
			return true;
		}

                @Override
		public void layerRemoved(final LayerCollectionEvent e) {
			for (final ILayer layer : e.getAffected()) {
				layer.removeLayerListenerRecursively(this);
			}
			treeModel.refresh();
		}

                @Override
		public void nameChanged(LayerListenerEvent e) {
		}

                @Override
		public void styleChanged(LayerListenerEvent e) {
			treeModel.refresh();
		}

                @Override
		public void visibilityChanged(LayerListenerEvent e) {
			treeModel.refresh();
		}

                @Override
		public void selectionChanged(SelectionEvent e) {
			treeModel.refresh();
		}

                @Override
		public void multipleModification(MultipleEditionEvent e) {
			treeModel.refresh();
		}

                @Override
		public void singleModification(EditionEvent e) {
			treeModel.refresh();
		}

                @Override
		public void cancel(DataSource ds) {
		}

                @Override
		public void commit(DataSource ds) {
			treeModel.refresh();
		}

                @Override
		public void open(DataSource ds) {
			treeModel.refresh();
		}

	}
        
        
	private final class TocMapContextListener implements MapContextListener {
                @Override
		public void layerSelectionChanged(MapContext mapContext) {
			setTocSelection(mapContext);
		}

                @Override
		public void activeLayerChanged(ILayer previousActiveLayer,
				MapContext mapContext) {
			treeModel.refresh();
		}
	}
        
        /**
         * The user drop a list of EditableSource in the TOC tree
         */
        private class DropDataSourceListProcess implements BackgroundJob {

		private ILayer dropNode;
		private List<EditableSource> draggedResources;

                public DropDataSourceListProcess(ILayer dropNode, List<EditableSource> draggedResources) {
                    this.dropNode = dropNode;
                    this.draggedResources = draggedResources;
                }


                @Override
		public void run(ProgressMonitor pm) {
			int index;
			if (!dropNode.acceptsChilds()) {
				ILayer parent = dropNode.getParent();
				if (parent.acceptsChilds()) {
					index = parent.getIndex(dropNode);
					dropNode = parent;
				} else {
					LOGGER.error(I18N.tr("Cannot create layer on {0}",dropNode.getName())); //$NON-NLS-1$
					return;
				}
			} else {
				index = dropNode.getLayerCount();
			}
			DataManager dataManager = (DataManager) Services
					.getService(DataManager.class);
			for (int i = 0; i < draggedResources.size(); i++) {
				String sourceName = draggedResources.get(i).getId();
				if (pm.isCancelled()) {
					break;
				} else {
					pm.progressTo(100 * i / draggedResources.size());
					try {
						dropNode.insertLayer(dataManager
								.createLayer(sourceName), index);
					} catch (LayerException e) {
						throw new RuntimeException(I18N.tr("Cannot add the layer to the destination") , e); //$NON-NLS-1$
					}
				}
			}
		}

		public String getTaskName() {
			return I18N.tr("Load the data source droped into the toc."); //$NON-NLS-1$
		}
	}
}