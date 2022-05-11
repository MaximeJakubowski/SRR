import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RenamingTest {

    private Op applyRenaming(Map<String, String> renaming, String query) {
        Map<Var, Var> varrename = new HashMap<>();
        for (String key: renaming.keySet())
            varrename.put(Var.alloc(key), Var.alloc(renaming.get(key)));

        return Algebra.compile(QueryTransformOps.transform(QueryFactory.create(query), varrename));
    }

    @Test
    void renamingEmpty() {
        Map<String, String> renaming = new HashMap<>();

        String query = "SELECT (COUNT(?o) AS ?c) WHERE { ?s <p> ?o } GROUP BY ?s ";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = applyRenaming(renaming, query);

        assertEquals(rewrittenQuery, queryOp);
    }

    @Test
    void renamingGroup() {
        Map<String, String> renaming = new HashMap<>();
        renaming.put("c", "r_c");

        String query = "SELECT (COUNT(?o) AS ?c) WHERE { ?s <p> ?o } GROUP BY ?s ";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = applyRenaming(renaming, query);

        System.out.println(queryOp);
        System.out.println(rewrittenQuery);
    }

    @Test
    void renamingOrder() {
        Map<String, String> renaming = new HashMap<>();
        renaming.put("c", "r_c");

        String query = """
                SELECT ?c
                WHERE { ?x <name> ?c }
                ORDER BY ?c""";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = applyRenaming(renaming, query);

        System.out.println(queryOp);
        System.out.println(rewrittenQuery);
    }

    @Test
    void renamingGraph() {
        Map<String, String> renaming = new HashMap<>();
        renaming.put("g", "r_g");

        String query = "SELECT ?c WHERE { ?g <a> <graph> GRAPH ?g { ?x <name> ?c } }";
        Op queryOp = Algebra.compile(QueryFactory.create(query));

        Op rewrittenQuery = applyRenaming(renaming, query);

        System.out.println(queryOp);
        System.out.println(rewrittenQuery);
    }
}
