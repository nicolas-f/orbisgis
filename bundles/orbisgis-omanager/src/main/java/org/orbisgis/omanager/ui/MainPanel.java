/*
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
package org.orbisgis.omanager.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.orbisgis.omanager.plugin.api.CustomPlugin;
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
public class MainPanel extends JPanel implements CustomPlugin {
    private static final Dimension MINIMUM_BUNDLE_LIST_DIMENSION = new Dimension(100,50);
    private static final Dimension MINIMUM_BUNDLE_DESCRIPTION_DIMENSION = new Dimension(250,50);
    private static final int MINIMUM_SEARCH_COLUMNS = 10;
    private static final I18n I18N = I18nFactory.getI18n(MainPanel.class);
    private static final Logger LOGGER = LoggerFactory.getLogger("gui."+MainPanel.class);
    private static final int BORDER_PIXEL_GAP = 2;
    private static final int PROPERTY_TEXT_SIZE_INCREMENT = 3;
    private static final int PROPERTY_TITLE_SIZE_INCREMENT = 4;
    private static final String DEFAULT_CATEGORY = "orbisgis";
    private ItemFilterStatusFactory.Status radioFilterStatus = ItemFilterStatusFactory.Status.ALL;
    private Map<String,ImageIcon> buttonIcons = new HashMap<>();

    // Bundle Category filter
    private JTextField bundleSearchField = new JTextField(MINIMUM_SEARCH_COLUMNS);
    private JTextPane bundleDetails = new JTextPane();
    private JList<BundleItem> bundleList = new JList<>();
    private JPanel bundleActions = new JPanel();
    private JButton repositoryRemove;
    private BundleListModel bundleListModel;
    private FilteredModel<BundleItem, BundleListModel> filterModel;
    private JPanel bundleDetailsAndActions = new JPanel(new BorderLayout());
    private JSplitPane splitPane;
    private ActionBundleFactory actionFactory;
    private BundleDetailsTransformer bundleHeader = new BundleDetailsTransformer();
    private BundleContext bundleContext;
    private RepositoryAdminTracker repositoryAdminTrackerCustomizer;
    private ServiceTracker<RepositoryAdmin,RepositoryAdmin> repositoryAdminTracker;
    private AtomicBoolean awaitingFilteringThread = new AtomicBoolean(false);
    private long lastTypedWordInFindTextField = 0;
    /**
     * in ms Launch a search if the user don't type any character within this time.
     */
    private static final long LAUNCH_SEARCH_IDLE_TIME = 300;
    private final boolean isPlugin;
    /**
     * Constructor of the main plugin panel
     * @param isPlugin specify plugin category
     */
    public MainPanel(boolean isPlugin) {
        super(new BorderLayout());
        this.isPlugin=isPlugin;
    }

    /**
     * @param bundleContext Bundle context instance in order to manage them.
     */
    protected void initialize(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        initRepositoryTracker();
        actionFactory = new ActionBundleFactory(bundleContext,this, isPlugin);
        // Main Panel (South button, center Split Pane)
        // Buttons on south of main panel
        JPanel southButtons = new JPanel();
        southButtons.setLayout(new BoxLayout(southButtons, BoxLayout.X_AXIS));
        addSouthButtons(southButtons);
        add(southButtons, BorderLayout.SOUTH);
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
        add(splitPane);
        bundleListModel =  new BundleListModel(bundleContext,repositoryAdminTrackerCustomizer);
        filterModel = new FilteredModel<>(bundleListModel);
        bundleList.setModel(filterModel);
        bundleListModel.install();
        bundleList.setCellRenderer(new BundleListRenderer(bundleList));
        bundleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bundleList.addListSelectionListener(EventHandler.create(ListSelectionListener.class,this,"onBundleSelectionChange",""));
        applyFilters();
    }

    /**
     * Get an icon for a button
     * @param iconName
     * @return 
     */
    private ImageIcon getBtIcon(String iconName) {
        ImageIcon icon = buttonIcons.get(iconName);
        if(icon==null) {
            try {
                icon = new ImageIcon(MainPanel.class.getResource(iconName + ".png"));
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
        repositoryAdminTracker = new ServiceTracker<>(bundleContext,
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
        final BundleItem selectedItem = bundleList.getSelectedValue();
        if(selectedItem != null) {
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
    
    private void addSouthButtons(JPanel southButtons) {

        JButton addFile = new ButtonIcon(getBtIcon("install_plugin_from_disk"));
        addFile.setToolTipText(I18N.tr("Add a plugin from disk. Dependencies are not automatically resolved."));
        addFile.addActionListener(EventHandler.create(ActionListener.class, this, "onAddBundleJar"));
        southButtons.add(addFile);

        JButton addUrl = new ButtonIcon(getBtIcon("install_plugin_from_url"));
        addUrl.setToolTipText(I18N.tr("Add a plugin from a URL (file:// or http(s)://). Dependencies are not automatically resolved."));
        addUrl.addActionListener(EventHandler.create(ActionListener.class,this,"onAddBundleJarUri"));
        southButtons.add(addUrl);

        southButtons.add(new JSeparator(JSeparator.VERTICAL));
        JButton repositoryUrls = new ButtonIcon(getBtIcon("repository_add"));
        repositoryUrls.setToolTipText(I18N.tr("Add a remote bundle repository."));
        repositoryUrls.addActionListener(EventHandler.create(ActionListener.class,this,"onAddBundleRepository"));
        southButtons.add(repositoryUrls);

        repositoryRemove = new ButtonIcon(getBtIcon("repository_remove"));
        repositoryRemove.setToolTipText(I18N.tr("Remove a remote bundle repository."));
        repositoryRemove.addActionListener(EventHandler.create(ActionListener.class,this,"onRemoveBundleRepository"));
        southButtons.add(repositoryRemove);
        onRepositoryChange();

        JButton refreshRepositories = new ButtonIcon(getBtIcon("repository_refresh"));
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
            LOGGER.error(ex.getLocalizedMessage(), ex);
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
        for (Map.Entry<String, String> entry : itemDetails.entrySet()) {
            String originalKey = entry.getKey();
            String key = bundleHeader.convert(originalKey);
            if (!key.isEmpty() && !entry.getValue().isEmpty()) {
                if (originalKey.equalsIgnoreCase("Bnd-LastModified")) {
                    Date bundleUpdate = new Date(Long.valueOf(entry.getValue()));
                    addDescriptionItem(key, DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(bundleUpdate), document);
                } else {
                    addDescriptionItem(key, entry.getValue(), document);
                }
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
        List<ItemFilter<BundleListModel>> filters = new ArrayList<>();
        ItemFilter<BundleListModel> radioFilter = ItemFilterStatusFactory.getFilter(radioFilterStatus);
        if(radioFilter!=null) {
            filters.add(radioFilter);
        }
        
        if(isPlugin){
            filters.add(new ItemFilterCategory(DEFAULT_CATEGORY));
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
            for(File selected : selection) {
                try {
                    LOGGER.info(I18N.tr("Install bundle {0}", selected.toURI().toString()));
                    bundleContext.installBundle(selected.toURI().toString());
                } catch (BundleException ex) {
                    LOGGER.error(ex.getLocalizedMessage(),ex);
                }
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

    @Override
    public Component getGUI() {
        return this;
    }

    @Override
    public String getName() {
        if(isPlugin){
            return I18N.tr("Plugins");
        }
        return I18N.tr("Systems");
    }

    @Override
    public Icon getIcon() {
        if (isPlugin) {
            return new ImageIcon(MainPanel.class.getResource("defaultIcon.png"));
        } else {
            return new ImageIcon(MainPanel.class.getResource("plugin_system.png"));
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
            return null;
        }

        @Override
        public String toString() {
            return "MainPanel#DownloadOBRProcess";
        }

        @Override
        protected void done() {
            bundleListModel.update();
        }
    }
}
