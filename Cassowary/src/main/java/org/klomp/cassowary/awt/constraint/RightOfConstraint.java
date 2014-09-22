/*
 * RightOf relational constraint.  The target CC (the second) is to be 
 * right of the first (source) CC at all times, with respect to their bounding
 * boxes.
 *
 * $Id: RightOfConstraint.java,v 1.4 1999/12/16 02:10:00 gjb Exp $
 *
 */

package org.klomp.cassowary.awt.constraint;

import org.klomp.cassowary.CL;
import org.klomp.cassowary.CLInternalError;
import org.klomp.cassowary.ClSimplexSolver;
import org.klomp.cassowary.RequiredConstraintFailureException;
import org.klomp.cassowary.awt.component.ConstrComponent;
import org.klomp.cassowary.awt.component.SelPoint;
import org.klomp.cassowary.clconstraint.ClLinearInequality;

public class RightOfConstraint extends AdjacencyConstraint {

    public RightOfConstraint(ClSimplexSolver solver, ConstrComponent srcCC, ConstrComponent targetCC) {

        super(solver, srcCC, targetCC);
    }

    // Add constraints to solver. This entails establishing a constraint on
    // every SelPoint in the target to be right of the rightmost SP in the src.
    @Override
    public void addConstraints() {

        if (ccList.size() != 2) {
            System.out.println("RightOfConstr.addConstr: " + ccList.size() + " CC's, not required 2!");
            return;
        }
        ConstrComponent srcCC = ccList.get(0);
        ConstrComponent targetCC = ccList.get(1);

        int size = targetCC.selPoints.size();
        if (relConstrs.size() != size) {
            // Need to create new constraints
            if (!relConstrs.isEmpty()) {
                relConstrs.clear();
            }
            for (int i = 0; i < size; i++) {
                relConstrs.add(null);
            }
            for (int a = 0; a < targetCC.selPoints.size(); a++) {
                SelPoint sp = targetCC.selPoints.get(a);
                try {
                    ClLinearInequality cli = new ClLinearInequality(sp.X(), CL.GEQ, srcCC.rightSP.X());
                    relConstrs.set(a, cli);
                } catch (CLInternalError e) {
                    System.out.println("RightOfConstr.constructor: ExCLInternalError" + " on #" + a);
                    return;
                }

            }
        }

        for (int a = 0; a < relConstrs.size(); a++) {
            ClLinearInequality cli = relConstrs.get(a);
            try {
                if (cli != null)
                    solver.addConstraint(cli);
            } catch (CLInternalError e) {
                System.out.println("RightOfConstr.addConstr: ExCLInternalError" + " adding #" + a + " = " + cli);
            } catch (RequiredConstraintFailureException e) {
                System.out.println("RightOfConstr.addConstr: ExCLRequiredFailure " + "adding #" + a + " = " + cli);
            }
        }

    }

    // Method to convert constraint to a string
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("RightOfConstraint: ");

        if (ccList.size() != 2) {
            sb.append(" ILL-FORMED CONSTRAINT WITH " + ccList.size());
            sb.append(" INSTEAD OF 2 CC's");
        } else {
            ConstrComponent srcCC = ccList.get(0);
            ConstrComponent targetCC = ccList.get(1);
            sb.append("srcCC = " + srcCC);
            sb.append(", targetCC = " + targetCC);
        }
        return sb.toString();
    }

}

/*
 * $Log: RightOfConstraint.java,v $ Revision 1.4 1999/12/16 02:10:00 gjb * java/cda/Makefile.am: Put Constraint/*, Main/* files
 * into the distribution and build with them.
 * 
 * * java/demos/*.java: Move everything into the EDU.Washington.grad.gjb.cassowary_demos package.
 * 
 * * java/cda/classes/run.html, java/demos/quaddemo.htm: Fix nl convention, name class explicitly w/ package in front, w/o
 * trailing .class.
 * 
 * * java/cda/**: Move everything into the EDU.Washington.grad.noth.cda package.
 * 
 * Revision 1.3 1998/06/23 02:08:50 gjb Added import of cassowary package so that the cda doesn't need to be in the same package
 * as the solver.
 * 
 * Revision 1.2 1998/05/09 00:30:28 gjb Remove cr-s
 * 
 * Revision 1.1 1998/05/09 00:11:02 gjb Added
 * 
 * Revision 1.2 1998/04/20 09:51:59 Michael Moved draw method into base class
 * 
 * Revision 1.1 1998/04/02 06:58:12 Michael Initial check-in
 */
