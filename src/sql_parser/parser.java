package sql_parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.OracleHint;

import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.util.SelectUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import utility.common;


public class parser {

	private static CCJSqlParserManager pm = new CCJSqlParserManager();
	static String curr_file = "";
	
	final static int TEST_CASE = 0;
	final static int PARAM = 1;
	final static int USER = 2;
	final static int ITEM4 = 3;
	final static int ITEM5 = 4;
	final static int ITEM6 = 5;
	final static int ITEM7 = 6;
	final static int ITEM8 = 7;
	
	final static String COMMON = "common1";
	final static String SUB_CASE_NUMBER = "[SUB_CASE_NUMBER]";
	
	final static String RULE_1_FILE = "rule_1.txt";
	final static String PARSER_EXCEPTION = "parser_exceptions.txt";
	

	public static void main(String[] args) throws Exception {
		// delete old log
		
		common.initLogFile(RULE_1_FILE);
		common.initLogFile(PARSER_EXCEPTION);
		
		// get list of files
		List<String> all_files = Arrays.asList("input.csv");
		// check each file
		for (String file : all_files) {
			curr_file = file;
			Files.write(Paths.get(RULE_1_FILE), ("########## File = ("+curr_file+") ##############\r\n").getBytes(), StandardOpenOption.APPEND);
            
			
			List<String> new_csv_file = new ArrayList<String>();
			
			List<String> all_line = common.readFile(file);
			
			for (String line : all_line) {
				if(line.isEmpty()) {
					new_csv_file.add("");
					continue;
				}
				if(line.startsWith("#")){
					new_csv_file.add(line);
					continue;
				}
				List<String> patterns  = common.getPattern(line);
				if(patterns.get(TEST_CASE).length()>0) {
					patterns.set(USER, SUB_CASE_NUMBER);
					String ln  = common.patternToLine(patterns);
					new_csv_file.add(ln);
					continue;
				}
				
				// rule 1
				line = checkRule_1(line);
				new_csv_file.add(line);
			}
			// Calculate Sub-case number
			Update_SubCaseNumber(new_csv_file);
			common.writeToFile("output.csv", new_csv_file);
		}
	}
	
	public static String checkRule_1(String line) throws JSQLParserException, IOException {
		List<String> patterns  = common.getPattern(line);
		// command line
		String param = patterns.get(PARAM);
		System.out.println(param);
		Statement statement=null;
		try {
			statement = pm.parse(new StringReader(param));
		}catch(JSQLParserException e) {
			// output JSQLParserException to file
			String log = "PARAM = ["+param+"]  (File = "+curr_file+")";
			Files.write(Paths.get(PARSER_EXCEPTION), log.getBytes(), StandardOpenOption.APPEND);
			return line;
		}
		if (statement instanceof Select) {
            Select selectStatement = (Select) statement;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
            
            for (Iterator<String> iter = tableList.iterator(); iter.hasNext(); ) {
                String tableName = (String) iter.next(); // table name
                // log for RULE_1_FILE
                String log = "Table =["+tableName+"] Line = ["+line+ "]\r\n";
                int next_line_indx = log.indexOf("["+line);
                log = log + new String(new char[next_line_indx]).replace('\0', ' ');
                // replace table name = new name
                line = line.replaceAll(" "+tableName, " "+COMMON +"." + tableName);
                // ouput log to file
                log = log + "["+line+"]\r\n";
                Files.write(Paths.get(RULE_1_FILE), log.getBytes(), StandardOpenOption.APPEND);
            }
        }
		
		return line;
	}
	
	public static void Update_SubCaseNumber(List<String> csv_file) throws Exception {
		 
		 for (int index = 0; index < csv_file.size(); index++) {

			if(csv_file.get(index).contains(SUB_CASE_NUMBER)) {
				int i = index+1;
				for(; i<csv_file.size(); i++) {
					// find the next test cases or #
					if(csv_file.get(i).contains(SUB_CASE_NUMBER) || csv_file.get(i).startsWith("#") ) {
						break;
					}
				}
				
				if( i==csv_file.size() ) {
					// if last test cases, check for space
					i = index+1;
					for(; i<csv_file.size(); i++) {
						if(csv_file.get(i).trim().length() == 0) {
							break;
						}
					}
				}
				
				csv_file.set(index, csv_file.get(index).replace(SUB_CASE_NUMBER, String.valueOf(i-index-1)));
			}
		}
	}
	
	
	
	
    public static void testGetTableList() throws Exception {

        String sql = "SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
                + " WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)";
        
        sql = "select * from (select * from ue3);";
        net.sf.jsqlparser.statement.Statement statement = pm.parse(new StringReader(sql));

        // now you should use a class that implements StatementVisitor to decide what to do
        // based on the kind of the statement, that is SELECT or INSERT etc. but here we are only
        // interested in SELECTS
        if (statement instanceof Select) {
            Select selectStatement = (Select) statement;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
            
            for (Iterator<String> iter = tableList.iterator(); iter.hasNext();) {
                String tableName = (String) iter.next();
                System.out.println(tableName);
            }
        }
        statement = pm.parse(new StringReader("create table x(id text, name text)"));
        if (statement instanceof CreateTable) {
        	System.out.println("CreateTable");
        	CreateTable createTable = (CreateTable) statement;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(createTable);
            
            for (Iterator<String> iter = tableList.iterator(); iter.hasNext();) {
                String tableName = (String) iter.next();
                System.out.println(tableName);
            }
        }
        statement = pm.parse(new StringReader("CREATE VIEW `view_name` AS SELECT statement;"));
        if (statement instanceof CreateView) {
        	System.out.println("CreateView");
        	CreateView createView = (CreateView) statement;
        }
        statement = pm.parse(new StringReader("ALTER TABLE users\r\n" + 
        		"ADD COLUMN `count` SMALLINT(6) NOT NULL AFTER `lastname`,\r\n" + 
        		"ADD COLUMN `log` VARCHAR(12) NOT NULL AFTER `count`,\r\n" + 
        		"ADD COLUMN `status` INT(10) UNSIGNED NOT NULL AFTER `log`;"));
        if (statement instanceof Alter) {
        	System.out.println("Alter");
        	Alter alter = (Alter) statement;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            
        }
        statement = pm.parse(new StringReader("INSERT INTO Customers (CustomerName, ContactName, Address, City, PostalCode, Country) VALUES ('Cardinal', 'Tom B. Erichsen', 'Skagen 21', 'Stavanger', '4006', 'Norway')"));
        if (statement instanceof Insert) {
        	System.out.println("Insert");
        	Insert insert = (Insert) statement;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(insert);
            
            int i = 1;
            for (Iterator iter = tableList.iterator(); iter.hasNext(); i++) {
                String tableName = (String) iter.next();
                System.out.println(tableName);
            }
        }
        int i=0;
    }

}
