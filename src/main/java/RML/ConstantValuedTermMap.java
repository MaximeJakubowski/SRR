package RML;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.graph.Node_Literal;

import java.util.Collections;

public class ConstantValuedTermMap extends TermMap {
    private final Node_Concrete value;

    public ConstantValuedTermMap(Node_Concrete value) {
        // we take care of literal nodes: otherwise the quotations are included.
        super(new PartialTerm(
                Collections.singletonList(value.isLiteral() ?
                        value.getLiteral().toString() :
                        value.toString())),
                value.isURI(),
                value.isLiteral(),
                value.isBlank());

        this.value = value;
    }

    @Override
    protected boolean matchesInstance(Node node) {
        return false;
    }

    @Override
    public TermMap overlap(TermMap other) {
        // if implementations for the other two cases (TemplateIRITermMap, LiteralTermMap) are correct,
        // then this is sufficient
        return other.overlap(this);
    }

    public Node_Concrete asNode() {
        return value;
    }
}
