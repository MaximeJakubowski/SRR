# SPARQL-RML Rewriter
Given a set of RML mappings which maps RDF data (source) to RDF data (target),
and given a SPARQL query formulated in terms of the target RDF data, rewrite the 
SPARQL query to a SPARQL query formulated in terms of the source RDF data.

## Usage
### Docker
Create a Docker image and run it based on the provided Dockerfile.

### Compiling
Build with Maven:

`mvn clean package`. 

The result is a CLI program. 
Run the program to get the help information:

`java -Djava.library.path=<path-to-jpl7-library> -jar <srr-filename>`

Where `<path-to-jpl7-library>` is the folder containing `libjpl.dylib` `libswipl.dylib`. 
This is to be found in the swipl installation directory.

## Dependencies
This software uses four libraries:
+ Apache Jena ARQ for SPARQL manipulation
+ JPL7 to interface with SWI-Prolog
+ JGraphT for some graph analysis used in the rewriting
+ PicoCLI for the Command Line Interface (embedded source code)

## Limitations
RML mappings may only use specific templates for termmaps. The templates must:
+ contain only one variable
+ the variable must occur at the end of the template

## Future
+ Add support for general templates
+ Replace prolog unification with Clojure.core.logic unification

## Credits
Thomas Delva for the original RML package