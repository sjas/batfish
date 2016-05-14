package org.batfish.question.boolean_expr.static_route;

import org.batfish.datamodel.StaticRoute;
import org.batfish.question.Environment;
import org.batfish.question.static_route_expr.StaticRouteExpr;

public class HasNextHopInterfaceStaticRouteBooleanExpr extends
      StaticRouteBooleanExpr {

   public HasNextHopInterfaceStaticRouteBooleanExpr(StaticRouteExpr caller) {
      super(caller);
   }

   @Override
   public Boolean evaluate(Environment environment) {
      StaticRoute staticRoute = _caller.evaluate(environment);
      return staticRoute.getNextHopInterface() != null;
   }

}
