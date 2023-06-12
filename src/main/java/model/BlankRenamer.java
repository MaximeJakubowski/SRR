package model;

import org.apache.jena.base.Sys;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;

import java.util.HashMap;
import java.util.Map;

import static model.AlgebraUtils.isJenaBlank;

/* Refreshes identifiers of a BGP
 */
public class BlankRenamer extends TransformCopy {
    private int blankCounter;

    public BlankRenamer(int startCounter) {
        this.blankCounter = startCounter;
    }

    public int getBlankCounter() { return this.blankCounter; }

    @Override
    public Op transform(OpBGP opBGP) {
        BasicPattern pattern = new BasicPattern();
        Map<String,String> renaming = createRenaming(opBGP.getPattern());

        for (Triple t : opBGP.getPattern())
            pattern.add(renameBlanks(t, renaming));

        return new OpBGP(pattern);
    }

    private String generateFreshIdentifier() {
        return "rbl".concat(String.valueOf(this.blankCounter++));
    }

    private Map<String,String> createRenaming(BasicPattern pattern) {
        Map<String,String> renaming = new HashMap<>();
        for (Triple t : pattern) {
            if (isJenaBlank(t.getSubject()) && !renaming.containsKey(t.getSubject().getName()))
                renaming.put(t.getSubject().getName(), generateFreshIdentifier());
            if (isJenaBlank(t.getObject()) && !renaming.containsKey(t.getObject().getName()))
                renaming.put(t.getObject().getName(), generateFreshIdentifier());
        }
        return renaming;
    }

    private Triple renameBlanks(Triple t, Map<String,String> renaming) {
        Node subject = t.getSubject();
        Node object = t.getObject();

        if (isJenaBlank(t.getSubject()))
            subject = NodeFactory.createBlankNode(renaming.get(subject.getName()));
        if (isJenaBlank(t.getObject()))
            object = NodeFactory.createBlankNode(renaming.get(object.getName()));

        return new Triple(subject, t.getPredicate(), object);
    }
}
