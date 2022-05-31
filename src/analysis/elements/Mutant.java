package analysis.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Mutant {
    private final String name;
    private final List<Constraint> appliedConstraints;

    public Mutant(String name) {
        this.appliedConstraints = new ArrayList<>();
        this.name = name;
    }

    public void addConstraint(Constraint c) {
        this.appliedConstraints.add(c);
    }

    public String getName() {
        return name;
    }

    public List<Constraint> getAppliedConstraints() {
        return appliedConstraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mutant mutant = (Mutant) o;
        return name.equals(mutant.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
