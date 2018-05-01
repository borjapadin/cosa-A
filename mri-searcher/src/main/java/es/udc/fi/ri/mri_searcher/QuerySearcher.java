package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class QuerySearcher {
	
	private static String queriesPath = "C:\\Users\\Uxia\\Desktop\\RI\\P2\\cacm\\query.text";
	private static int LAST_QUERY = 64;
	private static int firstQueryID;
	

	public static List<String> identifyQueries(String queries) throws IOException {
		int opt = 0;
		int int1 = 0, int2 = 0;
		
		if (queries.equals("all")) {
			//todas las queries
			opt = 1;
		} else if (queries.contains("-")) {
			//queries en el rango int1-int2
			opt = 3; 
			String[] numbers = queries.split("-");
			int1 = Integer.parseInt(numbers[0]);
			int2 = Integer.parseInt(numbers[1]);
		} else {
			//query int1
			opt = 2;
			int1 = Integer.parseInt(queries);
		}
		return launchQueries(opt, int1, int2);
	}
	
	private static List<String> launchQueries(int opt, int int1, int int2) throws IOException {
		List<String> queries = null;
		
		switch (opt) {
		
		case 1:
			//todas las queries
			queries = searchQueries(1, LAST_QUERY);
			setFirstQueryID(1); 
			break;
		case 2:
			//query int1
			queries = new ArrayList<>();
			queries.add(searchQuery(int1));
			setFirstQueryID(int1);
			break;
		case 3:
			//queries en el rango int1-int2
			queries = searchQueries(int1, int2);
			setFirstQueryID(Math.min(int1, int2));
			break;
		default:
			System.err.println("Error in parameter \"-queries\"");
			System.exit(1);
			break;
		}
		return queries;
	}
	
	
	public static String searchQuery(int int1) throws IOException {
		System.out.println("---> Indexamos la query " + int1 + " en: " + queriesPath);
		InputStream stream = Files.newInputStream(Paths.get(queriesPath));
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);

		/* First the contents are converted to a string */
		String text = strBuffer.toString();

		String[] lines = text.split("\n");

		StringBuilder query = new StringBuilder();

		int n = 0;
		int i = 0;
		while (i < lines.length) {
			if (lines[i].startsWith(".I")) {
				if (++n == int1) {
					i += 2; //Para que no pille el .I
					while ((!lines[i].startsWith(".N")) && (!lines[i].startsWith(".A"))) {
						query.append(lines[i++]);
					}
					break;
				}
			}
			i++;
		}
		//System.out.println("W: " + query);
		return query.toString();
	}
	
	
	private static List<String> searchQueries(int int1, int int2) throws IOException {
		System.out.println("---> Indexamos las queries " + int1 + "-" + int2 + " en: " + queriesPath);
		InputStream stream = Files.newInputStream(Paths.get(queriesPath));
		String str = IOUtils.toString(stream, "UTF-8");
		StringBuffer strBuffer = new StringBuffer(str);
		
		/* First the contents are converted to a string */
		String text = strBuffer.toString();

		String[] lines = text.split("\n");

		StringBuilder query = new StringBuilder();
		List<String> queryList = new ArrayList<>();

		int min = Math.min(int1, int2);
		int max = Math.max(int1, int2);
		int n = 0;
		int i = 0;
		while (i < lines.length) {
			if (lines[i].startsWith(".I")) {
				n++;
				if ((n >= min) && (n <= max)) {
					i += 2;
					while ((!lines[i].startsWith(".N")) && (!lines[i].startsWith(".A"))) {
						query.append(lines[i++]);
					}
					//System.out.println("Añadimos W (LINEA " + i + ")" + query);
					queryList.add(query.toString());
					query = new StringBuilder();
					i--;
				} else if (n > max)
					break;
			}
			i++;
		}
		return queryList;
	}
	
	public static int getFirstQueryID() {
		return firstQueryID;
	}

	public static void setFirstQueryID(int firstQueryID) {
		QuerySearcher.firstQueryID = firstQueryID;
	}

}
