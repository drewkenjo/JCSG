package org.jlab.detector.volume;

import org.jlab.detector.units.SystemOfUnits.Angle;
import org.jlab.detector.units.SystemOfUnits.Length;
import org.jlab.geometry.prim.Tube;

public class G4Tubs extends Geant4Basic {

	public G4Tubs( String name, double innerRadius, double outerRadius, double length, double startPhi, double deltaPhi )
	{
		super( new Tube( innerRadius, outerRadius, length, startPhi, deltaPhi ) );
		setName( name );
		setType("Tube");
		setDimensions( Length.value(innerRadius), Length.value(outerRadius), Length.value(length), Angle.value(startPhi), Angle.value(deltaPhi) );
	}

}
