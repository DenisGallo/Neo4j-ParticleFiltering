package pfiltering;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class WeightingGraphProcBatched {


	//the db
    @Context
    public GraphDatabaseService db;
    
    //useful for logging
    @Context
    public Log log;
    
    @Procedure(value = "graphweight.weight.batched", mode=Mode.WRITE)
    @Description("Procedure that adds weights to the current graph based on relationship label frequency")
    public Stream<Output> weightGraph( @Name("double") double batches) {
    	
    	long totalRelationshipCount=0;
    	Result rs;
   	 	String type;
   	 	HashMap<String, Double> relSet=new HashMap<String, Double>();
   	 	double value;
   	 	boolean modified=false;
   	 	
   	 	//getting total relationship count
   	 	
   	 	rs=db.execute("MATCH (n)-[r]->() RETURN count(r) as cnt");
   	 	totalRelationshipCount=(long) rs.next().get("cnt");
   	 	
   	 	//initialize set
   	 	
   	 	for(RelationshipType rt:db.getAllRelationshipTypes()) {
   	 		relSet.put(rt.toString(), 0.0);
   	 	}
   	 	
   	 	//getting relationship count for each type
   	 	
   	 	for(Relationship r:db.getAllRelationships()) {
   	 		type=r.getType().toString();
   	 		value=relSet.get(type)+1;
   	 		relSet.put(type, value);
   	 	}
   	 	
   	 	//calculating
   	 	
   	 	for(String s:relSet.keySet()) {
   	 		value=(-1*Math.log10(1.0*relSet.get(s)/totalRelationshipCount));
   	 		relSet.put(s, value);
   	 	}
   	 	
   	 	//update
   	 	for(Relationship r:db.getAllRelationships()) {
   	 		if(r.getProperty("exemplarWeight", null)==null){
   	 			type=r.getType().toString();
   	 			value=relSet.get(type);
   	 			r.setProperty("exemplarWeight", value);	
   	 			modified=true;
   	 			batches--;
   	 			if(batches==0)
   	 				break;
   	 		}
   	 	}
   	 	if(modified) {
   	   	 	List<Node> returnList=new ArrayList<Node>();
   	 		Node n = null;
   	 		returnList.add(n);
   	 		return returnList.stream().map(t->new Output(t));
   	 	}
   	 	else
   	 		return null;

    	
    }
    
    
    //support class to return a stream of T, where T is a Path 
    public class Output{
    	public Node out;
    		public Output(Node out) {
    			this.out=out;
    		}
    }
}
