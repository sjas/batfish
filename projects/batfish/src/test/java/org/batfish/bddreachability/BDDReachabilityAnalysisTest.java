package org.batfish.bddreachability;

import static org.batfish.bddreachability.TestNetwork.DST_PREFIX_1;
import static org.batfish.bddreachability.TestNetwork.DST_PREFIX_2;
import static org.batfish.bddreachability.TestNetwork.LINK_1_NETWORK;
import static org.batfish.bddreachability.TestNetwork.LINK_2_NETWORK;
import static org.batfish.bddreachability.TestNetwork.POST_SOURCE_NAT_ACL_DEST_PORT;
import static org.batfish.bddreachability.TestNetwork.SOURCE_NAT_ACL_IP;
import static org.batfish.bddreachability.TestNetwork.SOURCE_NAT_POOL_IP;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.symbolic.bdd.BDDMatchers.intersects;
import static org.batfish.symbolic.bdd.BDDMatchers.isOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.symbolic.bdd.BDDAcl;
import org.batfish.symbolic.bdd.BDDInteger;
import org.batfish.symbolic.bdd.BDDOps;
import org.batfish.symbolic.bdd.BDDPacket;
import org.batfish.symbolic.bdd.IpSpaceToBDD;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.Drop;
import org.batfish.z3.state.NeighborUnreachable;
import org.batfish.z3.state.NodeAccept;
import org.batfish.z3.state.NodeDropAclIn;
import org.batfish.z3.state.NodeDropAclOut;
import org.batfish.z3.state.NodeDropNoRoute;
import org.batfish.z3.state.NodeDropNullRoute;
import org.batfish.z3.state.NodeInterfaceNeighborUnreachable;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.PostInVrf;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdge;
import org.batfish.z3.state.PreOutEdgePostNat;
import org.batfish.z3.state.PreOutVrf;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class BDDReachabilityAnalysisTest {
  private static final String FLOW_TAG = "FLOW_TAG";
  private static BDDReachabilityAnalysis GRAPH;
  private static BDDReachabilityAnalysisFactory GRAPH_FACTORY;
  private static TestNetwork NET;

  private BDDOps _bddOps;

  private Ip _dstIface1Ip;
  private BDD _dstIface1IpBDD;
  private Ip _dstIface2Ip;
  private BDD _dstIface2IpBDD;
  private String _dstIface1Name;
  private String _dstIface2Name;
  private String _dstName;
  private NodeAccept _dstNodeAccept;
  private PostInVrf _dstPostInVrf;
  private PreInInterface _dstPreInInterface1;
  private PreInInterface _dstPreInInterface2;
  private PreOutEdge _dstPreOutEdge1;
  private PreOutEdge _dstPreOutEdge2;
  private PreOutEdgePostNat _dstPreOutEdgePostNat1;
  private PreOutEdgePostNat _dstPreOutEdgePostNat2;
  private PreOutVrf _dstPreOutVrf;

  private BDD _link1DstIpBDD;
  private String _link1DstName;

  private BDD _link1SrcIpBDD;

  private BDD _link2DstIpBDD;
  private String _link2DstName;

  private BDD _link2SrcIpBDD;
  private String _link2SrcName;

  private String _srcName;
  private NodeAccept _srcNodeAccept;
  private PostInVrf _srcPostInVrf;
  private PreInInterface _srcPreInInterface1;
  private PreInInterface _srcPreInInterface2;
  private PreOutEdge _srcPreOutEdge1;
  private PreOutEdge _srcPreOutEdge2;
  private PreOutEdgePostNat _srcPreOutEdgePostNat1;
  private PreOutEdgePostNat _srcPreOutEdgePostNat2;
  private PreOutVrf _srcPreOutVrf;

  @BeforeClass
  public static void initFactory() throws IOException {
    NET = new TestNetwork();
    NET._batfish.computeDataPlane(false);
    DataPlane dataPlane = NET._batfish.loadDataPlane();
    GRAPH_FACTORY =
        new BDDReachabilityAnalysisFactory(NET._configs, dataPlane.getForwardingAnalysis(), false);

    IpSpaceAssignment assignment =
        IpSpaceAssignment.builder()
            .assign(
                new InterfaceLocation(NET._srcNode.getName(), NET._link1Src.getName()),
                UniverseIpSpace.INSTANCE)
            .build();
    GRAPH = GRAPH_FACTORY.bddReachabilityAnalysis(assignment);
  }

  @Before
  public void setup() {
    _bddOps = new BDDOps(BDDPacket.factory);
    _dstIface1Ip = DST_PREFIX_1.getStartIp();
    _dstIface1IpBDD = dstIpBDD(_dstIface1Ip);
    _dstIface2Ip = DST_PREFIX_2.getStartIp();
    _dstIface2IpBDD = dstIpBDD(_dstIface2Ip);
    _dstIface1Name = NET._dstIface1.getName();
    _dstIface2Name = NET._dstIface2.getName();
    _dstName = NET._dstNode.getHostname();
    _dstNodeAccept = new NodeAccept(_dstName);
    _dstPostInVrf = new PostInVrf(_dstName, DEFAULT_VRF_NAME);
    _dstPreOutVrf = new PreOutVrf(_dstName, DEFAULT_VRF_NAME);

    _link1DstIpBDD = dstIpBDD(LINK_1_NETWORK.getEndIp());
    _link1DstName = NET._link1Dst.getName();

    _link1SrcIpBDD = dstIpBDD(LINK_1_NETWORK.getStartIp());

    _link2DstIpBDD = dstIpBDD(LINK_2_NETWORK.getEndIp());
    _link2DstName = NET._link2Dst.getName();

    _link2SrcIpBDD = dstIpBDD(LINK_2_NETWORK.getStartIp());
    _link2SrcName = NET._link2Src.getName();

    _srcName = NET._srcNode.getHostname();
    _srcNodeAccept = new NodeAccept(_srcName);
    _srcPostInVrf = new PostInVrf(_srcName, DEFAULT_VRF_NAME);

    _dstPreInInterface1 = new PreInInterface(_dstName, _link1DstName);
    _dstPreInInterface2 = new PreInInterface(_dstName, _link2DstName);

    _srcPreInInterface1 = new PreInInterface(_srcName, NET._link1Src.getName());
    _srcPreInInterface2 = new PreInInterface(_srcName, _link2SrcName);

    _dstPreOutEdge1 = new PreOutEdge(_dstName, _link1DstName, _srcName, NET._link1Src.getName());
    _dstPreOutEdge2 = new PreOutEdge(_dstName, _link2DstName, _srcName, _link2SrcName);
    _dstPreOutEdgePostNat1 =
        new PreOutEdgePostNat(_dstName, _link1DstName, _srcName, NET._link1Src.getName());
    _dstPreOutEdgePostNat2 =
        new PreOutEdgePostNat(_dstName, _link2DstName, _srcName, _link2SrcName);
    _srcPreOutEdge1 = new PreOutEdge(_srcName, NET._link1Src.getName(), _dstName, _link1DstName);
    _srcPreOutEdge2 = new PreOutEdge(_srcName, _link2SrcName, _dstName, _link2DstName);
    _srcPreOutEdgePostNat1 =
        new PreOutEdgePostNat(_srcName, NET._link1Src.getName(), _dstName, _link1DstName);
    _srcPreOutEdgePostNat2 =
        new PreOutEdgePostNat(_srcName, _link2SrcName, _dstName, _link2DstName);
    _srcPreOutVrf = new PreOutVrf(_srcName, DEFAULT_VRF_NAME);
  }

  private static List<Ip> bddIps(BDD bdd) {
    BDDInteger bddInteger = GRAPH_FACTORY.getIpSpaceToBDD().getBDDInteger();

    return bddInteger
        .getValuesSatisfying(bdd, 10)
        .stream()
        .map(Ip::new)
        .collect(Collectors.toList());
  }

  private static BDD bddTransition(StateExpr preState, StateExpr postState) {
    return GRAPH_FACTORY.getBDDTransitions().get(preState).get(postState);
  }

  private static BDD dstIpBDD(Ip ip) {
    return new IpSpaceToBDD(BDDPacket.factory, new BDDPacket().getDstIp()).toBDD(ip);
  }

  private static BDD dstPortBDD(int destPort) {
    return new BDDPacket().getDstPort().value(destPort);
  }

  private static BDD srcIpBDD(Ip ip) {
    return new IpSpaceToBDD(BDDPacket.factory, new BDDPacket().getSrcIp()).toBDD(ip);
  }

  private BDD or(BDD... bdds) {
    return _bddOps.or(bdds);
  }

  private static BDD vrfAcceptBDD(String node) {
    return GRAPH_FACTORY.getVrfAcceptBDDs().get(node).get(DEFAULT_VRF_NAME);
  }

  @Test
  public void testVrfAcceptBDDs() {
    assertThat(
        vrfAcceptBDD(_dstName),
        equalTo(or(_link1DstIpBDD, _link2DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
    assertThat(vrfAcceptBDD(_srcName), equalTo(or(_link1SrcIpBDD, _link2SrcIpBDD)));
  }

  @Test
  public void testBDDTransitions_NodeAccept_Accept() {
    assertThat(bddTransition(_srcNodeAccept, Accept.INSTANCE), isOne());
    assertThat(bddTransition(_dstNodeAccept, Accept.INSTANCE), isOne());
  }

  @Test
  public void testBDDTransitions_PostInVrf_outEdges() {
    BDD nodeAccept = bddTransition(_srcPostInVrf, _srcNodeAccept);
    BDD nodeDropNoRoute = bddTransition(_srcPostInVrf, new NodeDropNoRoute(_srcName));
    BDD preOutVrf = bddTransition(_srcPostInVrf, _srcPreOutVrf);

    // test that out edges are mutually exclusive
    assertThat(nodeAccept, not(intersects(nodeDropNoRoute)));
    assertThat(nodeAccept, not(intersects(preOutVrf)));
    assertThat(nodeDropNoRoute, not(intersects(preOutVrf)));
  }

  @Test
  public void testBDDTransitions_PostInVrf_NodeAccept() {
    assertThat(
        bddTransition(_srcPostInVrf, new NodeAccept(_srcName)),
        equalTo(or(_link1SrcIpBDD, _link2SrcIpBDD)));
    assertThat(
        bddTransition(_dstPostInVrf, new NodeAccept(_dstName)),
        equalTo(or(_link1DstIpBDD, _link2DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
  }

  @Test
  public void testBDDTransitions_PostInVrf_PreOutVrf() {
    assertThat(
        bddTransition(_dstPostInVrf, _dstPreOutVrf), equalTo(or(_link1SrcIpBDD, _link2SrcIpBDD)));

    assertThat(
        bddTransition(_srcPostInVrf, _srcPreOutVrf),
        equalTo(or(_link1DstIpBDD, _link2DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
  }

  @Test
  public void testBDDTransitions_PreInInterface_NodeDropAclIn() {
    NodeDropAclIn dstDropAclIn = new NodeDropAclIn(_dstName);
    assertThat(bddTransition(_dstPreInInterface1, dstDropAclIn), equalTo(dstIpBDD(_dstIface2Ip)));
    assertThat(bddTransition(_dstPreInInterface2, dstDropAclIn), nullValue());
  }

  @Test
  public void testBDDTransitions_PreInInterface_PostInVrf() {
    // link1: not(_dstIface2Ip)
    assertThat(
        bddTransition(_dstPreInInterface1, _dstPostInVrf), equalTo(dstIpBDD(_dstIface2Ip).not()));
    // link2: universe
    assertThat(bddTransition(_dstPreInInterface2, _dstPostInVrf), isOne());
  }

  @Test
  public void testBDDTransitions_PreOutVrf_outEdges() {
    String link1SrcName = NET._link1Src.getName();
    String link2SrcName = NET._link2Src.getName();
    BDD nodeDropNullRoute = bddTransition(_srcPreOutVrf, new NodeDropNullRoute(_srcName));
    BDD nodeInterfaceNeighborUnreachable1 =
        bddTransition(_srcPreOutVrf, new NodeInterfaceNeighborUnreachable(_srcName, link1SrcName));
    BDD nodeInterfaceNeighborUnreachable2 =
        bddTransition(_srcPreOutVrf, new NodeInterfaceNeighborUnreachable(_srcName, link2SrcName));
    BDD preOutEdge1 = bddTransition(_srcPreOutVrf, _srcPreOutEdge1);
    BDD preOutEdge2 = bddTransition(_srcPreOutVrf, _srcPreOutEdge2);
    BDD postNatAclBDD = dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT);

    assertThat(nodeDropNullRoute, nullValue());

    assertThat(nodeInterfaceNeighborUnreachable1, equalTo(_link1SrcIpBDD));
    assertThat(nodeInterfaceNeighborUnreachable2, equalTo(_link2SrcIpBDD.and(postNatAclBDD)));

    assertThat(
        bddIps(preOutEdge1),
        containsInAnyOrder(_dstIface1Ip, _dstIface2Ip, NET._link1Dst.getAddress().getIp()));
    assertThat(
        bddIps(preOutEdge2), containsInAnyOrder(_dstIface2Ip, NET._link2Dst.getAddress().getIp()));

    // ECMP: _dstIface1Ip is routed out both edges
    assertThat(preOutEdge1.and(preOutEdge2), equalTo(dstIpBDD(_dstIface2Ip)));
  }

  @Test
  public void testBDDTransitions_PreOutVrf_NodeInterfaceNeighborUnreachable() {
    /*
     * These predicates include the IP address of the interface, which is technically wrong.
     * It doesn't matter because those addresses can't get to PreOutVrf from PostInVrf.
     */
    assertThat(
        bddTransition(
            _dstPreOutVrf, new NodeInterfaceNeighborUnreachable(_dstName, _dstIface1Name)),
        equalTo(_dstIface1IpBDD));
    assertThat(
        bddTransition(
            _dstPreOutVrf, new NodeInterfaceNeighborUnreachable(_dstName, _dstIface2Name)),
        equalTo(_dstIface2IpBDD));
    assertThat(
        bddTransition(_dstPreOutVrf, new NodeInterfaceNeighborUnreachable(_dstName, _link1DstName)),
        equalTo(_link1DstIpBDD));
    assertThat(
        bddTransition(_dstPreOutVrf, new NodeInterfaceNeighborUnreachable(_dstName, _link2DstName)),
        equalTo(_link2DstIpBDD));
  }

  @Test
  public void testBDDTransitions_PreOutVrf_PreOutEdge() {
    assertThat(
        bddTransition(_srcPreOutVrf, _srcPreOutEdge1),
        equalTo(or(_link1DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
    assertThat(
        bddTransition(_srcPreOutVrf, _srcPreOutEdge2),
        equalTo(or(_link2DstIpBDD, _dstIface2IpBDD)));

    assertThat(bddTransition(_dstPreOutVrf, _dstPreOutEdge1), equalTo(_link1SrcIpBDD));
    assertThat(bddTransition(_dstPreOutVrf, _dstPreOutEdge2), equalTo(_link2SrcIpBDD));
  }

  @Test
  public void testBDDTransitions_PreOutEdgePostNat_NodeDropAclOut() {
    assertThat(bddTransition(_dstPreOutEdgePostNat1, new NodeDropAclOut(_dstName)), nullValue());
    assertThat(bddTransition(_dstPreOutEdgePostNat2, new NodeDropAclOut(_dstName)), nullValue());
    assertThat(bddTransition(_srcPreOutEdgePostNat1, new NodeDropAclOut(_srcName)), nullValue());
    assertThat(
        bddTransition(_srcPreOutEdgePostNat2, new NodeDropAclOut(_srcName)),
        equalTo(dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT).not()));
  }

  @Test
  public void testBDDTransitions_PreOutEdgePostNat_PreInInterface() {
    assertThat(bddTransition(_dstPreOutEdgePostNat1, _srcPreInInterface1), isOne());
    assertThat(bddTransition(_dstPreOutEdgePostNat2, _srcPreInInterface2), isOne());
    assertThat(bddTransition(_srcPreOutEdgePostNat1, _dstPreInInterface1), isOne());
    assertThat(
        bddTransition(_srcPreOutEdgePostNat2, _dstPreInInterface2),
        equalTo(dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT)));
  }

  @Test
  public void testGraph_terminalStates() {
    Set<StateExpr> terminalStates = GRAPH.getLeafStates();
    assertThat(
        terminalStates,
        equalTo(ImmutableSet.of(Accept.INSTANCE, Drop.INSTANCE, NeighborUnreachable.INSTANCE)));
  }

  @Test
  public void testBDDNetworkGraph_sourceNat_match() {
    IpSpaceAssignment assignment =
        IpSpaceAssignment.builder()
            .assign(
                new InterfaceLocation(NET._srcNode.getName(), NET._link1Src.getName()),
                SOURCE_NAT_ACL_IP.toIpSpace())
            .build();

    BDDReachabilityAnalysis graph =
        GRAPH_FACTORY.bddReachabilityAnalysis(assignment, _dstIface2Ip.toIpSpace());

    BDD dstIpBDD = GRAPH_FACTORY.getIpSpaceToBDD().toBDD(_dstIface2Ip);
    BDD natPoolIpBDD = srcIpBDD(SOURCE_NAT_POOL_IP);
    BDD natAclIpBDD = srcIpBDD(SOURCE_NAT_ACL_IP);
    BDD postNatAclBDD = dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT);

    BDD srcNatAclBDD = BDDAcl.create(NET._link2SrcSourceNatAcl).getBdd();
    assertThat(srcNatAclBDD, equalTo(natAclIpBDD));

    BDD unNattedHeader = dstIpBDD.and(natAclIpBDD);
    BDD nattedHeader = dstIpBDD.and(natPoolIpBDD).and(postNatAclBDD);

    OriginateVrf originateVrf = new OriginateVrf(_srcName, Configuration.DEFAULT_VRF_NAME);
    PreOutEdgePostNat preOutEdge2PostNat =
        new PreOutEdgePostNat(_srcName, _link2SrcName, _dstName, _link2DstName);

    assertThat(graph.getNatRoots(), hasKey(preOutEdge2PostNat));
    Map<StateExpr, StateExpr> natRoots = graph.getNatRoots().get(preOutEdge2PostNat);
    assertThat(natRoots, hasKey(originateVrf));
    StateExpr logicalRoot = natRoots.get(originateVrf);

    /*
     * Before backward-propagation, it's difficult the see the multipath inconsistency.
     * Accept gets nattedHeader originating from logicalRoot, and Drop gets unNattedHeader
     * originating from originateVrf.
     */
    Map<StateExpr, Map<StateExpr, BDD>> reachableStates = graph.getReachableStates();
    assertThat(reachableStates.get(Accept.INSTANCE), hasEntry(logicalRoot, nattedHeader));
    assertThat(reachableStates.get(Drop.INSTANCE), hasEntry(originateVrf, unNattedHeader));

    /*
     * After backward-propagation, we can see the inconsistency more clearly. The sets of
     * packets that reach Accept and Drop from originateVrf overlap...
     */
    BDD unNattedHeaderWithPostNatAclConstraint = unNattedHeader.and(postNatAclBDD);
    Map<StateExpr, Map<StateExpr, BDD>> rootToLeafBDDs = graph.getRootToLeafBDDs();
    assertThat(
        rootToLeafBDDs,
        hasEntry(
            equalTo(originateVrf),
            hasEntry(Accept.INSTANCE, unNattedHeaderWithPostNatAclConstraint)));
    assertThat(
        rootToLeafBDDs, hasEntry(equalTo(originateVrf), hasEntry(Drop.INSTANCE, unNattedHeader)));

    /*
     * ... and we detect a violation for the intersection.
     */
    List<MultipathInconsistency> inconsistencies = graph.computeMultipathInconsistencies();
    assertThat(inconsistencies, hasSize(1));
    MultipathInconsistency inconsistency = inconsistencies.get(0);
    assertThat(inconsistency.getOriginateState(), equalTo(originateVrf));
    assertThat(
        inconsistency.getFinalStates(), equalTo(ImmutableSet.of(Accept.INSTANCE, Drop.INSTANCE)));
    assertThat(inconsistency.getBDD(), equalTo(unNattedHeaderWithPostNatAclConstraint));
  }

  @Test
  public void testBDDNetworkGraph_sourceNat_noMatch() {
    IpSpaceAssignment assignment =
        IpSpaceAssignment.builder()
            .assign(
                new InterfaceLocation(NET._srcNode.getName(), NET._link1Src.getName()),
                Ip.MAX.toIpSpace())
            .build();

    BDDReachabilityAnalysis graph =
        GRAPH_FACTORY.bddReachabilityAnalysis(assignment, _dstIface2Ip.toIpSpace());

    BDD dstIpBDD = GRAPH_FACTORY.getIpSpaceToBDD().toBDD(_dstIface2Ip);
    BDD srcIpBDD = srcIpBDD(Ip.MAX);
    BDD postNatAclBDD = dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT);

    OriginateVrf originateVrf = new OriginateVrf(_srcName, Configuration.DEFAULT_VRF_NAME);
    BDD preOutEdgePostNatLink2 =
        graph
            .getReachableStates()
            .get(new PreOutEdgePostNat(_srcName, _link2SrcName, _dstName, _link2DstName))
            .get(originateVrf);

    assertThat(preOutEdgePostNatLink2, equalTo(dstIpBDD.and(srcIpBDD)));
    List<MultipathInconsistency> inconsistencies = graph.computeMultipathInconsistencies();
    assertThat(inconsistencies, hasSize(1));
    MultipathInconsistency inconsistency = inconsistencies.get(0);
    assertThat(
        inconsistency.getFinalStates(), equalTo(ImmutableSet.of(Accept.INSTANCE, Drop.INSTANCE)));
    assertThat(inconsistency.getBDD(), equalTo(dstIpBDD.and(srcIpBDD).and(postNatAclBDD)));

    Flow flow = graph.multipathInconsistencyToFlow(inconsistency, FLOW_TAG);
    assertThat(flow, hasDstIp(_dstIface2Ip));
  }
}