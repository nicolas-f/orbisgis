/*
 * UrbSAT is a set of spatial functionalities to build morphological
 * and aerodynamic urban indicators. It has been developed on
 * top of GDMS and OrbisGIS. UrbSAT is distributed under GPL 3
 * license. It is produced by the geomatic team of the IRSTV Institute
 * <http://www.irstv.cnrs.fr/>, CNRS FR 2488:
 *    Erwan BOCHER, scientific researcher,
 *    Thomas LEDUC, scientific researcher,
 *    Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * This file is part of UrbSAT.
 *
 * UrbSAT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * UrbSAT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with UrbSAT. If not, see <http://www.gnu.org/licenses/>.
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
package org.gdms.sql.customQuery.spatial.raster.convert;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.memory.ObjectMemoryDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.function.FunctionValidator;
import org.gdms.sql.strategies.IncompatibleTypesException;
import org.gdms.sql.strategies.SemanticException;
import org.grap.io.GeoreferencingException;
import org.grap.model.GeoRaster;
import org.orbisgis.progress.IProgressMonitor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class RasterToPolygons implements CustomQuery {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
			Value[] values, IProgressMonitor pm) throws ExecutionException {
		final SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(
				tables[0]);
		try {
			sds.open();
			if (1 == values.length) {
				// if no raster's field's name is provided, the default (first)
				// one is arbitrarily chosen.
				sds.setDefaultGeometry(values[0].toString());
			}

			final ObjectMemoryDriver driver = new ObjectMemoryDriver(
					getMetadata(null));

			final long rowCount = sds.getRowCount();
			for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
				final GeoRaster geoRasterSrc = sds.getRaster(rowIndex);
				final float halfPixelSize_X = geoRasterSrc.getMetadata()
						.getPixelSize_X() / 2;
				final float halfPixelSize_Y = geoRasterSrc.getMetadata()
						.getPixelSize_Y() / 2;
				final float[] pixels = geoRasterSrc.getGrapImagePlus()
						.getFloatPixels();

				for (int l = 0, i = 0; l < geoRasterSrc.getHeight(); l++) {

					if (l / 100 == l / 100.0) {
						if (pm.isCancelled()) {
							break;
						} else {
							pm
									.progressTo((int) (100 * l * rowIndex / (geoRasterSrc
											.getHeight() * rowCount)));
						}
					}
					for (int c = 0; c < geoRasterSrc.getWidth(); c++) {
						final float height = pixels[i];
						if (!Float.isNaN(height)) {
							final Point2D pixelCentroid = geoRasterSrc
									.fromPixelGridCoordToRealWorldCoord(c, l);

							final Coordinate[] coordinates = new Coordinate[5];
							coordinates[0] = new Coordinate(pixelCentroid
									.getX()
									- halfPixelSize_X, pixelCentroid.getY()
									+ halfPixelSize_Y, height);
							coordinates[1] = new Coordinate(pixelCentroid
									.getX()
									+ halfPixelSize_X, pixelCentroid.getY()
									+ halfPixelSize_Y, height);
							coordinates[2] = new Coordinate(pixelCentroid
									.getX()
									+ halfPixelSize_X, pixelCentroid.getY()
									- halfPixelSize_Y, height);
							coordinates[3] = new Coordinate(pixelCentroid
									.getX()
									- halfPixelSize_X, pixelCentroid.getY()
									- halfPixelSize_Y, height);
							coordinates[4] = coordinates[0];

							final LinearRing shell = geometryFactory
									.createLinearRing(coordinates);
							final Geometry polygon = geometryFactory
									.createPolygon(shell, null);

							driver.addValues(new Value[] {
									ValueFactory.createValue(i),
									ValueFactory.createValue(polygon),
									ValueFactory.createValue(height) });
						}
						i++;
					}
				}
			}
			sds.cancel();
			return driver;
		} catch (DriverException e) {
			throw new ExecutionException(e);
		} catch (GeoreferencingException e) {
			throw new ExecutionException(e);
		} catch (FileNotFoundException e) {
			throw new ExecutionException(e);
		} catch (IOException e) {
			throw new ExecutionException(e);
		}
	}

	public String getDescription() {
		return "Transform a Raster into a set of polygons (one rectangle per pixel)";
	}

	public String getName() {
		return "RasterToPolygons";
	}

	public String getSqlOrder() {
		return "select RasterToPolygons([raster]) from mytif;";
	}

	public Metadata getMetadata(Metadata[] tables) throws DriverException {
		return new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "index",
				"the_geom", "height" });
	}

	public void validateTables(Metadata[] tables) throws SemanticException,
			DriverException {
		FunctionValidator.failIfBadNumberOfTables(this, tables, 1);
		FunctionValidator.failIfNotRasterDataSource(this, tables[0], 0);
	}

	public void validateTypes(Type[] types) throws IncompatibleTypesException {
		FunctionValidator.failIfBadNumberOfArguments(this, types, 0, 1);
		if (1 == types.length) {
			FunctionValidator.failIfNotOfType(this, types[0], Type.RASTER);
		}
	}
}