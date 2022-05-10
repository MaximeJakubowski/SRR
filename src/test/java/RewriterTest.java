import RML.Parser;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class RewriterTest {

    void rewriteTest (String filename, String inputquery) {
        Parser parser = new Parser(filename);

        Op userquery = Algebra.compile(QueryFactory.create(inputquery));
        Op rewrittenQuery = Transformer.transform(new Rewriter(parser.getSourceSet()), userquery);

        System.out.println("INPUT QUERY:\n============");
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


}
