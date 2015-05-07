package org.orbisgis.scp;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.h2.util.OsgiDataSourceFactory;
import org.junit.Test;

import java.awt.*;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * @author Nicolas Fortin
 */
public class SQLCompletionProviderTest {

    @Test //(timeout = 500)
    public void testBounds() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless());
        // Create H2 DataSource
        org.h2.Driver driver = org.h2.Driver.load();
        OsgiDataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(driver);
        Properties properties = new Properties();
        properties.setProperty(OsgiDataSourceFactory.JDBC_URL, RSyntaxSQLParserTest.DATABASE_PATH);

        // Create document
        SQLCompletionProvider autoComplete = new SQLCompletionProvider(dataSourceFactory.createDataSource(properties), true);

        List completions = autoComplete.getCompletionByInputText("alte");
        assertEquals(1, completions.size());
        assertEquals("ALTER", ((Completion)completions.get(0)).getReplacementText());
    }

    @Test //(timeout = 500)
    public void testBoundsMultiLine() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless());
        // Create H2 DataSource
        org.h2.Driver driver = org.h2.Driver.load();
        OsgiDataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(driver);
        Properties properties = new Properties();
        properties.setProperty(OsgiDataSourceFactory.JDBC_URL, RSyntaxSQLParserTest.DATABASE_PATH);

        // Create document
        SQLCompletionProvider autoComplete = new SQLCompletionProvider(dataSourceFactory.createDataSource(properties), true);

        List completions = autoComplete.getCompletionByInputText("CREATE TABLE toto as select 1;\n");
        assertEquals(1, completions.size());
        assertEquals("TABLE ", ((Completion)completions.get(0)).getReplacementText());
    }
}
