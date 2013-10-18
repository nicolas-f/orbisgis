package org.orbisgis.omanager.ui;

import org.apache.log4j.Logger;
import org.osgi.framework.Constants;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Contains Bundle description text and bundle dependency tab.
 * @author Nicolas Fortin
 */
public class BundleDetailsPanel extends JTabbedPane {
    private static final I18n I18N = I18nFactory.getI18n(BundleDetailsPanel.class);
    private static final Logger LOGGER = Logger.getLogger(BundleDetailsPanel.class);
    private static final int PROPERTY_TEXT_SIZE_INCREMENT = 3;
    private static final int PROPERTY_TITLE_SIZE_INCREMENT = 4;

    private JTextPane bundleDetails = new JTextPane();
    private DefaultTableModel tableModel = new DefaultTableModel();
    private JTable bundleDependency = new JTable(tableModel);
    private BundleDetailsTransformer bundleHeader = new BundleDetailsTransformer();

    /**
     * Create tabs.
     */
    public void init() {
        addTab(I18N.tr("Description"), new JScrollPane(bundleDetails));
        addTab(I18N.tr("Dependency"), new JScrollPane(bundleDependency));
        bundleDetails.setEditable(false);
    }

    /**
     * @param selectedItem Load view content from this item
     */
    public void loadBundleItem(BundleItem selectedItem) {
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
                if(originalKey.equalsIgnoreCase("Bnd-LastModified")) {
                    Date bundleUpdate = new Date(Long.valueOf(entry.getValue()));
                    addDescriptionItem(key, DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG).format(bundleUpdate),document);
                } else {
                    addDescriptionItem(key,entry.getValue(),document);
                }
            }
        }
        bundleDetails.setCaretPosition(0); // Got to the beginning of the document
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
}
