package RML;

/**
 * An RML reference is defined by a source and a string.
 */
public class RMLReference {
    private final String reference;
    private final Source source;

    public RMLReference(String reference, Source source) {
        // if a SPARQL variable ends with ".value" we discard it and keep only the variable name
        if (source instanceof SPARQLSource && reference.endsWith(".value")) {
            this.reference = reference.substring(0, reference.length() - 6);
        } else {
            this.reference = reference;
        }
        this.source = source;
    }

    public String getReference() {
        return reference;
    }

    public Source getSource() {
        return source;
    }
}
