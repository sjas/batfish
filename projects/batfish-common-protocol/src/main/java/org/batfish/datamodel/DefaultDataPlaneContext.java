package org.batfish.datamodel;

import com.google.common.collect.Table;
import com.google.common.graph.ValueGraph;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nonnull;

public class DefaultDataPlaneContext implements DataPlaneContext {

  private final DataPlane _dataPlane;
  private final Topology _topology;

  public DefaultDataPlaneContext(@Nonnull DataPlane dataPlane, @Nonnull Topology topology) {
    _dataPlane = dataPlane;
    _topology = topology;
  }

  @Override
  public Table<String, String, Set<BgpRoute>> getBgpRoutes(boolean multipath) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public ValueGraph<BgpPeerConfigId, BgpSessionProperties> getBgpTopology() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Map<String, Configuration> getConfigurations() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public DataPlane getDataPlane() {
    return _dataPlane;
  }

  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public ForwardingAnalysis getForwardingAnalysis() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Map<Ip, Set<String>> getIpOwners() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Map<Ip, Map<String, Set<String>>> getIpVrfOwners() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public SortedMap<String, SortedMap<String, Map<Prefix, Map<String, Set<String>>>>>
      getPrefixTracingInfoSummary() {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> getRibs() {
    return _ribs.get();
  }

  @Override
  public Topology getTopology() {
    return _topology;
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topology.getEdges();
  }
}
