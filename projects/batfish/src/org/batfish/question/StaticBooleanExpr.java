package org.batfish.question;

import org.batfish.main.BatfishException;

public enum StaticBooleanExpr implements BooleanExpr {
   FALSE,
   TRUE;

   @Override
   public boolean evaluate(Environment environment) {
      switch (this) {
      case FALSE:
         return false;
      case TRUE:
         return true;
      default:
         throw new BatfishException("Invalid StaticBooleanExpr");
      }
   }

}
