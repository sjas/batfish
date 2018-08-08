package org.batfish.datamodel;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public interface DataPlane extends Serializable {

  /** Mapping: hostname -> vrfName -> bgpBestPathRibRoutes */
  Map<String, Map<String, Set<BgpRoute>>> getBgpBestPathRibRoutes();

  /** Mapping: hostname -> vrfName -> bgpMultipathRibRoutes */
  Map<String, Map<String, Set<BgpRoute>>> getBgpMultipathRibRoutes();

  /** Mapping: hostname -> vrfName -> mainRibRoutes */
  Map<String, Map<String, Set<AbstractRoute>>> getMainRibRoutes();

  /** Mapping: hostname -> vrfName -> receivedBgpAdvertisements */
  Map<String, Map<String, Set<BgpAdvertisement>>> getReceivedBgpAdvertisements();

  /** Mapping: hostname -> vrfName -> receivedBgpRoutes */
  Map<String, Map<String, Set<BgpRoute>>> getReceivedBgpRoutes();

  /** Mapping: hostname -> vrfName -> receivedIsisL1Routes */
  Map<String, Map<String, Set<IsisRoute>>> getReceivedIsisL1Routes();

  /** Mapping: hostname -> vrfName -> receivedIsisL2Routes */
  Map<String, Map<String, Set<IsisRoute>>> getReceivedIsisL2Routes();

  /** Mapping: hostname -> vrfName -> receivedOspfExternalType1Routes */
  Map<String, Map<String, Set<OspfExternalType1Route>>> getReceivedOspfExternalType1Routes();

  /** Mapping: hostname -> vrfName -> receivedOspfExternalType2Routes */
  Map<String, Map<String, Set<OspfExternalType2Route>>> getReceivedOspfExternalType2Routes();

  /** Mapping: hostname -> vrfName -> sentBgpAdvertisements */
  Map<String, Map<String, Set<BgpAdvertisement>>> getSentBgpAdvertisements();
}
