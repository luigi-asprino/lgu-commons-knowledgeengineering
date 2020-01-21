package it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.tdb.TDBFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import it.cnr.istc.stlab.lgu.commons.iterations.ProgressCounter;

/**
 * 
 * A WalkGenerator derived from RDF2Vec
 * https://datalab.rwth-aachen.de/embedding/RDF2Vec/
 * 
 * @author lgu
 *
 */
public class WalkGenerator {

	private static Logger logger = LogManager.getLogger(WalkGenerator.class);

	private int depth = 8, numWalksPerEntity = 200;
	private boolean excludeLiteralsFromPath = false;
	private boolean createAVirtualDocumentForEachEntity = false;
	private String separator = " ", entitySelectorQuery = defaultEntitySelectorQuery;
	private static final String defaultEntitySelectorQuery = "SELECT DISTINCT ?entity WHERE {?entity ?p _:o}";
	private static final String STRING_TOKENIZER_DELIMITERS = ".;,:?!'`\\/";
	private List<QueryWalks> queryWalks = new ArrayList<>();
	private Map<String, String> prefixMap = new HashMap<String, String>();

	public enum QueryWalks {
		RDF2Vec, RDF2Vec_NO_PREDICATES, RDF2Vec_INGOING
	}

	public void generateWalks(String tdbFilePath, String fileOut) throws IOException {
		Dataset d = TDBFactory.createDataset(tdbFilePath);
		generateWalks(d, fileOut);
	}

	public void generateWalks(Dataset d, String fileOut) throws IOException {

		List<String> queries = new ArrayList<>();

		for (QueryWalks q : queryWalks) {
			switch (q) {
			case RDF2Vec:
				queries.add(generateQueryRDF2Vec(depth, numWalksPerEntity));
				break;
			case RDF2Vec_NO_PREDICATES:
				queries.add(generateQueryNoPredicates(depth, numWalksPerEntity));
				break;
			case RDF2Vec_INGOING:
				queries.add(generateQueryRDF2VecIngoing(depth, numWalksPerEntity));
				break;
			}
		}

		if (queries.isEmpty()) {
			queries.add(generateQueryRDF2Vec(depth, numWalksPerEntity));
		}

		logger.info("Queries for walks\n\n");
		for (String q : queries) {
			logger.info(String.format("\n\n%s\n\n", QueryFactory.create(q).toString(Syntax.syntaxSPARQL_11)));
			logger.info("\n\n");
		}

		FileOutputStream fos = new FileOutputStream(new File(fileOut));
		Set<String> entities = selectEntities(d, entitySelectorQuery);

		logger.info(String.format("Number of entities %s", entities.size()));
		ProgressCounter pc = new ProgressCounter(entities.size());
		for (String e : entities) {
			if (!createAVirtualDocumentForEachEntity) {
				for (String query : queries) {
					ParameterizedSparqlString pss = new ParameterizedSparqlString(query);
					pss.setIri("entity", e);
					// FORMAT <walk1_entity1>\n<walk2_entity2>\n...\n<walk_entityN>
					executeQuery(d, pss.toString(), e, separator, prefixMap, excludeLiteralsFromPath).forEach(s -> {
						try {
							fos.write(s.getBytes());
							fos.write('\n');
							fos.flush();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					});
				}
			} else {
				// FORMAT URI entity\t<walk1> <walk2>...<walkN>
				fos.write(e.getBytes());
				fos.write(' ');
				fos.write('#');
				fos.write('#');
				fos.write('\t');

				for (String query : queries) {
					ParameterizedSparqlString pss = new ParameterizedSparqlString(query);
					pss.setIri("entity", e);

					executeQuery(d, pss.toString(), e, separator, prefixMap, excludeLiteralsFromPath).forEach(s -> {
						try {
							fos.write(s.getBytes());
							fos.write(' ');
							fos.flush();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					});
					fos.write(' ');
				}
				fos.write('\n');
			}
			pc.increase();
		}
		fos.close();
	}

	private static Set<String> selectEntities(Dataset d, String baseQuery) {
		Set<String> result = new HashSet<>();
		d.begin(ReadWrite.READ);
		QueryExecution qe = QueryExecutionFactory.create(baseQuery, d);
		ResultSet rs = qe.execSelect();
		while (rs.hasNext()) {
			QuerySolution querySolution = (QuerySolution) rs.next();
			if (querySolution.get("entity").isURIResource()) {
				result.add(querySolution.get("entity").asResource().getURI());
			}
		}

		qe.close();
		d.end();
		return result;
	}

	private static List<String> executeQuery(Dataset d, String queryStr, String entity, String separator,
			Map<String, String> prefixMap, boolean excludeLiteralsFromPath) {
		List<String> walkList = new ArrayList<>();
		Query query = QueryFactory.create(queryStr);

		d.begin(ReadWrite.READ);
		QueryExecution qe = QueryExecutionFactory.create(query, d);
		ResultSet results = qe.execSelect();

		while (results.hasNext()) {
			QuerySolution result = results.next();
			String singleWalk = shortenEntity(entity, prefixMap) + separator;
			// construct the walk from each node or property on the path
			for (String var : results.getResultVars()) {
				try {
					// clean it if it is a literal
					if (result.get(var) != null && result.get(var).isLiteral() && !excludeLiteralsFromPath) {
						String val = result.getLiteral(var).getValue().toString();
						val = val.replace("\n", " ").replace("\t", " ").replace(separator, " ");
						StringTokenizer st = new StringTokenizer(val, STRING_TOKENIZER_DELIMITERS);
						while (st.hasMoreElements()) {
							singleWalk += st.nextToken().toLowerCase() + separator;
						}
					} else if (result.get(var) != null) {
						if (!result.get(var).isURIResource() && excludeLiteralsFromPath) {
							continue;
						}
						singleWalk += shortenEntity(result.get(var).toString().replace(separator, ""), prefixMap)
								+ separator;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			walkList.add(singleWalk);
		}
		qe.close();
		d.end();

		return walkList;
	}

	private static String shortenEntity(String uri, Map<String, String> prefixMap) {
		for (Map.Entry<String, String> pair : prefixMap.entrySet()) {
			if (uri.startsWith(pair.getValue())) {
				return uri.replace(pair.getValue(), pair.getKey() + ":");
			}
		}
		return uri;
	}

	private static String generateQueryRDF2Vec(int depth, int numberWalks) {
		String selectPart = "SELECT ?p ?o1";
		String mainPart = "{ ?entity ?p ?o1  ";
		String query = "";

		for (int i = 1; i < depth; i++) {
			mainPart += ". OPTIONAL {?o" + i + " ?p" + i + "?o" + (i + 1);
			selectPart += " ?p" + i + "?o" + (i + 1);
		}

		for (int i = 1; i < depth; i++) {
			mainPart += "}";
		}

		query = selectPart + " WHERE " + mainPart + "} LIMIT " + numberWalks;
		return query;
	}

	private static String generateQueryRDF2VecIngoing(int depth, int numberWalks) {
		String selectPart = "SELECT ?p ?o1";
		String mainPart = "{ ?o1 ?p ?entity  ";
		String query = "";

		for (int i = 1; i < depth; i++) {
			mainPart += ". OPTIONAL {?o" + (i + 1) + " ?p" + i + "?o" + (i);
			selectPart += " ?p" + i + "?o" + (i + 1);
		}

		for (int i = 1; i < depth; i++) {
			mainPart += "}";
		}

		query = selectPart + " WHERE " + mainPart + "} LIMIT " + numberWalks;
		return query;
	}

	private static String generateQueryNoPredicates(int depth, int numberWalks) {

		String selectPart = "SELECT ?o1";
		String mainPart = "{ ?entity ?p ?o1  ";
		String query = "";
		for (int i = 1; i < depth; i++) {
			mainPart += "  OPTIONAL {?o" + i + " ?p" + i + "?o" + (i + 1);
			selectPart += " ?o" + (i + 1);
		}

		for (int i = 1; i < depth; i++) {
			mainPart += "}";
		}

		query = selectPart + " WHERE " + mainPart + "} LIMIT " + numberWalks;
		return query;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getNumWalksPerEntity() {
		return numWalksPerEntity;
	}

	public void setNumWalksPerEntity(int numWalksPerEntity) {
		this.numWalksPerEntity = numWalksPerEntity;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public String getEntitySelectorQuery() {
		return entitySelectorQuery;
	}

	public void setEntitySelectorQuery(String entitySelectorQuery) {
		this.entitySelectorQuery = entitySelectorQuery;
	}

	public void setQueryWalks(QueryWalks... queryWalks) {
		this.queryWalks = new ArrayList<>();
		for (QueryWalks q : queryWalks) {
			this.queryWalks.add(q);
		}
	}

	public Map<String, String> getPrefixMap() {
		return prefixMap;
	}

	public void setPrefixMap(Map<String, String> prefixMap) {
		this.prefixMap = prefixMap;
	}

	public boolean isExcludeLiteralsFromPath() {
		return excludeLiteralsFromPath;
	}

	public void setExcludeLiteralsFromPath(boolean excludeLiteralsFromPath) {
		this.excludeLiteralsFromPath = excludeLiteralsFromPath;
	}

	public boolean isCreateAVirtualDocumentForEachEntity() {
		return createAVirtualDocumentForEachEntity;
	}

	public void setCreateAVirtualDocumentForEachEntity(boolean createAVirtualDocumentForEachEntity) {
		this.createAVirtualDocumentForEachEntity = createAVirtualDocumentForEachEntity;
	}
	
	public static void writeEntityListFromQuery(Dataset dataset, String query, String varName, String fileOut)
			throws IOException {

		FileOutputStream fos = new FileOutputStream(new File(fileOut));

		dataset.begin(ReadWrite.READ);

		QueryExecution qexec = QueryExecutionFactory.create(QueryFactory.create(query), dataset);
		ResultSet rs = qexec.execSelect();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			fos.write((qs.get(varName).asResource().getURI() + "\n").getBytes());
		}

		fos.close();

		dataset.commit();
		dataset.end();
	}



}
