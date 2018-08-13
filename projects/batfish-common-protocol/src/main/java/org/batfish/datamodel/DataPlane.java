package org.batfish.datamodel;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;

public interface DataPlane extends Serializable {

  /** SortedMapping: hostname -> vrfName -> bgpBestPathRibRoutes */
  SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> getBgpBestPathRibRoutes();

  /** SortedMapping: hostname -> vrfName -> bgpMultipathRibRoutes */
  SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> getBgpMultipathRibRoutes();

  /** SortedMapping: hostname -> vrfName -> mainRibRoutes */
  SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getMainRibRoutes();

  /** SortedMapping: hostname -> vrfName -> receivedBgpAdvertisements */
  SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>> getReceivedBgpAdvertisements();

  /** SortedMapping: hostname -> vrfName -> receivedBgpRoutes */
  SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> getReceivedBgpRoutes();

  /** SortedMapping: hostname -> vrfName -> receivedIsisL1Routes */
  SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> getReceivedIsisL1Routes();

  /** SortedMapping: hostname -> vrfName -> receivedIsisL2Routes */
  SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> getReceivedIsisL2Routes();

  /** SortedMapping: hostname -> vrfName -> receivedOspfExternalType1Routes */
  SortedMap<String, SortedMap<String, SortedSet<OspfExternalType1Route>>>
      getReceivedOspfExternalType1Routes();

  /** SortedMapping: hostname -> vrfName -> receivedOspfExternalType2Routes */
  SortedMap<String, SortedMap<String, SortedSet<OspfExternalType2Route>>>
      getReceivedOspfExternalType2Routes();

  /** SortedMapping: hostname -> vrfName -> sentBgpAdvertisements */
  SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>> getSentBgpAdvertisements();
}
