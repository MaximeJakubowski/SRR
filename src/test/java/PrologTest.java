import RML.*;
import model.NodeTerm;
import model.PrologSAT;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Literal;
import org.jpl7.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrologTest {

    @Test
    public void basicTest() {
        Query q = new Query("X=1");
        q.hasSolution();
    }


    @Test
    void equalityGeneration() {
        SPARQLSource source = new SPARQLSource("someid", "someendpoint",
                new SPARQLQuery("SELECT ?s ?p ?o WHERE { ?s ?p ?o }"));
        Set<List<NodeTerm>> pairs = new HashSet<>();
        pairs.add(Arrays.asList(
                NodeTerm.create(NodeFactory.createLiteral("liter")),
                NodeTerm.create(new ConstantValuedTermMap((Node_Literal) NodeFactory.createLiteral("literal")))));
        pairs.add(Arrays.asList(
                NodeTerm.create(NodeFactory.createVariable("huh")),
                NodeTerm.create(new TemplateValuedTermMap("hu{s}", source))));
        pairs.add(Arrays.asList(
                NodeTerm.create(NodeFactory.createURI("http://www.ex.com/hello")),
                NodeTerm.create(new ReferenceValuedTermMap("o", source,true, false, false))));

        org.jpl7.Query goal = PrologSAT.equalityQueryFrom(pairs);

        System.out.println(goal.toString());
    }

    @Test
    void equalities() {
        SPARQLSource source = new SPARQLSource("someid", "someendpoint",
                new SPARQLQuery("SELECT ?s ?p ?o WHERE { ?s ?p ?o }"));
        Set<List<NodeTerm>> pairs = new HashSet<>();
        pairs.add(Arrays.asList(
                NodeTerm.create(NodeFactory.createLiteral("maxime")),
                NodeTerm.create(new ConstantValuedTermMap((Node_Literal) NodeFactory.createLiteral("maxime")))));
        pairs.add(Arrays.asList(
                NodeTerm.create(NodeFactory.createLiteral("huhh")),
                NodeTerm.create(new TemplateValuedTermMap("hu{s}", source, false, true, false))));

        org.jpl7.Query goal = PrologSAT.equalityQueryFrom(pairs);

        assert goal.hasSolution();
    }

    @Test
    void variablehandling() {
        Query goal = new Query(new Compound("=", new Term[] {new Variable("x"), new Atom("isfound")}));
        assert goal.hasSolution();
    }


}
