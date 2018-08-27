package org.batfish.datamodel.acl.normalize;

import static org.batfish.datamodel.acl.AclLineMatchExprs.FALSE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.acl.normalize.Normalizer.normalize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.junit.Test;

public class NormalizerTest {
  private static final AclLineMatchExpr A = permittedByAcl("a");
  private static final AclLineMatchExpr B = permittedByAcl("b");
  private static final AclLineMatchExpr C = permittedByAcl("c");
  private static final AclLineMatchExpr D = permittedByAcl("d");
  private static final AclLineMatchExpr E = permittedByAcl("e");

  @Test
  public void visitAndMatchExpr() {
    assertThat(normalize(and(not(TRUE))), equalTo(FALSE));
    assertThat(normalize(and(TRUE)), equalTo(TRUE));
    assertThat(normalize(and(FALSE)), equalTo(FALSE));
    assertThat(normalize(and(TRUE, FALSE)), equalTo(FALSE));
  }

  @Test
  public void visitAndMatchExpr_distributeOverOr() {
    AclLineMatchExpr expr = and(A, B, or(C, D));
    AclLineMatchExpr nf = or(and(A, B, C), and(A, B, D));
    assertThat(normalize(expr), equalTo(nf));
  }

  @Test
  public void visitAndMatchExpr_distributeOverOr2() {
    AclLineMatchExpr expr = and(A, B, or(and(C, D), E));
    AclLineMatchExpr nf = or(and(A, B, C, D), and(A, B, E));
    AclLineMatchExpr actual = normalize(expr);
    assertThat(actual, equalTo(nf));
  }

  @Test
  public void visitAndMatchExpr_expandAnd() {
    AclLineMatchExpr expr = and(A, and(B, C));
    AclLineMatchExpr nf = and(A, B, C);
    assertThat(normalize(expr), equalTo(nf));
  }

  @Test
  public void visitAndMatchExpr_nf() {
    // identity on normal forms
    AclLineMatchExpr expr = and(A, B, not(C));
    assertThat(normalize(expr), equalTo(expr));
  }

  @Test
  public void visitAndMatchExpr_recursionExpansion() {
    AclLineMatchExpr a = permittedByAcl("a");
    AclLineMatchExpr b = permittedByAcl("b");
    AclLineMatchExpr c = permittedByAcl("c");
    AclLineMatchExpr d = permittedByAcl("d");
    AclLineMatchExpr expr = and(a, or(and(b, c), d));
    AclLineMatchExpr nf = or(and(a, b, c), and(a, d));
    assertThat(normalize(expr), equalTo(nf));
  }

  @Test
  public void visitFalseExpr() {
    assertThat(normalize(FALSE), equalTo(FALSE));
  }

  @Test
  public void visitMatchHeaderSpace() {
    AclLineMatchExpr matchHeaderSpace = match(HeaderSpace.builder().build());
    assertThat(normalize(matchHeaderSpace), equalTo(matchHeaderSpace));
  }

  @Test
  public void visitMatchSrcInterface() {
    MatchSrcInterface matchSrcInterface = AclLineMatchExprs.matchSrcInterface("foo");
    assertThat(normalize(matchSrcInterface), equalTo(matchSrcInterface));
  }

  @Test
  public void visitNotMatchExpr() {
    AclLineMatchExpr a = permittedByAcl("a");
    assertThat(normalize(not(FALSE)), equalTo(TRUE));
    assertThat(normalize(not(a)), equalTo(not(a)));
    assertThat(normalize(not(not(a))), equalTo(a));
  }

  @Test
  public void visitOriginatingFromDevice() {
    assertThat(normalize(ORIGINATING_FROM_DEVICE), equalTo(ORIGINATING_FROM_DEVICE));
  }

  @Test
  public void visitOrMatchExpr() {
    assertThat(normalize(or(TRUE)), equalTo(TRUE));
    assertThat(normalize(or(FALSE)), equalTo(FALSE));
    assertThat(normalize(or(TRUE, FALSE)), equalTo(TRUE));
    assertThat(normalize(or(not(TRUE))), equalTo(FALSE));
  }

  @Test
  public void visitOrMatchExpr_nf() {
    // identity on normal forms
    AclLineMatchExpr expr = or(A, B, not(C));
    assertThat(normalize(expr), equalTo(expr));
  }

  @Test
  public void visitOrMatchExpr_expandOr() {
    // normalize recursively
    AclLineMatchExpr expr = or(A, or(B, C));
    AclLineMatchExpr nf = or(A, B, C);
    assertThat(normalize(expr), equalTo(nf));
  }

  @Test
  public void visitOrMatchExpr_dontDistributeOverAnd() {
    AclLineMatchExpr a = permittedByAcl("a");
    AclLineMatchExpr b = permittedByAcl("b");
    AclLineMatchExpr c = permittedByAcl("c");
    AclLineMatchExpr d = permittedByAcl("d");
    AclLineMatchExpr expr = or(a, b, and(c, d));
    assertThat(normalize(expr), equalTo(expr));
  }

  @Test
  public void visitOrMatchExpr_recursionExpansion() {
    AclLineMatchExpr a = permittedByAcl("a");
    AclLineMatchExpr b = permittedByAcl("b");
    AclLineMatchExpr c = permittedByAcl("c");
    AclLineMatchExpr d = permittedByAcl("d");
    AclLineMatchExpr expr = or(a, and(or(b, c), d));
    AclLineMatchExpr nf = or(a, and(b, d), and(c, d));
    assertThat(normalize(expr), equalTo(nf));
  }

  @Test
  public void visitPermittedByAcl() {
    AclLineMatchExpr expr = permittedByAcl("a");
    assertThat(normalize(expr), equalTo(expr));
  }

  @Test
  public void visitTrueExpr() {
    assertThat(normalize(TRUE), equalTo(TRUE));
  }
}
