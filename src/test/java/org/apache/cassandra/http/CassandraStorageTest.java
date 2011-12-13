package org.apache.cassandra.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CassandraStorageTest {
	private static final String KEY = "TEST_ROW";
	private static final String COLUMN_FAMILY = "TEST_CF";
	private static final String KEYSPACE = "TEST_KEYSPACE";

	@BeforeClass
	public static void setup() throws Exception {
		HttpDaemon.main(new String[] { "-e", "-yaml", "cassandra_test.yaml" });
		Thread.sleep(2000);
	}

	@AfterClass
	public static void teardown() throws Exception {
		HttpDaemon.shutdown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDatabaseServices() throws Exception {
		CassandraStorage dataService = HttpDaemon.getDataService();
		JSONObject slice = new JSONObject();
		slice.put("FIRST_NAME", "John");
		slice.put("LAST_NAME", "Smith");

		try { // CLEANUP FROM BEFORE
			dataService.dropKeyspace(KEYSPACE);
		} catch (Exception e) {
		}

		// CREATE KEYSPACE
		dataService.addKeyspace(KEYSPACE);
		dataService.setKeyspace(KEYSPACE);

		// CREATE COLUMN FAMILY
		dataService.createColumnFamily(KEYSPACE, COLUMN_FAMILY);

		// INSERT THE ROW
		dataService.setColumn(KEYSPACE, COLUMN_FAMILY, KEY, slice, ConsistencyLevel.ONE, false);

		// FETCH THE ROW (VERIFY INSERT ROW)
		String json = dataService.getSlice(KEYSPACE, COLUMN_FAMILY, KEY, ConsistencyLevel.ONE);
		assertEquals("{\"FIRST_NAME\":\"John\",\"LAST_NAME\":\"Smith\"}", json);

		// ADD A COLUMN
		dataService.addColumn(KEYSPACE, COLUMN_FAMILY, KEY, "STATE", "CA", ConsistencyLevel.ONE, false);

		// FETCH THE ROW (VERIFY ADD COLUMN)
		json = dataService.getSlice(KEYSPACE, COLUMN_FAMILY, KEY, ConsistencyLevel.ONE);
		assertEquals("{\"STATE\":\"CA\",\"FIRST_NAME\":\"John\",\"LAST_NAME\":\"Smith\"}", json);

		// DELETE THE ROW
		dataService.deleteRow(KEYSPACE, COLUMN_FAMILY, "TEST_SLICE", ConsistencyLevel.ONE, false);
		json = dataService.getSlice(KEYSPACE, COLUMN_FAMILY, "TEST_SLICE", ConsistencyLevel.ONE);
		assertEquals(null, json);

		// DROP COLUMN FAMILY
		dataService.dropColumnFamily(COLUMN_FAMILY);
		boolean threw = false;
		try {
			json = dataService.getSlice(KEYSPACE, COLUMN_FAMILY, "TEST_SLICE", ConsistencyLevel.ONE);
		} catch (InvalidRequestException ire) {
			threw = true;
		}
		assertTrue("Expected exception when accessing dropped column family.", threw);

		// DROP KEY SPACE
		dataService.dropKeyspace(KEYSPACE);
		boolean threw1 = false;
		try {
			json = dataService.getSlice(KEYSPACE, COLUMN_FAMILY, "TEST_SLICE", ConsistencyLevel.ONE);
		} catch (InvalidRequestException ire) {
			threw1 = true;
		}
		assertTrue("Expected exception when accessing dropped key space.", threw1);
	}
}