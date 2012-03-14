/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information. OrbisGIS is
 * distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 *
 *
 * Team leader : Erwan BOCHER, scientific researcher,
 *
 * User support leader : Gwendall Petit, geomatic engineer.
 *
 * Previous computer developer : Pierre-Yves FADET, computer engineer, Thomas LEDUC, 
 * scientific researcher, Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * Copyright (C) 2010 Erwan BOCHER, Alexis GUEGANNO, Maxence LAURENT, Antoine GOURLAY
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
 *
 * or contact directly:
 * info@orbisgis.org
 */
package org.gdms.data.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gdms.data.values.Value;

/**
 * Indicates the pattern that the field values must have
 * 
 */
public class PatternConstraint extends AbstractConstraint {
	private String constraintValue;
	private Pattern pattern;

	public PatternConstraint(final String constraintValue) {
		this.constraintValue = constraintValue;
		pattern = Pattern.compile(constraintValue);
	}

	PatternConstraint(byte[] constraintBytes) {
		this(new String(constraintBytes));
	}

        @Override
	public int getConstraintCode() {
		return Constraint.PATTERN;
	}

        @Override
	public String getConstraintValue() {
		return constraintValue;
	}

        @Override
	public String check(Value value) {
		if (!value.isNull()) {
			Matcher m = pattern.matcher(value.toString());
			if (!m.matches()) {
				return "The value doesn't match the pattern '"
						+ constraintValue + "'";
			}
		}
		return null;
	}

        @Override
	public byte[] getBytes() {
		return constraintValue.getBytes();
	}

        @Override
	public int getType() {
		return CONSTRAINT_TYPE_STRING_LITERAL;
	}
}