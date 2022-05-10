package RML;

import org.apache.jena.graph.Node;

public class TemplateValuedTermMap extends TermMap {
    private final String template;

    public TemplateValuedTermMap(String template, Source source, boolean iri, boolean literal, boolean blank) {
        super(new PartialTerm(template, source), iri, literal, blank);
        this.template = template;
    }

    public TemplateValuedTermMap(String template, Source source) {
        this(template, source, true, false, false);
    }

    @Override
    protected boolean matchesInstance(Node node) {
        return false;
    }

    public String getTemplate() {
        return template;
    }

    @Override
    public TermMap overlap(TermMap other) {
        // TODO calculating overlap should be done on `PartialTerm`s if done at all in Java (currently done in Prolog)
        return null;
    }

    @Override
    public String toString() {
        return "<" + template.replaceAll("\\.\\*", "{}") + ">";
    }
}
