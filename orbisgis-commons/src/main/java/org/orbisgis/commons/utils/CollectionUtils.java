/**
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
package org.orbisgis.commons.utils;

/**
 * Utility class for dealing with arrays.
 * 
 * @author Fernando GONZALEZ CORTES
 * @author Thomas LEDUC
 * @author Antoine Gourlay <antoine@gourlay.fr>
 */
public final class CollectionUtils {

        /**
         * Check if the given array contains the given object.
         *
         * Note that object references are compared, not their value or equality.
         *
         * @param <T> 
         * @param collection an array
         * @param testObject an object
         * @return true if the array contains the object
         */
        public static <T> boolean contains(T[] collection, T testObject) {
                for (T object : collection) {
                        if (object == testObject) {
                                return true;
                        }
                }

                return false;
        }

        /**
         * Gets a comma-separated String describing an array.
         *
         * The .toString() method of the objects is used.
         *
         * @param <T> 
         * @param array an array, possibly empty
         * @return a comma-separated representation String, possibly empty
         */
        public static <T> String getCommaSeparated(T[] array) {
                StringBuilder ret = new StringBuilder("");
                String separator = "";
                for (T object : array) {
                        ret.append(separator).append(object);
                        separator = ", ";
                }

                return ret.toString();
        }

        /**
         * Check if the given array contains the given integer.
         *
         * @param array an array
         * @param element an integer
         * @return true if the array contains the integer
         */
        public static boolean contains(int[] array, int element) {
                for (int i = 0; i < array.length; i++) {
                        if (array[i] == element) {
                                return true;
                        }
                }

                return false;
        }

        /**
         * Gets the index of the first occurrence of an object inside an array,
         * or -1 if not found.
         *
         * Note that object references are compared, not their value or equality.
         *
         * @param <T> 
         * @param array an array
         * @param element an Object to look for
         * @return an integer between 0 and array.length, or -1 if not found
         */
        public static <T> int indexOf(T[] array, T element) {
                for (int i = 0; i < array.length; i++) {
                        if (array[i] == element) {
                                return i;
                        }
                }
                return -1;

        }

        private CollectionUtils() {
        }
}
