import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

public class JenaTest {

    @Test
    void queryTest() {
        Op query = Algebra.compile(QueryFactory.create("SELECT ?s WHERE { ?s ?p ?o FILTER (<p> = ?o) }"));

        System.out.println(query);
    }

    @Test
    void variablenames() {
        Node nodevar = NodeFactory.createVariable("var");
        Var arqvar = Var.alloc("var");

        assert ! nodevar.toString().equals(arqvar.getVarName());
    }

    @Test
    void literalnames() {
        Node lit = NodeFactory.createLiteral("lit");

        assert ! lit.toString().equals(lit.getLiteral().toString());
    }
}
