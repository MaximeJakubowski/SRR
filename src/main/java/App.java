import RML.Parser;
import RML.Source;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(name = "srr", version = "ssr 1.0", mixinStandardHelpOptions = true)
public class App implements Runnable {
    @CommandLine.Option(
            names = {"-f", "--queryfile"},
            description = "<query> is a file containing a SPARQL query.")
    private boolean queryIsFile;

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "<query>",
            description = "SPARQL query string to be rewritten.")
    private String query;


    @CommandLine.Parameters(
            index = "1..*",
            paramLabel = "<mappingFiles>",
            description = "RML file(s) containing the mappings.")
    private String[] mappingFilenames;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        String queryString = query;
        if (queryIsFile)
            try {
                queryString = Files.readString(Paths.get(query));
            } catch (Exception e) {
                System.out.println("File %s not found.%n");
            }

        if (mappingFilenames == null)
            System.out.println(queryString);


        Set<Source> sourceSet = new HashSet<>();
        for (String mappingFilename: mappingFilenames) {
            try {
                Parser parser = new Parser(mappingFilename);
                sourceSet.addAll(parser.getSourceSet());
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        Query queryInputQuery = QueryFactory.create(queryString);
        Op userquery = Algebra.compile(queryInputQuery);

        Op rewrittenQuery = Transformer.transform(new Rewriter(sourceSet), userquery);

        Query queryRewrittenQuery = OpAsQuery.asQuery(rewrittenQuery);

        // Use the original projection, i.e., eliminate 'select star' bug
        VarExprList proj = queryInputQuery.getProject();
        if (proj.size() == proj.getVars().size() && // the projection contains no expressions
                queryRewrittenQuery.getProject().size() != proj.size()) { // the projection of the input is different from the projection of the rewriting
            List<Var> projvars = new ArrayList<>();
            proj.forEachVar(projvars::add); // create a new projection
            rewrittenQuery = new OpProject(rewrittenQuery, projvars);
            queryRewrittenQuery = OpAsQuery.asQuery(rewrittenQuery);
        }

        System.out.println(queryRewrittenQuery);
    }
}
