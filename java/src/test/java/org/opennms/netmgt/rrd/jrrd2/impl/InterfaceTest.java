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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opennms.netmgt.rrd.jrrd2.api.FetchResults;
import org.opennms.netmgt.rrd.jrrd2.api.JRrd2Exception;
import org.opennms.netmgt.rrd.jrrd2.impl.Interface;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class InterfaceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() {
        Path library = Paths.get(System.getProperty("user.dir"), "..", "dist", "libjrrd2.so");
        System.setProperty("opennms.library.jrrd2", library.toString());
        Interface.init();
    }

    @Test(expected=JRrd2Exception.class)
    public void createFailsWhenFilenameIsNull() throws JRrd2Exception {
        Interface.rrd_create_r(null, 1, 1, new String[]{"DS:test:GAUGE:900:0:100", "RRA:MIN:0.5:1:1000"});
    }

    @Test(expected=JRrd2Exception.class)
    public void createFailsWhenArgvIsNull() throws JRrd2Exception {
        Interface.rrd_create_r("test.rrd", 1, 1, null);
    }

    @Test(expected=JRrd2Exception.class)
    public void createFailsWhenArgvIsEmpty() throws JRrd2Exception {
        Interface.rrd_create_r("test.rrd", 1, 1, new String[]{});
    }

    @Test
    public void canCreate() throws JRrd2Exception {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;

        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{"DS:test:GAUGE:900:0:100", "RRA:MIN:0.5:1:1000"});

        assertThat(rrdFile.isFile(), is(true));
    }

    @Test(expected=JRrd2Exception.class)
    public void updateFailsWhenFilenameIsNull() throws JRrd2Exception {
        Interface.rrd_update_r(null, "", new String[]{});
    }

    @Test(expected=JRrd2Exception.class)
    public void updateFailsWhenArgvIsNull() throws JRrd2Exception {
        Interface.rrd_update_r("", "", null);
    }

    @Test(expected=JRrd2Exception.class)
    public void updateFailsWhenFileIsMissing() throws JRrd2Exception {
        File missingFile = Paths.get("should", "not", "exist").toFile();
        assertThat(missingFile.isFile(), is(false));
        Interface.rrd_update_r(missingFile.getAbsolutePath(), "", new String[]{});
    }

    @Test(expected=JRrd2Exception.class)
    public void updateFailsWhenNoValuesAreGiven() throws JRrd2Exception {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;

        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{"DS:test:GAUGE:900:0:100", "RRA:MIN:0.5:1:1000"});

        assertThat(rrdFile.isFile(), is(true));

        Interface.rrd_update_r(rrdFile.getAbsolutePath(), "", new String[]{});
    }

    @Test
    public void canUpdate() throws JRrd2Exception {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;

        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{
            "DS:test:GAUGE:900:0:100",
            "RRA:MIN:0.5:1:1000"});

        assertThat(rrdFile.isFile(), is(true));

        Interface.rrd_update_r(rrdFile.getAbsolutePath(), null, new String[]{
            String.format("%d:%d", 1424700001, 1)
        });
    }

    @Test(expected=JRrd2Exception.class)
    public void fetchFailsWhenFilenameIsNull() throws JRrd2Exception {
        Interface.rrd_fetch_r(null, "AVERAGE", 0, 1, 1);
    }

    @Test(expected=JRrd2Exception.class)
    public void fetchFailsWhenCfIsNull() throws JRrd2Exception {
        Interface.rrd_fetch_r("", null, 0, 1, 1);
    }

    @Test
    public void canFetchFromMultipleDatasources() throws JRrd2Exception, InterruptedException {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;

        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{
            "DS:x:GAUGE:900:0:100",
            "DS:y:GAUGE:900:0:100",
            "RRA:MIN:0.5:1:1000",
            "RRA:AVERAGE:0.5:1:1000"
        });

        assertThat(rrdFile.isFile(), is(true));

        for (int i = 1; i <= 100; i++) {
            final long timestamp = start + (i * step);
            Interface.rrd_update_r(rrdFile.getAbsolutePath(), "x:y", new String[]{
                String.format("%d:%d:%d", timestamp, i, 100)
            });
        }

        FetchResults results = Interface.rrd_fetch_r(rrdFile.getAbsolutePath(), "MIN", 1424700000L, 1424800800L, 1);

        assertThat(results, is(notNullValue()));
        
        assertThat(results.getStep(), is(step));
        assertThat(results.getStart(), is(1424700900L));
        assertThat(results.getEnd(), is(1424801700L));
        
        assertThat(results.getColumns()[0], is("x"));
        assertThat(results.getColumns()[1], is("y"));

        assertThat(results.getValues()[0][0], is(1.0));
        assertThat(results.getValues()[0][1], is(2.0));
        assertThat(results.getValues()[1][0], is(100.0));
        assertThat(results.getValues()[1][1], is(100.0));
    }

    @Test(expected=JRrd2Exception.class)
    public void xportFailsWhenArgvIsNull() throws JRrd2Exception {
        Interface.rrd_xport(null);
    }

    @Test(expected=JRrd2Exception.class)
    public void xportFailsWhenArgvIsEmpty() throws JRrd2Exception {
        Interface.rrd_xport(new String[]{});
    }

    @Test
    public void canXport() throws JRrd2Exception {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;
        
        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{
            "DS:x:GAUGE:900:0:100",
            "DS:y:GAUGE:900:0:100",
            "RRA:MIN:0.5:1:1000",
            "RRA:AVERAGE:0.5:1:1000"
        });

        assertThat(rrdFile.isFile(), is(true));

        for (int i = 1; i <= 100; i++) {
            final long timestamp = start + (i * step);
            Interface.rrd_update_r(rrdFile.getAbsolutePath(), "x:y", new String[]{
                String.format("%d:%d:%d", timestamp, i, 100)
            });
        }

        final String[] argv = new String[] {
                "xport",
                "--start",
                Long.toString(start),
                "--end",
                Long.toString(1424800000L),
                String.format("DEF:x=%s:x:MIN", rrdFile.getAbsolutePath()),
                String.format("DEF:y=%s:y:MIN", rrdFile.getAbsolutePath()),
                "XPORT:x:xx",
                "XPORT:y:yy"
        };

        FetchResults results = Interface.rrd_xport(argv);

        assertThat(results, is(notNullValue()));

        assertThat(results.getStep(), is(step));
        assertThat(results.getStart(), is(1424700900L));
        assertThat(results.getEnd(), is(1424800800L));

        assertThat(results.getColumns()[0], is("xx"));
        assertThat(results.getColumns()[1], is("yy"));

        assertThat(results.getValues()[0][0], is(1.0));
        assertThat(results.getValues()[0][1], is(2.0));
        assertThat(results.getValues()[1][0], is(100.0));
        assertThat(results.getValues()[1][1], is(100.0));
    }
}
