CREATE (n:User {keycloakId: 'user-1', username: 'user-1-name'})
CREATE (n:User {keycloakId: 'user-2', username: 'user-2-name'})
CREATE (n:User {keycloakId: 'user-3', username: 'user-3-name'})
CREATE (n:User {keycloakId: 'user-4', username: 'user-4-name'})

CREATE (n:Project {projectName: 'project-1-user-1'})
CREATE (n:Project {projectName: 'project-2-user-1'})
CREATE (n:Project {projectName: 'project-3-user-2'})
CREATE (n:Project {projectName: 'project-4-user-2'})
CREATE (n:Project {projectName: 'project-5-user-2'})
CREATE (n:Project {projectName: 'project-6-user-3'})
CREATE (n:Project {projectName: 'project-7-user-3'})

MATCH (u:User) WHERE id(u) = 0
MATCH (p:Project) WHERE id(p) = 4
CREATE (u)-[r:OWNER]->(p)

MATCH (u:User) WHERE id(u) = 0
MATCH (p:Project) WHERE id(p) = 5
CREATE (u)-[r:OWNER]->(p)

MATCH (u:User) WHERE id(u) = 1
MATCH (p:Project) WHERE id(p) = 6
CREATE (u)-[r:OWNER]->(p)

MATCH (u:User) WHERE id(u) = 1
MATCH (p:Project) WHERE id(p) = 7
CREATE (u)-[r:OWNER]->(p)

MATCH (u:User) WHERE id(u) = 1
MATCH (p:Project) WHERE id(p) = 8
CREATE (u)-[r:OWNER]->(p)

MATCH (u:User) WHERE id(u) = 2
MATCH (p:Project) WHERE id(p) = 9
CREATE (u)-[r:OWNER]->(p)

MATCH (u:User) WHERE id(u) = 2
MATCH (p:Project) WHERE id(p) = 10
CREATE (u)-[r:OWNER]->(p)





