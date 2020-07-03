
# Exemplar Query Search

Welcome to the Particle Filtering Test Environment.
In this repository it is possible to:

  - Compile the particle filtering procedure
  - Use the weighting graph procedure
  - Use both of the weighted or unweighted forms of particle filtering

This project is presented in the following paper:

> Gallo, Denis; Lissandrini, Matteo; and Velegrakis, Yannis.
> "Personalized Page Rank on Knowledge Graphs: Particle Filtering is all you need!."
>
> Proceedings of the 23th International Conference on Extending Database Technology, EDBT 2020 (169-180). 
>
> http://dx.doi.org/10.5441/002/edbt.2020.54


## What's inside

You can find the java files of particle filtering and weighting graph, a pom file.

  - The particle filtering file contains the main algorithm.
  - The particle filtering unlabelled file contains the algorithm for not weighted graphs.
  - The weighting graph file contains the algorithm to weight a graph based on edge label frequency
  - The batched version of weighting graph allows the weighting of graphs with more than 1M edges
  - The pom file is needed to compile the java files into a jar.

## Guide

First of all a "maven install" is needed on the pom file to create the jar file. The jar file can be found then in `/target/testprocedure.jar`. This jar file needs to be put into the /plugins folder of your neo4j installation. When the jar file is inside the `/plugins` folder, restart your neo4j server. After restarting the server, all procedures are available. It could be that you need to allow external procedures in your configuration file of Neo4j, since these type of procedures are marked as "insecure".

The procedure can be called by:

```
CALL particlefiltering([nodeList], min_threshold, num_particles);
CALL particlefiltering.unlabelled([nodeList], min_threshold, num_particles);
CALL graphweight.weight();
CALL apoc.periodic.commit(“CALL graphweight.weight.batched({limit}) YIELD out RETURN count(*)”, {limit:1000000});
```


In order to use the standard version of particlefiltering (the first one) you need to launch the graphweight procedure first. This needs to be done only once, since this procedure writes weights as relationship property into the graph.
If your graph has more than 1M edges, the batched version of graphweight is highly recommended. You need to have the official apoc procedure already installed in neo4j.
The input parameters of particlefiltering are:

- [nodeList] which needs to be a list of nodes. Not node IDs, not nodes, but a list of nodes. To transform nodes to a list, the easiest way is something like MATCH (n) RETURN collect(n). 
- min_threshold is the minimum score that a node needs to reach to stay within the results. With min_threshold=0, all the nodes that receive a score will also be printed as output .
- num_particles is the initial amount of particles that are put into the input nodes. The simples way to choose a number of particles is using some order of 10: 100, 1000 for example.

The outputs of particlefiltering (standard and unlabelled) are rows of <nodeID, score>


# Example usage

Let's assume we have a graph of 10k nodes and 100k edges and we want to use the particle filtering algorithm with 2 random input nodes with a degree of >20, no min_threshold and 1000 initial particles. 

First of all the graph needs to be weighted if this has not be done yet. 

CALL graphweight.weight();

Then, you can run the particlefiltering algorithm:

```
MATCH (n)-[r]-() WITH n, count(r) as cnt 
   WHERE cnt>20 WITH n as ids, rand() as r 
   ORDER BY r 
   LIMIT 2 WITH collect(ids) as idList
CALL particlefiltering(idList, 0, 1000) YIELD nodeId, score 
   RETURN nodeId, score ORDER BY score DESC
```

In the first part 2 random nodes with degree>20 are collected and transformed into a list, in the second part the particlefiltering algorithm is executed with the list, a threshold and initial particles as inputs. Results are printed as rows of <nodeId, score> ordered by the score.

