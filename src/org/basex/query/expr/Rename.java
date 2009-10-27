package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import static org.basex.query.QueryTokens.*;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Item;
import org.basex.query.item.Nod;
import org.basex.query.item.Seq;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.iter.SeqIter;
import org.basex.query.up.primitives.RenamePrimitive;
import org.basex.query.util.Err;
import org.basex.util.Atts;

/**
 * Rename expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Lukas Kircher
 */
public final class Rename extends Update {
  /**
   * Constructor.
   * @param tg target expression
   * @param n new name expression
   */
  public Rename(final Expr tg, final Expr n) {
    super(tg, n);
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final Iter t = SeqIter.get(expr[0].iter(ctx));
    final Item i = t.next();

    // check target constraints
    if(i == null) Err.or(UPSEQEMP, this);
    if(t.size() != 1) Err.or(UPWRTRGTYP, this);

    CFrag ex = null;
    if(i.type == Type.ELM) {
      ex = new CElem(expr[1], new Expr[0], new Atts());
    } else if(i.type == Type.ATT) {
      ex = new CAttr(expr[1], new Expr[0], false);
    } else if(i.type == Type.PI) {
      ex = new CPI(expr[1], Seq.EMPTY);
    } else {
      Err.or(UPWRTRGTYP, this);
    }
    ctx.updates.addPrimitive(
        new RenamePrimitive((Nod) i, ex.atomic(ctx).qname().ln()));
    return Iter.EMPTY;
  }

  @Override
  public String toString() {
    return RENAME + "...";
  }
}
