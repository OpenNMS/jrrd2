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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opennms.netmgt.rrd.jrrd2.FetchResults;
import org.opennms.netmgt.rrd.jrrd2.Interface;
import org.opennms.netmgt.rrd.jrrd2.JniRrdException;

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

    @Test(expected=JniRrdException.class)
    public void createFailsWhenFilenameIsNull() throws JniRrdException {
        Interface.rrd_create_r(null, 1, 1, new String[]{"DS:test:GAUGE:900:0:100", "RRA:MIN:0.5:1:1000"});
    }

    @Test(expected=JniRrdException.class)
    public void createFailsWhenArgvIsNull() throws JniRrdException {
        Interface.rrd_create_r("test.rrd", 1, 1, null);
    }

    @Test(expected=JniRrdException.class)
    public void createFailsWhenArgvIsEmpty() throws JniRrdException {
        Interface.rrd_create_r("test.rrd", 1, 1, new String[]{});
    }

    @Test
    public void canCreate() throws JniRrdException {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;

        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{"DS:test:GAUGE:900:0:100", "RRA:MIN:0.5:1:1000"});

        assertThat(rrdFile.isFile(), is(true));
    }

    @Test(expected=JniRrdException.class)
    public void updateFailsWhenFilenameIsNull() throws JniRrdException {
        Interface.rrd_update_r(null, "", new String[]{});
    }

    @Test(expected=JniRrdException.class)
    public void updateFailsWhenArgvIsNull() throws JniRrdException {
        Interface.rrd_update_r("", "", null);
    }

    @Test(expected=JniRrdException.class)
    public void updateFailsWhenFileIsMissing() throws JniRrdException {
        File missingFile = Paths.get("should", "not", "exist").toFile();
        assertThat(missingFile.isFile(), is(false));
        Interface.rrd_update_r(missingFile.getAbsolutePath(), "", new String[]{});
    }

    @Test(expected=JniRrdException.class)
    public void updateFailsWhenNoValuesAreGiven() throws JniRrdException {
        File rrdFile = new File(tempFolder.getRoot(), "test.rrd");
        assertThat(rrdFile.isFile(), is(false));

        final long start = 1424700000;
        final long step = 900;

        Interface.rrd_create_r(rrdFile.getAbsolutePath(), step, start, new String[]{"DS:test:GAUGE:900:0:100", "RRA:MIN:0.5:1:1000"});

        assertThat(rrdFile.isFile(), is(true));

        Interface.rrd_update_r(rrdFile.getAbsolutePath(), "", new String[]{});
    }

    @Test
    public void canUpdate() throws JniRrdException {
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

    @Test(expected=JniRrdException.class)
    public void fetchFailsWhenFilenameIsNull() throws JniRrdException {
        Interface.rrd_fetch_r(null, "AVERAGE", 0, 1, 1);
    }

    @Test(expected=JniRrdException.class)
    public void fetchFailsWhenCfIsNull() throws JniRrdException {
        Interface.rrd_fetch_r("", null, 0, 1, 1);
    }

    @Test
    public void canFetchFromMultipleDatasources() throws JniRrdException, InterruptedException {
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

    @Test(expected=JniRrdException.class)
    public void xportFailsWhenArgvIsNull() throws JniRrdException {
        Interface.rrd_xport(null);
    }

    @Test(expected=JniRrdException.class)
    public void xportFailsWhenArgvIsEmpty() throws JniRrdException {
        Interface.rrd_xport(new String[]{});
    }

    @Test
    public void canXport() throws JniRrdException {
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
