package RML;

import java.util.*;

public class SPARQLSource extends Source {
    private final SPARQLQuery query;

    public SPARQLSource(String id, String endpoint, SPARQLQuery query) {
        super(id, endpoint);
        this.query = query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SPARQLSource that = (SPARQLSource) o;
        return query.equals(that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), query);
    }

    public SPARQLQuery getQuery() {
        return query;
    }
}
