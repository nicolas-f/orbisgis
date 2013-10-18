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

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.LayerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Dialog that handle bundles.
 * @author Nicolas Fortin
 */
public class MainPanel extends JPanel {
    private static final Dimension MINIMUM_BUNDLE_LIST_DIMENSION = new Dimension(100,50);
    private static final Dimension MINIMUM_BUNDLE_DESCRIPTION_DIMENSION = new Dimension(250,50);
    private static final int MINIMUM_SEARCH_COLUMNS = 10;
    private static final I18n I18N = I18nFactory.getI18n(MainPanel.class);
    private static final Logger LOGGER = Logger.getLogger("gui."+MainPanel.class);
    private static final int BORDER_PIXEL_GAP = 2;
    private static final int PROPERTY_TEXT_SIZE_INCREMENT = 3;
    private static final int PROPERTY_TITLE_SIZE_INCREMENT = 4;
    private static final String DEFAULT_CATEGORY = "OrbisGIS";
    private ItemFilterStatusFactory.Status radioFilterStatus = ItemFilterStatusFactory.Status.ALL;
    private Map<String,ImageIcon> buttonIcons = new HashMap<>();

    // Bundle Category filter
    private JComboBox bundleCategory = new JComboBox();
    private JTextField bundleSearchField = new JTextField(MINIMUM_SEARCH_COLUMNS);
    private JTextPane bundleDetails = new JTextPane();
    private JList<BundleItem> bundleList = new JList<>();
    private JPanel bundleActions = new JPanel();
    private JButton repositoryRemove;
    private BundleListModel bundleListModel;
    private FilteredModel<BundleListModel,BundleItem > filterModel;
    private JPanel bundleDetailsAndActions = new JPanel(new BorderLayout());
    private JSplitPane splitPane;
    private ActionBundleFactory actionFactory;
    private BundleDetailsTransformer bundleHeader = new BundleDetailsTransformer();
    private BundleContext bundleContext;
    private RepositoryAdminTracker repositoryAdminTrackerCustomizer;
    private ServiceTracker<RepositoryAdmin,RepositoryAdmin> repositoryAdminTracker;
    private AtomicBoolean awaitingFilteringThread = new AtomicBoolean(false);
    private long lastTypedWordInFindTextField = 0;
    /** Wait label */
    private final JPanel southButtons = new JPanel();
    private final ProgressLayerUI layerUI = new ProgressLayerUI();
    /**
     * in ms Launch a search if the user don't type any character within this time.
     */
    private static final long LAUNCH_SEARCH_IDLE_TIME = 300;
    /**
     * Constructor of the main plugin panel
     * @param bundleContext Bundle context instance in order to manage them.
     */
    public MainPanel(BundleContext bundleContext) {
        super(new BorderLayout());
        JPanel subLayer = new JPanel(new BorderLayout());
        JLayer<JPanel> jlayer = new JLayer<>(subLayer, layerUI);
        ImageIcon loading = getIcon("loading", "gif");
        layerUI.setWaitIcon(loading);
        loading.setImageObserver(layerUI);
        this.bundleContext = bundleContext;
        initRepositoryTracker();
        actionFactory = new ActionBundleFactory(bundleContext,this);
        // Main Panel (South button, center Split Pane)
        // Buttons on south of main panel
        southButtons.setLayout(new BoxLayout(southButtons, BoxLayout.X_AXIS));
        addSouthButtons(southButtons);
        subLayer.add(southButtons, BorderLayout.SOUTH);
        // Right Side of Split Panel, Bundle Description and button action on selected bundle
        bundleActions.setLayout(new BoxLayout(bundleActions,BoxLayout.X_AXIS));
        //bundleDetails.setPreferredSize(DEFAULT_DETAILS_DIMENSION);
        bundleDetails.setEditable(false);
        bundleDetails.setMinimumSize(MINIMUM_BUNDLE_DESCRIPTION_DIMENSION);
        bundleDetailsAndActions.add(new JScrollPane(bundleDetails),BorderLayout.CENTER);
        bundleDetailsAndActions.add(bundleActions,BorderLayout.SOUTH);
        // Left Side of Split Panel (Filters north, bundles center)
        JPanel leftOfSplitGroup = new JPanel(new BorderLayout(BORDER_PIXEL_GAP,BORDER_PIXEL_GAP));
        bundleList.setMinimumSize(MINIMUM_BUNDLE_LIST_DIMENSION);
        leftOfSplitGroup.add(createFilterComponents(), BorderLayout.NORTH);
        leftOfSplitGroup.add(new JScrollPane(bundleList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),BorderLayout.CENTER);

        setDefaultDetailsMessage();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,leftOfSplitGroup,bundleDetailsAndActions);
        subLayer.add(splitPane);
        add(jlayer);
        bundleListModel =  new BundleListModel(bundleContext,repositoryAdminTrackerCustomizer);
        filterModel = new FilteredModel<>(bundleListModel);
        bundleList.setModel(filterModel);
        bundleListModel.install();
        bundleList.setCellRenderer(new BundleListRenderer(bundleList));
        bundleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bundleList.addListSelectionListener(EventHandler.create(ListSelectionListener.class,this,"onBundleSelectionChange",""));
        bundleList.getModel().addListDataListener(EventHandler.create(ListDataListener.class,this,"onListUpdate"));
        onListUpdate();
        applyFilters();
    }

    private void showWait(String message) {
        layerUI.setMessage(message);
        layerUI.start();
    }

    private void hideWait() {
        layerUI.stop();
    }

    private ImageIcon getIcon(String iconName) {
        return getIcon(iconName, "png");
    }
    private ImageIcon getIcon(String iconName, String ext) {
        ImageIcon icon = buttonIcons.get(iconName);
        if(icon==null) {
            try {
                icon = new ImageIcon(MainPanel.class.getResource(iconName + "." + ext));
                buttonIcons.put(iconName,icon);
            } catch (Exception ex) {
                LOGGER.error("Cannot retrieve icon "+iconName,ex);
                return new ImageIcon();
            }
        }
        return icon;
    }

    private void initRepositoryTracker() {
        repositoryAdminTrackerCustomizer = new RepositoryAdminTracker(bundleContext);
        repositoryAdminTracker = new ServiceTracker<RepositoryAdmin, RepositoryAdmin>(bundleContext,
                RepositoryAdmin.class,repositoryAdminTrackerCustomizer);
        repositoryAdminTracker.open();
        repositoryAdminTrackerCustomizer.getPropertyChangeSupport().addPropertyChangeListener(EventHandler.create(PropertyChangeListener.class,this,"onRepositoryChange"));

    }
    /**
     * The user select another plugin in the list.
     * @param e Event object
     */
    public void onBundleSelectionChange(ListSelectionEvent e) {
        if(!e.getValueIsAdjusting()) {
            readSelectedBundle();
        }
    }
    private void readSelectedBundle() {
        Object selected = bundleList.getSelectedValue();
        if(selected instanceof BundleItem) {
            final BundleItem selectedItem = (BundleItem)selected;
            if(!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setBundleDetailsAndActions(selectedItem);
                    }
                });
            } else {
                setBundleDetailsAndActions(selectedItem);
            }
        } else {
            setDefaultDetailsMessage();
        }
    }
    /**
     * Called by the service tracker when there is an update.
     */
    public void onRepositoryChange() {
         if(repositoryRemove!=null) {
             // If there is no repositories, the user can't delete anything
             repositoryRemove.setEnabled(
                     !repositoryAdminTrackerCustomizer.getRepositories().isEmpty());
         }
    }
    /**
     * The Data Model of the Bundle list has been updated.
     */
    public void onListUpdate() {
        Object oldValue = bundleCategory.getSelectedItem();
        readSelectedBundle();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        // Update the list of categories
        Set<String> categories = new HashSet<String>();
        int size = bundleList.getModel().getSize();
        ListModel listModel = bundleList.getModel();
        for(int i=0; i<size; i++) {
            categories.addAll(((BundleItem) listModel.getElementAt(i)).getBundleCategories());
        }
        if(!categories.contains(DEFAULT_CATEGORY)) {
            categories.add(DEFAULT_CATEGORY);
        }
        model.addElement(I18N.tr("All"));
        List<String> sortedCategories = new ArrayList<String>(categories);
        Collections.sort(sortedCategories,String.CASE_INSENSITIVE_ORDER);
        String selectedValue= DEFAULT_CATEGORY;
        if(oldValue instanceof String) {
            selectedValue = (String)oldValue;
        }
        for(String category : sortedCategories) {
            model.addElement(category);
            if(category.equalsIgnoreCase(selectedValue)) {
                model.setSelectedItem(category);
            }
        }
        bundleCategory.setModel(model);
    }
    private void addSouthButtons(JPanel southButtons) {

        JButton addFile = new ButtonIcon(getIcon("install_plugin_from_disk"));
        addFile.setToolTipText(I18N.tr("Add a plugin from disk. Dependencies are not automatically resolved."));
        addFile.addActionListener(EventHandler.create(ActionListener.class, this, "onAddBundleJar"));
        southButtons.add(addFile);

        JButton addUrl = new ButtonIcon(getIcon("install_plugin_from_url"));
        addUrl.setToolTipText(I18N.tr("Add a plugin from a URL (file:// or http(s)://). Dependencies are not automatically resolved."));
        addUrl.addActionListener(EventHandler.create(ActionListener.class,this,"onAddBundleJarUri"));
        southButtons.add(addUrl);

        southButtons.add(new JSeparator(JSeparator.VERTICAL));
        JButton repositoryUrls = new ButtonIcon(getIcon("repository_add"));
        repositoryUrls.setToolTipText(I18N.tr("Add a remote bundle repository."));
        repositoryUrls.addActionListener(EventHandler.create(ActionListener.class,this,"onAddBundleRepository"));
        southButtons.add(repositoryUrls);

        repositoryRemove = new ButtonIcon(getIcon("repository_remove"));
        repositoryRemove.setToolTipText(I18N.tr("Remove a remote bundle repository."));
        repositoryRemove.addActionListener(EventHandler.create(ActionListener.class,this,"onRemoveBundleRepository"));
        southButtons.add(repositoryRemove);
        onRepositoryChange();

        JButton refreshRepositories = new ButtonIcon(getIcon("repository_refresh"));
        refreshRepositories.setToolTipText(I18N.tr("Reload available plugins from all remote repositories."));
        refreshRepositories.addActionListener(EventHandler.create(ActionListener.class,this,"onReloadPlugins"));
        southButtons.add(refreshRepositories);
    }
    /**
     * Message on bundle details message frame when no bundle is selected, and remove all actions.
     */
    private void setDefaultDetailsMessage() {
        bundleActions.removeAll();
        bundleDetailsAndActions.setVisible(false);
    }
    private void addDescriptionItem(String propertyKey,String propertyValue ,Document document) {
        addDescriptionItem(propertyKey, propertyValue, document, PROPERTY_TEXT_SIZE_INCREMENT);
    }
    private void addDescriptionItem(String propertyKey,String propertyValue ,Document document, int keySize) {
        try {
            SimpleAttributeSet sc = new SimpleAttributeSet();
            sc.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.TRUE);
            int standardSize = StyleConstants.CharacterConstants.getFontSize(sc);
            sc.addAttribute(StyleConstants.CharacterConstants.Size, standardSize + keySize);
            document.insertString(document.getLength(), propertyKey, sc);
            if(!propertyValue.isEmpty()) {
                document.insertString(document.getLength()," : "+propertyValue+"\n\n",new SimpleAttributeSet());
            } else {
                document.insertString(document.getLength(),"\n"+propertyValue+"\n\n",new SimpleAttributeSet());
            }
        } catch (BadLocationException ex) {
            LOGGER.error(ex);
        }
    }
    private void addHeaderItem(String property,Map<String,String> headers, Document document) {
        String value = headers.get(property);
        if(value!=null) {
            addDescriptionItem(I18N.tr(property),value,document);
        }
    }

    private void setBundleDetailsAndActions(BundleItem selectedItem) {
        bundleActions.removeAll();
        // Set description
        bundleDetails.setText("");
        Document document = bundleDetails.getDocument();
        Map<String,String> itemDetails = selectedItem.getDetails();
        // Plugin Name
        // Title, Description, Version, Category, then other parameters
        addDescriptionItem(selectedItem.getPresentationName(),"",document,PROPERTY_TITLE_SIZE_INCREMENT);
        // Description
        addHeaderItem(Constants.BUNDLE_DESCRIPTION, itemDetails, document);
        // Version
        addHeaderItem(Constants.BUNDLE_VERSION, itemDetails, document);
        // Categories
        StringBuilder cat = new StringBuilder();
        for(String category : selectedItem.getBundleCategories()) {
            if(cat.length()>0) {
                cat.append(", ");
            }
            cat.append(I18N.tr(category.trim()));
        }
        if(cat.length()>0) {
            addDescriptionItem(I18N.tr(Constants.BUNDLE_CATEGORY),cat.toString(),document);
        }

        // Add other properties
        for(Map.Entry<String,String> entry : itemDetails.entrySet()) {
            String originalKey = entry.getKey();
            String key = bundleHeader.convert(originalKey);
            if(!key.isEmpty() && !entry.getValue().isEmpty()) {
                addDescriptionItem(key,entry.getValue(),document);
            }
        }
        bundleDetails.setCaretPosition(0); // Got to the beginning of the document
        // Set buttons
        List<Action> actions = actionFactory.create(selectedItem);
        for(Action action : actions) {
            JButton actionButton = new ButtonIcon(action);
            bundleActions.add(actionButton);
        }
        boolean hidden = !bundleDetailsAndActions.isVisible();
        if(hidden) {
            bundleDetailsAndActions.setVisible(true);
            int lastSize = splitPane.getLastDividerLocation();
            if(lastSize >= getWidth() - bundleDetailsAndActions.getMinimumSize().width) {
                lastSize = -1;
            }
            splitPane.setDividerLocation(lastSize); // -1 to let swing compute preferred size
        } else {
            // Save split pane position
            splitPane.setLastDividerLocation(splitPane.getDividerLocation());
            splitPane.updateUI(); // Without this instruction buttons are disabled on some L&F
        }
    }
    private void applyFilters() {
        List<ItemFilter<BundleListModel>> filters = new ArrayList<ItemFilter<BundleListModel>>();
        ItemFilter<BundleListModel> radioFilter = ItemFilterStatusFactory.getFilter(radioFilterStatus);
        if(radioFilter!=null) {
            filters.add(radioFilter);
        }
        if(bundleCategory.getSelectedIndex()>0
                && bundleCategory.getSelectedItem() instanceof String) {
            filters.add(new ItemFilterCategory((String)bundleCategory.getSelectedItem()));
        }
        String filterTextValue = bundleSearchField.getText().trim();
        if(!filterTextValue.isEmpty()) {
            filters.add(new ItemFilterContains(filterTextValue));
        }
        if(filters.size()>=1) {
            filterModel.setFilter(new ItemFilterAndGroup(filters));
        } else if(filters.size()==1) {
            filterModel.setFilter(filters.get(0));
        } else {
            filterModel.setFilter(null);
        }
    }
    /**
     * User click on "All states" radio button
     */
    public void onRemoveStateFilter() {
        radioFilterStatus = ItemFilterStatusFactory.Status.ALL;
        applyFilters();
    }
    /**
     * User click on "Installed" radio button
     */
    public void onFilterBundleInstall() {
        radioFilterStatus = ItemFilterStatusFactory.Status.INSTALLED;
        applyFilters();
    }
    /**
     * User click on "Update" radio button
     */
    public void onFilterBundleUpdate() {
        radioFilterStatus = ItemFilterStatusFactory.Status.UPDATE;
        applyFilters();
    }
    /**
     * User click on an item on the category list.
     */
    public void onFilterByBundleCategory() {
        applyFilters();
    }
    /**
     * User click on "Refresh" button.
     */
    public void onReloadPlugins() {
        showWait(I18N.tr("Reload available plugins from all remote repositories."));
        DownloadOBRProcess reloadAction = new DownloadOBRProcess();
        reloadAction.execute();
    }
    public void onAddBundleJarUri() {
        String errMessage = "";
        String chosenURL = "";
        do {
            StringBuilder message = new StringBuilder(I18N.tr("Enter the plugin URL (file:// or http(s)://):"));
            if(!errMessage.isEmpty()) {
                message.append("\n");
                message.append(errMessage);
            }
            chosenURL = (String) JOptionPane.showInputDialog(
                    this,
                    message.toString(),
                    I18N.tr("Add a plugin file"),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    chosenURL);
            //If a string was returned, say so.
            if ((chosenURL != null)) {
                try {
                    URI userURI = new URI(chosenURL.trim().replaceAll(" ", "%20"));
                    bundleContext.installBundle(userURI.toString());
                    return;
                } catch(Exception ex) {
                    errMessage = ex.getLocalizedMessage();
                }
            }
        } while(chosenURL!=null && !errMessage.isEmpty());
    }
    /**
     * User click on install Plug-in from disk button.
     */
    public void onAddBundleJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                I18N.tr("OSGi Jar"), "jar");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File[] selection = chooser.getSelectedFiles();
            try {
                for(File selected : selection) {
                    try {
                        showWait(I18N.tr("Install bundle {0}", selected.toURI().toString()));
                        bundleContext.installBundle(selected.toURI().toString());
                    } catch (BundleException ex) {
                        LOGGER.error(ex.getLocalizedMessage(),ex);
                    }
                }
            } finally {
                hideWait();
            }
        }

    }
    /**
     * User want to add repository url.
     */
    public void onAddBundleRepository() {
        String errMessage = "";
        String chosenURL = "";
        do {
            chosenURL = showInputURI(chosenURL,errMessage);
            //If a string was returned, say so.
            if ((chosenURL != null)) {
                List<URL> urls = repositoryAdminTrackerCustomizer.getRepositoriesURL();
                try {
                    URI userURI = new URI(chosenURL.trim().replaceAll(" ", "%20"));
                    // TODO: How can a list of URLs contain a URI?
                    if(urls.contains(userURI.toURL())) {
                        errMessage = I18N.tr("This repository URL already exists");
                    } else {
                        repositoryAdminTrackerCustomizer.addRepository(userURI.toURL());
                        return;
                    }
                } catch(Exception ex) {
                    errMessage = ex.getLocalizedMessage();
                }
            }
        } while(chosenURL!=null && !errMessage.isEmpty());

    }
    private String showInputURI(String defaultValue,String errorMessage) {
        StringBuilder message = new StringBuilder(I18N.tr("Enter the repository URL:"));
        if(!errorMessage.isEmpty()) {
            message.append("\n");
            message.append(errorMessage);
        }
        return (String) JOptionPane.showInputDialog(
                this,
                message.toString(),
                I18N.tr("Add a plugin repository"),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultValue);
    }
    /**
     * User want to remove repository url.
     */
    public void onRemoveBundleRepository() {
        List<URL> urls = repositoryAdminTrackerCustomizer.getRepositoriesURL();
        if(!urls.isEmpty()) {
            URL chosenURL = (URL) JOptionPane.showInputDialog(
                    this,
                    I18N.tr("Select the server to remove"),
                    I18N.tr("Remove plugin repository"),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    urls.toArray(new URL[urls.size()]),
                    urls.get(0));

            //If a string was returned, say so.
            if ((chosenURL != null) && (urls.contains(chosenURL))) {
                repositoryAdminTrackerCustomizer.removeRepository(chosenURL);
            }
        }
    }

    /**
     * Remove trackers and listeners
     */
    public void dispose() {
        if(repositoryAdminTracker!=null) {
            repositoryAdminTracker.close();
        }
        bundleListModel.uninstall();
    }

    private void createRadioButton(String label,String toolTipText,boolean state,String methodName,ButtonGroup filterGroup,JPanel radioBar) {
        JRadioButton noStateFilter = new JRadioButton(label,state);
        noStateFilter.setToolTipText(toolTipText);
        noStateFilter.addActionListener(EventHandler.create(ActionListener.class, this, methodName));
        filterGroup.add(noStateFilter);
        radioBar.add(noStateFilter);
    }
    private JPanel createFilterComponents() {
        // Make main radio panel
        JPanel radioBar = new JPanel();
        radioBar.setLayout(new BoxLayout(radioBar, BoxLayout.X_AXIS));
        // Create radio buttons
        ButtonGroup filterGroup = new ButtonGroup();
        createRadioButton(I18N.tr("All"),I18N.tr("Show all bundles."),true,
                "onRemoveStateFilter",filterGroup,radioBar);
        createRadioButton(I18N.tr("Installed"),I18N.tr("Show only installed bundles."),false,"onFilterBundleInstall",
                filterGroup, radioBar);
        createRadioButton(I18N.tr("Updatable"), I18N.tr("Show only bundles that can be updated."), false,
                "onFilterBundleUpdate", filterGroup, radioBar);

        // Category
        bundleCategory.addActionListener(EventHandler.create(ActionListener.class,this,"onFilterByBundleCategory"));
        radioBar.add(bundleCategory);
        // Find text
        bundleSearchField.getDocument().addDocumentListener(EventHandler.create(DocumentListener.class,this,"onSearchTextChange"));
        bundleSearchField.addActionListener(EventHandler.create(ActionListener.class, this, "onSearchTextValidate"));
        bundleSearchField.setToolTipText(I18N.tr("Search for a plugin."));
        // Use MINIMUM_SEARCH_COLUMNS as minimum size
        bundleSearchField.setMinimumSize(bundleSearchField.getPreferredSize());
        radioBar.add(bundleSearchField);
        return radioBar;
    }

    /**
     * Called when the user validate the research terms
     */
    public void onSearchTextValidate() {
        lastTypedWordInFindTextField = 0;
        launchTextFindProcess();
    }
    /**
     * Called when a user type a character in the text filter
     */
    public void onSearchTextChange() {
        lastTypedWordInFindTextField = System.currentTimeMillis();
        launchTextFindProcess();
    }

    private void launchTextFindProcess() {
        if(!awaitingFilteringThread.getAndSet(true)) {
            CheckBundleFilteringTextInput process = new CheckBundleFilteringTextInput();
            process.execute();
        }
    }
    private class CheckBundleFilteringTextInput extends SwingWorker<Boolean,Boolean> {
        long oldLastTypedWordInFindTextField;
        @Override
        protected Boolean doInBackground() throws Exception {
            oldLastTypedWordInFindTextField = lastTypedWordInFindTextField;
            return System.currentTimeMillis()-oldLastTypedWordInFindTextField > LAUNCH_SEARCH_IDLE_TIME;
        }

        @Override
        public String toString() {
            return "MainPanel#CheckBundleFilteringTextInput";
        }

        @Override
        protected void done() {
            try {
                Boolean executeFilters = get();
                if(executeFilters) {
                    applyFilters();
                }
                awaitingFilteringThread.set(false);
                // If the user type a letter or validate while filtering, launch the filter again
                if(!executeFilters ||
                        oldLastTypedWordInFindTextField!=lastTypedWordInFindTextField) {
                    launchTextFindProcess();
                }
            } catch(Exception ex) {
                awaitingFilteringThread.set(false);
                LOGGER.error(ex.getLocalizedMessage(),ex);
            }
        }
    }
    private class DownloadOBRProcess extends SwingWorker {
        @Override
        protected Object doInBackground() throws Exception {
            repositoryAdminTrackerCustomizer.refresh();
            Thread.sleep(5000);
            return null;
        }

        @Override
        public String toString() {
            return "MainPanel#DownloadOBRProcess";
        }

        @Override
        protected void done() {
            hideWait();
            bundleListModel.update();
        }
    }
    private static class ProgressLayerUI extends LayerUI<JPanel> implements ImageObserver {
        private int interpolationCount;
        private int interpolationMax = 15;
        private int interpolationDrawn;
        private boolean running;
        private boolean blackInterpolating;
        private ImageIcon icon;
        private static final float LAYER_OPACITY = 0.5f;
        private String message;
        private Font messageFont;


        /**
         * @param icon Icon
         */
        public void setWaitIcon(ImageIcon icon) {
            this.icon = icon;
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
                else if (interpolationCount < interpolationMax) {
                    if(interpolationDrawn == interpolationCount) {
                        interpolationCount++;
                    }
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
            float fade = Math.max(0, Math.min(1, (float) interpolationCount / (float) interpolationMax));
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
            g2.drawString(message, (int)(( w / 2.f ) - (textSize.getWidth() / 2.f)), (int)(h / 2 + iconHeight / 2 + textSize.getHeight()));
            g2.setComposite(urComposite);
            g2.dispose();
            interpolationDrawn = interpolationCount;
        }

        public void start() {
            if (running) {
                return;
            }
            // Run a thread for animation.
            running = true;
            blackInterpolating = false;
            interpolationCount = 0;
            firePropertyChange("interpolationCount", null, interpolationCount);
        }

        public void stop() {
            blackInterpolating = true;
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
        private void setMessage(String message) {
            this.message = message;
        }
    }
}
