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

public class JRrd2 {

    private static final ThreadLocal<Void> rrdContext =
        new ThreadLocal<Void>() {
            @Override protected Void initialValue() {
                Interface.rrd_get_context();
                return null;
            }
        };

    public JRrd2() {
        Interface.init();
    }

    public void create(final String filename, final long step, final long start, String[] argv) throws JniRrdException {
        rrdContext.get();
        Interface.rrd_create_r(filename, step, start, argv);
    }

    public void update(final String filename, final String template, final String[] argv) throws JniRrdException {
        rrdContext.get();
        Interface.rrd_update_r(filename, template, argv);
    }

    public FetchResults fetch(String filename, String cf, long start, long end, long step) throws JniRrdException {
        rrdContext.get();
        return Interface.rrd_fetch_r(filename, cf, start, end, step);
    }

    public FetchResults xport(long start, long end, long step, long maxrows, String[] argv) throws JniRrdException {
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
