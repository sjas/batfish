package org.batfish.datamodel;

public enum FlowDisposition {
  ACCEPTED,
  DELIVERED,
  DENIED_IN,
  DENIED_OUT,
  LOOP,
  NO_ROUTE,
  NULL_ROUTED
}
