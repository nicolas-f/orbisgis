/**
 * OrbisToolBox is an OrbisGIS plugin dedicated to create and manage processing.
 * <p/>
 * OrbisToolBox is distributed under GPL 3 license. It is produced by CNRS <http://www.cnrs.fr/> as part of the
 * MApUCE project, funded by the French Agence Nationale de la Recherche (ANR) under contract ANR-13-VBDU-0004.
 * <p/>
 * OrbisToolBox is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p/>
 * OrbisToolBox is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with OrbisToolBox. If not, see
 * <http://www.gnu.org/licenses/>.
 * <p/>
 * For more information, please consult: <http://www.orbisgis.org/> or contact directly: info_at_orbisgis.org
 */

package org.orbisgis.orbistoolbox.view.ui;

import net.miginfocom.swing.MigLayout;
import org.orbisgis.orbistoolbox.model.*;
import org.orbisgis.orbistoolbox.model.Output;
import org.orbisgis.orbistoolbox.model.Process;
import org.orbisgis.orbistoolbox.view.ToolBox;
import org.orbisgis.orbistoolbox.view.ui.dataui.DataUI;
import org.orbisgis.orbistoolbox.view.ui.dataui.DataUIManager;
import org.orbisgis.orbistoolbox.view.utils.ProcessExecutionData;
import org.orbisgis.orbistoolbox.view.utils.TreeNodeWps;
import org.orbisgis.sif.UIPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Frame used to configure and run a process.
 *
 * @author Sylvain PALOMINOS
 **/

public class ProcessUIPanel extends JPanel implements UIPanel {

    /** TabbedPane containing the configuration panel, the info panel and the execution panel */
    private JTabbedPane tabbedPane;
    /** Map of the label containing the outputs values and their identifier*/
    private List<JLabel> outputJLabelList;
    /** DataUIManager used to create the UI corresponding the the data */
    private DataUIManager dataUIManager;
    /** Label containing the state of the process (running, completed or idle) */
    private JLabel stateLabel;

    private ProcessExecutionData processExecutionData;
    /**TextPane used to display the process execution log.*/
    private JTextPane logPane;

    /**
     * Main constructor with no ProcessExecutionData.
     * @param process Process represented.
     * @param toolBox Toolbox
     */
    public ProcessUIPanel(Process process, ToolBox toolBox) {
        this.setLayout(new BorderLayout());

        outputJLabelList = new ArrayList<>();
        dataUIManager = toolBox.getDataUIManager();

        processExecutionData = new ProcessExecutionData(toolBox, process);
        processExecutionData.setState(ProcessExecutionData.ProcessState.IDLE);
        processExecutionData.setProcessUIPanel(this);
        processExecutionData.setInputDataMap(dataUIManager.getInputDefaultValues(process));
        processExecutionData.setOutputDataMap(dataUIManager.getOutputDefaultValues(process));

        toolBox.saveProcessExecutionData(processExecutionData);

        buildUI();
    }

    /**
     * Constructor with an existing processUIData.
     * @param processExecutionData Data for the UI
     * @param toolBox ToolBox
     */
    public ProcessUIPanel(ProcessExecutionData processExecutionData, ToolBox toolBox){
        this.setLayout(new BorderLayout());
        this.processExecutionData = processExecutionData;

        outputJLabelList = new ArrayList<>();
        dataUIManager = toolBox.getDataUIManager();

        buildUI();
        processExecutionData.setProcessUIPanel(this);

        //According to the process state, open the good tab
        switch(processExecutionData.getState()){
            case IDLE:
                tabbedPane.setSelectedIndex(0);
                break;
            case RUNNING:
                tabbedPane.setSelectedIndex(2);
                break;
            case COMPLETED:
            case ERROR:
                List<String> results = new ArrayList<>();
                for(Map.Entry<URI, Object> entry : processExecutionData.getOutputDataMap().entrySet()){
                    results.add(entry.getValue().toString());
                }
                setOutputs(results, processExecutionData.getState().toString());
                tabbedPane.setSelectedIndex(2);
                break;
        }
        //Print the process execution log.
        for(Map.Entry<String, Color> entry : processExecutionData.getLogMap().entrySet()){
            print(entry.getKey(), entry.getValue());
        }

        processExecutionData.setState(ProcessExecutionData.ProcessState.IDLE);
    }

    /**
     * Build the UI of the ProcessFrame with the data of the processUIData.
     */
    private void buildUI(){
        //Adds to the tabbedPane the 3 panels
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Configuration", buildUIConf(processExecutionData));
        tabbedPane.addTab("Information", buildUIInfo(processExecutionData));
        tabbedPane.addTab("Execution", buildUIExec(processExecutionData));
        this.add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Returns the processExecutionData.
     * @return The processExecutionData.
     */
    public ProcessExecutionData getProcessExecutionData(){
        return processExecutionData;
    }

    /**
     * Run the process.
     * @return True if the process has already been launch, false otherwise.
     */
    public boolean runProcess(){
        if(processExecutionData.getState().equals(ProcessExecutionData.ProcessState.IDLE) ||
                processExecutionData.getState().equals(ProcessExecutionData.ProcessState.ERROR) ||
                processExecutionData.getState().equals(ProcessExecutionData.ProcessState.COMPLETED)) {
            clearLogPanel();
            processExecutionData.runProcess();
            //Select the execution tab
            stateLabel.setText(processExecutionData.getState().getValue());
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * Build the UI of the given process according to the given data.
     * @param processExecutionData Process data.
     * @return The UI for the configuration of the process.
     */
    public JComponent buildUIConf(ProcessExecutionData processExecutionData){
        JPanel panel = new JPanel(new MigLayout("fill"));
        //For each input, display its title, its abstract and gets its UI from the dataUIManager
        for(Input i : processExecutionData.getProcess().getInput()){
            JPanel inputPanel = new JPanel(new MigLayout("fill"));
            inputPanel.setBorder(BorderFactory.createTitledBorder(i.getTitle()));
            JLabel inputAbstrac = new JLabel(i.getResume());
            inputAbstrac.setFont(inputAbstrac.getFont().deriveFont(Font.ITALIC));
            inputPanel.add(inputAbstrac, "wrap");
            DataUI dataUI = dataUIManager.getDataUI(i.getDataDescription().getClass());
            if(dataUI!=null) {
                inputPanel.add(dataUI.createUI(i, processExecutionData.getInputDataMap()), "wrap");
            }
            panel.add(inputPanel, "growx, wrap");
        }

        //For each output, display its title, its abstract and gets its UI from the dataUIManager
        for(Output o : processExecutionData.getProcess().getOutput()){
            DataUI dataUI = dataUIManager.getDataUI(o.getDataDescription().getClass());
            if(dataUI!=null) {
                JComponent component = dataUI.createUI(o, processExecutionData.getOutputDataMap());
                if(component != null) {
                    JPanel outputPanel = new JPanel(new MigLayout("fill"));
                    outputPanel.setBorder(BorderFactory.createTitledBorder(o.getTitle()));
                    JLabel outputAbstrac = new JLabel(o.getResume());
                    outputAbstrac.setFont(outputAbstrac.getFont().deriveFont(Font.ITALIC));
                    outputPanel.add(outputAbstrac, "wrap");
                    outputPanel.add(component, "wrap");
                    panel.add(outputPanel, "growx, wrap");
                }
            }
        }
        return panel;
    }

    /**
     * Build the UI of the given process according to the given data.
     * @param processExecutionData Process data.
     * @return The UI for the configuration of the process.
     */
    public JComponent buildUIInfo(ProcessExecutionData processExecutionData){
        JPanel panel = new JPanel(new MigLayout("fill"));
        Process p  = processExecutionData.getProcess();
        //Process info
        JLabel titleContentLabel = new JLabel(p.getTitle());
        JLabel abstracContentLabel = new JLabel();
        if(p.getResume() != null) {
            abstracContentLabel.setText(p.getResume());
        }
        else{
            abstracContentLabel.setText("-");
            abstracContentLabel.setFont(abstracContentLabel.getFont().deriveFont(Font.ITALIC));
        }

        JPanel processPanel = new JPanel(new MigLayout());
        processPanel.setBorder(BorderFactory.createTitledBorder("Process :"));
        processPanel.add(titleContentLabel, "wrap, align left");
        processPanel.add(abstracContentLabel, "wrap, align left");

        //Input info
        JPanel inputPanel = new JPanel(new MigLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Inputs :"));

        for(Input i : p.getInput()){
            inputPanel.add(new JLabel(dataUIManager.getIconFromData(i)));
            inputPanel.add(new JLabel(i.getTitle()), "align left, wrap");
            if(i.getResume() != null) {
                JLabel abstrac = new JLabel(i.getResume());
                abstrac.setFont(abstrac.getFont().deriveFont(Font.ITALIC));
                inputPanel.add(abstrac, "span 2, wrap");
            }
            else {
                inputPanel.add(new JLabel("-"), "span 2, wrap");
            }
        }

        //Output info
        JPanel outputPanel = new JPanel(new MigLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Outputs :"));

        for(Output o : p.getOutput()){
            outputPanel.add(new JLabel(dataUIManager.getIconFromData(o)));
            outputPanel.add(new JLabel(o.getTitle()), "align left, wrap");
            if(o.getResume() != null) {
                JLabel abstrac = new JLabel(o.getResume());
                abstrac.setFont(abstrac.getFont().deriveFont(Font.ITALIC));
                outputPanel.add(abstrac, "span 2, wrap");
            }
            else {
                outputPanel.add(new JLabel("-"), "align center, span 2, wrap");
            }
        }

        panel.add(processPanel, "growx, wrap");
        panel.add(inputPanel, "growx, wrap");
        panel.add(outputPanel, "growx, wrap");

        return panel;
    }

    /**
     * Build the UI of the given process according to the given data.
     * @param processExecutionData Process data.
     * @return The UI for the configuration of the process.
     */
    public JComponent buildUIExec(ProcessExecutionData processExecutionData){
        JPanel panel = new JPanel(new MigLayout("fill"));

        JPanel executorPanel = new JPanel(new MigLayout());
        executorPanel.setBorder(BorderFactory.createTitledBorder("Executor :"));
        executorPanel.add(new JLabel("localhost"));

        JPanel statusPanel = new JPanel(new MigLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status :"));
        stateLabel = new JLabel(processExecutionData.getState().getValue());
        statusPanel.add(stateLabel);

        JPanel resultPanel = new JPanel(new MigLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Result :"));
        for(Output o : processExecutionData.getProcess().getOutput()) {
            JLabel title = new JLabel(o.getTitle()+" : ");
            JLabel result = new JLabel();
            result.putClientProperty("URI", o.getIdentifier());
            outputJLabelList.add(result);
            resultPanel.add(title);
            resultPanel.add(result, "wrap");
        }

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log :"));
        logPane = new JTextPane();
        logPane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(logPane);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(executorPanel, "growx, wrap");
        panel.add(statusPanel, "growx, wrap");
        panel.add(resultPanel, "growx, wrap");
        panel.add(logPanel, "growx, growy, wrap");

        return panel;
    }

    /**
     * Sets the outputs label with the outputs results.
     * @param outputs Outputs results.
     */
    public void setOutputs(List<String> outputs, String state) {
        for(int i=0; i<processExecutionData.getProcess().getOutput().size(); i++) {
            outputJLabelList.get(i).setText(outputs.get(i));
        }
        stateLabel.setText(state);
    }

    @Override
    public URL getIconURL() {
        return null;
    }

    @Override
    public String getTitle() {
        return processExecutionData.getProcess().getTitle();
    }

    @Override
    public String validateInput() {
        //In each case return a string to avoid the SIFDialog close on clicking on running.
        if(!runProcess()){
            return "";
        }
        else{
            return "Process already running";
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

    /**
     * Add the provided text with the provided color to the GUI document
     * @param text The text that will be added
     * @param color The color used to show the text
     */
    public void print(String text, Color color) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
                StyleConstants.Foreground, color);
        int len = logPane.getDocument().getLength();
        try {
            logPane.setCaretPosition(len);
            logPane.getDocument().insertString(len, text+"\n", aset);
        } catch (BadLocationException e) {
            LoggerFactory.getLogger(ProcessUIPanel.class).error("Cannot show the log message", e);
        }
        logPane.setCaretPosition(logPane.getDocument().getLength());
    }

    /**
     * Clear the log panel.
     */
    public void clearLogPanel(){
        try {
            logPane.getDocument().remove(1, logPane.getDocument().getLength() - 1);
        } catch (BadLocationException e) {
            LoggerFactory.getLogger(ProcessUIPanel.class).error(e.getMessage());
        }
    }

    /**
     * Add to the processExecutionData all the node concerned by the process execution state.
     * @param listNode List of node concerned by the process state.
     */
    public void setProcessStateListener(List<TreeNodeWps> listNode){
        for(TreeNodeWps node : listNode){
            processExecutionData.addPropertyChangeListener(node);
        }
    }
}
