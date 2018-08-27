package org.batfish.datamodel.acl.normalize;

import static org.batfish.datamodel.acl.normalize.Negate.negate;

import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

/**
 * Normalize {@link AclLineMatchExpr AclLineMatchExprs} to a DNF-style form: a single or at the
 * root, all ands as immediate children of the or.
 */
public final class Normalizer implements GenericAclLineMatchExprVisitor<AclLineMatchExpr> {
  private static final Normalizer INSTANCE = new Normalizer();

  private Normalizer() {}

  public static AclLineMatchExpr normalize(AclLineMatchExpr expr) {
    return expr.accept(INSTANCE);
  }

  private static AclLineMatchExpr and(List<AclLineMatchExpr> exprs) {
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
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
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
    List<List<AclLineMatchExpr>> orOfAnds = new ArrayList<>();
    orOfAnds.add(new ArrayList<>());

    for (AclLineMatchExpr conjunct : normalizedConjuncts) {
      if (conjunct instanceof OrMatchExpr) {
        /* concatenate each AND with each disjunct, multiplying the number of ANDs in orOfAnds
         * by the number of disjuncts within the OrMatchExpr. i.e. this is where the blow-up caused
         * by normalization happens.
         */
        OrMatchExpr orMatchExpr = (OrMatchExpr) conjunct;
        List<List<AclLineMatchExpr>> newOrOfAnds = new ArrayList<>();
        for (AclLineMatchExpr disjunct : orMatchExpr.getDisjuncts()) {
          for (List<AclLineMatchExpr> ands : orOfAnds) {
            List<AclLineMatchExpr> newAnds = new ArrayList<>(ands);
            if (disjunct instanceof AndMatchExpr) {
              newAnds.addAll(((AndMatchExpr) disjunct).getConjuncts());
            } else {
              newAnds.add(disjunct);
            }
            newOrOfAnds.add(newAnds);
          }
        }
        orOfAnds = newOrOfAnds;
      } else {
        // add it to each AND
        assert !(conjunct instanceof AndMatchExpr);
        orOfAnds.forEach(ands -> ands.add(conjunct));
      }
    }

    return or(
        orOfAnds
            .stream()
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
    return negate(notMatchExpr.getOperand().accept(this));
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
