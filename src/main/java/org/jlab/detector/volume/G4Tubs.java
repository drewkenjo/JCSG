package org.jlab.detector.volume;

import org.jlab.geometry.prim.Tube;

import eu.mihosoft.vrl.v3d.Primitive;

public class G4Tubs extends Geant4Basic {

	public G4Tubs( String name, double innerRadius, double outerRadius, double length, double startPhi, double deltaPhi )
	{
		super( new Tube(  ) );
	}

}
