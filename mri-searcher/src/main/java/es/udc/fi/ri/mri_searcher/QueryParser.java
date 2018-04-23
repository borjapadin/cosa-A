package es.udc.fi.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class QueryParser {
	
	private static String queriesPath = "C:\\Users\\Uxia\\Desktop\\RI\\P2\\cacm\\query.text";

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
		System.out.println("W: " + query);
		return query.toString();
	}
	
	
	public static List<String> searchQueries(int int1, int int2) throws IOException {
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
					System.out.println("Añadimos W (LINEA " + i + ")" + query);
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

}
