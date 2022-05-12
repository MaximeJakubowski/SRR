import RML.Parser;
import RML.Source;
import org.apache.jena.base.Sys;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

@CommandLine.Command(name = "SRR", version = "SRR 1.0", mixinStandardHelpOptions = true)
public class SRR implements Runnable {
    @CommandLine.Option(
            required = true,
            names = {"-r", "--rewrite"},
            description = "SPARQL query string to be rewritten.")
    private String queryString;

    @CommandLine.Parameters(
            arity="1..*",
            paramLabel = "<mappingFiles>",
            description = "At least one RML file containing the mappings")
    private String[] mappingFilenames;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SRR()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        Set<Source> sourceSet = new HashSet<>();
        for (String mappingFilename: mappingFilenames) {
            try {
                Parser parser = new Parser(mappingFilename);
                sourceSet.addAll(parser.getSourceSet());
            } catch (Exception e) {
                System.out.printf("File %s not found.%n", mappingFilename);
            }
        }

        Op userquery = Algebra.compile(QueryFactory.create(queryString));

        Op rewrittenQuery = Transformer.transform(new Rewriter(sourceSet), userquery);

        System.out.println(OpAsQuery.asQuery(rewrittenQuery));
    }
}
