package org.batfish.representation.juniper;

import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.PolicyMapClause;
import org.batfish.datamodel.PolicyMapMatchColorLine;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.MatchColor;
import org.batfish.main.Warnings;

public final class PsFromColor extends PsFrom {

   /**
    *
    */
   private static final long serialVersionUID = 1L;

   private final int _color;

   public PsFromColor(int color) {
      _color = color;
   }

   @Override
   public void applyTo(PolicyMapClause clause, PolicyStatement ps,
         JuniperConfiguration jc, Configuration c, Warnings warnings) {
      PolicyMapMatchColorLine line = new PolicyMapMatchColorLine(_color);
      clause.getMatchLines().add(line);
   }

   public int getColor() {
      return _color;
   }

   @Override
   public BooleanExpr toBooleanExpr(JuniperConfiguration jc, Configuration c,
         Warnings warnings) {
      return new MatchColor(_color);
   }

}
