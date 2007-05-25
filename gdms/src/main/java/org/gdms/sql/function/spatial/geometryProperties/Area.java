package org.gdms.sql.function.spatial.geometryProperties;

import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.spatial.GeometryValue;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

public class Area implements Function {

	public Function cloneFunction() {
		return new Area();
	}

	public Value evaluate(Value[] args) throws FunctionException {
		GeometryValue gv = (GeometryValue) args[0];		
		return ValueFactory.createValue(gv.getGeom().getArea());
	}

	public String getName() {
		return "Area";
	}

	public int getType(int[] types) {
		
		return types[0];
	}

	public boolean isAggregate() {
		return false;
	}

}
