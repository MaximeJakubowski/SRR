import RML.*;
import model.NodeTerm;
import model.UnrolledTriplesMap;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.Var;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RewriterTest {

    void rewriteTest (String filename, String inputquery) {
        Parser parser = new Parser(filename);

        for (Source source : parser.getSourceSet())
            for (TriplesMap triplesMap : source.getTriplesMaps())
                for (PredicateObjectMap predicateObjectMap : triplesMap.getPredicateObjectMaps()) {
                    UnrolledTriplesMap utm = new UnrolledTriplesMap(
                            triplesMap.getSubject(),
                            predicateObjectMap,
                            source);
                    System.out.println("\nMAPPING\n=======");
                    System.out.println(OpAsQuery.asQuery(((SPARQLSource) utm.getSource()).getQuery().getQueryOp()));
                    System.out.print("-->  ");
                    System.out.print(utm.getSubjectMap().getGenerator().getStructure());
                    System.out.print("; ");
                    System.out.print(utm.getPredicateObjectMap().getPredicate().getGenerator().getStructure());
                    System.out.print("; ");
                    System.out.print(utm.getPredicateObjectMap().getObject().getGenerator().getStructure());
                    System.out.println();
                }

        Op userquery = Algebra.compile(QueryFactory.create(inputquery));
        Op rewrittenQuery = Transformer.transform(new Rewriter(parser.getSourceSet()), userquery);

        System.out.println("\nINPUT QUERY:\n============");
        System.out.println(inputquery);
        System.out.println("REWRITTEN QUERY:\n===============");
        System.out.println(OpAsQuery.asQuery(rewrittenQuery));
    }

    /**
     * Simple tests
     */
    @ParameterizedTest
    @CsvSource({
            "?s,    ?s, ?p, ?o",
            "?s,    ?s, ex:n, ?o",
            "?p,    ex:a, ?p, ex:name",
            "?s,    ?s, ex:n, \"name\"", // unsat, object generates iri
            "?n,    ex:a, ex:n, ?n",
            "?n,    ex:b, ex:n, ?n" // unsat, ex:b != ex:a
    })
    void simpleTests(String select, String subject, String predicate, String object) {
        String userqueryInput = String.format("""
                PREFIX ex: <http://example.com/ns#>
                SELECT %s WHERE { %s %s %s }
                """, select, subject, predicate, object);

        rewriteTest("src/test/resources/mapping1.ttl", userqueryInput);
    }

    @Test
    public void joinTest() {
        String userqueryInput = """
                PREFIX ex: <http://example.com/ns#>
                SELECT ?m ?p ?e WHERE { ?m ex:a ex:p . ?m ?p ?e }
                """;

        rewriteTest("src/test/resources/mapping2.ttl", userqueryInput);
    }

    @Test
    public void nameTest() {
        NodeTerm one = NodeTerm.create(Var.alloc("m"));
        NodeTerm two = NodeTerm.create(Var.alloc("m"));


        Graph<NodeTerm, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        g.addVertex(one);
        g.addVertex(two);
        assert one.equals(two);
    }

}
