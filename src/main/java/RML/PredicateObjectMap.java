package RML;

import org.apache.jena.graph.Triple;

public class PredicateObjectMap {
    private final String id;
    private final TermMap predicate;
    private final TermMap object;

    public PredicateObjectMap(String id, TermMap predicate, TermMap object) {
        this.id = id;
        this.predicate = predicate;
        this.object = object;
    }

    public boolean matches(Triple t) {
        return this.predicate.matches(t.getPredicate()) && this.object.matches(t.getObject());
    }

    public TermMap getPredicate() {
        return predicate;
    }

    public TermMap getObject() {
        return object;
    }

    public String getId() {
        return id;
    }
}
