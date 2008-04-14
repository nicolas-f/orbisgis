/*
 * The GDMS library (Generic Datasources Management System)
 * is a middleware dedicated to the management of various kinds of
 * data-sources such as spatial vectorial data or alphanumeric. Based
 * on the JTS library and conform to the OGC simple feature access
 * specifications, it provides a complete and robust API to manipulate
 * in a SQL way remote DBMS (PostgreSQL, H2...) or flat files (.shp,
 * .csv...). GDMS is produced  by the geomatic team of the IRSTV
 * Institute <http://www.irstv.cnrs.fr/>, CNRS FR 2488:
 *    Erwan BOCHER, scientific researcher,
 *    Thomas LEDUC, scientific researcher,
 *    Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * This file is part of GDMS.
 *
 * GDMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GDMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GDMS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult:
 *    <http://orbisgis.cerma.archi.fr/>
 *    <http://sourcesup.cru.fr/projects/orbisgis/>
 *    <http://listes.cru.fr/sympa/info/orbisgis-developers/>
 *    <http://listes.cru.fr/sympa/info/orbisgis-users/>
 *
 * or contact directly:
 *    erwan.bocher _at_ ec-nantes.fr
 *    fergonco _at_ gmail.com
 *    thomas.leduc _at_ cerma.archi.fr
 */
package org.gdms.sql.function.spatial.extract;

import java.util.List;

import org.gdms.data.types.Type;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.jts.operation.UniqueSegmentsExtracter;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionValidator;
import org.gdms.sql.function.spatial.AbstractSpatialFunction;
import org.gdms.sql.strategies.IncompatibleTypesException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @author bocher
 * 
 */
public class ToMultiSegments extends AbstractSpatialFunction {

	GeometryFactory factory = new GeometryFactory();

	public Value evaluate(Value[] args) throws FunctionException {
		if (args[0].isNull()) {
			return ValueFactory.createNullValue();

		} else {
			final Geometry geom = args[0].getAsGeometry();

			final UniqueSegmentsExtracter uniqueSegmentsExtracter = new UniqueSegmentsExtracter(
					geom);

			final List<LineString> lineList = uniqueSegmentsExtracter
					.getSegmentAsLineString();
			return ValueFactory
					.createValue(factory.createMultiLineString(lineList
							.toArray(new LineString[0])));

		}
	}

	public String getDescription() {

		return "Convert a geometry into a sets of unique segments stored in a MultiLineString ";
	}

	public String getName() {
		return "ToMultiSegments";
	}

	public String getSqlOrder() {

		return "select ToMultiSegments(the_geom) from myTable;";
	}

	public boolean isAggregate() {

		return false;
	}

	public void validateTypes(Type[] argumentsTypes)
			throws IncompatibleTypesException {

		FunctionValidator.failIfBadNumberOfArguments(this, argumentsTypes, 1);
		FunctionValidator.failIfNotOfType(this, argumentsTypes[0],
				Type.GEOMETRY);
	}

}