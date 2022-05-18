package model;

import java.util.List;
import java.util.Set;

public interface EqualitySAT {
    boolean isSAT(Set<List<NodeTerm>> equalities);
}
