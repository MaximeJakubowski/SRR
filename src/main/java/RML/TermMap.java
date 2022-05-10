package RML;

import org.apache.jena.graph.Node;

import java.util.Set;


public abstract class TermMap {
    // node kind
    private boolean iri;
    private boolean literal;
    private boolean blank;

    // represent how terms are generated
    private PartialTerm generator;

    // TODO at some point add language tags, data types and URI encoding

    public TermMap(PartialTerm generator, boolean iri, boolean literal, boolean blank) {
        this.generator = generator;

        // bitwise xor
        assert iri ^ literal ^ blank;
        this.iri = iri;
        this.literal = literal;
        this.blank = blank;
    }

    private TermMap() {}

    public Set<RMLReference> getReferences() {
        return generator.getReferences();
    }

    public PartialTerm getGenerator() {
        return generator;
    }

    public boolean isIri() {
        return iri;
    }

    public boolean isLiteral() {
        return literal;
    }

    public boolean isBlank() {
        return blank;
    }

    public void setIri(boolean iri) {
        this.iri = iri;
        if (iri) {
            this.blank = this.literal = false;
        }
    }

    public void setLiteral(boolean literal) {
        this.literal = literal;
        if (literal) {
            this.iri = this.blank = false;
        }
    }

    public void setBlank(boolean blank) {
        this.blank = blank;
        if (blank) {
            this.literal = this.iri = false;
        }
    }

    public boolean matches(Node node) {
        return node.isVariable() || this.matchesInstance(node);
    }

    protected abstract boolean matchesInstance(Node node);

    public abstract TermMap overlap(TermMap other);
}
