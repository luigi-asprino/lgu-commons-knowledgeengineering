package it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf.runners;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec.RDF2Vec;
import it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec.WalkGenerator;

public class RDF2VecRunner {
	
	private static Logger logger = LogManager.getLogger(RDF2VecRunner.class);

	public static void main(String[] args) throws ConfigurationException, IOException, InterruptedException {

		Configurations configs = new Configurations();
		Configuration config = configs.properties("config.properties");

		LogManager.getLogger(WalkGenerator.class).info("test");

		String walksFile = config.getString("workdir") + "/walks";
		String entityList = config.getString("workdir") + "/entities";

		logger.info("RDF2Vec");

		new File(config.getString("workdir")).mkdir();

		Dataset d = TDBFactory.createDataset(config.getString("workdir") + "/tdb");

		logger.info("Reading from input file");
		if (config.getBoolean("loadTriples"))
			RDFDataMgr.read(d, config.getString("input"));

		WalkGenerator wg = new WalkGenerator();

		if (!new File(walksFile).exists()) {
			logger.info("Generate walks");
			wg.setEntitySelectorQuery(
					"SELECT DISTINCT ?entity {?entity a <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Object> } LIMIT 10");
			wg.generateWalks(d, walksFile);
		}

		if (!new File(entityList).exists()) {
			logger.info("Create entity list");
			WalkGenerator.writeEntityListFromQuery(d,
					"SELECT ?r {?r a <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Object> } LIMIT 10", "r",
					entityList);
		}

		RDF2Vec.computeSimilarity(config.getString("pythonBin"), config.getString("projectBasePath"), walksFile, false,
				entityList, config.getString("workdir") + "/similarities", config.getInt("dimensionality"),
				config.getInt("threshold"));
	}

}
