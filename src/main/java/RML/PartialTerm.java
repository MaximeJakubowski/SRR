package RML;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to represent "terms with gaps", i.e., strings with certain gaps to be filled in by references/variables.
 * This class can be used to represent an URI template, but it is more general and does not enforce URI encoding.
 */
public class PartialTerm {
    private final List<Object> structure;

    public PartialTerm(List<Object> structure) {
        for (Object object : structure) {
            assert object instanceof String || object instanceof RMLReference;
        }
        this.structure = new ArrayList<>(structure);
    }

    public PartialTerm(String template, Source source) {
        this.structure = new ArrayList<>();
        // split template on curly braces groups, and also extract matches between curly braces
        String[] parts = template.split("\\{.*?\\}", -1);
        Pattern p = Pattern.compile("\\{(.*?)\\}");
        Matcher m = p.matcher(template);
        int partIndex = 0;
        while (m.find()) {
            structure.add(parts[partIndex]);
            structure.add(new RMLReference(m.group(1), source));
            partIndex += 1;
        }
    }

    public Set<RMLReference> getReferences() {
        Set<RMLReference> references = new HashSet<>();
        for (Object object : structure) {
            if (object instanceof RMLReference) {
                references.add((RMLReference) object);
            }
        }
        return references;
    }

    public List<Object> getStructure() {
        return structure;
    }
}
