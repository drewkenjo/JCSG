package org.jlab.detector.geant4.v2.SVT;

import org.jlab.detector.geant4.v2.Geant4Factory;
import org.jlab.detector.geant4.v2.SVT.SVTConstants;
import org.jlab.detector.volume.G4Box;
import org.jlab.detector.volume.G4World;

public class SVTModule extends Geant4Factory
{
	public SVTModule()
	{
		motherVolume = new G4World( "module");
		
		for( int sensor = 0; sensor < SVTConstants.NSENSORS; sensor++ )
		{
			//if( VERBOSE ) System.out.println("   sp "+ sensor );
			G4Box sensorVol = new G4Box( "sensorPhysical"+"_sp"+ (sensor+1), SVTConstants.PHYSSENWID*0.1, SVTConstants.SILICONTHK*0.1, SVTConstants.PHYSSENLEN*0.1 );
			sensorVol.setMother( motherVolume.getMother() );
			sensorVol.setName( sensorVol.getName() + (sensor+1) ); // add switch for hybrid, intermediate and far labels?
			
			// module length = || DZ |  AL  | DZ |MG| DZ |  AL  | DZ |MG| DZ |  AL  | DZ ||
			//                  ^<-mid->^<-----stepLen----->^<-----stepLen----->^
			
			double deadZnLen   = SVTConstants.DEADZNLEN;
			double activeZnLen = SVTConstants.ACTIVESENLEN;
			double microGapLen = SVTConstants.MICROGAPLEN;
			double moduleLen   = SVTConstants.MODULELEN;
			double sensorZ = 0.0;
			double sensorPhysicalMidPos = deadZnLen + activeZnLen/2; // mid
			double stepLen = activeZnLen + deadZnLen + microGapLen + deadZnLen; // stepLen
			sensorZ = sensorPhysicalMidPos + sensor*stepLen - moduleLen/2;
			sensorVol.translate( 0.0, 0.0, sensorZ*0.1 );
			//sensorVol.setPosition( 0.0, 0.0, (SVTConstants.DEADZNLEN + sensor*( SVTConstants.ACTIVESENLEN + SVTConstants.DEADZNLEN + SVTConstants.MICROGAPLEN + SVTConstants.DEADZNLEN) - SVTConstants.MODULELEN/2.0 + SVTConstants.ACTIVESENLEN/2.0)*0.1 );
			
			//Util.moveChildrenToMother( sensorVol );
		}
	}
	
}
