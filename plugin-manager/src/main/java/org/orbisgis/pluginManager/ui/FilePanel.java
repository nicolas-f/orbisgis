/**
 *
 */
package org.orbisgis.pluginManager.ui;

import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;

import org.orbisgis.pluginManager.util.SimpleFileFilter;
import org.sif.SQLUIPanel;

public class FilePanel implements SQLUIPanel {

	public static final String FIELD_NAME = "file";

	public JFileChooser fileChooser;

	private Map<String, String> formatAndDescription;

	public FilePanel(Map<String, String> formatAndDescription) {

		this.formatAndDescription = formatAndDescription;
	}

	public FilePanel() {
		formatAndDescription = new HashMap<String, String>();
	}

	public Component getComponent() {
		return getFileChooser();
	}

	protected JFileChooser getFileChooser() {
		fileChooser = new JFileChooser();
		fileChooser.setControlButtonsAreShown(false);
		fileChooser.setMultiSelectionEnabled(true);

		if (formatAndDescription != null) {
			for (String key : formatAndDescription.keySet()) {
				fileChooser.addChoosableFileFilter(new SimpleFileFilter(key,
						formatAndDescription.get(key)));
			}

		}
		return fileChooser;
	}

	public URL getIconURL() {
		return null;
	}

	public String getTitle() {
		return "New file";
	}

	public void initialize() {

	}

	public String validate() {
		if (fileChooser.getSelectedFile() == null) {
			return "An element have to be selected";
		}

		return null;
	}

	public String[] getErrorMessages() {
		return new String[0];
	}

	public String[] getFieldNames() {
		return new String[] { FIELD_NAME };
	}

	public int[] getFieldTypes() {
		return new int[] { SQLUIPanel.STRING };
	}

	public String getId() {
		return FileWizard.FILE_CHOOSER_SIF_ID;
	}

	public String[] getValidationExpressions() {
		return new String[0];
	}

	public String[] getValues() {
		String ret = "";
		File[] selectedFiles = fileChooser.getSelectedFiles();
		String separator = "";
		for (File file : selectedFiles) {
			ret = ret + separator + file.getAbsolutePath();
			separator = "||";
		}

		return new String[] { ret };
	}

	public void setValue(String fieldName, String fieldValue) {
		String[] files = fieldValue.split("\\Q||\\E");
		File[] selectedFiles = new File[files.length];
		for (int i = 0; i < selectedFiles.length; i++) {
			selectedFiles[i] = new File(files[i]);
		}
		fileChooser.setSelectedFiles(selectedFiles);
	}

	public File getSelectedFile() {
		return fileChooser.getSelectedFile();
	}

	public File[] getSelectedFiles() {
		return fileChooser.getSelectedFiles();
	}
}