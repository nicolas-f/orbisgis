package org.gdms.data.values;

import java.sql.Types;

import org.gdms.data.types.Type;

/**
 *
 */
public class ShortValue extends NumericValue {
	private short value;

	/**
	 * Crea un nuevo ShortValue.
	 *
	 * @param s
	 *            DOCUMENT ME!
	 */
	ShortValue(short s) {
		value = s;
	}

	/**
	 * Crea un nuevo ShortValue.
	 */
	ShortValue() {
	}

	/**
	 * @see org.gdms.data.values.NumericValue#byteValue()
	 */
	public byte byteValue() {
		return (byte) value;
	}

	/**
	 * @see org.gdms.data.values.NumericValue#shortValue()
	 */
	public short shortValue() {
		return value;
	}

	/**
	 * @see org.gdms.data.values.NumericValue#intValue()
	 */
	public int intValue() {
		return (int) value;
	}

	/**
	 * @see org.gdms.data.values.NumericValue#longValue()
	 */
	public long longValue() {
		return (long) value;
	}

	/**
	 * @see org.gdms.data.values.NumericValue#floatValue()
	 */
	public float floatValue() {
		return (float) value;
	}

	/**
	 * @see org.gdms.data.values.NumericValue#doubleValue()
	 */
	public double doubleValue() {
		return (double) value;
	}

	/**
	 * @see org.gdms.data.values.Value#getStringValue(org.gdms.data.values.ValueWriter)
	 */
	public String getStringValue(ValueWriter writer) {
		return writer.getStatementString(value, Types.SMALLINT);
	}

	/**
	 * @see org.gdms.data.values.Value#getType()
	 */
	public int getType() {
		return Type.SHORT;
	}

	@Override
	public int getDecimalDigitsCount() {
		return 0;
	}

	public short getValue() {
		return value;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "" + value;
	}

	public byte[] getBytes() {
		byte[] ret = new byte[2];
		ret[0] = (byte) ((value >>> 8) & 0xFF);
		ret[1] = (byte) ((value >>> 0) & 0xFF);

		return ret;
	}

	public static Value readBytes(byte[] buffer) {
		return new ShortValue(
				(short) (((0xff & buffer[0]) << 8) + ((0xff & buffer[1]) << 0)));
	}
}