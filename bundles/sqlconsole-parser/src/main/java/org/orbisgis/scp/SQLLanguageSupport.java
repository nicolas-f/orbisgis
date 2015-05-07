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
package org.orbisgis.scp;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.rsta.ac.LanguageSupport;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.h2.util.OsgiDataSourceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides the support for Gdms SQL syntax in the console.
 * 
 * This class needs a registered SQLMetadataManager in order to work, i.e.
 * the code below should return a valid instance of SQLMetadataManager
 * <code>
 * SQLMetadataManager metManager = Services.getService(SQLMetadataManager.class);
 * </code>
 * 
 * This class installs the following on the text area
 *  - a Parser implementation that highlight error in the SQL
 *  - a CompletionProvider that auto-completes SQL queries
 * 
 * @author Antoine Gourlay
 * @author Nicolas Fortin
 */
@Component(service = LanguageSupport.class ,servicefactory = true, property = {"language=sql"})
public class SQLLanguageSupport extends AbstractLanguageSupport {
        private Logger log = LoggerFactory.getLogger(SQLLanguageSupport.class);
        private RSyntaxSQLParser parser;
        private SQLCompletionProvider sqlCompletionProvider;
        private DataSource dataSource;

        @Override
        public void install(RSyntaxTextArea textArea) {
                // install parser
                try {
                        if(dataSource == null) {
                            // Create H2 memory DataSource
                            org.h2.Driver driver = org.h2.Driver.load();
                            OsgiDataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(driver);
                            Properties properties = new Properties();
                            properties.setProperty(DataSourceFactory.JDBC_URL, "jdbc:h2:mem:syntax");
                            dataSource = dataSourceFactory.createDataSource(properties);
                        }
                        // SQL parser take too much time to evaluate, currently deactivated
                        //parser = new RSyntaxSQLParser(dataSource);
                        //textArea.putClientProperty(PROPERTY_LANGUAGE_PARSER, parser);
                        //textArea.addParser(parser);

                        // install auto-completion
                        sqlCompletionProvider = new SQLCompletionProvider(dataSource, false);
                        AutoCompletion autoCompletion = createAutoCompletion(sqlCompletionProvider);
                        autoCompletion.setAutoCompleteSingleChoices(true);
                        autoCompletion.setShowDescWindow(true);
                        autoCompletion.install(textArea);
                        installImpl(textArea, autoCompletion);
                } catch (SQLException ex) {
                    log.error(ex.getLocalizedMessage(), ex);
                }

        }

        /**
         * @param dataSource DataSource to use in auto completion
         */
        @Reference
        public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            if( parser != null ) {
                parser.setDataSource(dataSource);
            }
            if(sqlCompletionProvider!=null) {
                sqlCompletionProvider.setDataSource(dataSource);
            }
        }

        /**
         * @param dataSource DataSource to unset
         */
        public void unsetDataSource(DataSource dataSource) {
            this.dataSource = null;
            if( parser!=null ) {
                parser.setDataSource(null);
            }
            if(sqlCompletionProvider!=null) {
                sqlCompletionProvider.setDataSource(null);
            }
        }


        @Override
        public void uninstall(RSyntaxTextArea textArea) {
                // remove completion
                uninstallImpl(textArea);
                
                // remove parser
                Object parser = textArea.getClientProperty(PROPERTY_LANGUAGE_PARSER);
                if(parser instanceof RSyntaxSQLParser) {
                    textArea.removeParser((Parser)parser);
                }
                textArea.putClientProperty(PROPERTY_LANGUAGE_PARSER, null);
        }
}
