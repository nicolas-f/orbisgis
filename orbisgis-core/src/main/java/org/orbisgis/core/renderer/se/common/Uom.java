package org.orbisgis.core.renderer.se.common;

import org.orbisgis.core.renderer.se.parameter.ParameterException;

/**
 * This enumeration contains all the units of measure that are allowed in 
 * Symbology Encoding.
 * @author alexis, maxence
 */
public enum Uom {

	PX, IN, MM, PT, PERCENT, GM, GFT;


    private static final double PT_IN_INCH = 72.0;
    private static final double MM_IN_INCH = 25.4;
    private static final double IN_IN_FOOT = 12;
    private static final double ONE_THOUSAND = 1000;
    private static final double ONE_HUNDRED = 100;
    
    
	/**
	 * Convert a value to the corresponding value in pixel
	 *
	 * Note that converting ground unit to pixel is done by using a constant scale
	 *
	 * @param value the value to convert
	 * @param uom unit of measure for value
	 * @param dpi the current resolution
	 * @param scale the current scale (for converting ground meters and ground feet to media units)
	 * @param v100p the value to return when uom is "percent" and value is 100 (%)
	 * @return
	 * @throws ParameterException
	 *
	 * @todo return integer !!!
	 */
	public static double toPixel(double value, Uom uom, Double dpi, Double scale, Double v100p) throws ParameterException {
		if (uom == null) {
			return value; // no uom ? => return as Pixel !
		}

		// TODO DPI depends on context ! (e.g pdf 300dpi) => Should add dpi into MapTransform

		if (dpi == null && uom != Uom.PX){
			throw new ParameterException("DPI is invalid");
		}

		switch (uom) {
			case IN:
				return value * dpi; // [IN] * [PX]/[IN] => [PX]
			case MM:
				return (value / MM_IN_INCH) * dpi; // [MM] * [IN]/[MM] * [PX]/[IN] => [PX]
			case PT: // 1PT == 1/72[IN] whatever dpi is
				return (value / PT_IN_INCH) * dpi; // 1/72[IN] * 72 *[PX]/[IN] => [PX]
			case GM:
				if (scale == null){
					throw new ParameterException("Scale is invalid");
				}
				return (value * ONE_THOUSAND * dpi) / (scale * MM_IN_INCH);
			case GFT:
				if (scale == null){
					throw new ParameterException("Scale is invalid");
				}
				return (value * IN_IN_FOOT * dpi) / (scale);
			case PERCENT:
				if (v100p == null){
					return value;
					//throw new ParameterException("100% value is invalid");
				}
				return value * v100p / ONE_HUNDRED;
			case PX:
			default:
				return value; // [PX]
		}
	}

        /**
         * Build an {@code Uom} from a OGC code that represents a unit of 
         * measure.
         * @param unitOfMeasure
         * @return 
         */
	public static Uom fromOgcURN(String unitOfMeasure) {
		if (unitOfMeasure.equals("urn:ogc:def:uom:se::in")) {
			return Uom.IN;
		}
		else if (unitOfMeasure.equals("urn:ogc:def:uom:se::px")) {
			return Uom.PX;
		}
		else if (unitOfMeasure.equals("urn:ogc:def:uom:se::pt")) {
			return Uom.PT;
		}
		else if (unitOfMeasure.equals("urn:ogc:def:uom:se::percent")) {
			return Uom.PERCENT;
		}
		else if (unitOfMeasure.equals("urn:ogc:def:uom:se::gm")) {
			return Uom.GM;
		}
		else if (unitOfMeasure.equals("urn:ogc:def:uom:se::gf")) {
			return Uom.GFT;
		} else {
			return Uom.MM;
		}
	}

        /**
         * Build an OGC code that represents a unit of measure from this
         * {@code Uom}.
         * @return 
         */
	public String toURN() {
		return "urn:ogc:def:uom:se::" + this.name().toLowerCase();
	}
}