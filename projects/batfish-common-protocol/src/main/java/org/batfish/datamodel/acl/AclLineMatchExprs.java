package org.batfish.datamodel.acl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Prefix;

public final class AclLineMatchExprs {

  private AclLineMatchExprs() {}

  public static final FalseExpr FALSE = FalseExpr.INSTANCE;

  public static final OriginatingFromDevice ORIGINATING_FROM_DEVICE =
      OriginatingFromDevice.INSTANCE;

  public static final TrueExpr TRUE = TrueExpr.INSTANCE;

  public static AclLineMatchExpr and(AclLineMatchExpr... exprs) {
    return and(
        Arrays.stream(exprs)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  public static AclLineMatchExpr and(SortedSet<AclLineMatchExpr> exprs) {
    if (exprs.isEmpty()) {
      return TrueExpr.INSTANCE;
    }
    if (exprs.size() == 1) {
      return exprs.first();
    }
    return new AndMatchExpr(exprs);
  }

  public static MatchHeaderSpace match(HeaderSpace headerSpace) {
    return new MatchHeaderSpace(headerSpace);
  }

  public static MatchHeaderSpace matchDst(IpSpace ipSpace) {
    return new MatchHeaderSpace(HeaderSpace.builder().setDstIps(ipSpace).build());
  }

  public static MatchHeaderSpace matchDst(Ip ip) {
    return matchDst(ip.toIpSpace());
  }

  public static MatchHeaderSpace matchDst(Prefix prefix) {
    return matchDst(prefix.toIpSpace());
  }

  public static MatchHeaderSpace matchDstIp(String ip) {
    return matchDst(new Ip(ip).toIpSpace());
  }

  public static MatchSrcInterface matchSrcInterface(String... iface) {
    return new MatchSrcInterface(ImmutableList.copyOf(iface));
  }

  public static NotMatchExpr not(AclLineMatchExpr expr) {
    return new NotMatchExpr(expr);
  }

  public static AclLineMatchExpr or(AclLineMatchExpr... exprs) {
    return or(
        Arrays.stream(exprs).collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural())));
  }

  public static AclLineMatchExpr or(SortedSet<AclLineMatchExpr> exprs) {
    if (exprs.isEmpty()) {
      return FalseExpr.INSTANCE;
    }
    if (exprs.size() == 1) {
      return exprs.first();
    }
    return new OrMatchExpr(exprs);
  }

  public static PermittedByAcl permittedByAcl(String aclName) {
    return new PermittedByAcl(aclName);
  }
}
