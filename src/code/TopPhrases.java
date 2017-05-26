package code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 
 * Given a large file that does not fit in memory (say 10GB), find the top
 * 100000 most frequent phrases. The file has 50 phrases per line separated by a
 * pipe (|). Assume that the phrases do not contain pipe. Example line may look
 * like: Foobar Candy | Olympics 2012 | PGA | CNET | Microsoft Bing .... The
 * above line has 5 phrases in visible region.
 * 
 * 
 * This is going to work in the following way: 
 * - 1 stream the source file and create statistics on chunks that are stored in a temp file for e.g: 
 * 8|foo
 * 7|hello 
 * ---- created on chunk 1
 * 3|bar
 * 9|foo
 * --- created on chunk 2
 * 
 * - 2 sort the file using the external sort alg. (https://en.wikipedia.org/wiki/External_sorting) 
 * 9|foo
 * 8|foo 
 * 7|hello
 * 3|bar
 * 
 * - 3 stream the result file and create a sorted map that keeps on the top with  the most occurrences
 * 
 * @author andreit
 *
 */
public class TopPhrases {

	private static final int DEFAULT_LIMIT = 100000;

	public static class StatisitcsComputer {
		private Integer threshold;
		private Integer limit;
		private Map<String, Long> buffer = new HashMap<>();
		private BufferedWriter writer;

		public StatisitcsComputer(BufferedWriter writer, Integer limit) {
			super();
			this.writer = writer;
			this.limit = limit;
			this.threshold = limit;
		}

		/**
		 * Parse line, compute stats and update the buffer
		 * 
		 * @param statisitcs
		 *            this stores the stats
		 * @param line
		 *            the line that contains the phases from file
		 * @throws IOException
		 */
		public void update(String line) throws IOException {

			String[] phrases = line.split("\\|");
			for (String phrase : phrases) {
				Long count = buffer.get(phrase);
				buffer.put(phrase, (count == null) ? 1 : count + 1);
			}

			if (threshold == 0) {
				// Write to output file the stats and clean the map
				flush();
			}

			// Cyclic counter
			threshold = threshold == 0 ? limit : threshold - 1;

		}

		/**
		 * Write current statistics to file and release buffer
		 * 
		 * @throws IOException
		 */
		private void flush() throws IOException {

			buffer = buffer.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(
					Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));

			for (Entry<String, Long> entry : buffer.entrySet()) {
				writer.write(entry.getValue() + "|" + entry.getKey() + "\n");
			}
			buffer = new HashMap<String, Long>();
			writer.flush();
		}

	}

	/**
	 * Implements https://en.wikipedia.org/wiki/External_sorting. This will sort
	 * the stats files desc
	 * 
	 * @author andreit
	 *
	 */
	public static class ExternalSort {

		public static class Stat {
			Long count;
			String phase;

			public Stat(String line) {
				String[] token = line.split("\\|");
				this.count = Long.parseLong(token[0]);
				this.phase = (token.length == 2) ? token[1] : "";
			}

			@Override
			public String toString() {
				return count + "|" + phase;
			}

		}

		private static final String TEMP_FILE_PREFIX = "sort-file-";

		public void externalSort(String fileName) throws IOException {

			File file = new File(fileName);
			long fileSize = file.length();
			int slices = (int) Math.ceil((double) fileSize / DEFAULT_LIMIT);

			split(fileName, fileSize, slices);
			merge(fileName, fileSize, slices);

		}

		private int split(String fileName, long fileSize, int slices) throws FileNotFoundException, IOException {

			long size = DEFAULT_LIMIT < fileSize ? DEFAULT_LIMIT : fileSize;
			List<Stat> buffer = new ArrayList<Stat>((int) size);

			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);

			// Iterate through the elements in the file
			for (int i = 0; i < slices; i++) {

				for (String line = br.readLine(); line != null; line = br.readLine()) {
					buffer.add(new Stat(line));
				}

				// Sort elements
				Collections.sort(buffer, new Comparator<Stat>() {
					@Override
					public int compare(Stat o1, Stat o2) {
						if (o2.count == o1.count) {
							return o2.phase.compareTo(o1.phase);
						}
						return o2.count.compareTo(o1.count);
					}
				});

				// Write the sorted numbers to temp file

				String outFile = TEMP_FILE_PREFIX + Integer.toString(i) + ".tmp";
				FileWriter fw = new FileWriter(outFile);
				PrintWriter pw = new PrintWriter(fw);
				for (Stat elem : buffer) {
					// System.err.println(outFile + " " + elem);
					pw.println(elem);
				}
				pw.close();
				fw.close();
			}

			br.close();
			fr.close();
			return slices;
		}

		private void merge(String fileName, long fileSize, int slices) throws FileNotFoundException, IOException {
			// Now open each file and merge them, then write back to disk
			Stat[] stopStats = new Stat[slices];
			BufferedReader[] brs = new BufferedReader[slices];
			for (int i = 0; i < slices; i++) {
				brs[i] = new BufferedReader(new FileReader(TEMP_FILE_PREFIX + Integer.toString(i) + ".tmp"));
				String t = brs[i].readLine();
				if (t != null) {
					stopStats[i] = new Stat(t);
				}
			}

			FileWriter fw = new FileWriter(fileName);
			PrintWriter pw = new PrintWriter(fw);

			for (int i = 0; i < fileSize; i++) {
				Stat min = stopStats[0];
				int minFile = 0;

				for (int j = 0; j < slices; j++) {
					if (min.count > stopStats[j].count) {
						min = stopStats[j];
						minFile = j;
					}
				}

				pw.println(min);
				String line = brs[minFile].readLine();
				if (line != null) {
					stopStats[minFile] = new Stat(line);
				}
			}

			for (int i = 0; i < slices; i++) {
				brs[i].close();
			}

			pw.close();
			fw.close();
		}

	}

	/**
	 * Stream the file and store statistics about the phases in new output file.
	 * Basically the new file will have on each line the count and the phase
	 * split by | for e.g 10 | mac os
	 * 
	 * @param inputFileName
	 * @param outputFileName
	 * @param limit
	 * @throws IOException
	 */
	private static void computeStats(String inputFileName, String outputFileName, Integer limit) throws IOException {

		InputStream inputStream = TopPhrases.class.getResourceAsStream(inputFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		OutputStream outputStream = new FileOutputStream(new File(outputFileName));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

		// The alg. is pretty simple we compute stats on chunks on the big file
		StatisitcsComputer statisitcs = new StatisitcsComputer(writer, limit);

		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			statisitcs.update(line);
		}

		statisitcs.flush();
		inputStream.close();

		ExternalSort sorter = new ExternalSort();
		sorter.externalSort(outputFileName);

		outputStream.flush();
		outputStream.close();

	}

	private static Map<String, Long> getTopPhrases(String inputFileName, int limit) throws IOException {

		// Create a temp file to store the stats
		File temp = File.createTempFile("temp-file-name", ".tmp");
		String outputFileName = temp.getAbsolutePath();
		// System.err.println(outputFileName);
		// Compute stats
		computeStats(inputFileName, outputFileName, limit);
		BufferedReader reader = new BufferedReader(new FileReader(outputFileName));

		System.err.println(outputFileName);

		// Merge results
		Map<String, Long> map = new HashMap<>(limit + 1);
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {

			// Parse line and compute stats
			String[] phrases = line.split("\\|");
			if (phrases.length != 2) {
				continue;
			}

			Long count = Long.parseLong(phrases[0]);
			String phrase = phrases[1];
			long newCount = (map.get(phrase) == null) ? count : map.get(phrase) + count;
			map.put(phrase, newCount);
			map = limit(map, limit + 1);
		}

		reader.close();
		return limit(map, limit);

	}

	/**
	 * Sort the map by count and limit it
	 * 
	 * @param statistics
	 * @param limit
	 * @return
	 */
	private static Map<String, Long> limit(Map<String, Long> statistics, int limit) {

		return statistics.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(limit)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));

	}

	public static void main(String[] args) {
		Logger logger = Logger.getLogger(TopPhrases.class.getName());

		try {
			System.out.println(getTopPhrases("phrases.txt", DEFAULT_LIMIT));
			System.out.println(getTopPhrases("phrases.txt", 3));
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
	}

}