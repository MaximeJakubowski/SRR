package RML;

import org.apache.jena.graph.Node;

import java.sql.Ref;
import java.util.Collections;

public class ReferenceValuedTermMap extends TermMap {
    private RMLReference reference;

    public ReferenceValuedTermMap(String reference, Source source, boolean iri, boolean literal, boolean blank) {
        this(new RMLReference(reference, source), iri, literal, blank);
    }

    public ReferenceValuedTermMap(RMLReference reference, boolean iri, boolean literal, boolean blank) {
        super(new PartialTerm(Collections.singletonList(reference)), iri, literal, blank);
        this.reference = reference;
    }

    public ReferenceValuedTermMap(String reference, Source source) {
        this(new RMLReference(reference, source), false, true, false);
    }

    public ReferenceValuedTermMap(RMLReference reference) {
        super(new PartialTerm(Collections.singletonList(reference)), false, true, false);
        this.reference = reference;
    }

    public ReferenceValuedTermMap(ReferenceValuedTermMap other) {
        super(new PartialTerm(Collections.singletonList(other.reference)),
                other.isIri(), other.isLiteral(), other.isBlank());
        this.reference = other.reference;
    }

    @Override
    protected boolean matchesInstance(Node node) {
        return false;
    }

    @Override
    public TermMap overlap(TermMap other) {
        return null;
    }
}
