package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class common {
	
	static List<Special> special_list = Arrays.asList( new Special( '"' ),
			   new Special( '\'' ),
			   new Special( '(', ')' ));
	
	public static List<String> readFile(String file_path) throws IOException{
		List<String> list_line = new ArrayList<String>();
		// Open the file
		FileInputStream fstream = new FileInputStream(file_path);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
		  // Print the content on the console
		  list_line.add(strLine);
		  System.out.println (strLine);
		}
		br.close();
		return list_line;
	} 
	
	public static void initLogFile(String file_path) throws IOException {
		Path path = Paths.get(file_path);
		Files.deleteIfExists(path);
		Files.createFile(path);
	}
	
	public static void writeToFile(String file_path, List<String> list_line) throws IOException {
		final String ENDCODING = "UTF-8";
		Path file = Paths.get(file_path);
		Files.write(file, list_line, Charset.forName(ENDCODING) );
	}
	
	public static List<String> getPattern(String line){
		List<String> list_pattern = new ArrayList<String>();
		int start = 0;
		int curr_index = line.indexOf(",",start);
		if(curr_index == -1) {
			return list_pattern;
		}
		String finding = null;
		for(int i=0; i<line.length(); i++) {
			if( i==(line.length()-1)) {
				if(line.charAt(i)==',') {
					list_pattern.add(line.substring(start,i));
					list_pattern.add("");
				}else {
					list_pattern.add(line.substring(start));
				}
				break;
			}
			if(line.charAt(i)==',') {
				if(finding==null) {
					list_pattern.add(line.substring(start, i));
					start = i+1;
				} else {
					continue;
				}
			}else {
				if( finding!=null && (finding.equals(""+line.charAt(i))) ) {
					finding=null;
					continue;
				}
				else if(finding == null) {
					for (Special special_c : special_list) {
						if(special_c.first_str == line.charAt(i)) {
							finding = ""+special_c.second_str;
						}
					}
				}
			}
		}
		return list_pattern;
	}
	
	public static String patternToLine(List<String> patterns){
		String line = "";
		for (String ptrn : patterns) {
			line = line + ptrn + ",";
		}
		return line;
	}
}

class Special
{
    public char first_str; 
    public char second_str; 

	public Special(char special_str) {
		this.first_str = special_str;
		this.second_str = special_str;
	}
	public Special(char first_str, char second_str) {
		this.first_str = first_str;
		this.second_str = second_str;
	}
};


