package RML;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an RML logical source.
 */
public class Source {
    private final String id;
    private final String endpoint;
    private final Set<TriplesMap> triplesMaps;

    public Source(String id, String endpoint, Set<TriplesMap> triplesMaps) {
        this.id = id;
        this.endpoint = endpoint;
        this.triplesMaps = triplesMaps;
    }

    public Source(String id, String endpoint) {
        this.id = id;
        this.endpoint = endpoint;
        this.triplesMaps = new HashSet<>();
    }

    public Set<TriplesMap> getTriplesMaps() {
        return triplesMaps;
    }

    public String getId() {
        return id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return id.equals(source.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
