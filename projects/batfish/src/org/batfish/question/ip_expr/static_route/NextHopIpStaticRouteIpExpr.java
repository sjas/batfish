package org.batfish.question.ip_expr.static_route;

import org.batfish.datamodel.Ip;
import org.batfish.datamodel.StaticRoute;
import org.batfish.question.Environment;
import org.batfish.question.static_route_expr.StaticRouteExpr;

public final class NextHopIpStaticRouteIpExpr extends StaticRouteIpExpr {

   public NextHopIpStaticRouteIpExpr(StaticRouteExpr caller) {
      super(caller);
   }

   @Override
   public Ip evaluate(Environment environment) {
      StaticRoute staticRoute = _caller.evaluate(environment);
      return staticRoute.getNextHopIp();
   }

}
