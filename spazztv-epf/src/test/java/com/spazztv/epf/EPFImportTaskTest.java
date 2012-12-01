package com.spazztv.epf;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.spazztv.epf.dao.EPFDbException;
import com.spazztv.epf.dao.EPFDbWriter;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EPFImporterQueue.class, EPFDbWriter.class})
public class EPFImportTaskTest {

	String storefrontEpfFile = "testdata/epf_files/storefront";
	EPFImportTask importTask;
	EPFImportTranslator importXlator;
	EPFDbWriter dbWriter;
	EPFImporterQueue importerQueue;
	long recordsExpected = 0;

	EPFExportType exportType;
	String tableName;
	LinkedHashMap<String, String> columnsAndTypes;
	List<String> primaryKey;

	@Before
	public void setUp() throws FileNotFoundException, EPFFileFormatException {
		// Setting up importXlator to read the storefront data file
		// Setting up dbWriter as a mock object
		EPFFileReader fileReader = new EPFFileReader(storefrontEpfFile);
		dbWriter = EasyMock.createMock(EPFDbWriter.class);

		importTask = new EPFImportTask(storefrontEpfFile, dbWriter);

		importXlator = new EPFImportTranslator(fileReader);
		recordsExpected = importXlator.getTotalExpectedRecords();
		exportType = importXlator.getExportType();
		tableName = importXlator.getTableName();
		primaryKey = importXlator.getPrimaryKey();
		columnsAndTypes = importXlator.getColumnAndTypes();

		importerQueue = EasyMock.createMock(EPFImporterQueue.class);
	}

	// dbWriter.initImport(importXlator.getExportType(),
	// importXlator.getTableName(), importXlator.getColumnAndTypes(),
	// importXlator.getTotalDataRecords());
	@Test
	public void testSetupImportDataStore() throws EPFDbException {
		EasyMock.reset(dbWriter);
		dbWriter.initImport(exportType, tableName, columnsAndTypes, primaryKey,
				recordsExpected);
		EasyMock.expectLastCall().times(1);
		EasyMock.replay(dbWriter);
		importTask.setupImportDataStore();
		EasyMock.verify(dbWriter);
	}

	@Test
	public void testImportData() throws EPFDbException {
		EasyMock.reset(dbWriter);
		dbWriter.insertRow(EasyMock.anyObject(String[].class));
		EasyMock.expectLastCall().times(
				(int) importXlator.getTotalExpectedRecords());
		EasyMock.replay(dbWriter);
		importTask.importData();
		EasyMock.verify(dbWriter);
	}

	@Test
	public void testFinalizeImport() throws EPFDbException {
		EasyMock.reset(dbWriter);
		dbWriter.finalizeImport();
		EasyMock.expectLastCall().times(1);
		EasyMock.replay(dbWriter);
		
		EasyMock.reset(importerQueue);
		importerQueue.setCompleted(storefrontEpfFile);
		EasyMock.expectLastCall().times(1);
		EasyMock.replay(importerQueue);

		PowerMock.mockStatic(EPFImporterQueue.class);
		EPFImporterQueue.getInstance();
		EasyMock.expectLastCall()
				.andReturn(importerQueue).times(1);
		PowerMock.replay(EPFImporterQueue.class);
		
		importTask.finalizeImport();
		
		PowerMock.verify(EPFImporterQueue.class);
		EasyMock.verify(dbWriter);
		EasyMock.verify(importerQueue);
	}
}
