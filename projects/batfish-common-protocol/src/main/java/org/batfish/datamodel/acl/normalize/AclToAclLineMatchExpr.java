package org.batfish.datamodel.acl.normalize;

import static org.batfish.datamodel.acl.AclLineMatchExprs.not;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.batfish.common.util.NonRecursiveSupplier;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
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

/** Reduce an {@link org.batfish.datamodel.IpAccessList} to a single {@link AclLineMatchExpr}. */
public final class AclToAclLineMatchExpr
    implements GenericAclLineMatchExprVisitor<AclLineMatchExpr> {
  private final Map<String, Supplier<AclLineMatchExpr>> _namedAclThunks;

  AclToAclLineMatchExpr(Map<String, IpAccessList> namedAcls) {
    _namedAclThunks = createThunks(namedAcls);
  }

  Map<String, Supplier<AclLineMatchExpr>> createThunks(Map<String, IpAccessList> namedAcls) {
    ImmutableMap.Builder<String, Supplier<AclLineMatchExpr>> thunks = ImmutableMap.builder();
    namedAcls.forEach(
        (name, acl) ->
            thunks.put(name, new NonRecursiveSupplier<>(() -> this.computeAclLineMatchExpr(acl))));
    return thunks.build();
  }

  AclLineMatchExpr computeAclLineMatchExpr(IpAccessList acl) {
    List<AclLineMatchExpr> rejects = new ArrayList<>();
    ImmutableSortedSet.Builder<AclLineMatchExpr> permitBuilder =
        new Builder<>(Comparator.naturalOrder());
    for (IpAccessListLine line : acl.getLines()) {
      AclLineMatchExpr expr = line.getMatchCondition().accept(this);
      if (line.getAction() == LineAction.ACCEPT) {
        if (rejects.isEmpty()) {
          permitBuilder.add(expr);
        } else {
          ImmutableSortedSet.Builder<AclLineMatchExpr> conjuncts =
              new Builder<>(Comparator.naturalOrder());
          conjuncts.addAll(rejects);
          conjuncts.add(expr);
          permitBuilder.add(new AndMatchExpr(conjuncts.build()));
        }
      } else {
        rejects.add(not(expr));
      }
    }
    return new OrMatchExpr(permitBuilder.build());
  }

  public static AclLineMatchExpr toAclLineMatchExpr(
      IpAccessList acl, Map<String, IpAccessList> namedAcls) {
    return new AclToAclLineMatchExpr(namedAcls).computeAclLineMatchExpr(acl);
  }

  @Override
  public AclLineMatchExpr visitAndMatchExpr(AndMatchExpr andMatchExpr) {
    return new AndMatchExpr(
        andMatchExpr
            .getConjuncts()
            .stream()
            .map(this::visit)
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
    return new NotMatchExpr(notMatchExpr.getOperand().accept(this));
  }

  @Override
  public AclLineMatchExpr visitOriginatingFromDevice(OriginatingFromDevice originatingFromDevice) {
    return originatingFromDevice;
  }

  @Override
  public AclLineMatchExpr visitOrMatchExpr(OrMatchExpr orMatchExpr) {
    return new OrMatchExpr(
        orMatchExpr
            .getDisjuncts()
            .stream()
            .map(this::visit)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  @Override
  public AclLineMatchExpr visitPermittedByAcl(PermittedByAcl permittedByAcl) {
    return _namedAclThunks.get(permittedByAcl.getAclName()).get();
  }

  @Override
  public AclLineMatchExpr visitTrueExpr(TrueExpr trueExpr) {
    return trueExpr;
  }
}
