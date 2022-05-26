package com.rdfsonto.rdfsonto.model;


import lombok.Getter;
import org.springframework.data.neo4j.core.schema.*;


import java.util.Set;

@Node("User")
@Getter
public class UserNode {

    @Id
    @GeneratedValue
    long id;

    @Property("name")
    String username;

    @Relationship(type = "OWNS", direction = Relationship.Direction.OUTGOING)
    Set<ProjectNode> projectSet;

    public UserNode(String username, Set<ProjectNode> projectSet) {
        this.username = username;
        this.projectSet = projectSet;
    }
}
