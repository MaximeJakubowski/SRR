package model;

import RML.PartialTerm;
import RML.RMLReference;
import RML.TermMap;
import org.apache.jena.atlas.test.Gen;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.List;

/*
* Represents a Node or a Term.
* Terms are often nodes,
* sometimes terms are not nodes (only in the case of a template).
* This class interfaces Terms and Nodes
* */
public class NodeTerm {
    private final List<Object> structure;
    private final Node node;

    private enum GenType {
        URI,
        LITERAL,
        BLANK,
        ANY;
    };
    private final GenType type;

    private NodeTerm(List<Object> structure, boolean iri, boolean blank, boolean lit) {
        this.structure = new ArrayList<>(structure);
        this.node = null;

        if (iri) { this.type = GenType.URI; }
        else if (blank) { this.type = GenType.BLANK; }
        else if (lit) { this.type = GenType.LITERAL; }
        else { this.type = null; }
    }

    private NodeTerm(Node node) {
        this.node = node;
        this.structure = null;

        if (node.isVariable())
            this.type = GenType.ANY;
        else if (node.isURI())
            this.type = GenType.URI;
        else if (node.isLiteral())
            this.type = GenType.LITERAL;
        else
            this.type = GenType.BLANK;
    }

    public static NodeTerm create(TermMap termMap) {
        return new NodeTerm(
                termMap.getGenerator().getStructure(),
                termMap.isIri(), termMap.isBlank(), termMap.isLiteral()
        );
    }

    public static NodeTerm create(Node node) {
        return new NodeTerm(node);
    }

    public boolean isIRI() { return this.type == GenType.URI || this.type == GenType.ANY; };

    public boolean isLiteral() { return this.type == GenType.LITERAL || this.type == GenType.ANY; };

    public boolean isBlank() { return this.type == GenType.BLANK || this.type == GenType.ANY; };

    public boolean isAny() { return this.type == GenType.ANY; };

    public boolean isConcrete() {
        return (this.node != null && this.node.isConcrete()) ||
                (this.structure != null && this.structure.stream()
                        .noneMatch(obj -> obj instanceof RMLReference)); // depends heavily on lazy evaluation
    }

    public boolean isVariable() {
        return (this.node != null && this.node.isVariable()) ||
                (this.structure != null &&
                        this.structure.size() == 1 &&
                        this.structure.get(0) instanceof RMLReference); // depends heavily on lazy evaluation
    }

    public boolean isNode() {
        return isConcrete() || isVariable();
    }

    /*
    In true Apache Jena fashion:
    if not a node, die horribly
     */
    public Node asNode() {
        return this.node != null ? this.node: structureAsNode();
    }

    private Node structureAsNode() {
        if (isVariable())
            return Var.alloc(((RMLReference) this.structure.get(0)).getReference());
        if (isBlank())
            return NodeFactory.createBlankNode(new BlankNodeId(structureAsString()));
        if (isIRI())
            return NodeFactory.createURI(structureAsString());
        // it must be a literal
        return NodeFactory.createLiteral(structureAsString());
    }

    private String structureAsString() {
        assert isConcrete(); // otherwise die horribly

        String coll = "";
        for (Object obj: this.structure)
            coll = coll.concat((String) obj);

        return coll;
    }
}
