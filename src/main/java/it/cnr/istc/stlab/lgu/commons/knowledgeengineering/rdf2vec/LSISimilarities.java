package it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec;

import java.io.IOException;

import it.cnr.istc.stlab.lgu.commons.process.ProcessUtils;

public class LSISimilarities {

	public static void computeSimilarity(String pythonBin, String projectBasePath, String walksFilePath, boolean load,
			String folderOut, int dimensionality, int threshold) throws IOException, InterruptedException {

		String[] cmd;
		if (load) {
			cmd = new String[] { pythonBin, projectBasePath + "/src/main/python/similarity_lsi.py", "--load",
					walksFilePath, folderOut, dimensionality + "", threshold + "" };
		} else {
			cmd = new String[] { pythonBin, projectBasePath + "/src/main/python/similarity_lsi.py", walksFilePath,
					folderOut, dimensionality + "", threshold + "" };
		}

		ProcessUtils.executeCommand(cmd);
	}

}
