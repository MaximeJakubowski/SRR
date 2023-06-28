package RML;

import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;

import java.util.Set;
import java.util.HashSet;

public class Parser {
    private final Set<Source> sourceSet;
    private Model model;

    public Parser(String fileName) {
        // read file into model for querying
        readFile(fileName);

        this.sourceSet = this.parseModel();
    }

    private void readFile(String fileName) {
        this.model = ModelFactory.createDefaultModel();
        model.read(fileName);
    }

    private Set<Source> parseModel() {
        Set<Source> sources = new HashSet<>();
        Property logicalSourceProperty = new PropertyImpl("http://semweb.mmlab.be/ns/rml#logicalSource");
        NodeIterator logicalSources = this.model.listObjectsOfProperty(logicalSourceProperty);
        logicalSources.forEachRemaining(node -> sources.add(parseSource(node.asResource())));
        return sources;
    }

    private Source parseSource(Resource logicalSourceNode) {
        Property sourceProperty = new PropertyImpl("http://semweb.mmlab.be/ns/rml#source");
        RDFNode sourceNode = logicalSourceNode.getProperty(sourceProperty).getObject();

        Source source = null;
        if (sourceNode.isLiteral()) {
            source = new Source(logicalSourceNode.toString(), sourceNode.asLiteral().getString());
        } else {
            Property endpointProperty = new PropertyImpl("http://www.w3.org/ns/sparql-service-description#endpoint");
            Resource endpointId = sourceNode.asResource().getPropertyResourceValue(endpointProperty);
            Property queryProperty = new PropertyImpl("http://semweb.mmlab.be/ns/rml#query");
            RDFNode queryId = logicalSourceNode.asResource().getProperty(queryProperty).getObject();
            SPARQLQuery query = new SPARQLQuery(queryId.asLiteral().getString());
            source = new SPARQLSource(logicalSourceNode.toString(), endpointId.toString(), query);
        }

        Property logicalSourceProperty = new PropertyImpl("http://semweb.mmlab.be/ns/rml#logicalSource");
        // copy `source` variable into a new "effectively final" variable that can be used in the `res ->` lambda
        Source finalSource = source;
        model.listSubjectsWithProperty(logicalSourceProperty, logicalSourceNode).forEachRemaining(
                res -> finalSource.getTriplesMaps().add(parseTriplesMap(res, finalSource)));

        return source;
    }

    private TriplesMap parseTriplesMap(Resource triplesMap, Source source) {
        TermMap subjectMap = parseTermMapWithShortCuts(triplesMap, source, "subject");
        Set<PredicateObjectMap> predicateObjectMaps = new HashSet<>();
        Property predicateObjectMapProperty = new PropertyImpl("http://www.w3.org/ns/r2rml#predicateObjectMap");
        StmtIterator predObjStatements = triplesMap.listProperties(predicateObjectMapProperty);
        while (predObjStatements.hasNext()) {
            Resource predObjResource = predObjStatements.nextStatement().getObject().asResource();
            TermMap predicateMap = parseTermMapWithShortCuts(predObjResource, source, "predicate");
            TermMap objectMap = parseTermMapWithShortCuts(predObjResource, source, "object");
            predicateObjectMaps.add(new PredicateObjectMap(predObjResource.toString(), predicateMap, objectMap));
        }
        return new TriplesMap(triplesMap.toString(), source, subjectMap, predicateObjectMaps);
    }

    /**
     * Parses a term map for a triples map or predicate-object map. Finds term maps declared with the "full property",
     * i.e., rr:subjectMap/rr:predicateMap/rr:objectMap as well as those declared with the shortcuts rr:subject/
     * rr:predicate/rr:object
     *
     * @param parent   The triples map or predicate-object map for which a term map will be parsed.
     * @param source   The relevant source, needed to construct RML references
     * @param position Position of the parsed term map. Should be one of "subject", "predicate" or "object".
     * @return Parsed term map
     */
    private TermMap parseTermMapWithShortCuts(Resource parent, Source source, String position) {
        assert position.equals("subject") || position.equals("predicate") || position.equals("object");
        TermMap termMap = null;
        Property property = new PropertyImpl("http://www.w3.org/ns/r2rml#" + position + "Map");
        if (parent.hasProperty(property)) {
            termMap = parseTermMap(parent.getPropertyResourceValue(property), source);
        }
        Property shortcutProperty = new PropertyImpl("http://www.w3.org/ns/r2rml#" + position);
        if (parent.hasProperty(shortcutProperty)) {
            termMap = new ConstantValuedTermMap((Node_Concrete) parent.getProperty(shortcutProperty).getObject().asNode());
        }
        return termMap;
    }


    private TermMap parseTermMap(Resource termMapId, Source source) {
        TermMap constantValuedTermMap = parseConstantValuedTermMap(termMapId, source);
        TermMap referenceValuedTermMap = parseReferenceValuedTermMap(termMapId, source);
        TermMap templateValuedTermMap = parseTemplateValuedTermMap(termMapId, source);
        TermMap termMap = chooseTermMap(termMapId, constantValuedTermMap, referenceValuedTermMap, templateValuedTermMap);
        // override the node kind if it is set explicitly with rr:termType
        Property nodeKindProperty = new PropertyImpl("http://www.w3.org/ns/r2rml#termType");
        if (termMapId.hasProperty(nodeKindProperty)) {
            Resource nodeKind = termMapId.getPropertyResourceValue(nodeKindProperty);
            termMap.setIri(nodeKind.getURI().equals("http://www.w3.org/ns/r2rml#IRI"));
            termMap.setLiteral(nodeKind.getURI().equals("http://www.w3.org/ns/r2rml#Literal"));
            termMap.setBlank(nodeKind.getURI().equals("http://www.w3.org/ns/r2rml#BlankNode"));
        }
        return termMap;
    }

    private TermMap chooseTermMap(Resource termMapId, TermMap constant, TermMap reference, TermMap template) {
        TermMap termMap = null;
        int count = 0;
        if (constant != null) {
            count += 1;
            termMap = constant;
        }
        if (reference != null) {
            count += 1;
            termMap = reference;
        }
        if (template != null) {
            count += 1;
            termMap = template;
        }
        if (count != 1) {
            throw new IllegalArgumentException(
                    "Term map has not exactly one of rr:template/rr:constant/rml:reference: " + termMapId);
        }
        return termMap;
    }

    private TermMap parseTemplateValuedTermMap(Resource termMap, Source source) {
        Property templateProperty = new PropertyImpl("http://www.w3.org/ns/r2rml#template");
        if (termMap.hasProperty(templateProperty)) {
            String generator = termMap.getProperty(templateProperty).getObject().asLiteral().getString();
            return new TemplateValuedTermMap(generator, source, true, false, false);
        }
        return null;
    }

    private TermMap parseConstantValuedTermMap(Resource termMap, Source source) {
        Property constantProperty = new PropertyImpl("http://www.w3.org/ns/r2rml#constant");
        if (termMap.hasProperty(constantProperty)) {
            RDFNode constantValue = termMap.getProperty(constantProperty).getObject();
            return new ConstantValuedTermMap((Node_Concrete) constantValue.asNode());
        }
        return null;
    }

    private TermMap parseReferenceValuedTermMap(Resource termMap, Source source) {
        Property referenceProperty = new PropertyImpl("http://semweb.mmlab.be/ns/rml#reference");
        if (termMap.hasProperty(referenceProperty)) {
            String reference = termMap.getProperty(referenceProperty).getObject().asLiteral().getString();
            return new ReferenceValuedTermMap(reference, source, false, true, false);
        }
        return null;
    }

    public Set<Source> getSourceSet() {
        return sourceSet;
    }
}
