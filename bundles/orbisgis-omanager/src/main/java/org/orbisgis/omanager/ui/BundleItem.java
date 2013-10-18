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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.obr.Resource;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * A bundle that can be installed from local or/and on remote repository.
 * @author Nicolas Fortin
 */
public class BundleItem {
    private static final I18n I18N = I18nFactory.getI18n(BundleItem.class);
    private static final int MAX_SHORT_DESCRIPTION_CHAR_COUNT = 50;
    private String shortDesc;
    private Resource obrResource;      // only if a remote bundle is available
    private long bundleId = -1;        // Bundle id
    private BundleContext bundleContext;
    private static final Long KILO = 1024L;
    private static final Long MEGA = KILO * KILO;
    private static final Long LONG = MEGA * KILO;
    private static final Long TERA = LONG * KILO;
    private Dictionary<String,String> headers;

    /**
     * Constructor
     * @param bundleContext Active bundle context
     */
    public BundleItem(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @param bytes Bytes count
     * @return User Friendly output of this size
     */
    public static String getHumanReadableBytes(long bytes) {
        if(bytes >= TERA) {
            return String.format(I18N.tr("%.2f TB"),(double)bytes / TERA);
        } else if(bytes >= LONG) {
            return String.format(I18N.tr("%.2f GB"),(double)bytes / LONG);
        } else if(bytes >= MEGA) {
            return String.format(I18N.tr("%.2f MB"),(double)bytes / MEGA);
        } else if(bytes >= KILO) {
            return String.format(I18N.tr("%.2f kB"),(double)bytes / KILO);
        } else {
            return String.format(I18N.tr("%d bytes"),bytes);
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BundleItem)) {
            return false;
        }
        BundleItem other = (BundleItem)o;
        return getSymbolicName().equals(other.getSymbolicName()) && getVersion().equals(other.getVersion());
    }

    @Override
    public int hashCode() {
        return getSymbolicName().hashCode() + getVersion().hashCode();
    }
    /**
     * Returns the symbolic name of this bundle.
     * @return The symbolic name of this bundle or empty string if this bundle
     *         does not have a symbolic name.
     */
    String getSymbolicName()  {
        Bundle bundle = getBundle();
        if(bundle!=null) {
            return bundle.getSymbolicName();
        } else if(obrResource!=null){
            return obrResource.getSymbolicName();
        } else {
            return "";
        }
    }

    /**
     * @return Bundle version
     */
    Version getVersion() {
        Bundle bundle = getBundle();
        if(bundle!=null) {
            return bundle.getVersion();
        } else if(obrResource!=null) {
            return obrResource.getVersion();
        } else {
            return new Version(0,0,0);
        }
    }
    /**
     * @param obrResource OSGi bundle repository resource reference. (remote bundle)
     */
    public void setObrResource(Resource obrResource) {
        this.obrResource = obrResource;
    }

    /**
     * @param bundle Bundle reference
     */
    public void setBundle(Bundle bundle) {
        if(bundle!=null) {
            this.bundleId = bundle.getBundleId();
            headers = bundle.getHeaders();
        }
    }

    /**
     * @return The bundle reference, can be null
     */
    public Bundle getBundle() {
        if(bundleId!=-1) {
            try {
                return bundleContext.getBundle(bundleId);
            } catch (IllegalStateException ex) {
                // This bundle does not exists anymore
                bundleId = -1;
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * @return OSGi bundle repository resource, can be null (remote bundle)
     */
    public Resource getObrResource() {
        return obrResource;
    }

    /**
     * @return The bundle presentation name (artifact-id by default)
     */
    String getPresentationName() {
        Bundle bundle = getBundle();
        if(bundle!=null && headers!=null) {
            return headers.get(Constants.BUNDLE_NAME);
        } else if(obrResource!=null) {
            return obrResource.getPresentationName();
        } else {
            return "Unknown";
        }
    }

    @Override
    public String toString() {
        return getPresentationName();
    }

    /**
     * @return The bundle short description. (empty string if none)
     */
    public String getShortDescription() {
        if(shortDesc!=null) {
            return shortDesc;
        }
        String description=null;
        if(headers!=null) {
            description = headers.get(Constants.BUNDLE_DESCRIPTION);
        }
        if(obrResource!=null && obrResource.getProperties()!=null) {
            Object descrObj = obrResource.getProperties().get(Resource.DESCRIPTION);
            if(descrObj instanceof String) {
                description = (String)descrObj;
            }
        }
        if(description!=null) {
            // Limit size
            if(description.length()>MAX_SHORT_DESCRIPTION_CHAR_COUNT) {
                StringBuilder shortDescBuilder = new StringBuilder();
                for(String word : description.split(" ")) {
                    if(shortDescBuilder.length()+word.length() < MAX_SHORT_DESCRIPTION_CHAR_COUNT) {
                        shortDescBuilder.append(word);
                        shortDescBuilder.append(" ");
                    } else {
                        shortDescBuilder.append("..");
                        break;
                    }
                }
                shortDesc = shortDescBuilder.toString();
                description = shortDesc;
            }
            return description;
        } else {
            return "";
        }
    }

    /**
     * @return A map of bundle details to show on the right side of the GUI. (Title->Value)
     */
    public Map<String,String> getDetails() {
        Bundle bundle = getBundle();
        if(headers!=null) {
             // Copy deprecated dictionary into Map
             Dictionary<String,String> dic = headers;
             HashMap<String,String> details = new HashMap<>(dic.size());
             Enumeration<String> keys = dic.keys();
             while(keys.hasMoreElements()) {
                 String key = keys.nextElement();
                 details.put(key,dic.get(key));
             }
            return details;
        } else if(obrResource!=null) {
            Map resDetails = obrResource.getProperties();
            HashMap<String,String> details = new HashMap<String, String>(resDetails.size());
            Set<Map.Entry<String,Object>> pairs = resDetails.entrySet();
            for(Map.Entry<String,Object> entry : pairs) {
                if(entry.getValue()!=null) {
                    String value = entry.getValue().toString();
                    if(entry.getKey().equals(Resource.SIZE) && entry.getValue() instanceof Long) {
                        value = getHumanReadableBytes((Long)entry.getValue());
                    }
                    details.put(entry.getKey(),value);
                }
            }
            return details;
        } else {
            return new HashMap<>();
        }
    }

    /**
     * @return Bundle tags
     */
    public Collection<String> getBundleCategories() {
        if(headers!=null) {
            String categories = headers.get(Constants.BUNDLE_CATEGORY);
            if(categories!=null) {
                String[] catArray = categories.split(",");
                if(catArray.length==1) {
                    return Arrays.asList(categories);
                } else {
                    return Arrays.asList(catArray);
                }
            }
        } else if(obrResource!=null && obrResource.getCategories()!=null) {
            return Arrays.asList(obrResource.getCategories());
        }
        return new ArrayList<>();
    }

    /**
     * @return True if the start method can be called.
     */
    public boolean isStartReady() {
        Bundle bundle = getBundle();
        return (bundle!=null) && (bundle.getState()==Bundle.INSTALLED || bundle.getState()==Bundle.RESOLVED);
    }

    /**
     * @return True if the stop method can be called.
     */
    public boolean isStopReady() {
        Bundle bundle = getBundle();
        return (bundle!=null) && (bundle.getState()==Bundle.ACTIVE);
    }

    /**
     * @return True if the update method can be called.
     */
    public boolean isUpdateReady() {
        Bundle bundle = getBundle();
        return (bundle!=null) && (bundle.getState()!=Bundle.UNINSTALLED);
    }

    /**
     * @return True if the uninstall method can be called.
     */
    public boolean isUninstallReady() {
        Bundle bundle = getBundle();
        return (bundle!=null) && (bundle.getState()!=Bundle.UNINSTALLED);
    }

    /**
     * @return True if the resource can be deployed
     */
    public boolean isDeployReady() {
        Bundle bundle = getBundle();
        return bundle==null && obrResource!=null;
    }

    /**
     * @return True if the resource can be deployed and started
     */
    public boolean isDeployAndStartReady() {
        return isDeployReady();
    }
}
