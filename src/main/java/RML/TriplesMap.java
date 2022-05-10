package RML;

import org.apache.jena.graph.Triple;

import java.util.HashSet;
import java.util.Set;

public class TriplesMap {
    private final String id;
    private final TermMap subject;
    private final Set<PredicateObjectMap> predicateObjectMaps;
    private Source source;

    public TriplesMap(String id, Source source, TermMap subject, Set<PredicateObjectMap> predicateObjectMaps) {
        this.id = id;
        this.subject = subject;
        this.predicateObjectMaps = predicateObjectMaps;
        this.source = source;
    }

    public TriplesMap(String id, Source source, TermMap subject) {
        this.id = id;
        this.subject = subject;
        this.source = source;
        this.predicateObjectMaps  = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public TermMap getSubject() {
        return subject;
    }

    public Set<PredicateObjectMap> getPredicateObjectMaps() {
        return predicateObjectMaps;
    }

    public Source getSource() {
        return source;
    }

    public boolean matches(Triple triple) {
        if (this.subject.matches(triple.getSubject())) {
            for (PredicateObjectMap predicateObjectMap : this.predicateObjectMaps)
                if (predicateObjectMap.matches(triple)) return true;
        }
        return false;
    }
}
