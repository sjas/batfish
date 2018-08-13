package org.batfish.datamodel;

import com.google.common.collect.ImmutableSortedMap;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nonnull;

public class DataPlaneImpl implements DataPlane {

  public static class Builder {
    private SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> _bgpBestPathRibRoutes;

    private SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> _bgpMultipathRibRoutes;

    private SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> _mainRibRoutes;

    private SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
        _receivedBgpAdvertisements;

    private SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> _receivedBgpRoutes;

    private SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> _receivedIsisL1Routes;

    private SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> _receivedIsisL2Routes;

    private SortedMap<String, SortedMap<String, SortedSet<OspfExternalType1Route>>>
        _receivedOspfExternalType1Routes;

    private SortedMap<String, SortedMap<String, SortedSet<OspfExternalType2Route>>>
        _receivedOspfExternalType2Routes;

    private SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
        _sentBgpAdvertisements;

    private Builder() {
      _bgpBestPathRibRoutes = ImmutableSortedMap.of();
      _bgpMultipathRibRoutes = ImmutableSortedMap.of();
      _mainRibRoutes = ImmutableSortedMap.of();
      _receivedBgpAdvertisements = ImmutableSortedMap.of();
      _receivedBgpRoutes = ImmutableSortedMap.of();
      _receivedIsisL1Routes = ImmutableSortedMap.of();
      _receivedIsisL2Routes = ImmutableSortedMap.of();
      _receivedOspfExternalType1Routes = ImmutableSortedMap.of();
      _receivedOspfExternalType2Routes = ImmutableSortedMap.of();
      _sentBgpAdvertisements = ImmutableSortedMap.of();
    }

    public @Nonnull DataPlaneImpl build() {
      return new DataPlaneImpl(this);
    }

    public @Nonnull Builder setBgpBestPathRibRoutes(
        @Nonnull SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> bgpBestPathRibRoutes) {
      _bgpBestPathRibRoutes = bgpBestPathRibRoutes;
      return this;
    }

    public @Nonnull Builder setBgpMultipathRibRoutes(
        @Nonnull SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> bgpMultipathRibRoutes) {
      _bgpMultipathRibRoutes = bgpMultipathRibRoutes;
      return this;
    }

    public @Nonnull Builder setMainRibRoutes(
        @Nonnull SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> mainRibRoutes) {
      _mainRibRoutes = mainRibRoutes;
      return this;
    }

    public @Nonnull Builder setReceivedBgpAdvertisements(
        @Nonnull
            SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
                receivedBgpAdvertisements) {
      _receivedBgpAdvertisements = receivedBgpAdvertisements;
      return this;
    }

    public @Nonnull Builder setReceivedBgpRoutes(
        @Nonnull SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> receivedBgpRoutes) {
      _receivedBgpRoutes = receivedBgpRoutes;
      return this;
    }

    public @Nonnull Builder setReceivedIsisL1Routes(
        @Nonnull SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> receivedIsisL1Routes) {
      _receivedIsisL1Routes = receivedIsisL1Routes;
      return this;
    }

    public @Nonnull Builder setReceivedIsisL2Routes(
        @Nonnull SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> receivedIsisL2Routes) {
      _receivedIsisL2Routes = receivedIsisL2Routes;
      return this;
    }

    public @Nonnull Builder setReceivedOspfExternalType1Routes(
        @Nonnull
            SortedMap<String, SortedMap<String, SortedSet<OspfExternalType1Route>>>
                receivedOspfExternalType1Routes) {
      _receivedOspfExternalType1Routes = receivedOspfExternalType1Routes;
      return this;
    }

    public @Nonnull Builder setReceivedOspfExternalType2Routes(
        @Nonnull
            SortedMap<String, SortedMap<String, SortedSet<OspfExternalType2Route>>>
                receivedOspfExternalType2Routes) {
      _receivedOspfExternalType2Routes = receivedOspfExternalType2Routes;
      return this;
    }

    public @Nonnull Builder setSentBgpAdvertisements(
        @Nonnull
            SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
                sentBgpAdvertisements) {
      _sentBgpAdvertisements = sentBgpAdvertisements;
      return this;
    }
  }

  private static final long serialVersionUID = 1L;

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  private final SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> _bgpBestPathRibRoutes;

  private final SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> _bgpMultipathRibRoutes;

  private final SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> _mainRibRoutes;

  private final SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
      _receivedBgpAdvertisements;

  private final SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> _receivedBgpRoutes;

  private final SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> _receivedIsisL1Routes;

  private final SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> _receivedIsisL2Routes;

  private final SortedMap<String, SortedMap<String, SortedSet<OspfExternalType1Route>>>
      _receivedOspfExternalType1Routes;

  private final SortedMap<String, SortedMap<String, SortedSet<OspfExternalType2Route>>>
      _receivedOspfExternalType2Routes;

  private final SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
      _sentBgpAdvertisements;

  private DataPlaneImpl(Builder builder) {
    _bgpBestPathRibRoutes = builder._bgpBestPathRibRoutes;
    _bgpMultipathRibRoutes = builder._bgpMultipathRibRoutes;
    _mainRibRoutes = builder._mainRibRoutes;
    _receivedBgpAdvertisements = builder._receivedBgpAdvertisements;
    _receivedBgpRoutes = builder._receivedBgpRoutes;
    _receivedIsisL1Routes = builder._receivedIsisL1Routes;
    _receivedIsisL2Routes = builder._receivedIsisL2Routes;
    _receivedOspfExternalType1Routes = builder._receivedOspfExternalType1Routes;
    _receivedOspfExternalType2Routes = builder._receivedOspfExternalType2Routes;
    _sentBgpAdvertisements = builder._sentBgpAdvertisements;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> getBgpBestPathRibRoutes() {
    return _bgpBestPathRibRoutes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> getBgpMultipathRibRoutes() {
    return _bgpMultipathRibRoutes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getMainRibRoutes() {
    return _mainRibRoutes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
      getReceivedBgpAdvertisements() {
    return _receivedBgpAdvertisements;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<BgpRoute>>> getReceivedBgpRoutes() {
    return _receivedBgpRoutes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> getReceivedIsisL1Routes() {
    return _receivedIsisL1Routes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<IsisRoute>>> getReceivedIsisL2Routes() {
    return _receivedIsisL2Routes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<OspfExternalType1Route>>>
      getReceivedOspfExternalType1Routes() {
    return _receivedOspfExternalType1Routes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<OspfExternalType2Route>>>
      getReceivedOspfExternalType2Routes() {
    return _receivedOspfExternalType2Routes;
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<BgpAdvertisement>>>
      getSentBgpAdvertisements() {
    return _sentBgpAdvertisements;
  }
}
