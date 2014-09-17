/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.plugins;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.dummy.DummyTransferManager;
import org.syncany.plugins.dummy.DummyTransferSettings;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestConfigUtil;

import static org.junit.Assert.*;

public class TransferSettingsTest {
	private File tmpFile;
	private Config config;

	@Before
	public void before() throws Exception {
		tmpFile = File.createTempFile("syncany-transfer-settings-test", "tmp");
		Config config = TestConfigUtil.createDummyConfig();
		assertNotNull(Plugins.get("dummy"));
		assertNotNull(config);
	}

	@After
	public void after() throws Exception {
		tmpFile.delete();
		config = null;
	}

	@Test
	public void testRestore() throws Exception {

		final String fooTest = "foo-test";
		final String bazTest = "baz-test";
		final int numberTest = 1234;

		final DummyTransferSettings ts = new DummyTransferSettings();
		final DummyTransferSettings nts = new DummyTransferSettings();
		final ConfigTO conf = new ConfigTO();

		conf.setConnectionTO(ts);
		conf.setMachineName("test");

		ts.foo = fooTest;
		ts.baz = bazTest;
		ts.number = numberTest;
		nts.foo = fooTest;
		nts.baz = bazTest;
		ts.subsettings = nts;

		Serializer serializer = new Persister();
		serializer.write(conf, tmpFile);

		System.out.println(new String(Files.readAllBytes(Paths.get(tmpFile.toURI()))));

		ConfigTO confRestored = ConfigTO.load(tmpFile);
		TransferPlugin plugin = Plugins.get(confRestored.getConnectionTO().getType(), TransferPlugin.class);
		assertNotNull(plugin);

		TransferSettings tsRestored = (TransferSettings) confRestored.getConnectionTO();
		assertNotNull(tsRestored);

		DummyTransferManager transferManager = plugin.createTransferManager(tsRestored, config);
		assertNotNull(transferManager);

		DummyTransferSettings dts = transferManager.getConnection();
		assertNotNull(dts);

		assertEquals(dts.foo, fooTest);
		assertEquals(dts.baz, bazTest);
		assertEquals(dts.number, numberTest);

	}

	@Test
	public void createNewValidConnectionTO() throws Exception {

		TransferPlugin p = Plugins.get("dummy", TransferPlugin.class);
		DummyTransferSettings ts = p.createEmptySettings();
		ts.foo = "foo-value";
		ts.number = 5;

		assertTrue(ts.isValid());

		DummyTransferManager dtm = p.createTransferManager(ts, config);
		DummyTransferSettings dts = dtm.getConnection();

		assertEquals(dts.foo, "foo-value");
		assertEquals(dts.number, 5);
		assertNull(dts.baz);

	}

	@Test
	public void createNewInvalidConnectionTO() throws Exception {

		TransferPlugin p = Plugins.get("dummy", TransferPlugin.class);
		DummyTransferSettings ts = p.createEmptySettings();

		assertFalse(ts.isValid());

	}

}
