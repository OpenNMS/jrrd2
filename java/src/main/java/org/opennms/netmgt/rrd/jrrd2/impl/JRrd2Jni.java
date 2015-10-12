/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is Copyright (C) 2002-2015 The OpenNMS Group, Inc.  All rights
 * reserved.  OpenNMS(R) is a derivative work, containing both original code,
 * included code and modified code that was published under the GNU General
 * Public License.  Copyrights for modified and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License with the Classpath
 * Exception; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.netmgt.rrd.jrrd2.impl;

import org.opennms.netmgt.rrd.jrrd2.api.FetchResults;
import org.opennms.netmgt.rrd.jrrd2.api.JRrd2;
import org.opennms.netmgt.rrd.jrrd2.api.JRrd2Exception;

/**
 * A wrapper class for the native interface to librrd.
 *
 * This class automatically loads and initializes
 * the required system libraries.
 *
 * @author jwhite
 * @version 2.0.0
 */
public class JRrd2Jni implements JRrd2 {

    /* A suggested by http://linux.die.net/man/1/rrdthreads:
     *   Every thread SHOULD call "rrd_get_context()" before its first call to any "librrd_th" function
     */
    private static final ThreadLocal<Void> rrdContext =
        new ThreadLocal<Void>() {
            @Override protected Void initialValue() {
                Interface.rrd_get_context();
                return null;
            }
        };

    public JRrd2Jni() {
        Interface.init();
    }

    @Override
    public void create(final String filename, final long step, final long start, String[] argv) throws JRrd2Exception {
        rrdContext.get();
        Interface.rrd_create_r(filename, step, start, argv);
    }

    @Override
    public void update(final String filename, final String template, final String[] argv) throws JRrd2Exception {
        rrdContext.get();
        Interface.rrd_update_r(filename, template, argv);
    }

    @Override
    public FetchResults fetch(String filename, String cf, long start, long end, long step) throws JRrd2Exception {
        rrdContext.get();
        return Interface.rrd_fetch_r(filename, cf, start, end, step);
    }

    @Override
    public FetchResults xport(long start, long end, long step, long maxrows, String[] argv) throws JRrd2Exception {
        final int numFixedArguments = maxrows > 0 ? 9 : 7;

        // Convert the parameters to command line arguments
        String[] allArgv = new String[numFixedArguments + argv.length];
        allArgv[0] = "xport";
        allArgv[1] = "--start";
        allArgv[2] = Long.toString(start);
        allArgv[3] = "--end";
        allArgv[4] = Long.toString(end);
        allArgv[5] = "--step";
        allArgv[6] = Long.toString(step);
        if (maxrows > 0) {
            allArgv[7] = "--maxrows";
            allArgv[8] = Long.toString(maxrows);
        }

        // Copy the elements from argvs
        for (int i = 0; i < argv.length; i++) {
            allArgv[i + numFixedArguments] = argv[i];
        }

        // Launch
        return Interface.rrd_xport(allArgv);
    }
}
