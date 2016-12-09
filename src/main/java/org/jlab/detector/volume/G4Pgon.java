package org.jlab.detector.volume;

import org.jlab.detector.units.Measurement;
import org.jlab.detector.units.SystemOfUnits.Angle;
import org.jlab.geometry.prim.Pgon;

/**
 * @author pdavies
 */

public class G4Pgon extends Geant4Basic {

	public G4Pgon( String name, int nsides, double phi0, double dphi ) {
		super( new Pgon( nsides,  phi0,  dphi ));
		setName( name );
		setType("Polyhedra");
		setDimensions( new Measurement(nsides,""), Angle.value(phi0), Angle.value(dphi) );
	}

}
