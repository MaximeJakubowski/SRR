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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

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
        System.out.println(OpAsQuery.asQuery(userquery));
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

        rewriteTest("src/test/resources/mapping1.rml.ttl", userqueryInput);
    }

    @Test
    void joinTest() {
        String userqueryInput = """
                PREFIX ex: <http://example.com/ns#>
                SELECT ?m ?p ?e WHERE { ?m ex:a ex:p . ?m ?p ?e }
                """;

        rewriteTest("src/test/resources/mapping2.rml.ttl", userqueryInput);
    }

    @Test
    void nameTest() {
        NodeTerm one = NodeTerm.create(Var.alloc("m"));
        NodeTerm two = NodeTerm.create(Var.alloc("m"));


        Graph<NodeTerm, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        g.addVertex(one);
        g.addVertex(two);
        assert one.equals(two);
    }

    @Test
    void coneyPrivacyTest() {
        try {
            String queryString = Files.readString(Paths.get("src/test/resources/all-answers.rq"));
            rewriteTest("src/test/resources/coney-privacy.rml.ttl", queryString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void fhirConeyTest() {
        try {
            String queryString = Files.readString(Paths.get("src/test/resources/all-answers.rq"));
            rewriteTest("src/test/resources/fhir-coney.rml.ttl", queryString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMaartenBugTest() {
        String userQuery =
                "prefix schema: <http://schema.org/> \n" +
                        "SELECT ?p \n" +
                        "WHERE {?p a schema:Person}";

        rewriteTest("src/test/resources/maarten.rml.ttl", userQuery);
    }

}
