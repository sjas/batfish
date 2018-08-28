package org.batfish.main;

import static org.batfish.datamodel.acl.normalize.Negate.negate;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.GenericAclLineMatchExprVisitor;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.acl.explanation.Conjuncts;
import org.batfish.symbolic.bdd.AclLineMatchExprToBDD;

/**
 * Normalize {@link AclLineMatchExpr AclLineMatchExprs} to a DNF-style form: a single or at the
 * root, all ands as immediate children of the or.
 */
public final class Normalizer implements GenericAclLineMatchExprVisitor<AclLineMatchExpr> {
  AclLineMatchExprToBDD _aclLineMatchExprToBDD;

  public Normalizer(AclLineMatchExprToBDD aclLineMatchExprToBDD) {
    _aclLineMatchExprToBDD = aclLineMatchExprToBDD;
  }

  public AclLineMatchExpr normalize(AclLineMatchExpr expr) {
    return expr.accept(this);
  }

  private static AclLineMatchExpr and(Set<AclLineMatchExpr> exprs) {
    if (exprs.contains(FalseExpr.INSTANCE)) {
      return FalseExpr.INSTANCE;
    }
    if (exprs
        .stream()
        .anyMatch(expr -> expr instanceof AndMatchExpr || expr instanceof OrMatchExpr)) {
      return null;
    }

    SortedSet<AclLineMatchExpr> conjuncts =
        exprs
            .stream()
            .filter(expr -> expr != TrueExpr.INSTANCE)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
    if (conjuncts.isEmpty()) {
      return TrueExpr.INSTANCE;
    } else if (conjuncts.size() == 1) {
      return conjuncts.first();
    } else {
      return new AndMatchExpr(conjuncts);
    }
  }

  private static AclLineMatchExpr or(ImmutableSortedSet<AclLineMatchExpr> exprs) {
    if (exprs.contains(TrueExpr.INSTANCE)) {
      return TrueExpr.INSTANCE;
    }
    SortedSet<AclLineMatchExpr> disjuncts =
        exprs
            .stream()
            .filter(expr -> expr != FalseExpr.INSTANCE)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
    if (disjuncts.isEmpty()) {
      return FalseExpr.INSTANCE;
    } else if (disjuncts.size() == 1) {
      return disjuncts.first();
    } else {
      return new OrMatchExpr(disjuncts);
    }
  }

  @Override
  public AclLineMatchExpr visitAndMatchExpr(AndMatchExpr andMatchExpr) {
    List<AclLineMatchExpr> normalizedConjuncts =
        andMatchExpr
            .getConjuncts()
            .stream()
            .map(this::visit)
            // expand any nested AndMatchExprs
            .flatMap(
                expr ->
                    expr instanceof AndMatchExpr
                        ? ((AndMatchExpr) expr).getConjuncts().stream()
                        : Stream.of(expr))
            .filter(expr -> expr != TrueExpr.INSTANCE)
            .collect(Collectors.toList());

    // Normalize subexpressions, combine all OR subexpressions, and then distribute the AND over
    // the single OR.
    Set<Conjuncts> orOfAnds = new HashSet<>();
    orOfAnds.add(new Conjuncts(_aclLineMatchExprToBDD));

    for (AclLineMatchExpr conjunct : normalizedConjuncts) {
      if (conjunct instanceof OrMatchExpr) {
        /* concatenate each AND with each disjunct, multiplying the number of ANDs in orOfAnds
         * by the number of disjuncts within the OrMatchExpr. i.e. this is where the blow-up caused
         * by normalization happens.
         */
        OrMatchExpr orMatchExpr = (OrMatchExpr) conjunct;
        Set<Conjuncts> newOrOfAnds = new HashSet<>();
        for (AclLineMatchExpr disjunct : orMatchExpr.getDisjuncts()) {
          for (Conjuncts conjuncts : orOfAnds) {
            Conjuncts newConjuncts = new Conjuncts(conjuncts);
            if (disjunct instanceof AndMatchExpr) {
              ((AndMatchExpr) disjunct).getConjuncts().forEach(newConjuncts::addConjunct);
            } else {
              newConjuncts.addConjunct(disjunct);
            }
            if (newConjuncts.isSatisfiable()) {
              newOrOfAnds.add(newConjuncts);
            } else {
              System.out.println("hey");
            }
          }
        }
        orOfAnds = newOrOfAnds;
      } else {
        // add it to each AND
        assert !(conjunct instanceof AndMatchExpr);
        orOfAnds.forEach(conjuncts -> conjuncts.addConjunct(conjunct));
      }
    }

    return or(
        orOfAnds
            .stream()
            .map(Conjuncts::getConjuncts)
            .map(Normalizer::and)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  @Override
  public AclLineMatchExpr visitFalseExpr(FalseExpr falseExpr) {
    return falseExpr;
  }

  @Override
  public AclLineMatchExpr visitMatchHeaderSpace(MatchHeaderSpace matchHeaderSpace) {
    return matchHeaderSpace;
  }

  @Override
  public AclLineMatchExpr visitMatchSrcInterface(MatchSrcInterface matchSrcInterface) {
    return matchSrcInterface;
  }

  @Override
  public AclLineMatchExpr visitNotMatchExpr(NotMatchExpr notMatchExpr) {
    AclLineMatchExpr negated = negate(notMatchExpr.getOperand());
    if (negated instanceof NotMatchExpr) {
      // hit a leaf
      return negated;
    }
    return negated.accept(this);
  }

  @Override
  public AclLineMatchExpr visitOriginatingFromDevice(OriginatingFromDevice originatingFromDevice) {
    return originatingFromDevice;
  }

  @Override
  public AclLineMatchExpr visitOrMatchExpr(OrMatchExpr orMatchExpr) {
    return or(
        orMatchExpr
            .getDisjuncts()
            .stream()
            // normalize
            .map(this::visit)
            // expand nested OrMatchExprs
            .flatMap(
                expr ->
                    (expr instanceof OrMatchExpr)
                        ? ((OrMatchExpr) expr).getDisjuncts().stream()
                        : Stream.of(expr))
            .filter(expr -> expr != FalseExpr.INSTANCE)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  @Override
  public AclLineMatchExpr visitPermittedByAcl(PermittedByAcl permittedByAcl) {
    return permittedByAcl;
  }

  @Override
  public AclLineMatchExpr visitTrueExpr(TrueExpr trueExpr) {
    return trueExpr;
  }
}
