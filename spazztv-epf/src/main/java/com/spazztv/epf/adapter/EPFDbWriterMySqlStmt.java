package com.spazztv.epf.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EPFDbWriterMySqlStmt {
	public static String DROP_TABLE_STMT = "DROP TABLE IF EXISTS %s";
	public static String CREATE_TABLE_STMT = "CREATE TABLE %s (%s) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci";
	public static String RENAME_TABLE_STMT = "ALTER TABLE %s RENAME %s";
	public static String PRIMARY_KEY_STMT = "ALTER TABLE %s ADD CONSTRAINT PRIMARY KEY (%s)";
	public static String INSERT_SQL_STMT = "%s INTO %s (%s) VALUES %s";
	public static String TABLE_EXISTS_SQL = "SHOW TABLES";
	public static String UNLOCK_TABLES = "UNLOCK TABLES";

	public static String UNION_CREATE_PT1 = "CREATE TABLE %s AS SELECT * FROM %s UNION ALL ";
	public static String UNION_CREATE_PT2 = "SELECT * FROM %s WHERE 0 = (SELECT COUNT(*) FROM %s %s)";
	public static String UNION_CREATE_WHERE = "WHERE %s.export_date <= %s.export_date";
	public static String UNION_CREATE_JOIN = "AND %s.%s = %s.%s ";

	private String insertTable;
	private String insertCommand;
	private int insertColumnCount = 0;
	private int insertRowCount;
	private String insertStatementSql;
	private String insertColumnStmt;
	private LinkedHashMap<String, String> insertColumnsAndTypes;

	public static Map<String, String> TRANSLATION_MAP = Collections
			.unmodifiableMap(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
				{
					put("CLOB", "LONGTEXT");
				}
			});

	public static List<String> UNQUOTED_TYPES = Collections
			.unmodifiableList(new ArrayList<String>() {
				private static final long serialVersionUID = 1L;
				{
					add("INTEGER");
					add("INT");
					add("BIGINT");
					add("TINYINT");
				}
			});

	private String translateColumnType(String columnType) {
		if (TRANSLATION_MAP.containsKey(columnType)) {
			return TRANSLATION_MAP.get(columnType);
		}
		return columnType;
	}

	public String dropTableStmt(String tableName) {
		return String.format(DROP_TABLE_STMT, tableName);
	}

	public String createTableStmt(String tableName,
			LinkedHashMap<String, String> columnsAndTypes) {
		String columnsToCreate = "";

		Iterator<Entry<String, String>> entrySet = columnsAndTypes.entrySet()
				.iterator();
		while (entrySet.hasNext()) {
			Entry<String, String> colNType = entrySet.next();
			columnsToCreate += "`" + colNType.getKey() + "` "
					+ translateColumnType(colNType.getValue());

			if (entrySet.hasNext()) {
				columnsToCreate += ", ";
			}
		}

		return String.format(CREATE_TABLE_STMT, tableName, columnsToCreate);
	}

	public LinkedHashMap<String, String> setupColumnAndTypesMap(
			LinkedHashMap<String, String> columnsAndTypes,
			List<String> currentColumns) {
		// if currentColumns == null, return columnsAndTypes
		LinkedHashMap<String, String> actualColAndTypes;
		if (currentColumns == null) {
			actualColAndTypes = columnsAndTypes;
		} else {
			// Create a columns and types map for only the columns in
			// currentColumns
			actualColAndTypes = new LinkedHashMap<String, String>();
			for (String currentColumn : currentColumns) {
				if (columnsAndTypes.containsKey(currentColumn)) {
					actualColAndTypes.put(currentColumn,
							columnsAndTypes.get(currentColumn));
				}
			}
		}
		return actualColAndTypes;
	}

	public String setPrimaryKeyStmt(String tableName, List<String> keyColumns) {

		String primaryKeyColumns = "";

		Iterator<String> i = keyColumns.iterator();
		while (i.hasNext()) {
			primaryKeyColumns += "`" + (String) i.next() + "`";
			if (i.hasNext()) {
				primaryKeyColumns += ",";
			}
		}

		return String.format(PRIMARY_KEY_STMT, tableName, primaryKeyColumns);
	}

	public String insertRowStmt(String tableName,
			LinkedHashMap<String, String> columnsAndTypes,
			List<List<String>> insertValues, String insertCommand) {

		if (insertValues == null) {
			return null;
		}
		if (insertValues.size() == 0) {
			return null;
		}

		if (!tableName.equals(insertTable)
				|| insertRowCount != insertValues.size()
				|| insertColumnCount != insertValues.get(0).size()
				|| !insertCommand.equals(this.insertCommand)
				|| !columnsAndTypes.equals(insertColumnsAndTypes)) {
			insertStatementSql = createInsertPreparedStatmentSQL(tableName,
					columnsAndTypes, insertValues, insertCommand);
			insertTable = tableName;
			insertColumnsAndTypes = columnsAndTypes;
			this.insertCommand = insertCommand;
			insertRowCount = insertValues.size();
			insertColumnCount = insertValues.get(0).size();
		}

		return insertStatementSql;
	}

	private String createInsertPreparedStatmentSQL(String tableName,
			LinkedHashMap<String, String> columnsAndTypes,
			List<List<String>> insertValues, String insertCommand) {

		StringBuffer s = new StringBuffer();
		s.append("(");
		for (int i = 0; i < insertValues.get(0).size(); i++) {
			if (s.length() > 1) {
				s.append(",");
			}
			s.append("?");
		}
		s.append(")");

		insertColumnStmt = s.toString();

		StringBuffer insertBuffer = new StringBuffer();

		for (int i = 0; i < insertValues.size(); i++) {
			if (insertBuffer.length() > 0) {
				insertBuffer.append(",");
			}
			insertBuffer.append(insertColumnStmt);
		}

		return String.format(INSERT_SQL_STMT, insertCommand, tableName,
				columnNames(columnsAndTypes), insertBuffer.toString());
	}

	public String mergeTableStmt(String tableName, String incTableName,
			String unionTableName, List<String> primaryKey) {
		String mergeWhere = String.format(UNION_CREATE_WHERE, tableName,
				incTableName);

		Iterator<String> keyColumns = primaryKey.iterator();
		while (keyColumns.hasNext()) {
			String keyColumn = keyColumns.next();
			mergeWhere += " "
					+ String.format(UNION_CREATE_JOIN, tableName, keyColumn,
							incTableName, keyColumn);
		}

		String unionCreateTableSQL = String.format(UNION_CREATE_PT1,
				unionTableName, incTableName);
		unionCreateTableSQL += " "
				+ String.format(UNION_CREATE_PT2, tableName, incTableName,
						mergeWhere);

		return unionCreateTableSQL;
	}

	public String renameTableStmt(String srcTableName, String destTableName) {
		return String.format(RENAME_TABLE_STMT, srcTableName, destTableName);
	}

	/**
	 * Return the column names as a string for an insert statement
	 */
	private String columnNames(LinkedHashMap<String, String> columnsAndTypes) {
		StringBuilder concatList = new StringBuilder();
		for (String word : columnsAndTypes.keySet()) {
			if (concatList.length() > 0) {
				concatList.append(",");
			}
			concatList.append("`" + word + "`");
		}
		return concatList.toString();
	}
}
