/*
 * Cassowary Incremental Constraint Solver
 * Original Smalltalk Implementation by Alan Borning
 * 
 * Java Implementation by:
 * Greg J. Badros
 * Erwin Bolwidt
 * 
 * (C) 1998, 1999 Greg J. Badros and Alan Borning
 * (C) Copyright 2012 Erwin Bolwidt
 * 
 * See the file LICENSE for legal details regarding this software
 */

package org.klomp.cassowary;

public class CLException extends RuntimeException {
    public CLException() {
        super("An error has occured in CL");
    }

    public CLException(String message) {
        super(message);
    }
}
