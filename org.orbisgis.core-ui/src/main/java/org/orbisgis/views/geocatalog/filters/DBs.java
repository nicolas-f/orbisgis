package org.orbisgis.views.geocatalog.filters;

import org.gdms.source.SourceManager;
import org.orbisgis.views.geocatalog.filter.IGeocatalogFilter;

public class DBs implements IGeocatalogFilter {

	@Override
	public boolean accept(SourceManager sm, String sourceName) {
		int type = sm.getSource(sourceName).getType();
		return (type & SourceManager.DB) == SourceManager.DB;
	}

}
