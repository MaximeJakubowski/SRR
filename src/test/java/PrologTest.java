import RML.*;
import model.FlatPTerm;
import model.NodeTermPair;
import model.PrologUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Literal;
import org.jpl7.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class PrologTest {

    @Test
    public void basicTest() {
        Query q = new Query("X=1");
        q.hasSolution();
    }

    @Test
    public void jpl7variableTest() {
        Node var = NodeFactory.createVariable("myvarname");
        Term varterm = PrologUtils.nodeAsTerm(var);
        System.out.println(varterm.toString());
    }

    @Test
    void equalityGeneration() {
        SPARQLSource source = new SPARQLSource("someid", "someendpoint",
                new SPARQLQuery("SELECT ?s ?p ?o WHERE { ?s ?p ?o }"));
        Set<NodeTermPair> pairs = new HashSet<>();
        pairs.add(new NodeTermPair(NodeFactory.createLiteral("liter"),
                new FlatPTerm(new ConstantValuedTermMap((Node_Literal) NodeFactory.createLiteral("literal")))));
        pairs.add(new NodeTermPair(NodeFactory.createVariable("huh"),
                new FlatPTerm(new TemplateValuedTermMap("hu{s}", source))));
        pairs.add(new NodeTermPair(NodeFactory.createURI("http://www.ex.com/hello"),
                new FlatPTerm(new ReferenceValuedTermMap("o", source,true, false, false))));

        org.jpl7.Query goal = PrologUtils.equalityQueryFrom(pairs);

        System.out.println(goal.toString());
    }

    @Test
    void equalities() {
        SPARQLSource source = new SPARQLSource("someid", "someendpoint",
                new SPARQLQuery("SELECT ?s ?p ?o WHERE { ?s ?p ?o }"));
        Set<NodeTermPair> pairs = new HashSet<>();
        pairs.add(new NodeTermPair(NodeFactory.createLiteral("maxime"),
                new FlatPTerm(new ConstantValuedTermMap((Node_Literal) NodeFactory.createLiteral("maxime")))));
        pairs.add(new NodeTermPair(NodeFactory.createLiteral("huhh"),
                new FlatPTerm(new TemplateValuedTermMap("hu{s}", source, false, true, false))));

        org.jpl7.Query goal = PrologUtils.equalityQueryFrom(pairs);

        assert goal.hasSolution();
    }

    @Test
    void variablehandling() {
        Query goal = new Query(new Compound("=", new Term[] {new Variable("x"), new Atom("isfound")}));
        assert goal.hasSolution();
    }


}
