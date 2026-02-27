# Knowledge Graphs for AI Agents - Research Report

**Project:** Steve AI (Minecraft Autonomous Agents)
**Date:** 2026-02-27
**Research Focus:** Graph-based knowledge representation for AI agents in Minecraft

---

## Executive Summary

This report researches knowledge graph technologies for implementing persistent, queryable world knowledge in the Steve AI project. Knowledge graphs enable AI agents to store facts as entity-relationship triples, supporting complex queries, multi-hop reasoning, and dynamic learning.

**Key Findings:**
- **GraphRAG** (2025-2026) combines knowledge graphs with vector search for enhanced LLM reasoning
- **Neo4j** and **Apache Jena** are leading graph databases with Java support
- **JGraphT** provides in-memory graph algorithms for real-time processing
- Minecraft's block/entity/crafting systems naturally map to graph structures
- Dynamic knowledge integration allows agents to learn from experience

---

## Table of Contents

1. [Knowledge Representation](#1-knowledge-representation)
2. [Entity Relationships](#2-entity-relationships)
3. [Graph Query Languages](#3-graph-query-languages)
4. [Knowledge Learning](#4-knowledge-learning)
5. [Minecraft Knowledge Domain](#5-minecraft-knowledge-domain)
6. [Graph Database Options](#6-graph-database-options)
7. [Implementation Approaches](#7-implementation-approaches)
8. [Recommendations](#8-recommendations)

---

## 1. Knowledge Representation

### 1.1 Triple-Based Model (RDF)

Knowledge graphs use **subject-predicate-object** triples as the fundamental unit:

```
(Steve, has_location, minecraft:overworld)
(oak_log, is_material_for, planks)
(creeper, drops, gunpowder)
(iron_ore, smelts_to, iron_ingot)
```

**Advantages:**
- Simple, uniform representation
- Direct mapping to natural language facts
- Efficient for relationship queries
- Schema-flexible (easy to add new fact types)

**Standards:**
- **RDF** (Resource Description Framework) - W3C standard
- **SPARQL** - Query language for RDF graphs
- **OWL** - Web Ontology Language for richer expressiveness

### 1.2 Property Graph Model (Neo4j)

Alternative model using **nodes** and **labeled relationships**:

```cypher
(Steve:Agent {name: "Steve1"})-[:LOCATED_AT]->(biome:Biome {name: "plains"})
(oak_log:Block {type: "oak_log"})-[:CRAFTS_INTO]->(planks:Item {count: 4})
```

**Advantages:**
- More intuitive for developers
- Faster traversal with native graph storage
- Rich property support on nodes/edges
- Cypher query language is SQL-like

### 1.3 Semantic Web Technologies

| Technology | Purpose | Use Case |
|------------|---------|----------|
| **RDF** | Triple-based data model | Foundation for knowledge graphs |
| **RDFS** | RDF Schema - basic ontology | Class hierarchies, property domains |
| **OWL** | Web Ontology Language | Complex constraints, reasoning |
| **SPARQL** | Query language | Retrieving triples with patterns |

**OWL Variants:**
- **OWL Lite** - Simple classification hierarchies
- **OWL DL** - Description Logic, complete reasoning
- **OWL Full** - Maximum expressiveness (undecidable)

---

## 2. Entity Relationships

### 2.1 Relationship Types

For Minecraft AI agents, key relationship categories:

**Spatial Relationships:**
- `LOCATED_AT` - Agent/Entity position
- `NEAR` - Proximity (within N blocks)
- `CONTAINS` - Inventory contents
- `PATH_TO` - Navigable path between locations

**Material Relationships:**
- `CRAFTS_INTO` - Recipe output
- `REQUIRES` - Recipe ingredients
- `SMELTS_TO` - Furnace transformations
- `TOOLS_REQUIRED` - Tool dependencies

**Entity Relationships:**
- `DROPS` - Mob loot tables
- `SPAWNS_IN` - Biome/conditions
- `ATTACKS` - Combat behavior
- `TRADES` - Villager offerings

**Temporal Relationships:**
- `LEARNED_AT` - Knowledge acquisition timestamp
- `OBSERVED` - Agent witnessed event
- `EXPIRES` - Time-limited facts

### 2.2 Graph Schema Example

```cypher
// Agent node
(:Agent {
  id: "steve_1",
  name: "Steve",
  level: 5,
  health: 20
})

// Block node
(:Block {
  id: "oak_log",
  type: "oak_log",
  hardness: 2.0,
  harvest_tool: "axe"
})

// Relationship
(steve:Agent)-[:KNOWS]->(oak_log:Block)
(steve:Agent)-[:HAS_INVENTORY {count: 64}]->(oak_log:Block)
(oak_log:Block)-[:CRAFTS_INTO {output: 4}]->(oak_planks:Item)
```

### 2.3 Relationship Properties

Edges can carry properties for richer semantics:

```cypher
[:CRAFTS_INTO {
  output_count: 4,
  recipe_shape: "1x1",
  experience_required: 0
}]
```

---

## 3. Graph Query Languages

### 3.1 Cypher (Neo4j)

**Pattern Matching:**
```cypher
// Find all recipes requiring wood
MATCH (ingredient)-[:REQUIRES]->(wood:Block {type: "oak_log"})
RETURN ingredient
```

**Path Finding:**
```cypher
// Find shortest path from logs to planks
MATCH path = shortestPath(
  (start:Block {type: "oak_log"})-[:CRAFTS_INTO*]-(end:Item {type: "oak_planks"})
)
RETURN path
```

**Multi-hop Queries:**
```cypher
// What can I make with oak logs?
MATCH (oak:Block {type: "oak_log"})-[:CRAFTS_INTO*1..3]-(result)
RETURN DISTINCT result
```

**Aggregation:**
```cypher
// Count all known blocks by type
MATCH (b:Block)
RETURN b.type as block_type, count(*) as count
ORDER BY count DESC
```

### 3.2 SPARQL (RDF)

**Basic Pattern:**
```sparql
PREFIX mc: <http://minecraft.org/schema/>

SELECT ?item WHERE {
  mc:oak_log mc:crafts_into ?item .
}
```

**Optional Matches:**
```sparql
SELECT ?block ?tool ?hardness WHERE {
  ?block a mc:Block .
  OPTIONAL { ?block mc:harvest_tool ?tool }
  OPTIONAL { ?block mc:hardness ?hardness }
}
```

**Property Paths:**
```sparql
// Multi-hop crafting paths
SELECT ?output WHERE {
  mc:oak_log (mc:crafts_into|mc:smelts_to)+ ?output .
}
```

### 3.3 Gremlin (Apache TinkerPop)

**Traversal Example:**
```java
// Find all craftable items from oak logs
g.V().has("Block", "type", "oak_log")
 .repeat(out("crafts_into"))
 .emit()
 .path()
```

**Algorithm Integration:**
```java
// Shortest path between locations
g.V().has("Location", "name", "base")
 .shortestPath()
 .to(__.has("Location", "name", "village"))
```

---

## 4. Knowledge Learning

### 4.1 Dynamic Knowledge Integration

Modern research (2025-2026) emphasizes **incremental knowledge graph updates**:

**AGENTiGraph Approach:**
```
G ← G ∪ {E_new, R_new}
```
- G = existing graph
- E_new = newly discovered entities
- R_new = newly discovered relationships

**Graphiti Framework (getzep):**
- **Time-aware** knowledge graphs
- Incremental updates without full rebuild
- Hybrid retrieval combining temporal and structural queries

### 4.2 Knowledge Extraction from LLMs

**Entity-Relationship Extraction Pipeline:**

1. **Parse LLM Output** - Extract structured data
2. **Entity Resolution** - Match to existing entities
3. **Relationship Validation** - Verify fact consistency
4. **Graph Update** - Add new triples/edges
5. **Conflict Resolution** - Handle contradictions

**Example:**
```java
// LLM observes: "I found diamonds at Y=-54"
Triple observation = new Triple(
  subject: "diamond_ore",
  predicate: "found_at",
  object: "y=-54",
  timestamp: Instant.now()
);

knowledgeGraph.addObservation(observation);
```

### 4.3 Learning from Experience

**Observation Pattern:**
```java
// Agent mines block and learns property
public void onBlockMined(Block block, ItemStack drop) {
    Triple fact = Triple.of(
        block.getRegistryName(),
        "drops",
        drop.getItem().getRegistryName()
    );
    memoryGraph.add(fact, Confidence.OBSERVED);
}
```

**Confidence Scoring:**
- **DEDUCED** (0.5) - LLM inference
- **OBSERVED** (1.0) - Direct interaction
- **VERIFIED** (1.0) - Multiple confirmations

### 4.4 Forgetting and Decay

**Temporal Knowledge:**
```cypher
[:LEARNED_AT {
  timestamp: 1234567890,
  confidence: 0.9,
  access_count: 5
}]
```

**Decay Strategies:**
- Time-based expiration
- Access-frequency retention
- Relevance scoring
- Memory consolidation (important facts strengthened)

---

## 5. Minecraft Knowledge Domain

### 5.1 Minecraft Ontology

**Block Hierarchy:**
```
Block
├── Natural
│   ├── Stone (stone, andesite, diorite, granite)
│   ├── Wood (oak_log, birch_log, spruce_log)
│   ├── Ore (iron_ore, gold_ore, diamond_ore, ancient_debris)
│   └── Fluid (water, lava, powder_snow)
├── Manufactured
│   ├── Planks (oak_planks, spruce_planks)
│   ├── Stone Products (smooth_stone, stone_bricks)
│   └── Redstone (redstone_wire, repeater, piston)
└── Special
    ├── Spawner, Chest, Furnace, Crafting_Table
    └── Shulker_Box, Beacon, Conduit
```

**Entity Hierarchy:**
```
Entity
├── Passive (cow, pig, sheep, chicken)
├── Neutral (iron_golem, wolf, bee)
├── Hostile (zombie, skeleton, creeper, witch)
├── Boss (wither, ender_dragon, warder)
└── Utility (item, arrow, experience_orb)
```

**Item Hierarchy:**
```
Item
├── Tool (pickaxe, axe, shovel, hoe, shears)
├── Weapon (sword, trident, bow, crossbow)
├── Armor (helmet, chestplate, leggings, boots)
├── Material (stick, iron_ingot, gold_ingot, diamond)
├── Food (bread, steak, golden_carrot)
└── Special (elytra, totem, ender_pearl)
```

### 5.2 Crafting Knowledge Graph

**Recipe Representation:**
```cypher
(oak_log:Block)-[:REQUIRES {count: 1}]->(planks_recipe:Recipe)
(planks_recipe:Recipe)-[:OUTPUTS {count: 4}]->(oak_planks:Item)

(stick:Item)-[:REQUIRES {count: 2}]->(wooden_pickaxe_recipe:Recipe)
(oak_planks:Item)-[:REQUIRES {count: 3}]->(wooden_pickaxe_recipe:Recipe)
(wooden_pickaxe_recipe:Recipe)-[:OUTPUTS {count: 1}]->(wooden_pickaxe:Item)
```

**Recipe Query:**
```cypher
// What can I craft with oak logs?
MATCH (oak_log:Block {type: "oak_log"})-[:REQUIRES]->(recipe:Recipe)-[:OUTPUTS]->(output:Item)
RETURN DISTINCT output.type, recipe
```

### 5.3 Spatial Knowledge

**Chunk-Based Partitioning:**
```cypher
(chunk:Chunk {x: 0, z: 0})-[:CONTAINS]->(block:Block {type: "diamond_ore", y: -54})
(chunk:Chunk)-[:NEIGHBORS]->(adjacent_chunk:Chunk)
```

**Biome Knowledge:**
```cypher
(plains:Biome)-[:SPAWNS]->(cow:Mob {frequency: "common"})
(plains:Biome)-[:CONTAINS]->(village:Structure {rarity: "rare"})
```

### 5.4 Block Properties Schema

```json
{
  "block_type": "oak_log",
  "properties": {
    "hardness": 2.0,
    "resistance": 2.0,
    "harvest_tool": "axe",
    "min_harvest_level": 0,
    "flammable": true,
    "stack_size": 64,
    "transparent": false
  },
  "drops": [
    {"item": "oak_log", "min": 1, "max": 1},
    {"item": "stick", "min": 0, "max": 2, "fortune_affected": true}
  ],
  "crafting_recipes": ["oak_planks", "charcoal", "oak_boat"]
}
```

### 5.5 Entity Behavior Graph

```cypher
(creeper:Mob)-[:EXPLODES_WHEN]->(distance:Condition {value: "< 3 blocks"})
(creeper:Mob)-[:DROPS]->(gunpowder:Item {min: 0, max: 2})
(creeper:Mob)-[:IGNITED_BY]->(flint_and_steel:Item)
(creeper:Mob)-[:FEARED_BY]->(cat:Mob {radius: 6})
```

---

## 6. Graph Database Options

### 6.1 Comparison Matrix

| Database | Type | Persistence | Java Support | Query Language | Best For |
|----------|------|-------------|--------------|----------------|----------|
| **Neo4j** | Native graph | Disk/In-memory | Excellent (Java) | Cypher | Production, complex queries |
| **JGraphT** | In-memory library | No (in-memory) | Native Java | Java API | Algorithms, real-time processing |
| **Apache Jena** | RDF triple store | Disk/In-memory | Native Java | SPARQL | Semantic web, RDF standards |
| **JanusGraph** | Graph database | Disk (Cassandra/HBase) | Java | Gremlin | Distributed, large-scale |
| **TinkerPop** | Framework | Various | Java | Gremlin | Standard interface |
| **RDFox** | RDF triple store | In-memory | Java (via API) | SPARQL + Datalog | Reasoning, high performance |

### 6.2 Neo4j

**Advantages:**
- Native graph storage (index-free adjacency)
- Cypher query language (intuitive, SQL-like)
- ACID transactions
- 65+ built-in graph algorithms
- Excellent Java driver
- Embedded mode for local storage
- Scalable to billions of nodes/relationships

**Disadvantages:**
- Commercial license for production
- Disk I/O can be bottleneck for large graphs
- Schema-less (can lead to inconsistency)

**Java Integration:**
```java
import org.neo4j.driver.*;

Driver driver = GraphDatabase.driver("bolt://localhost:7687");
Session session = driver.session();

session.run("CREATE (a:Agent {name: $name})", Parameters.value("name", "Steve"));

Result result = session.run(
  "MATCH (a:Agent {name: $name})-[:KNOWS]->(block:Block) RETURN block.type",
  Parameters.value("name", "Steve")
);
```

**Embedded Mode (No Server):**
```java
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

DatabaseManagementService dbs = new DatabaseManagementServiceBuilder(
  FileSystems.default().getPath("data/neo4j")
).build();

GraphDatabaseService graphDb = dbs.database("steve-knowledge");
```

### 6.3 JGraphT

**Advantages:**
- Pure Java library (no external dependencies)
- 50+ graph algorithms built-in
- In-memory (fast, no I/O)
- LGPL/EPL license (permissive)
- Supports millions of vertices/edges
- Easy integration with existing Java code

**Disadvantages:**
- No persistence (must implement yourself)
- No standard query language
- Not a database (library only)

**Algorithms Available:**
- Shortest path (Dijkstra, A*, Bellman-Ford)
- Minimum spanning tree (Prim, Kruskal)
- Maximum flow
- Graph isomorphism
- Connectivity (strongly/weakly connected components)
- Centrality (betweenness, closeness, PageRank)
- Coloring
- Cycle detection

**Java Example:**
```java
import org.jgrapht.*;
import org.jgrapht.graph.*;

// Create directed graph
Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

// Add vertices
graph.addVertex("oak_log");
graph.addVertex("oak_planks");
graph.addVertex("stick");

// Add edges
graph.addEdge("oak_log", "oak_planks");
graph.addEdge("oak_planks", "stick");

// Shortest path
ShortestPathAlgorithm<String, DefaultEdge> sp =
  new DijkstraShortestPath<>(graph);
GraphPath<String, DefaultEdge> path =
  sp.getPath("oak_log", "stick");
```

### 6.4 Apache Jena

**Advantages:**
- W3C standards compliant (RDF, SPARQL)
- Pure Java implementation
- In-memory and persistent models
- OWL reasoning support
- Open source (Apache 2.0)

**Disadvantages:**
- RDF model (less intuitive than property graphs)
- Slower than Neo4j for traversals
- Reasoning can be expensive

**Java Example:**
```java
import org.apache.jena.rdf.model.*;

Model model = ModelFactory.createDefaultModel();

Resource oakLog = model.createResource("mc:block/oak_log");
Property craftsInto = model.createProperty("mc:crafts_into");
Resource oakPlanks = model.createResource("mc:item/oak_planks");

oakLog.addProperty(craftsInto, oakPlanks);

// SPARQL query
String queryString =
  "PREFIX mc: <mc:> " +
  "SELECT ?planks WHERE { ?log mc:crafts_into ?planks }";
Query query = QueryFactory.create(queryString);
QueryExecution qe = QueryExecutionFactory.create(query, model);
ResultSet results = qe.execSelect();
```

### 6.5 Triple Stores

**RDFox:**
- Commercial but highly scalable
- In-memory RDF triple store
- Parallel Datalog reasoning
- Excellent performance for complex queries

**gStore:**
- Graph-based RDF store
- Native graph structure
- Open source

**Apache Jena TDB:**
- Disk-based triple store
- Pure Java
- SPARQL compliant

### 6.6 In-Memory vs Persistent

| Factor | In-Memory | Persistent |
|--------|-----------|------------|
| **Speed** | Fast (no I/O) | Slower (disk access) |
| **Capacity** | Limited by RAM | Limited by disk |
| **Persistence** | Lost on shutdown | Survives shutdown |
| **Use Case** | Session memory, fast queries | Long-term knowledge |
| **Examples** | JGraphT, Jena in-memory | Neo4j, Jena TDB |

**Hybrid Approach:**
- Hot knowledge in memory (recent observations)
- Cold knowledge on disk (world facts, recipes)
- Cache frequently accessed triples

---

## 7. Implementation Approaches

### 7.1 Architecture Options

**Option A: Embedded Neo4j (Recommended)**
```
Steve AI
├── KnowledgeGraph (Neo4j embedded)
│   ├── Agent Memory (subgraph per agent)
│   ├── World Knowledge (shared facts)
│   └── Recipe Database (static + learned)
├── LLM Integration
│   ├── Entity extraction
│   └── Query generation
└── Action Executor
    └── Graph-based planning
```

**Advantages:**
- No separate database server
- Fast local access
- ACID transactions
- Rich Cypher queries

**Disadvantages:**
- Storage on disk (I/O overhead)
- Neo4j licensing considerations

**Option B: JGraphT + Custom Persistence**
```
Steve AI
├── InMemoryGraph (JGraphT)
│   ├── Fast algorithm access
│   └── Session-based knowledge
├── PersistentStore (NBT/JSON)
│   ├── Long-term facts
│   └── World state snapshots
└── Sync Layer
    ├── Load on startup
    └── Save periodically
```

**Advantages:**
- Pure Java (no dependencies)
- Fast in-memory operations
- Permissive license
- Control over persistence

**Disadvantages:**
- Must implement query language
- Manual persistence management
- No ACID guarantees

**Option C: Hybrid (JGraphT + Neo4j)**
```
Steve AI
├── Working Memory (JGraphT)
│   ├── Current session facts
│   └── Fast algorithmic queries
├── Long-term Memory (Neo4j)
│   ├── Persistent agent knowledge
│   └── World ontology
└── Sync Service
    ├── Promote important facts
    └── Lazy load from Neo4j
```

**Advantages:**
- Best of both worlds
- Fast session operations
- Persistent long-term memory
- Flexible caching strategy

**Disadvantages:**
- Increased complexity
- Synchronization overhead
- Two APIs to maintain

### 7.2 Data Model Design

**Triple Schema for Minecraft:**

```java
public enum MinecraftPredicate {
  // Spatial
  LOCATED_AT, NEAR, CONTAINS, PATH_TO,

  // Material
  CRAFTS_INTO, REQUIRES, SMELTS_TO, TOOLS_REQUIRED,

  // Entity
  DROPS, SPAWNS_IN, ATTACKS, TRADES, BREEDS_WITH,

  // Properties
  HAS_HARDNESS, HAS_BLAST_RESISTANCE, IS_FLAMMABLE,

  // Agent
  KNOWS, HAS_LEARNED, HAS_OBSERVED, PREFERS,

  // Temporal
  LEARNED_AT, EXPIRES, LAST_ACCESSED
}

public class MinecraftTriple {
  private String subject;    // URI: "mc:block/oak_log"
  private MinecraftPredicate predicate;
  private String object;     // URI or literal
  private double confidence;
  private Instant learnedAt;
  private int accessCount;
}
```

**Graph Schema (Neo4j):**

```cypher
// Constraint definitions
CREATE CONSTRAINT ON (b:Block) ASSERT b.id IS UNIQUE
CREATE CONSTRAINT ON (i:Item) ASSERT i.id IS UNIQUE
CREATE CONSTRAINT ON (e:Entity) ASSERT e.id IS UNIQUE
CREATE CONSTRAINT ON (a:Agent) ASSERT a.id IS UNIQUE

// Indexes for common queries
CREATE INDEX ON :Block(type)
CREATE INDEX ON :Item(type)
CREATE INDEX ON :Entity(type)
CREATE INDEX ON :Agent(name)
```

### 7.3 Knowledge Graph Service

```java
public interface KnowledgeGraph {
  // CRUD operations
  void addTriple(String subject, String predicate, String object);
  void addTriple(Triple triple);
  Optional<Triple> getTriple(String subject, String predicate);
  List<Triple> getTriples(String subject);

  // Query operations
  List<Triple> query(String cypher);
  List<Path> findPath(String from, String to);
  List<String> findRelated(String entity, int maxDepth);

  // Learning operations
  void learnFromObservation(String observation);
  void updateConfidence(String subject, String predicate, double delta);

  // Persistence
  void save(String filepath);
  void load(String filepath);
  void exportToRDF(String filepath);
}
```

### 7.4 LLM Integration

**Entity Extraction:**

```java
public class EntityExtractor {
  private final OpenAIClient llm;

  public List<Triple> extractTriples(String text) {
    String prompt = """
      Extract facts from this text as (subject, predicate, object) triples.
      Use Minecraft URIs like mc:block/oak_log, mc:item/diamond, etc.

      Text: {text}

      Return JSON array of triples.
      """;

    String response = llm.complete(prompt);
    return parseTriples(response);
  }
}
```

**Query Generation:**

```java
public class QueryGenerator {
  private final OpenAIClient llm;

  public String generateCypher(String question) {
    String prompt = """
      Convert this question to a Neo4j Cypher query.
      Graph schema: Agent, Block, Item, Entity, Biome, Recipe

      Question: {question}

      Return only the Cypher query.
      """;

    return llm.complete(prompt);
  }
}
```

**GraphRAG Pattern:**

```java
public class GraphRAGRetriever {
  private final KnowledgeGraph graph;
  private final VectorStore vectorStore;

  public String retrieve(String query) {
    // 1. Vector search for relevant entities
    List<String> entities = vectorStore.search(query, topK=10);

    // 2. Graph traversal for relationships
    String cypher = """
      MATCH (e)-[r*1..2]-(related)
      WHERE e.id IN $entities
      RETURN e, r, related
      """;
    List<Record> graphContext = graph.query(cypher, Map.of("entities", entities));

    // 3. Combine for LLM context
    return formatContext(entities, graphContext);
  }
}
```

### 7.5 Caching Strategy

```java
public class GraphCacheManager {
  private final Cache<String, List<Triple>> tripleCache;
  private final KnowledgeGraph graph;

  public List<Triple> getTriples(String subject) {
    return tripleCache.get(subject, () -> {
      List<Triple> triples = graph.getTriples(subject);
      return triples;
    });
  }

  // Cache frequently accessed patterns
  private void warmUpCache() {
    // Load all recipes
    cacheTriples("mc:recipe");

    // Load all blocks
    cacheTriples("mc:block");

    // Load agent memory
    cacheTriples("mc:agent:steve_1");
  }
}
```

### 7.6 Performance Considerations

**Neo4j Optimization:**
```cypher
// Use index hints
MATCH (b:Block)
USING INDEX b:Block(type)
WHERE b.type = "oak_log"
RETURN b

// Limit traversal depth
MATCH path = (a)-[:KNOWS*1..3]-(related)
RETURN path

// Use projections for large results
MATCH (b:Block)
RETURN b.type, b.hardness, b.harvest_tool
```

**JGraphT Optimization:**
```java
// Use specific graph types
Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

// Use adjacency list for sparse graphs
Graph<String, DefaultEdge> graph = new AdjacencyListGraph<>();

// Cache frequently computed paths
ShortestPathAlgorithm<String, DefaultEdge> sp =
  new DijkstraShortestPath<>(graph);
```

**Batch Operations:**
```java
// Neo4j batch import
String query = """
  UNWIND $triples AS triple
  MERGE (s {id: triple.subject})
  MERGE (o {id: triple.object})
  MERGE (s)-[r:RELATIONSHIP]->(o)
  SET r.type = triple.predicate
  """;
graph.query(query, Map.of("triples", tripleList));
```

---

## 8. Recommendations

### 8.1 Recommended Stack

**For Steve AI Project:**

**Phase 1: MVP (Minimal Viable Product)**
- **JGraphT** for in-memory knowledge
- Custom triple persistence to NBT
- Basic Cypher-like query API

**Reasoning:**
- Zero dependencies
- Fast development
- Sufficient for initial agent knowledge
- Easy to iterate

**Phase 2: Production**
- **Neo4j Community Edition** (embedded)
- Migration from JGraphT
- Full Cypher query support
- Graph algorithms library

**Reasoning:**
- Proven, scalable solution
- Rich ecosystem
- Excellent Java support
- ACID transactions

### 8.2 Minecraft Ontology Priority

**Phase 1 (Core Knowledge):**
1. Block types and properties
2. Basic crafting recipes (planks, sticks, tools)
3. Agent location and inventory
4. Simple entity types (passive/hostile)

**Phase 2 (Extended Knowledge):**
1. All crafting recipes
2. Mob behaviors and drops
3. Biome information
4. Structure locations

**Phase 3 (Advanced):**
1. Redstone circuits
2. Enchanting
3. Trading
4. Player-built structures

### 8.3 Implementation Timeline

**Week 1: Foundation**
- [ ] Design triple schema
- [ ] Implement basic KnowledgeGraph interface
- [ ] Set up JGraphT in-memory graph
- [ ] Create NBT persistence layer

**Week 2: Core Knowledge**
- [ ] Import block definitions
- [ ] Import basic recipes
- [ ] Implement spatial queries
- [ ] Add agent memory tracking

**Week 3: LLM Integration**
- [ ] Entity extraction from LLM
- [ ] Natural language query generation
- [ ] GraphRAG prototype
- [ ] Knowledge learning from observations

**Week 4: Migration & Scaling**
- [ ] Evaluate Neo4j embedded
- [ ] Migrate schema to Neo4j
- [ ] Implement caching layer
- [ ] Performance testing

### 8.4 Key Design Decisions

**URI Scheme:**
```
mc:block/<block_type>        - mc:block/oak_log
mc:item/<item_type>          - mc:item/diamond_sword
mc:entity/<entity_type>      - mc:entity/creeper
mc:agent/<agent_id>          - mc:agent:steve_1
mc:biome/<biome_type>        - mc:biome/plains
mc:recipe/<recipe_id>        - mc:recipe:oak_planks
mc:location/<x>_<y>_<z>      - mc:location:100_64_-200
```

**Confidence Levels:**
```
1.0 - Direct observation (agent mined block)
0.9 - Multiple confirmations
0.7 - Single observation
0.5 - LLM deduction
0.3 - Inference
```

**Relationship Naming:**
- Use verbs for actions: `CRAFTS_INTO`, `DROPS`, `ATTACKS`
- Use nouns for properties: `HAS_HARDNESS`, `LOCATION`
- Use past tense for observations: `LEARNED_AT`, `OBSERVED`

### 8.5 Success Metrics

**Knowledge Coverage:**
- 100% of vanilla blocks
- 100% of vanilla recipes
- 80% of mob behaviors

**Query Performance:**
- Simple lookup: < 1ms
- Path finding (3 hops): < 10ms
- Complex queries: < 100ms

**Memory Usage:**
- In-memory graph: < 100MB (core knowledge)
- Persistent storage: < 500MB (full knowledge)

**Learning Rate:**
- New fact addition: < 10ms
- Batch learning (100 facts): < 1s

---

## Appendix A: Query Examples

### A.1 Common Query Patterns

**Find all craftable items:**
```cypher
MATCH (recipe:Recipe)-[:OUTPUTS]->(item:Item)
RETURN DISTINCT item.type
ORDER BY item.type
```

**Find shortest crafting path:**
```cypher
MATCH path = shortestPath(
  (start:Item {type: "oak_log"})-[:REQUIRES|OUTPUTS*]-(end:Item {type: "wooden_pickaxe"})
)
RETURN [node IN nodes(path) | node.type] as crafting_chain
```

**Find all blocks within range:**
```cypher
MATCH (agent:Agent {id: "steve_1"})-[:LOCATED_AT]->(loc:Location)
MATCH (block:Block)-[:LOCATED_AT]->(blockLoc:Location)
WHERE point.distance(loc.coordinates, blockLoc.coordinates) < 32
RETURN block.type, blockLoc.coordinates
```

**Find mob drops:**
```cypher
MATCH (mob:Entity)-[:DROPS]->(item:Item)
WHERE mob.type = "creeper"
RETURN item.type, item.min_drop, item.max_drop
```

**Find optimal smelting path:**
```cypher
MATCH path = (input:Item)-[:SMELTS_TO*]->(output:Item)
WHERE input.type = "iron_ore"
RETURN output.type, length(path) as steps
ORDER BY steps
```

### A.2 Graph Algorithm Examples

**PageRank for important blocks:**
```cypher
CALL gds.pageRank.stream('knowledge-graph')
YIELD nodeId, score
RETURN gds.util.asNode(nodeId).type as block, score
ORDER BY score DESC
LIMIT 10
```

**Community detection for block groups:**
```cypher
CALL gds.louvain.stream('knowledge-graph')
YIELD nodeId, community
RETURN community, collect(gds.util.asNode(nodeId).type) as blocks
```

**Centrality for hub items:**
```cypher
CALL gds.betweenness.stream('knowledge-graph')
YIELD nodeId, score
RETURN gds.util.asNode(nodeId).type as item, score
ORDER BY score DESC
LIMIT 20
```

---

## Appendix B: Resources

### B.1 Documentation

- [Neo4j Documentation](https://neo4j.com/docs/)
- [JGraphT Documentation](https://jgrapht.org/)
- [Apache Jena Documentation](https://jena.apache.org/documentation/)
- [Apache TinkerPop Documentation](https://tinkerpop.apache.org/docs/current/)

### B.2 Tutorials

- [Neo4j Graph Algorithms](https://neo4j.com/docs/graph-data-science/current/)
- [Cypher Query Language](https://neo4j.com/docs/cypher-manual/current/)
- [SPARQL 1.1 Query Language](https://www.w3.org/TR/sparql11-query/)

### B.3 Research Papers

- Graph-based Agent Memory (2026): [arXiv:2602.05665](https://arxiv.org/html/2602.05665v1)
- Graphs Meet AI Agents (2025): [arXiv:2506.18019](https://arxiv.org/html/2506.18019v1)
- GraphRAG and Agentic Architecture (2025): [Neo4j Blog](https://neo4j.com/blog/developer/graphrag-and-agentic-architecture-with-neoconverse/)

### B.4 Libraries

- [Neo4j Java Driver](https://neo4j.com/docs/java-manual/current/)
- [JGraphT on GitHub](https://github.com/jgrapht/jgrapht)
- [Apache Jena on GitHub](https://github.com/apache/jena)
- [Graphiti (Time-Aware KG)](https://github.com/getzep/graphiti)

---

## Appendix C: Sample Code

### C.1 Neo4j Setup for Minecraft

```java
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.*;

public class MinecraftKnowledgeGraph {
  private final GraphDatabaseService graphDb;

  public MinecraftKnowledgeGraph(String dbPath) {
    DatabaseManagementService managementService =
      new DatabaseManagementServiceBuilder(
        java.nio.file.Paths.get(dbPath)
      ).build();

    this.graphDb = managementService.database("minecraft");

    // Create constraints
    createConstraints();
  }

  private void createConstraints() {
    try (Transaction tx = graphDb.beginTx()) {
      tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (b:Block) REQUIRE b.id IS UNIQUE");
      tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (i:Item) REQUIRE i.id IS UNIQUE");
      tx.execute("CREATE CONSTRAINT IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE");
      tx.execute("CREATE INDEX IF NOT EXISTS FOR (b:Block) ON (b.type)");
      tx.execute("CREATE INDEX IF NOT EXISTS FOR (i:Item) ON (i.type)");
      tx.commit();
    }
  }

  public void addBlock(String blockType, double hardness, String harvestTool) {
    try (Transaction tx = graphDb.beginTx()) {
      Node block = tx.createNode(Label.label("Block"));
      block.setProperty("id", "mc:block:" + blockType);
      block.setProperty("type", blockType);
      block.setProperty("hardness", hardness);
      block.setProperty("harvest_tool", harvestTool);
      tx.commit();
    }
  }

  public List<String> findCraftingPath(String from, String to) {
    try (Transaction tx = graphDb.beginTx()) {
      String query = """
        MATCH path = shortestPath(
          (start:Item {id: $from})-[:REQUIRES|OUTPUTS*]-(end:Item {id: $to})
        )
        RETURN [node IN nodes(path) | node.id] as path
        """;

      Result result = tx.execute(query,
        Map.of("from", "mc:item:" + from, "to", "mc:item:" + to));

      if (result.hasNext()) {
        return (List<String>) result.next().get("path");
      }
      return List.of();
    }
  }

  public void shutdown() {
    graphDb.shutdown();
  }
}
```

### C.2 JGraphT Implementation

```java
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.shortestpath.*;

public class MinecraftGraph {
  private final Graph<String, CustomEdge> graph;
  private final Map<String, Map<String, Object>> nodeProperties;

  public MinecraftGraph() {
    this.graph = new DefaultDirectedGraph<>(CustomEdge.class);
    this.nodeProperties = new HashMap<>();
  }

  public void addNode(String id, String type, Map<String, Object> properties) {
    graph.addVertex(id);
    properties.put("type", type);
    nodeProperties.put(id, properties);
  }

  public void addEdge(String from, String to, String relation, Map<String, Object> properties) {
    CustomEdge edge = graph.addEdge(from, to);
    if (edge != null) {
      edge.setRelation(relation);
      edge.setProperties(properties);
    }
  }

  public List<String> findPath(String from, String to) {
    DijkstraShortestPath<String, CustomEdge> pathFinder =
      new DijkstraShortestPath<>(graph);

    GraphPath<String, CustomEdge> path = pathFinder.getPath(from, to);
    if (path != null) {
      return path.getVertexList();
    }
    return List.of();
  }

  public List<String> findRelated(String node, int maxDepth) {
    List<String> related = new ArrayList<>();
    Set<String> visited = new HashSet<>();

    Queue<Pair<String, Integer>> queue = new LinkedList<>();
    queue.add(Pair.of(node, 0));
    visited.add(node);

    while (!queue.isEmpty()) {
      Pair<String, Integer> current = queue.poll();
      String currentId = current.getFirst();
      int depth = current.getSecond();

      if (depth > 0) {
        related.add(currentId);
      }

      if (depth < maxDepth) {
        for (String neighbor : Graphs.neighborSetOf(graph, currentId)) {
          if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            queue.add(Pair.of(neighbor, depth + 1));
          }
        }
      }
    }

    return related;
  }

  static class CustomEdge extends DefaultWeightedEdge {
    private String relation;
    private Map<String, Object> properties = new HashMap<>();

    public void setRelation(String relation) {
      this.relation = relation;
    }

    public String getRelation() {
      return relation;
    }

    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }
  }
}
```

### C.3 LLM Integration

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

public class KnowledgeGraphLLMIntegration {
  private final OpenAIClient llm;
  private final KnowledgeGraph graph;
  private final ObjectMapper mapper = new ObjectMapper();

  public KnowledgeGraphLLMIntegration(OpenAIClient llm, KnowledgeGraph graph) {
    this.llm = llm;
    this.graph = graph;
  }

  public void learnFromObservation(String observation) {
    String prompt = """
      Extract facts from this Minecraft observation as triples.
      Use Minecraft URIs: mc:block/..., mc:item/..., mc:entity/...

      Observation: %s

      Return JSON: [{"subject": "...", "predicate": "...", "object": "...", "confidence": 0.9}]
      """.formatted(observation);

    String response = llm.complete(prompt);
    try {
      List<Map<String, Object>> triples = mapper.readValue(
        response,
        mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
      );

      for (Map<String, Object> triple : triples) {
        graph.addTriple(
          (String) triple.get("subject"),
          (String) triple.get("predicate"),
          (String) triple.get("object"),
          ((Number) triple.getOrDefault("confidence", 0.5)).doubleValue()
        );
      }
    } catch (Exception e) {
      // Handle parsing error
    }
  }

  public String queryKnowledge(String question) {
    // First, try to find relevant entities
    String entityPrompt = """
      Extract Minecraft entities from this question.
      Return comma-separated URIs like mc:block/oak_log, mc:item/diamond

      Question: %s
      """.formatted(question);

    String entities = llm.complete(entityPrompt);

    // Build graph query
    String cypher = """
      MATCH (e)-[r*1..2]-(related)
      WHERE e.id IN [%s]
      RETURN e, r, related
      LIMIT 20
      """.formatted(entities);

    List<Record> graphContext = graph.query(cypher);

    // Generate answer with context
    String answerPrompt = """
      Answer this question using the graph context.

      Question: %s

      Graph Context:
      %s

      Provide a helpful answer.
      """.formatted(question, formatContext(graphContext));

    return llm.complete(answerPrompt);
  }

  private String formatContext(List<Record> records) {
    StringBuilder sb = new StringBuilder();
    for (Record record : records) {
      sb.append(record.toString()).append("\n");
    }
    return sb.toString();
  }
}
```

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Orchestrator Research Team
