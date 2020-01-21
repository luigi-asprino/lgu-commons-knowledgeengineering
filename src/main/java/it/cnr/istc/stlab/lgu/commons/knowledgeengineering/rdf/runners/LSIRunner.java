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

import it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec.LSISimilarities;
import it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec.WalkGenerator;
import it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec.WalkGenerator.QueryWalks;

public class LSIRunner {

	private static final Logger logger = LogManager.getLogger(LSIRunner.class);

	public static void main(String[] args) throws ConfigurationException, IOException, InterruptedException {

		Configurations configs = new Configurations();
		Configuration config = configs.properties("config.properties");

		LogManager.getLogger(WalkGenerator.class).info("test");

		String walksFile = config.getString("workdir") + "/walks_lsi";

		logger.info("LSI");

		new File(config.getString("workdir")).mkdir();

		Dataset d = TDBFactory.createDataset(config.getString("workdir") + "/tdb");

		logger.info("Reading from input file");
		if (config.getBoolean("loadTriples"))
			RDFDataMgr.read(d, config.getString("input"));

		WalkGenerator wg = new WalkGenerator();

		if (!new File(walksFile).exists()) {
			wg.setDepth(8);
			wg.setQueryWalks(QueryWalks.RDF2Vec_INGOING, QueryWalks.RDF2Vec);
			wg.setCreateAVirtualDocumentForEachEntity(true);
			wg.setEntitySelectorQuery(
					"SELECT DISTINCT ?entity {?entity a <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Object> } LIMIT 1000");

			wg.generateWalks(d, walksFile);
		}

		LSISimilarities.computeSimilarity(config.getString("pythonBin"), config.getString("projectBasePath"), walksFile,
				false, config.getString("LSIFolderOut"), config.getInt("dimensionality"), config.getInt("threshold"));

	}

}