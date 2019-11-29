package pfiltering;


import java.util.HashMap;



import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

public class WeightingGraphProc {


	//the db
    @Context
    public GraphDatabaseService db;
    
    //useful for logging
    @Context
    public Log log;
    
    @Procedure(value = "graphweight.weight", mode=Mode.WRITE)
    @Description("Procedure that adds weights to the current graph based on relationship label frequency")
    public void weightGraph() {
    	
    	long totalRelationshipCount=0;
    	Result rs;
   	 	String type;
   	 	HashMap<String, Double> relSet=new HashMap<String, Double>();
   	 	double value;
   	 	
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
   	 		type=r.getType().toString();
   	 		value=relSet.get(type);
   	 		r.setProperty("exemplarWeight", value);
   	 	}
    	
    }
    
    
    //support class to return a stream of T, where T is a Path 
    public class Output{
    	public Path out;
    		public Output(Path out) {
    			this.out=out;
    		}
    }
}
