package exemplar;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import exemplar.ParticleFilteringUnlabelled.Output;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;


public class ParticleFilteringUnlabelled {
	

	
	//the db
    @Context
    public GraphDatabaseService db;
    
    
    //useful for logging
    @Context
    public Log log;
    
    
    //procedure declaration
    @Procedure(value = "particlefiltering", mode=Mode.READ)
    @Description("Returns nodes and an attached score given a nodeList, a score threshold, a #particles variable")
    public Stream<Output> search( @Name("nodeList") List<Node> nodeList, @Name("threshold") Double minThreshold, @Name("particles") Double num_particles){
    	

        
        //particle filtering

    	Map<Long,Double> p=new HashMap<Long,Double>();
    	Map<Long,Double> v=new HashMap<Long,Double>();
    	Map<Long, Double> aux;
    	double passing;
    	double c=0.15;
    	double tao=1.0/num_particles;
    	double min_threshold=minThreshold;
    	List<Long> neighbours=new ArrayList<Long>();
    	//PriorityQueue<Neighbour> neighbours = new PriorityQueue<>();
    	Map<Long, Double> pprNodes=new HashMap<Long, Double>(); //resulting list containing nodes and scores
    	
    	double currentweight;
    	for(Node n:nodeList) {
    		p.put(n.getId(), ((1.0/nodeList.size())*(num_particles))); 
    		v.put(n.getId(), ((1.0/nodeList.size())*(num_particles))); 
    	}
    	while(!p.isEmpty()) {
    		aux=new HashMap<Long, Double>();
    		for(Long node:p.keySet()) {
    			double particles=p.get(node)*(1-c);
    			Node startingNode=db.getNodeById(node);
    			//double totalweight=0;
    			//neighbours=new PriorityQueue<>();
    			neighbours=new ArrayList<Long>();
    			int neighboursCount=0;
    			for(Relationship relationship:startingNode.getRelationships()){
    				//currentweight=(double)relationship.getProperty("exemplarWeight");
    				neighbours.add(relationship.getOtherNode(startingNode).getId());
    				//totalweight+=currentweight;
    				neighboursCount++;
    			}
    			Collections.shuffle(neighbours);
    			for(Long n:neighbours) {
    			//while(!neighbours.isEmpty()) {
    				//Neighbour n=neighbours.remove();
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
    	//return v
    	
    	//filtering v with min_threshold
    	for (Entry<Long, Double> e:v.entrySet()) {
    		if(e.getValue()>=min_threshold)
    			pprNodes.put(e.getKey(), e.getValue());
    	}
    	
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
