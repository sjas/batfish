package org.batfish.main;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.symbolic.bdd.AclLineMatchExprToBDD;
import org.batfish.symbolic.bdd.BDDPacket;
import org.batfish.symbolic.bdd.BDDSourceManager;
import org.batfish.z3.BDDIpAccessListSpecializer;

public final class AclExplainer {
  private final AclLineMatchExprToBDD _aclLineMatchExprToBDD;
  private final BDDPacket _bddPacket;
  private final Map<String, IpSpace> _ipSpaces;
  private final BDDSourceManager _mgr;
  private final IpAccessList _acl;

  @VisibleForTesting
  AclExplainer(AclLineMatchExprToBDD aclLineMatchExprToBDD, IpAccessList acl) {
    _aclLineMatchExprToBDD = aclLineMatchExprToBDD;
    _bddPacket = _aclLineMatchExprToBDD.getBDDPacket();
    _ipSpaces = _aclLineMatchExprToBDD.getIpSpaces();
    _mgr = _aclLineMatchExprToBDD.getBDDSourceManager();
    _acl = acl;
  }

  public static List<IpAccessList> explain(
      AclLineMatchExprToBDD aclLineMatchExprToBDD, IpAccessList acl) {
    return new AclExplainer(aclLineMatchExprToBDD, acl).makeAnswerAcls();
  }

  @VisibleForTesting
  List<IpAccessList> makeAnswerAcls() {
    // mapping from headerspace BDD -> line number matching that space.
    Map<BDD, Integer> lineByHeaderSpaceBDD = new HashMap<>();
    for (int lineNum = 0; lineNum < _acl.getLines().size(); lineNum++) {
      IpAccessListLine line = _acl.getLines().get(lineNum);
      if (line.getAction() == LineAction.ACCEPT) {
        BDD matchLineBDD = _aclLineMatchExprToBDD.visit(line.getMatchCondition());
        if (matchLineBDD.isZero()) {
          continue;
        }
        lineByHeaderSpaceBDD.putIfAbsent(matchLineBDD, lineNum);
      }
    }

    // remove headerspaces matched by other lines
    Set<BDD> spaces = removeSubsetBDDs(lineByHeaderSpaceBDD.keySet());
    return lineByHeaderSpaceBDD
        .entrySet()
        .stream()
        .filter(entry -> spaces.contains(entry.getKey()))
        .map(entry -> makeAnswerAcl(entry.getValue(), entry.getKey()))
        .collect(ImmutableList.toImmutableList());
  }

  /** Remove BDDs in input set that are subsets of (i.e. imply) other BDDs in the set. */
  Set<BDD> removeSubsetBDDs(Collection<BDD> bddSet) {
    Set<BDD> result = new HashSet<>(bddSet);
    List<BDD> bdds = new ArrayList<>(bddSet);
    while (!bdds.isEmpty()) {
      BDD bdd1 = bdds.remove(0);
      for (BDD bdd2 : bdds) {
        if (bdd1.imp(bdd2).isOne()) {
          result.remove(bdd1);
        } else if (bdd2.imp(bdd1).isOne()) {
          result.remove(bdd2);
        }
      }
    }
    return ImmutableSet.copyOf(result);
  }

  @VisibleForTesting
  IpAccessList makeAnswerAcl(int lineNum, BDD reachAndMatchLineBDD) {
    BDDIpAccessListSpecializer specializer =
        new BDDIpAccessListSpecializer(_bddPacket, reachAndMatchLineBDD, _ipSpaces, _mgr);
    IpAccessList specializedAcl = specializer.specializeUpToLine(_acl, lineNum);
    List<IpAccessListLine> specializedLines = specializedAcl.getLines();

    ImmutableList.Builder<IpAccessListLine> lines = ImmutableList.builder();
    for (int i = 0; i < specializedLines.size(); i++) {
      IpAccessListLine specializedLine = specializedLines.get(i);
      // exclude any lines disjoint from headerspace matched by lineNum
      if (specializedLine.getMatchCondition() instanceof org.batfish.datamodel.acl.FalseExpr) {
        continue;
      }
      // remove any earlier ACCEPT lines
      if (i != lineNum && specializedLine.getAction() == LineAction.ACCEPT) {
        continue;
      }
      lines.add(specializedLine);
    }

    return IpAccessList.builder().setName(specializedAcl.getName()).setLines(lines.build()).build();
  }
}
