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

package org.orbisgis.orbistoolbox.view.utils;

import org.orbisgis.orbistoolbox.controller.processexecution.ExecutionWorker;
import org.orbisgis.orbistoolbox.model.Process;
import org.orbisgis.orbistoolbox.view.ToolBox;
import org.orbisgis.orbistoolbox.view.ui.ProcessUIPanel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * @author Sylvain PALOMINOS
 **/

public class ProcessExecutionData {

    public static final String STATE_PROPERTY = "STATE_PROPERTY";

    /** Map of input data (URI of the corresponding input) */
    private Map<URI, Object> inputDataMap;
    /** Map of output data (URI of the corresponding output) */
    private Map<URI, Object> outputDataMap;
    /** Process represented */
    private Process process;
    /**State of the process */
    private ProcessState state;
    private ToolBox toolBox;
    private ProcessUIPanel processUIPanel;
    /** Map of the log message and their color.*/
    private Map<String, Color> logMap;
    /** List of listeners for the processState*/
    private List<PropertyChangeListener> propertyChangeListenerList;

    public ProcessExecutionData(ToolBox toolBox, Process process){
        this.toolBox = toolBox;
        this.process = process;
        this.outputDataMap = new HashMap<>();
        this.inputDataMap = new HashMap<>();
        this.logMap = new LinkedHashMap<>();
        this.propertyChangeListenerList = new ArrayList<>();
    }

    public Map<String, Color> getLogMap(){
        return logMap;
    }

    public Map<URI, Object> getInputDataMap() {
        return inputDataMap;
    }

    public void setInputDataMap(Map<URI, Object> inputDataMap) {
        this.inputDataMap = inputDataMap;
    }

    public Map<URI, Object> getOutputDataMap() {
        return outputDataMap;
    }

    public void setOutputDataMap(Map<URI, Object> outputDataMap) {
        this.outputDataMap = outputDataMap;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        ProcessState oldState = this.state;
        this.state = state;
        firePropertyChange(oldState, state);
        toolBox.getComponent().revalidate();
        toolBox.getComponent().repaint();
    }

    public void setProcessUIPanel(ProcessUIPanel processUIPanel) {
        this.processUIPanel = processUIPanel;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener){
        if(!propertyChangeListenerList.contains(propertyChangeListener)){
            propertyChangeListenerList.add(propertyChangeListener);
        }
    }

    public void firePropertyChange(ProcessState oldValue, ProcessState newValue){
        PropertyChangeEvent event = new PropertyChangeEvent(process, STATE_PROPERTY, oldValue, newValue);
        for(PropertyChangeListener pcl : propertyChangeListenerList){
            pcl.propertyChange(event);
        }
    }

    /**
     * Run the process.
     */
    public void runProcess(){
        logMap = new HashMap<>();
        //Check that all the data field were filled.
        if(inputDataMap.size() == process.getInput().size()) {
            setState(ProcessState.RUNNING);
            //Run the process in a separated thread
            ExecutionWorker thread = new ExecutionWorker(process, outputDataMap, inputDataMap, toolBox, this);
            thread.execute();
        }
    }

    /**
     * Indicated that the process has ended and register the outputs results.
     * @param outputList Map of the outputs results.
     */
    public void endProcess(List<String> outputList){
        setState(ProcessState.COMPLETED);
        processUIPanel.setOutputs(outputList, ProcessState.COMPLETED.getValue());
        if(processUIPanel != null && SwingUtilities.getWindowAncestor(processUIPanel).isVisible()){
            this.setState(ProcessExecutionData.ProcessState.IDLE);
        }
    }

    /**
     * Append to the log a new entry.
     * @param time Time since the beginning when it appends.
     * @param logType Type of the message (INFO, WARN, ERROR ...).
     * @param message Message.
     */
    public void appendLog(long time, LogType logType, String message){
        Color color;
        switch(logType){
            case ERROR:
                color = Color.RED;
                break;
            case WARN:
                color = Color.ORANGE;
                break;
            case INFO:
            default:
                color = Color.BLACK;
        }
        Date date = new Date(time - 3600000);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String log = timeFormat.format(date) + " : " + logType.name() + " : " + message + "";
        logMap.put(log, color);
        if(processUIPanel != null){
            processUIPanel.print(log, color);
        }
    }


    public enum ProcessState{
        RUNNING("Running"),
        COMPLETED("Completed"),
        ERROR("Error"),
        IDLE("Idle");

        private String value;

        ProcessState(String value){
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum LogType{INFO, WARN, ERROR}
}
