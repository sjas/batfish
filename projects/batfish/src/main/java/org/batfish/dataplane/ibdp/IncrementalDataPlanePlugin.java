package org.batfish.dataplane.ibdp;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.ITracerouteEngine;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DataPlaneContext;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.IncrementalBdpAnswerElement;
import org.batfish.dataplane.TracerouteEngineImpl;

/** A batfish plugin that registers the Incremental Batfish Data Plane (ibdp) Engine. */
@AutoService(Plugin.class)
public class IncrementalDataPlanePlugin extends DataPlanePlugin {

  public static final String PLUGIN_NAME = "ibdp";

  private final Map<IncrementalDataPlaneContext, Map<Flow, Set<FlowTrace>>> _flowTraces;

  private IncrementalBdpEngine _engine;

  public IncrementalDataPlanePlugin() {
    _flowTraces = new HashMap<>();
  }

  @Override
  public ComputeDataPlaneResult computeDataPlane(boolean differentialContext) {
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Topology topology = _batfish.getEnvironmentTopology();
    return computeDataPlane(differentialContext, configurations, topology);
  }

  @Override
  public ComputeDataPlaneResult computeDataPlane(
      boolean differentialContext, Map<String, Configuration> configurations, Topology topology) {
    Set<BgpAdvertisement> externalAdverts = _batfish.loadExternalBgpAnnouncements(configurations);
    ComputeDataPlaneResult answer =
        _engine.computeDataPlane(differentialContext, configurations, topology, externalAdverts);
    double averageRoutes =
        ((IncrementalDataPlaneContext) answer._dataPlaneContext)
            .getNodes()
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr._mainRib.getRoutes().size())
            .average()
            .orElse(0.00d);
    _logger.infof(
        "Generated data-plane for testrig:%s; iterations:%s, avg entries per node:%.2f\n",
        _batfish.getTestrigName(),
        ((IncrementalBdpAnswerElement) answer._answerElement).getDependentRoutesIterations(),
        averageRoutes);
    return answer;
  }

  @Override
  protected void dataPlanePluginInitialize() {
    _engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(_batfish.getSettingsConfiguration()),
            _batfish.getLogger(),
            _batfish::newBatch);
  }

  @Override
  public Set<BgpAdvertisement> getAdvertisements() {
    DataPlane dp = _batfish.loadDataPlane();
    return Stream.concat(
            dp.getReceivedBgpAdvertisements()
                .values()
                .stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream),
            dp.getSentBgpAdvertisements()
                .values()
                .stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public ITracerouteEngine getTracerouteEngine() {
    return TracerouteEngineImpl.getInstance();
  }

  @Override
  public List<Flow> getHistoryFlows(DataPlaneContext dataPlaneContext) {
    IncrementalDataPlaneContext dpc = (IncrementalDataPlaneContext) dataPlaneContext;
    Map<Flow, Set<FlowTrace>> traces = _flowTraces.get(dpc);
    if (traces == null) {
      return ImmutableList.of();
    }
    return traces
        .entrySet()
        .stream()
        .flatMap(e -> Collections.nCopies(e.getValue().size(), e.getKey()).stream())
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<FlowTrace> getHistoryFlowTraces(DataPlaneContext dataPlaneContext) {
    IncrementalDataPlaneContext dpc = (IncrementalDataPlaneContext) dataPlaneContext;
    Map<Flow, Set<FlowTrace>> traces = _flowTraces.get(dpc);
    if (traces == null) {
      return ImmutableList.of();
    }
    return traces.values().stream().flatMap(Set::stream).collect(ImmutableList.toImmutableList());
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(
      DataPlaneContext dpc) {
    if (dpc instanceof IncrementalDataPlaneContext) {
      return IncrementalBdpEngine.getRoutes((IncrementalDataPlaneContext) dpc);
    } else {
      return dpc.getDataPlane().getMainRibRoutes();
    }
  }

  @Override
  public void processFlows(Set<Flow> flows, DataPlaneContext dataPlaneContext, boolean ignoreAcls) {
    _flowTraces.put(
        (IncrementalDataPlaneContext) dataPlaneContext,
        TracerouteEngineImpl.getInstance()
            .processFlows(dataPlaneContext, flows, dataPlaneContext.getFibs(), ignoreAcls));
  }

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }
}
