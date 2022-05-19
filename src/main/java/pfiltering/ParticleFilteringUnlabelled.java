package pfiltering;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

//import exemplar.ParticleFilteringUnlabelled.Output;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;


public class ParticleFilteringUnlabelled {
	

	
	//the db
    @Context
    public GraphDatabaseService db;
    
    
    //useful for logging
    @Context
    public Log log;

	/**
	 *
	 * Gallo, Denis; Lissandrini, Matteo; and Velegrakis, Yannis.
	 * “Personalized Page Rank on Knowledge Graphs: Particle Filtering is all you need!.”
	 * Proceedings of the 23th International Conference on Extending Database Technology, EDBT 2020 (169-180).
	 * http://dx.doi.org/10.5441/002/edbt.2020.54
	 *
	 * @param nodeList the list of the starting nodes for the algorithm
	 * @param nodeWeights a list of the weights assigned to each starting node. If set to null then all nodes have the same weight
	 * @param minThreshold the minimum threshold a node needs to each to be considered in the result list
	 * @param num_particles the number of particles to inject to the starting nodes
	 * @return a list of nodes with an attached score based on the particle filtering algorithm
	 */
    
    //procedure declaration
    @Procedure(value = "particlefiltering.unlabelled", mode=Mode.READ)
    @Description("Returns nodes and an attached score given a nodeList, a score threshold, a #particles variable")
    public Stream<Output> search( @Name("nodeList") List<Node> nodeList, @Name("nodeWeights") List<Double> nodeWeights, @Name("threshold") Double minThreshold, @Name("particles") Double num_particles){
    	

        
        //particle filtering

    	Map<Long,Double> p= new HashMap<>();
    	Map<Long,Double> v= new HashMap<>();
    	Map<Long, Double> aux;
    	double passing;
    	double c=0.15;
    	double tao=1.0/num_particles;
    	double min_threshold=minThreshold;
    	List<Long> neighbours;
    	Map<Long, Double> pprNodes= new HashMap<>(); //resulting list containing nodes and scores
		Transaction t=db.beginTx();
    	
		// If the nodeWeights list is not specified then populate it and set it to 1.0s
    	if (nodeWeights == null) {
			nodeWeights = new ArrayList<Double>(Collections.nCopies(nodeList.size(), 1.0));
        }

        for(Integer i=0; i<nodeList.size(); i++) {
			Node n = nodeList.get(i);
			p.put(n.getId(), ((1.0/nodeList.size())*(num_particles*nodeWeights.get(i)))); 
			v.put(n.getId(), ((1.0/nodeList.size())*(num_particles*nodeWeights.get(i)))); 
		}

    	while(!p.isEmpty()) {
    		aux= new HashMap<>();
    		for(Long node:p.keySet()) {
    			double particles=p.get(node)*(1-c);
    			Node startingNode=t.getNodeById(node);
    			//Node startingNode=db.getNodeById(node);
    			neighbours= new ArrayList<>();
    			int neighboursCount=0;
    			for(Relationship relationship:startingNode.getRelationships()){
    				neighbours.add(relationship.getOtherNode(startingNode).getId());
    				neighboursCount++;
    			}
    			Collections.shuffle(neighbours);
    			for(Long n:neighbours) {
    				if (particles<=tao)
    					break;
    				passing=particles/(neighboursCount);
    				if (passing<=tao)
    					passing=tao;
    				particles=particles-passing;
    				if(aux.containsKey(n)){
    					passing+=aux.get(n);
    				}
    				aux.put(n, passing);
    			}
    		}
    		p=aux;
    		for(Long node:p.keySet()) {
    			if(v.containsKey(node)) {
    				double value=v.get(node)+p.get(node);
    				v.put(node, value);
    			}
    			else
    				v.put(node, p.get(node));
    		}
    	}
    	
    	//filtering v with min_threshold
    	for (Entry<Long, Double> e:v.entrySet()) {
    		if(e.getValue()>=min_threshold)
    			pprNodes.put(e.getKey(), e.getValue());
    	}

		t.commit();
		t.close();
    	
    	//returning <ids, scores>
    	return pprNodes.entrySet().stream().map(es->new Output(es.getKey(), es.getValue()));


    }
    
    
    

        
    public class Neighbour implements Comparable<Neighbour> {
        private Long key;
        private double value;

        public Neighbour(Long key, double value) {
            this.key = key;
            this.value = value;
        }

        public double getValue() {
        	return this.value;
        }
        
        public Long getKey() {
        	return this.key;
        }

        @Override
        public int compareTo(Neighbour other) {
            if( this.value>other.getValue())
            	return -1;
            if(this.value<other.getValue())
            	return 1;
            return 0;
        }
    }
    
    protected class Output{
    	public long nodeId;
    	public double score;
    	
    	public Output(long nodeId, double score) {
    		this.nodeId=nodeId;
    		this.score=score;
    	}
    }

}
