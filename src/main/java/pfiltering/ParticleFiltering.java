package pfiltering;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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
import org.neo4j.graphdb.Direction;

//import exemplar.ParticleFiltering.Output;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;


public class ParticleFiltering {
	

	
	//the db
    @Context
    public GraphDatabaseService db;
    
    
    //useful for logging
    @Context
    public Log log;

	/**
	 *
	 *
	 * Gallo, Denis; Lissandrini, Matteo; and Velegrakis, Yannis.
	 * “Personalized Page Rank on Knowledge Graphs: Particle Filtering is all you need!.”
	 * Proceedings of the 23th International Conference on Extending Database Technology, EDBT 2020 (169-180).
	 * http://dx.doi.org/10.5441/002/edbt.2020.54
	 *
	 * @param nodeList the list of the starting nodes for the algorithm
	 * @param minThreshold the minimum threshold a node needs to each to be considered in the result list
	 * @param num_particles the number of particles to inject to the starting nodes
	 * @return a list of nodes with an attached score based on the particle filtering algorithm
	 */

    //procedure declaration
    @Procedure(value = "particlefiltering", mode=Mode.READ)
    @Description("Returns nodes and an attached score given a nodeList, a score threshold, a #particles variable")
    public Stream<Output> search( @Name("nodeList") List<Node> nodeList, @Name("threshold") Double minThreshold, @Name("particles") Double num_particles){
    	

        
        //particle filtering

    	Map<String,Double> p= new HashMap<>();
    	Map<String,Double> v= new HashMap<>();
    	Map<String, Double> aux;
    	double passing;
    	double c=0.15;
    	double tao=1.0/num_particles;
    	double min_threshold=minThreshold;
    	PriorityQueue<Neighbour> neighbours;
    	Map<String, Double> pprNodes= new HashMap<>(); //resulting list containing nodes and scores
		Transaction t=db.beginTx();
    	
    	double currentweight;
    	for(Node n:nodeList) {
    		p.put(n.getElementId(), ((1.0/nodeList.size())*(num_particles)));
    		v.put(n.getElementId(), ((1.0/nodeList.size())*(num_particles)));
    	}
    	while(!p.isEmpty()) {
    		aux= new HashMap<>();
    		for(String node:p.keySet()) {
    			double particles=p.get(node)*(1-c);
    			Node startingNode=t.getNodeByElementId(node);
    			//Node startingNode=db.getNodeById(node);
    			double totalweight=0;
    			neighbours=new PriorityQueue<>();
    			for(Relationship relationship:startingNode.getRelationships(Direction.BOTH)){
    				currentweight=(double)relationship.getProperty("exemplarWeight");
    				neighbours.add(new Neighbour(relationship.getOtherNode(startingNode).getElementId(), currentweight));
    				totalweight+=currentweight;
    			}
    			while(!neighbours.isEmpty()) {
    				Neighbour n=neighbours.remove();
    				if (particles<=tao)
    					break;
    				passing=particles*(n.getValue()/totalweight);
    				if (passing<=tao)
    					passing=tao;
    				particles=particles-passing;
    				if(aux.containsKey(n.getKey())){
    					passing+=aux.get(n.getKey());
    				}
    				aux.put(n.getKey(), passing);
    			}
    		}
    		p=aux;
    		for(String node:p.keySet()) {
    			if(v.containsKey(node)) {
    				double value=v.get(node)+p.get(node);
    				v.put(node, value);
    			}
    			else
    				v.put(node, p.get(node));
    		}
    	}
    	//return v
    	
    	//filtering v with min_threshold
    	for (Entry<String, Double> e:v.entrySet()) {
    		if(e.getValue()>=min_threshold)
    			pprNodes.put(e.getKey(), e.getValue());
    	}

		t.commit();
    	t.close();

    	//returning <ids, scores>
    	return pprNodes.entrySet().stream().map(es->new Output(es.getKey(), es.getValue()));


    }
    
    
    

        
    public class Neighbour implements Comparable<Neighbour> {
        private String key;
        private double value;

        public Neighbour(String key, double value) {
            this.key = key;
            this.value = value;
        }

        public double getValue() {
        	return this.value;
        }
        
        public String getKey() {
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
    	public String nodeId;
    	public double score;
    	
    	public Output(String nodeId, double score) {
    		this.nodeId=nodeId;
    		this.score=score;
    	}
    }

}
