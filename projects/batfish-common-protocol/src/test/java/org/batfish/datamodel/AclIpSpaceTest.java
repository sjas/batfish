package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

public class AclIpSpaceTest {
  /*
   * Permit everything in 1.1.0.0/16 except for 1.1.1.0/24.
   */
  private static final AclIpSpace _aclIpSpace =
      AclIpSpace.builder()
          .thenRejecting(Prefix.parse("1.1.1.0/24").toIpSpace())
          .thenPermitting(Prefix.parse("1.1.0.0/16").toIpSpace())
          .build();

  @Test
  public void testContainsIp() {
    assertThat(_aclIpSpace, not(containsIp(new Ip("1.1.1.0"))));
    assertThat(_aclIpSpace, containsIp(new Ip("1.1.0.0")));
    assertThat(_aclIpSpace, not(containsIp(new Ip("1.0.0.0"))));
  }

  @Test
  public void testComplement() {
    IpSpace notIpSpace = _aclIpSpace.complement();
    assertThat(notIpSpace, containsIp(new Ip("1.1.1.0")));
    assertThat(notIpSpace, not(containsIp(new Ip("1.1.0.0"))));
    assertThat(notIpSpace, containsIp(new Ip("1.0.0.0")));
  }

  @Test
  public void testIntersection() {
    PrefixIpSpace prefixIpSpace = Prefix.parse("1.2.3.0/24").toIpSpace();
    assertThat(AclIpSpace.intersection(UniverseIpSpace.INSTANCE, null), nullValue());
    assertThat(
        AclIpSpace.intersection(null, EmptyIpSpace.INSTANCE, prefixIpSpace),
        equalTo(EmptyIpSpace.INSTANCE));
    assertThat(
        AclIpSpace.intersection(null, prefixIpSpace, UniverseIpSpace.INSTANCE),
        equalTo(prefixIpSpace));
  }

  @Test
  public void testUnion() {
    PrefixIpSpace prefixIpSpace = Prefix.parse("1.2.3.0/24").toIpSpace();
    assertThat(AclIpSpace.union(null, EmptyIpSpace.INSTANCE), nullValue());
    assertThat(
        AclIpSpace.union(null, EmptyIpSpace.INSTANCE, UniverseIpSpace.INSTANCE),
        equalTo(UniverseIpSpace.INSTANCE));
    assertThat(
        AclIpSpace.union(null, EmptyIpSpace.INSTANCE, prefixIpSpace), equalTo(prefixIpSpace));
  }
}
