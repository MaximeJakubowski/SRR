package model;

import model.FlatPTerm;
import org.apache.jena.graph.Node;

public class NodeTermPair {
    private final Node fromtp;
    private final FlatPTerm term;

    public NodeTermPair(Node fromtp, FlatPTerm term) {
        this.fromtp = fromtp;
        this.term = term;
    }

    public Node getNode() {
        return fromtp;
    }

    public FlatPTerm getTerm() {
        return term;
    }
}
