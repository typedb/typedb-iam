[![Factory](https://factory.vaticle.com/api/status/vaticle/typedb-iam/badge.svg)](https://factory.vaticle.com/vaticle/typedb-iam)
[![Discord](https://img.shields.io/discord/665254494820368395?color=7389D8&label=chat&logo=discord&logoColor=ffffff)](https://vaticle.com/discord)
[![Discussion Forum](https://img.shields.io/discourse/https/forum.vaticle.com/topics.svg)](https://forum.vaticle.com)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-typedb-796de3.svg)](https://stackoverflow.com/questions/tagged/typedb)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-typeql-3dce8c.svg)](https://stackoverflow.com/questions/tagged/typeql)

## TypeDB IAM: Open Source IAM Schema and Simulation

TypeDB IAM is a project built to demonstrate TypeDB's features through a simulated identity and access management system. It serves to showcase how an IAM system can be built and queried using TypeDB, while providing benchmarking data through the perfomance of the simulation's agents and comparisons with other databases.

### Agents and actions

The core of the simulation is its agents and the actions they perform. An instance of an Agent class represents a user interacting with the system, and its methods represent actions that that user might perform on the system. A UserAgent instance might create a new file, a SupervisorAgent one might assign new members to a user group, and a PolicyManagerAgent one might define new segregation of duty requirements that must be adhered to.

### Iterations

The actions that agents take, the number of times they perform them, and in what order are defined by a list of agent actions in a config file. A simulation comprises multiple iterations that each represent a fixed unit of time in the lifecycle of the IAM system. Each iteration, every agent action in the config file is run sequentially. While the actions performed each iteration remain the same, specific query parameters are determined randomly to ensure each iteration is unique. All random numbers are generated from a single user-provided seed, allowing for completely reproducable results.

### Partitions

The data within a single database instance is split into up to 100 partitions, each one representing a different company's IAM system. Data in each partition is isolated from the others, and each partition is operated on by a separate instance of each agent. This allows the agents to operate on their respective partitions in parallel while still ensuring that any data generated is fully deterministic.
