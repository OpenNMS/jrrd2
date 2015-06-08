/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.netmgt.rrd.jrrd2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A native interface to librrd.
 *
 * @author Jesse White <jesse@opennms.org>
 * @version 2.0.0
 */
public final class Interface {

	private static final Logger LOG = LoggerFactory.getLogger(Interface.class);

    private static final String LIBRARY_NAME = "jrrd2";

    private static final String PROPERTY_NAME = "opennms.library.jrrd2";

    private static boolean m_loaded = false;

    protected static native void rrd_get_context();

    protected static native void rrd_create_r(String filename, long pdp_step, long last_up, String[] argv) throws JniRrdException;

    protected static native void rrd_update_r(String filename, String template, String[] argv) throws JniRrdException;

    protected static native FetchResults rrd_fetch_r(String filename, String cf, long start, long end, long step) throws JniRrdException;

    protected static synchronized native FetchResults rrd_xport(String[] argv) throws JniRrdException;

    /**
     * Load the jrrd library and create the singleton instance of the interface.
     * 
     * @throws SecurityException
     *             if we don't have permission to load the library
     * @throws UnsatisfiedLinkError
     *             if the library doesn't exist
     */
    public static synchronized void init() throws SecurityException, UnsatisfiedLinkError {
        if (m_loaded) {
            return;
        }

        String property = System.getProperty(PROPERTY_NAME);
        if (property != null) {
            LOG.debug("System property '{}' set to '{}'. Attempting to load {} library from this location.", PROPERTY_NAME,  System.getProperty(PROPERTY_NAME), LIBRARY_NAME);
            System.load(property);
        } else {
            LOG.debug("System property '{}' no set. Attempting to load library using System.loadLibrary(\"{}\").", PROPERTY_NAME, LIBRARY_NAME);
            System.loadLibrary(LIBRARY_NAME);
        }
        LOG.info("Successfully loaded {} library.", LIBRARY_NAME);
    }

    public static synchronized void reload() throws SecurityException, UnsatisfiedLinkError {
        m_loaded = false;
        init();
    }
}
