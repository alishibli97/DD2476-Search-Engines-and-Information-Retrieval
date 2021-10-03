import java.util.*;
import java.util.stream.IntStream;
import java.io.*;

public class PageRank {

	class Document implements Comparable<Document> {

		String docNumber;
		double docScore;

		Document(String docNumber, double docScore) {
			this.docNumber = docNumber;
			this.docScore = docScore;
		}

		public int compareTo(Document doc) {

			if (this.docScore == doc.docScore)
				return 0;
			else if (this.docScore < doc.docScore)
				return 1;
			else
				return -1;
		}
	}

	/**
	 * Maximal number of documents. We're assuming here that we don't have more docs
	 * than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 * Mapping from document names to document numbers.
	 */
	HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

	/**
	 * Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

	/**
	 * A memory-efficient representation of the transition matrix. The outlinks are
	 * represented as a HashMap, whose keys are the numbers of the documents linked
	 * from.
	 * <p>
	 *
	 * The value corresponding to key i is a HashMap whose keys are all the numbers
	 * of documents j that i links to.
	 * <p>
	 *
	 * If there are no outlinks from i, then the value corresponding key i is null.
	 */
	HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

	/**
	 * The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];

	/**
	 * The probability that the surfer will be bored, stop following links, and take
	 * a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 * Convergence criterion: Transition probabilities do not change more that
	 * EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/* --------------------------------------------- */

	public PageRank(String filename) {
		int noOfDocs = readDocs(filename);

		Map<String, Boolean> options = new HashMap<String, Boolean>();
		options.put("power_iteration", false);
		options.put("monte_carlo_1", true);
		options.put("monte_carlo_2", false);
		options.put("monte_carlo_4", false);
		options.put("monte_carlo_5", false);

		iterate(noOfDocs, 1000, options);
	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and fills the data structures.
	 *
	 * @return the number of documents read.
	 */
	int readDocs(String filename) {
		int fileIndex = 0;
		try {
			System.err.print("Reading file... ");
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				String title = line.substring(0, index);
				Integer fromdoc = docNumber.get(title);
				if (fromdoc == null) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = fileIndex++;
					docNumber.put(title, fromdoc);
					docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
				while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumber.get(otherTitle);
					if (otherDoc == null) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put(otherTitle, otherDoc);
						docName[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if (link.get(fromdoc) == null) {
						link.put(fromdoc, new HashMap<Integer, Boolean>());
					}
					if (link.get(fromdoc).get(otherDoc) == null) {
						link.get(fromdoc).put(otherDoc, true);
						out[fromdoc]++;
					}
				}
			}
			if (fileIndex >= MAX_NUMBER_OF_DOCS) {
				System.err.print("stopped reading since documents table is full. ");
			} else {
				System.err.print("done. ");
			}
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		System.err.println("Read " + fileIndex + " number of documents");
		return fileIndex;
	}

	/* --------------------------------------------- */

	/*
	 * Chooses a probability vector a, and repeatedly computes aP, aP^2, aP^3...
	 * until aP^i = aP^(i+1).
	 */
	void iterate(int numberOfDocs, int maxIterations, Map<String, Boolean> options) {

		int N = 1000;

		// YOUR CODE HERE
		double[] x = new double[numberOfDocs];
		if (options.get("power_iteration"))
			x = this.power_iteration(numberOfDocs, maxIterations);
		else if (options.get("monte_carlo_1"))
			x = this.monte_carlo_1(numberOfDocs, N);
		else if (options.get("monte_carlo_2"))
			x = this.monte_carlo_2(numberOfDocs, N);
		else if (options.get("monte_carlo_4"))
			x = this.monte_carlo_4(numberOfDocs, N);
		else if (options.get("monte_carlo_5"))
			this.monte_carlo_5(numberOfDocs, N);

		ArrayList<Document> docs = new ArrayList<Document>();
		for (int i = 0; i < numberOfDocs; i++) docs.add(new Document(docName[i], x[i]));
		Collections.sort(docs);
		for (int i = 0; i < 30; i++) print(String.valueOf(i + 1) + ": " + docs.get(i).docNumber + " " + String.valueOf(docs.get(i).docScore));

		if(!options.get("power_iteration")){
			try {
				FileWriter writer = new FileWriter("output.txt");
				for (Document doc : docs)
					writer.write(doc.docNumber + "," + doc.docScore + System.lineSeparator());
				writer.close();
			} catch (Exception e) {
				// TODO: handle exception
			}	
		}

	}

	/* --------------------------------------------- */

	private double[] power_iteration(int numberOfDocs, int maxIterations) {
		double[] x = new double[numberOfDocs];

		Arrays.fill(x, BORED / numberOfDocs);
		double diff = Double.MAX_VALUE;

		int iter = 0;
		while (iter < maxIterations) {

			double[] x_new = new double[numberOfDocs];
			Arrays.fill(x_new, 0.54 / numberOfDocs);
			for (int i = 0; i < numberOfDocs; i++) {
				if (link.get(i) != null) {
					for (int key : link.get(i).keySet()) {
						if (link.get(i).get(key))
							x_new[key] += (x[i] / out[i]) * (1 - BORED);
					}
				}
			}

			diff = 0;
			for (int k = 0; k < numberOfDocs; k++) {
				diff += Math.abs(x[k] - x_new[k]);
			}
			System.out.println("Ending iteration " + iter + ", difference: " + diff);

			System.arraycopy(x_new, 0, x, 0, numberOfDocs);

			if (diff < EPSILON)
				break;

			iter++;
		}

		if (diff < EPSILON) {
			System.out.println("Converged at iteration " + iter);
		} else
			System.out.println("Exceeded number of iteration " + maxIterations);
		
		double sum = 0.0;
		for(int i=0;i<numberOfDocs;i++) sum+=x[i];
		for(int i=0;i<numberOfDocs;i++) x[i]/=sum;

		return x;

	}

	private double[] monte_carlo_1(int numberOfDocs, int maxIterations) {
		int[] result = new int[numberOfDocs];

		Random random = new Random();

		for (int iter = 0; iter < maxIterations; iter++) {
			// print("Starting iteration "+iter);
			
			int i = random.nextInt(numberOfDocs);
			while (random.nextDouble() > BORED) {
				if (out[i] > 0) {
					Integer[] outlinks = link.get(i).keySet().toArray(new Integer[0]);
					int next_i = random.nextInt(outlinks.length);
					i = outlinks[next_i];
				} else {
					i = random.nextInt(numberOfDocs);
				}
			}
			result[i]++;
		}

		double[] x = new double[numberOfDocs];
		for (int i = 0; i < x.length; i++) {
			x[i] = 1.0 * result[i] / maxIterations;
		}
		
		double sum = 0.0;
		for (int i = 0; i < x.length; i++) sum += x[i];
		for (int i = 0; i < x.length; i++) x[i] /= sum;

		return x;
	}

	private double[] monte_carlo_2(int numberOfDocs, int maxIterations) {
		int[] result = new int[numberOfDocs];
		
		Random random = new Random();
		
		for (int iter = 0; iter < maxIterations; iter++) {
			print("Starting iteration "+iter);

			for (int start_i = 0; start_i < numberOfDocs; start_i++) {
				int i = start_i;
				while (random.nextDouble() > BORED) {
					if (out[i] > 0) {
						Integer[] outlinks = link.get(i).keySet().toArray(new Integer[0]);
						int next_i = random.nextInt(outlinks.length);
						i = outlinks[next_i];
					} else {
						i = random.nextInt(numberOfDocs);
					}
				}
				result[i]++;
			}
		}
		
		double[] x = new double[numberOfDocs];
		for (int i = 0; i < x.length; i++) {
			x[i] = 1.0 * result[i] / maxIterations;
		}
		
		double sum = 0.0;
		for (int i = 0; i < x.length; i++) sum += x[i];
		for (int i = 0; i < x.length; i++) x[i] /= sum;

		return x;

	}

	private double[] monte_carlo_4(int numberOfDocs, int maxIterations) {
		int[] result = new int[numberOfDocs];
		
		Random random = new Random();
		
		for (int iter = 0; iter < maxIterations; iter++) {
			print("Starting iteration "+iter);

			for (int start_i = 0; start_i < numberOfDocs; start_i++) {
				int i = start_i;
				while (random.nextDouble() > BORED) {
					if (out[i] > 0) {
						Integer[] outlinks = link.get(i).keySet().toArray(new Integer[0]);
						int next_i = random.nextInt(outlinks.length);
						i = outlinks[next_i];
					} else {
						break;
					}
					result[i]++;
				}
			}
		}
		
		double[] x = new double[numberOfDocs];
		for (int i = 0; i < x.length; i++) {
			x[i] = 1.0 * result[i] / maxIterations;
		}
		
		double sum = 0.0;
		for (int i = 0; i < x.length; i++) sum += x[i];
		for (int i = 0; i < x.length; i++) x[i] /= sum;

		return x;

	}

	private double[] monte_carlo_5(int numberOfDocs, int maxIterations) {
		int[] result = new int[numberOfDocs];
		
		Random random = new Random();
		
		double next_target = 0.0;
		for (int iter = 0; iter < maxIterations; iter++) {
			print("Starting iteration "+iter);

			int i = random.nextInt(numberOfDocs);
			while (random.nextDouble() > BORED) {
				if (out[i] > 0) {
					Integer[] outlinks = link.get(i).keySet().toArray(new Integer[0]);
					int next_i = random.nextInt(outlinks.length);
					i = outlinks[next_i];
				} else {
					break;
				}
				result[i]++;
			}
		}
		
		double[] x = new double[numberOfDocs];
		for (int i = 0; i < x.length; i++) {
			x[i] = 1.0 * result[i] / maxIterations;
		}
		
		double sum = 0.0;
		for (int i = 0; i < x.length; i++) sum += x[i];
		for (int i = 0; i < x.length; i++) x[i] /= sum;
		
		return x;

	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Please give the name of the link file");
		} else {
			new PageRank(args[0]);
		}
	}

	public static void print(Object o) {
		System.out.println(String.valueOf(o));
	}
}
