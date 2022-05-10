import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RenamingTest {

    @Test
    void renamingEmpty() {
        Map<String, String> renaming = new HashMap<>();

        String query = "SELECT (COUNT(?o) AS ?c) WHERE { ?s <p> ?o } GROUP BY ?s ";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = Transformer.transform(new VariableRenamer(renaming), queryOp);

        assertEquals(rewrittenQuery, queryOp);
    }

    @Test
    void renamingGroup() {
        Map<String, String> renaming = new HashMap<>();
        renaming.put("c", "r_c");

        String query = "SELECT (COUNT(?o) AS ?c) WHERE { ?s <p> ?o } GROUP BY ?s ";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = Transformer.transform(new VariableRenamer(renaming), queryOp);

        System.out.println(queryOp);
        System.out.println(rewrittenQuery);
    }

    @Test
    void renamingOrder() {
        Map<String, String> renaming = new HashMap<>();
        renaming.put("c", "r_c");

        String query = "SELECT ?c\n" +
                "WHERE { ?x <name> ?c }\n" +
                "ORDER BY ?c";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = Transformer.transform(new VariableRenamer(renaming), queryOp);

        System.out.println(queryOp);
        System.out.println(rewrittenQuery);
    }

    @Test
    void renamingGraph() {
        Map<String, String> renaming = new HashMap<>();
        renaming.put("g", "r_g");

        String query = "SELECT ?c WHERE { ?g <a> <graph> GRAPH ?g { ?x <name> ?c } }";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = Transformer.transform(new VariableRenamer(renaming), queryOp);

        System.out.println(queryOp);
        System.out.println(rewrittenQuery);
    }
}
