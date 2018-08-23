package org.batfish.main;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessList.Builder;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.symbolic.bdd.AclLineMatchExprToBDD;
import org.batfish.symbolic.bdd.BDDPacket;
import org.batfish.symbolic.bdd.IpSpaceToBDD;
import org.junit.Before;
import org.junit.Test;

public class AclExplainerTest {
  private static final Prefix PREFIX1_16 = Prefix.parse("1.0.0.0/16");
  private static final Prefix PREFIX1_24 = Prefix.parse("1.0.0.0/24");
  private static final Prefix PREFIX2 = Prefix.parse("2.0.0.0/16");

  private static final HeaderSpace DST_PORT_80 =
      HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(80, 80))).build();
  private static final HeaderSpace DST_PORT_22 =
      HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(22, 22))).build();
  private static final HeaderSpace DST_PORT_20_TO_30 =
      HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(20, 30))).build();
  private static final HeaderSpace DST_PREFIX1_16_DST_PORT_22 =
      DST_PORT_22.toBuilder().setDstIps(PREFIX1_16.toIpSpace()).build();
  private static final HeaderSpace DST_PREFIX1_24_DST_PORT_22 =
      DST_PORT_22.toBuilder().setDstIps(PREFIX1_24.toIpSpace()).build();
  private static final HeaderSpace DST_PREFIX2 =
      HeaderSpace.builder().setDstIps(PREFIX2.toIpSpace()).build();

  private static final IpAccessListLine PERMIT_DST_PORT_80 =
      IpAccessListLine.acceptingHeaderSpace(DST_PORT_80);
  private static final IpAccessListLine REJECT_DST_PREFIX1_16_DST_PORT_22 =
      IpAccessListLine.rejectingHeaderSpace(DST_PREFIX1_16_DST_PORT_22);
  private static final IpAccessListLine REJECT_DST_PREFIX1_24_DST_PORT_22 =
      IpAccessListLine.rejectingHeaderSpace(DST_PREFIX1_24_DST_PORT_22);
  private static final IpAccessListLine PERMIT_DST_PORT_22 =
      IpAccessListLine.acceptingHeaderSpace(DST_PORT_22);
  private static final IpAccessListLine PERMIT_DST_PORT_20_TO_30 =
      IpAccessListLine.acceptingHeaderSpace(DST_PORT_20_TO_30);
  private static final IpAccessListLine REJECT_DST_PREFIX2 =
      IpAccessListLine.rejectingHeaderSpace(DST_PREFIX2);
  private static final Builder ACL_BUILDER = IpAccessList.builder().setName("ACL");

  private AclLineMatchExprToBDD _aclLineMatchExprToBDD;

  private IpSpaceToBDD _dstIpSpaceToBDD;

  private BDDPacket _pkt;

  @Before
  public void setup() {
    _pkt = new BDDPacket();
    _aclLineMatchExprToBDD =
        new AclLineMatchExprToBDD(_pkt.getFactory(), _pkt, ImmutableMap.of(), ImmutableMap.of());
    _dstIpSpaceToBDD = new IpSpaceToBDD(_pkt.getFactory(), _pkt.getDstIp());
  }

  @Test
  public void removeSubsetBDDs() {
    AclExplainer aclExplainer = new AclExplainer(_aclLineMatchExprToBDD, null);
    BDD ip1 = new Ip("1.0.0.0").toIpSpace().accept(_dstIpSpaceToBDD);
    BDD ip2 = new Ip("2.0.0.0").toIpSpace().accept(_dstIpSpaceToBDD);
    BDD wc =
        new IpWildcard(new Ip("0.0.0.0"), new Ip("255.0.255.0"))
            .toIpSpace()
            .accept(_dstIpSpaceToBDD);
    BDD prefix1 = PREFIX1_16.toIpSpace().accept(_dstIpSpaceToBDD);
    Set<BDD> orig = ImmutableSet.of(ip1, prefix1);
    Set<BDD> reduced = ImmutableSet.of(prefix1);

    assertThat(aclExplainer.removeSubsetBDDs(orig), equalTo(reduced));

    orig = ImmutableSet.of(ip1, ip2);
    assertThat(aclExplainer.removeSubsetBDDs(orig), equalTo(orig));

    orig = ImmutableSet.of(ip1, wc);
    reduced = ImmutableSet.of(wc);
    assertThat(aclExplainer.removeSubsetBDDs(orig), equalTo(reduced));

    orig = ImmutableSet.of(prefix1, wc);
    assertThat(aclExplainer.removeSubsetBDDs(orig), equalTo(orig));
  }

  @Test
  public void makeAnswerAcl() {}

  @Test
  public void makeAnswerAcls1() {
    IpAccessList acl =
        ACL_BUILDER
            .setLines(
                ImmutableList.of(
                    REJECT_DST_PREFIX2,
                    PERMIT_DST_PORT_80,
                    REJECT_DST_PREFIX1_16_DST_PORT_22,
                    PERMIT_DST_PORT_22,
                    PERMIT_DST_PORT_22,
                    PERMIT_DST_PORT_20_TO_30))
            .build();
    List<IpAccessList> explanations =
        new AclExplainer(_aclLineMatchExprToBDD, acl).makeAnswerAcls();

    assertThat(
        explanations,
        containsInAnyOrder(
            ACL_BUILDER.setLines(ImmutableList.of(REJECT_DST_PREFIX2, PERMIT_DST_PORT_80)).build(),
            ACL_BUILDER
                .setLines(
                    ImmutableList.of(
                        REJECT_DST_PREFIX2,
                        REJECT_DST_PREFIX1_16_DST_PORT_22,
                        PERMIT_DST_PORT_20_TO_30))
                .build()));
  }

  @Test
  public void remoteRedundantRejectLines() {
    IpAccessList acl1 =
        ACL_BUILDER
            .setLines(
                ImmutableList.of(
                    REJECT_DST_PREFIX1_24_DST_PORT_22,
                    REJECT_DST_PREFIX1_16_DST_PORT_22,
                    PERMIT_DST_PORT_22))
            .build();
    // reorder the reject lines. shouldn't make a difference
    IpAccessList acl2 =
        ACL_BUILDER
            .setLines(
                ImmutableList.of(
                    REJECT_DST_PREFIX1_16_DST_PORT_22,
                    REJECT_DST_PREFIX1_24_DST_PORT_22,
                    PERMIT_DST_PORT_22))
            .build();
    List<IpAccessList> explanations =
        new AclExplainer(_aclLineMatchExprToBDD, acl1).makeAnswerAcls();
    IpAccessList explanation =
        ACL_BUILDER
            .setLines(ImmutableList.of(REJECT_DST_PREFIX1_16_DST_PORT_22, PERMIT_DST_PORT_22))
            .build();
    assertThat(explanations, contains(explanation));

    explanations = new AclExplainer(_aclLineMatchExprToBDD, acl2).makeAnswerAcls();
    assertThat(explanations, contains(explanation));
  }
}
