package org.batfish.question.prefix_expr.static_route;

import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.question.Environment;
import org.batfish.question.static_route_expr.StaticRouteExpr;

public final class PrefixStaticRoutePrefixExpr extends StaticRoutePrefixExpr {

   public PrefixStaticRoutePrefixExpr(StaticRouteExpr caller) {
      super(caller);
   }

   @Override
   public Prefix evaluate(Environment environment) {
      StaticRoute staticRoute = _caller.evaluate(environment);
      return staticRoute.getPrefix();
   }

}
