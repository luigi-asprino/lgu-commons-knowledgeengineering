#######
# Nota far partire con Anaconda (da terminale con python3)
# 
#####

from gensim.test.utils import datapath
from gensim import utils
from gensim.utils import tokenize
import gensim.models
import logging
import numpy as np
import sys
import argparse
import faiss  # make faiss available
from sklearn.preprocessing import normalize

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)


class Corpus(object):
    
    """An interator that yields sentences (lists of str)."""
    
    # default constructor 
    def __init__(self, corpus_path): 
        self.corpus_path = corpus_path

    def __iter__(self):
        corpus_datapath = datapath(self.corpus_path)
        for line in open(corpus_datapath):
            # assume there's one document per line, tokens separated by whitespace
            yield line.split(" ")


if __name__ == '__main__':
    
    parser = argparse.ArgumentParser(description='Compute the pairwise similarity of a list of entities using RDF2Vec.')
    
    parser.add_argument('walks', metavar='W',  help='The file containing the RDF walks.')
    parser.add_argument('entities', metavar='E',  help='The file containing the list of entities.')
    parser.add_argument('out', metavar='O',  help='The output file.')
    parser.add_argument('dimensionality', metavar='d',  type=int, help='The dimensionality of the embeddings.')
    parser.add_argument('threshold', metavar='t',  type=int, help='The number of similar entities to take for each entity.')
    parser.add_argument('--load', default=False, action="store_true" , help='Use a pre-computed model.')
    
    args = parser.parse_args()
    
    #filepath = "/Users/lgu/Desktop/pss_walks"
    filepath = args.walks
    filepath_model = filepath + "_model"
    
    #filepath_entitylist = "/Users/lgu/Desktop/pss_entityList"
    filepath_entitylist = args.entities
    
    #filepath_out = "/Users/lgu/Desktop/similarities"
    filepath_out = args.out
    
    #dimensionality = 200
    dimensionality =  args.dimensionality
    
    threshold = args.threshold
    
    load = args.load
    
    #print(filepath+" "+filepath_model+" "+filepath_entitylist+" "+filepath_out+" "+str(dimensionality)+" "+str(load))
    
    if(not load):
        logging.info("Compute Model")
        
        # load corpus
        sentences = Corpus(filepath)
    
        # Train model 
        model = gensim.models.Word2Vec(sentences=sentences, size=dimensionality, workers=5, window=10, sg=1, negative=15, iter=5)
    
        # Save model
        model.save(filepath_model)
    else:

        # loading model
        logging.info("Loading Model")
        model = gensim.models.Word2Vec.load(filepath_model)
    
    fp_el1 = open(filepath_entitylist, 'r')
    
    entityList = fp_el1.read().splitlines()
    
    m = np.zeros((len(entityList), dimensionality), dtype="float32")
        
    for i in range(0, len(entityList)):
        # print(model[entityList[i]])
        m[i] = np.array(model[entityList[i]], dtype=float)
    
    m = normalize(m, copy=False)
    
    index = faiss.IndexFlatIP(dimensionality)  # build the index cosine distance
    index.add(m)
    
    #lims, D, I = index.range_search(m, threshold)
    
    D, I = index.search(m, threshold)  # actual search 
    
    fp_out = open(filepath_out, "w") 
    
    step = 1
    logging.info(len(m))
    logging.info(len(I))
    
    for i in range(0, len(m)):
        for ii in range(0,len(I[i])):
            if(ii!=i):
                #print(f"{entityList[i]}\t{entityList[I[i][ii]]}\t{D[i][ii]}\n")
                fp_out.write(f"{entityList[i]}\t{entityList[I[i][ii]]}\t{D[i][ii]}\n")
            
    #for i in range(0, len(m)):
    #    for ii in range(0,len(I[lims[i]:lims[i + 1]])) :
    #        if(I[lims[i]:lims[i + 1]][ii] != i):
    #            fp_out.write(f"{entityList[i]}\t{entityList[I[lims[i]:lims[i + 1]][ii]]}\t{D[lims[i]:lims[i + 1]][ii]}\n")
                #print (f"{entityList[i]}\t{entityList[I[lims[i]:lims[i + 1]][ii]]}\t{D[lims[i]:lims[i + 1]][ii]}")
        
    fp_out.close()
        
