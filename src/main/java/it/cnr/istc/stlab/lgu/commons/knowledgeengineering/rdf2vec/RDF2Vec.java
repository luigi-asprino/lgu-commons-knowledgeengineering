package it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RDF2Vec {

	private String pythonBin, rdf2vecScriptPath, walksFilePath, entityListFilePath,
			similaritiesRDF2VecFilePath = "similarities";
	private boolean load = false;
	private int dimensionality = 400, threshold = 10;

	public RDF2Vec(String pythonBin, String rdf2vecScriptPath, String walksFilePath) {
		super();
		this.pythonBin = pythonBin;
		this.rdf2vecScriptPath = rdf2vecScriptPath;
		this.walksFilePath = walksFilePath;
	}

	public static void computeSimilarity(String pythonBin, String projectBasePath, String walksFilePath, boolean load,
			String entityListFilePath, String similaritiesRDF2VecFilePath, int dimensionality, int threshold)
			throws IOException, InterruptedException {

		String[] cmd;
		if (load) {
			cmd = new String[] { pythonBin, projectBasePath + "/src/main/python/rdf2vec.py", "--load", walksFilePath,
					entityListFilePath, similaritiesRDF2VecFilePath, dimensionality + "", threshold + "" };
		} else {
			cmd = new String[] { pythonBin, projectBasePath + "/src/main/python/rdf2vec.py", walksFilePath,
					entityListFilePath, similaritiesRDF2VecFilePath, dimensionality + "", threshold + "" };
		}

		Process p = Runtime.getRuntime().exec(cmd);

		Runnable rerr = () -> {
			BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String buff = null;
			try {
				Thread.sleep(1000);
				while ((buff = err.readLine()) != null) {
					System.err.println(buff);
					Thread.sleep(100);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		};

		Runnable rin = () -> {
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String buff = null;
			try {
				while ((buff = in.readLine()) != null) {
					System.out.println(buff);
					Thread.sleep(100);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

		};

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.execute(rin);
		executorService.execute(rerr);

		p.waitFor();

		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		executorService.shutdown();
	}

	public String getPythonBin() {
		return pythonBin;
	}

	public void setPythonBin(String pythonBin) {
		this.pythonBin = pythonBin;
	}

	public String getRdf2vecScriptPath() {
		return rdf2vecScriptPath;
	}

	public void setRdf2vecScriptPath(String rdf2vecScriptPath) {
		this.rdf2vecScriptPath = rdf2vecScriptPath;
	}

	public String getWalksFilePath() {
		return walksFilePath;
	}

	public void setWalksFilePath(String walksFilePath) {
		this.walksFilePath = walksFilePath;
	}

	public String getEntityListFilePath() {
		return entityListFilePath;
	}

	public void setEntityListFilePath(String entityListFilePath) {
		this.entityListFilePath = entityListFilePath;
	}

	public String getSimilaritiesRDF2VecFilePath() {
		return similaritiesRDF2VecFilePath;
	}

	public void setSimilaritiesRDF2VecFilePath(String similaritiesRDF2VecFilePath) {
		this.similaritiesRDF2VecFilePath = similaritiesRDF2VecFilePath;
	}

	public boolean isLoad() {
		return load;
	}

	public void setLoad(boolean load) {
		this.load = load;
	}

	public int getDimensionality() {
		return dimensionality;
	}

	public void setDimensionality(int dimensionality) {
		this.dimensionality = dimensionality;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

}
