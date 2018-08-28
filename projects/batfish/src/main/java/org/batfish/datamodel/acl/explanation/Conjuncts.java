package org.batfish.datamodel.acl.explanation;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.symbolic.bdd.AclLineMatchExprToBDD;
import org.batfish.symbolic.bdd.BDDOps;

/**
 * Manages a set of conjoined {@link AclLineMatchExpr} for constructing {@link AndMatchExpr
 * AndMatchExprs}. Uses {@link BDD BDDs} to detect inconsistent conjunctions and to remove redundant
 * conjuncts.
 */
public final class Conjuncts {
  private final AclLineMatchExprToBDD _aclLineMatchExprToBDD;
  BDD _bdd;
  Map<AclLineMatchExpr, BDD> _conjuncts;

  public Conjuncts(AclLineMatchExprToBDD aclLineMatchExprToBDD) {
    _aclLineMatchExprToBDD = aclLineMatchExprToBDD;
    _bdd = _aclLineMatchExprToBDD.getBDDPacket().getFactory().one();
    _conjuncts = new HashMap<>();
  }

  public Conjuncts(Conjuncts other) {
    _aclLineMatchExprToBDD = other._aclLineMatchExprToBDD;
    _bdd = other._bdd;
    _conjuncts = new HashMap<>(other._conjuncts);
  }

  public void addConjunct(AclLineMatchExpr expr) {
    if (_bdd.isZero()) {
      return;
    }

    BDD bdd = expr.accept(_aclLineMatchExprToBDD);
    BDD newBDD = bdd.and(_bdd);
    if (newBDD.isZero()) {
      // add for debugging purposes, so we can see the inconsistency.
      _conjuncts.put(expr, bdd);
      _bdd = newBDD;
      return;
    }
    if (_bdd.equals(newBDD)) {
      // conjunct didn't change anything. discard
      return;
    }
    List<AclLineMatchExpr> toRemove = new ArrayList<>();
    _conjuncts.forEach(
        (conj, conjBDD) -> {
          if (bdd.imp(conjBDD).isOne()) {
            toRemove.add(conj);
          }
        });
    toRemove.forEach(_conjuncts::remove);
    _conjuncts.put(expr, bdd);
    _bdd = newBDD;

    if (!_bdd.equals(new BDDOps(_bdd.getFactory()).and(_conjuncts.values()))) {
      return;
    }
  }

  public Set<AclLineMatchExpr> getConjuncts() {
    return _bdd.isZero() ? ImmutableSet.of(FalseExpr.INSTANCE) : _conjuncts.keySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Conjuncts)) {
      return false;
    }
    Conjuncts other = (Conjuncts) o;
    return Objects.equals(_bdd, other._bdd) && Objects.equals(_conjuncts, other._conjuncts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_bdd, _conjuncts);
  }

  public boolean isSatisfiable() {
    return !_bdd.isZero();
  }
}
