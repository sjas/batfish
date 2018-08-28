package org.batfish.main;

import static org.batfish.datamodel.acl.AclLineMatchExprs.FALSE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;

import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class NormalizerTest {
  private static final AclLineMatchExpr A = permittedByAcl("a");
  private static final AclLineMatchExpr B = permittedByAcl("b");
  private static final AclLineMatchExpr C = permittedByAcl("c");
  private static final AclLineMatchExpr D = permittedByAcl("d");
  private static final AclLineMatchExpr E = permittedByAcl("e");

  private Normalizer normalizer = null;

  private AclLineMatchExpr normalize(AclLineMatchExpr expr) {
    return normalizer.normalize(expr);
  }

  @Test
  public void visitAndMatchExpr() {
    Assert.assertThat(normalize(and(not(TRUE))), Matchers.equalTo(FALSE));
    Assert.assertThat(normalize(and(TRUE)), Matchers.equalTo(TRUE));
    Assert.assertThat(normalize(and(FALSE)), Matchers.equalTo(FALSE));
    Assert.assertThat(normalize(and(TRUE, FALSE)), Matchers.equalTo(FALSE));
  }

  @Test
  public void visitAndMatchExpr_distributeOverOr() {
    AclLineMatchExpr expr = and(A, B, or(C, D));
    AclLineMatchExpr nf = or(and(A, B, C), and(A, B, D));
    Assert.assertThat(normalize(expr), Matchers.equalTo(nf));
  }

  @Test
  public void visitAndMatchExpr_distributeOverOr2() {
    AclLineMatchExpr expr = and(A, B, or(and(C, D), E));
    AclLineMatchExpr nf = or(and(A, B, C, D), and(A, B, E));
    AclLineMatchExpr actual = normalize(expr);
    Assert.assertThat(actual, Matchers.equalTo(nf));
  }

  @Test
  public void visitAndMatchExpr_expandAnd() {
    AclLineMatchExpr expr = and(A, and(B, C));
    AclLineMatchExpr nf = and(A, B, C);
    Assert.assertThat(normalize(expr), Matchers.equalTo(nf));
  }

  @Test
  public void visitAndMatchExpr_nf() {
    // identity on normal forms
    AclLineMatchExpr expr = and(A, B, not(C));
    Assert.assertThat(normalize(expr), Matchers.equalTo(expr));
  }

  @Test
  public void visitAndMatchExpr_recursionExpansion() {
    AclLineMatchExpr a = permittedByAcl("a");
    AclLineMatchExpr b = permittedByAcl("b");
    AclLineMatchExpr c = permittedByAcl("c");
    AclLineMatchExpr d = permittedByAcl("d");
    AclLineMatchExpr expr = and(a, or(and(b, c), d));
    AclLineMatchExpr nf = or(and(a, b, c), and(a, d));
    Assert.assertThat(normalize(expr), Matchers.equalTo(nf));
  }

  @Test
  public void visitFalseExpr() {
    Assert.assertThat(normalize(FALSE), Matchers.equalTo(FALSE));
  }

  @Test
  public void visitMatchHeaderSpace() {
    AclLineMatchExpr matchHeaderSpace = match(HeaderSpace.builder().build());
    Assert.assertThat(normalize(matchHeaderSpace), Matchers.equalTo(matchHeaderSpace));
  }

  @Test
  public void visitMatchSrcInterface() {
    MatchSrcInterface matchSrcInterface = AclLineMatchExprs.matchSrcInterface("foo");
    Assert.assertThat(normalize(matchSrcInterface), Matchers.equalTo(matchSrcInterface));
  }

  @Test
  public void visitNotMatchExpr() {
    AclLineMatchExpr a = permittedByAcl("a");
    Assert.assertThat(normalize(not(FALSE)), Matchers.equalTo(TRUE));
    Assert.assertThat(normalize(not(a)), Matchers.equalTo(not(a)));
    Assert.assertThat(normalize(not(not(a))), Matchers.equalTo(a));
  }

  @Test
  public void visitOriginatingFromDevice() {
    Assert.assertThat(
        normalize(ORIGINATING_FROM_DEVICE), Matchers.equalTo(ORIGINATING_FROM_DEVICE));
  }

  @Test
  public void visitOrMatchExpr() {
    Assert.assertThat(normalize(or(TRUE)), Matchers.equalTo(TRUE));
    Assert.assertThat(normalize(or(FALSE)), Matchers.equalTo(FALSE));
    Assert.assertThat(normalize(or(TRUE, FALSE)), Matchers.equalTo(TRUE));
    Assert.assertThat(normalize(or(not(TRUE))), Matchers.equalTo(FALSE));
  }

  @Test
  public void visitOrMatchExpr_nf() {
    // identity on normal forms
    AclLineMatchExpr expr = or(A, B, not(C));
    Assert.assertThat(normalize(expr), Matchers.equalTo(expr));
  }

  @Test
  public void visitOrMatchExpr_expandOr() {
    // normalize recursively
    AclLineMatchExpr expr = or(A, or(B, C));
    AclLineMatchExpr nf = or(A, B, C);
    Assert.assertThat(normalize(expr), Matchers.equalTo(nf));
  }

  @Test
  public void visitOrMatchExpr_dontDistributeOverAnd() {
    AclLineMatchExpr a = permittedByAcl("a");
    AclLineMatchExpr b = permittedByAcl("b");
    AclLineMatchExpr c = permittedByAcl("c");
    AclLineMatchExpr d = permittedByAcl("d");
    AclLineMatchExpr expr = or(a, b, and(c, d));
    Assert.assertThat(normalize(expr), Matchers.equalTo(expr));
  }

  @Test
  public void visitOrMatchExpr_recursionExpansion() {
    AclLineMatchExpr a = permittedByAcl("a");
    AclLineMatchExpr b = permittedByAcl("b");
    AclLineMatchExpr c = permittedByAcl("c");
    AclLineMatchExpr d = permittedByAcl("d");
    AclLineMatchExpr expr = or(a, and(or(b, c), d));
    AclLineMatchExpr nf = or(a, and(b, d), and(c, d));
    Assert.assertThat(normalize(expr), Matchers.equalTo(nf));
  }

  @Test
  public void visitPermittedByAcl() {
    AclLineMatchExpr expr = permittedByAcl("a");
    Assert.assertThat(normalize(expr), Matchers.equalTo(expr));
  }

  @Test
  public void visitTrueExpr() {
    Assert.assertThat(normalize(TRUE), Matchers.equalTo(TRUE));
  }
}
