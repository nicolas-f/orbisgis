//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.11.26 at 05:18:02 PM CET 
//


package org.orbisgis.views.geocatalog.persistence;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.orbisgis.views.geocatalog.persistence package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.orbisgis.views.geocatalog.persistence
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ActiveFilter }
     * 
     */
    public ActiveFilter createActiveFilter() {
        return new ActiveFilter();
    }

    /**
     * Create an instance of {@link Catalog }
     * 
     */
    public Catalog createCatalog() {
        return new Catalog();
    }

    /**
     * Create an instance of {@link Tag }
     * 
     */
    public Tag createTag() {
        return new Tag();
    }

}
