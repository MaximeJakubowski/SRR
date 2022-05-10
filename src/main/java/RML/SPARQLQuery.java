package RML;

import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;

public class SPARQLQuery {
    private final String queryString;
    private final Op queryOp;

    public SPARQLQuery(String queryString) {
        this.queryString = queryString;
        this.queryOp = Algebra.compile(QueryFactory.create(queryString));
    }

    public String getQueryString() {
        return queryString;
    }

    public Op getQueryOp() {
        return queryOp;
    }
}
