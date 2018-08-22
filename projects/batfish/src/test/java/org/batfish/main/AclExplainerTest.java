package org.batfish.main;

import static org.hamcrest.MatcherAssert.assertThat;
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
  private static final Prefix PREFIX1 = Prefix.parse("1.0.0.0/16");
  private static final Prefix PREFIX2 = Prefix.parse("2.0.0.0/16");

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
    BDD prefix1 = PREFIX1.toIpSpace().accept(_dstIpSpaceToBDD);
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
    HeaderSpace dstPort80 =
        HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(80, 80))).build();
    HeaderSpace dstPort22 =
        HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(22, 22))).build();
    HeaderSpace dstPort20To30 =
        HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(20, 30))).build();
    HeaderSpace dstPrefix1DstPort22 = dstPort22.toBuilder().setDstIps(PREFIX1.toIpSpace()).build();
    HeaderSpace dstPrefix2 = HeaderSpace.builder().setDstIps(PREFIX2.toIpSpace()).build();

    IpAccessListLine permitDstPort80 = IpAccessListLine.acceptingHeaderSpace(dstPort80);
    IpAccessListLine rejectDstPrefix1DstPort22 =
        IpAccessListLine.rejectingHeaderSpace(dstPrefix1DstPort22);
    IpAccessListLine permitDstPort22 = IpAccessListLine.acceptingHeaderSpace(dstPort22);
    IpAccessListLine permitDstPort20To30 = IpAccessListLine.acceptingHeaderSpace(dstPort20To30);
    IpAccessListLine rejectDstPrefix2 = IpAccessListLine.rejectingHeaderSpace(dstPrefix2);
    Builder aclBuilder = IpAccessList.builder().setName("ACL");
    IpAccessList acl =
        aclBuilder
            .setLines(
                ImmutableList.of(
                    rejectDstPrefix2,
                    permitDstPort80,
                    rejectDstPrefix1DstPort22,
                    permitDstPort22,
                    permitDstPort22,
                    permitDstPort20To30))
            .build();
    List<IpAccessList> explanations =
        new AclExplainer(_aclLineMatchExprToBDD, acl).makeAnswerAcls();

    assertThat(
        explanations,
        containsInAnyOrder(
            aclBuilder.setLines(ImmutableList.of(rejectDstPrefix2, permitDstPort80)).build(),
            aclBuilder
                .setLines(
                    ImmutableList.of(
                        rejectDstPrefix2, rejectDstPrefix1DstPort22, permitDstPort20To30))
                .build()));
  }
}
