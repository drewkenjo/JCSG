package org.jlab.detector.geant4.v2.SVT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.jlab.detector.geant4.v2.Misc.Util;
import org.jlab.detector.volume.Geant4Basic;

/**
 * <h1> Geometry for the SVT </h1>
 * 
 * length unit: mm (3D Primitives), cm (Geant4Basic volume positions) <br>
 * angle unit: deg <br>
 * 
 * Conventions:
 * <ul>
 * <li> svt = four concentric regions / superlayers </li>
 * <li> region / superlayer = ring of a variable number of sectors </li>
 * <li> sector = pair of sensor  modules and backing structure, connected and stabilised by copper and peek supports </li>
 * <li> module = triplet of sensors </li>
 * <li> sensor = silicon with etched strips in active region </li>
 * <li> layer = plane of sensitive strips, spanning active regions of module </li>
 * <li> strip = sensitive line </li>
 * </ul>
 * 
 * @author pdavies
 * @version 0.2.6
 */
public class SVTVolumeFactory
{
	private final Geant4Basic motherVol = new G4Tubs("svt", 0, 1, 2, 0, 360 );
	private final HashMap< String, String > parameters = new LinkedHashMap<>(); // store core parameters from CCDB and build switches for GEMC's perl scripts
	private final HashMap< String, String > properties = new LinkedHashMap<>(); // author, email, date
	
	private int regionMin, regionMax, moduleMin, moduleMax, layerMin, layerMax;
	private int[] sectorMin, sectorMax;
	
	private boolean bShift = false; // switch to select whether alignment shifts are applied
	private double scaleT = 1.0, scaleR = 1.0;
	
	
	/**
	 * A switch to control whether debug information is displayed
	 * Default: false
	 */
	public boolean VERBOSE = false;
	
	/**
	 * A switch to control whether the sensor active and dead zones are made instead of the physical volume
	 * BUILDSENSORS=true must also be set.
	 * Default: false
	 */
	public boolean BUILDSENSORZONES = false;
	
	/**
	 * A switch to control weather the physical sensors are made inside the strip modules.
	 * Default: false
	 */
	public boolean BUILDSENSORS = false;
	
	/**
	 * A switch to make only the sensors, without the passive materials
	 * Default: true
	 */
	public boolean BUILDPASSIVES = true;
	
	/**
	 * A parameter to add space between volumes to prevent overlaps
	 * Default: 0.0
	 */
	public double VOLSPACER = 0.0;
	
	
	/**
	 * Returns one specified box volume.
	 * 
	 * @param aName a key in the MATERIALDIMENSIONS HashMap
	 * @return Geant4Basic a box positioned at the origin
	 * @throws IllegalArgumentException unknown material
	 */
	public static Geant4Basic createNamedBox( String aName ) throws IllegalArgumentException
	{
		double[] dims = SVTConstants.MATERIALDIMENSIONS.get( aName );
		if( dims == null )
			throw new IllegalArgumentException("unknown material: \""+ aName + "\"");
		return new Geant4Basic( aName, "Box", dims[0]*0.1, dims[1]*0.1, dims[2]*0.1 );
	}
	
	
	/**
	 * Constructs a new geometry factory for detector volumes.
	 * Please run {@code SVTConstants.connect() } first.
	 * 
	 * @param cp a DatabaseConstantProvider that has loaded the necessary tables
	 * @param applyAlignmentShiftsFromCcdb a switch to set whether the alignment shifts from CCDB will be applied
	 */
	public SVTVolumeFactory( ConstantProvider cp, boolean applyAlignmentShiftsFromCcdb )
	{
		SVTConstants.load( cp );
		setApplyAlignmentShifts( applyAlignmentShiftsFromCcdb );
		if( bShift == true ){ SVTConstants.loadAlignmentShifts( cp ); }
		
		sectorMin = new int[SVTConstants.NREGIONS];
		sectorMax = new int[SVTConstants.NREGIONS];
		
		// default behaviour
		setRange( 1, SVTConstants.NREGIONS, new int[]{ 1, 1, 1, 1 }, SVTConstants.NSECTORS, 1, SVTConstants.NMODULES ); // all regions, sectors, and modules
		
		Geant4Basic top = new Geant4Basic("none", "Box", 0,0,0 );
		motherVol.setMother( top );
	}
	
	
	/**
	 * Populates the HashMaps with constants.
	 */
	public void putParameters()
	{
		System.out.println("factory: populating HashMap with parameters");
		
		properties.put("unit_length", "mm");
		properties.put("unit_angle", "deg");
		properties.put("author", "P. Davies");
		properties.put("email", "pdavies@jlab.org");
		properties.put("date", "7/15/16");
		
		parameters.put("nregions", Integer.toString(SVTConstants.NREGIONS) );
		
		for( int r = 0; r < SVTConstants.NREGIONS; r++)
		{
			parameters.put("nsectors_r"+(r+1), Integer.toString( SVTConstants.NSECTORS[r] ) );
			//parameters.put("z_start_r"+(r+1),  Double.toString(  SVTConstants.Z0ACTIVE[r] ) );
		}
		parameters.put("nmodules", Integer.toString(SVTConstants.NMODULES) );
		parameters.put("nsensors", Integer.toString(SVTConstants.NSENSORS) );
		
		parameters.put("bsensorzones", Integer.toString(BUILDSENSORZONES ? 1 : 0 ) );
		parameters.put("bsensors", Integer.toString(BUILDSENSORS ? 1 : 0 ) );
		
		//parameters.put("nlayers", Integer.toString(SVTConstants.NLAYERS) );
		//parameters.put("ntotalsectors", Integer.toString(SVTConstants.NTOTALSECTORS) );
		//parameters.put("silicon_thk", Double.toString(SVTConstants.SILICONTHK) );
		// ...
		
		/*for( Map.Entry< String, String > entry : parameters.entrySet() )
		{
			System.out.println( entry.getKey() +" "+ entry.getValue() );
		}
		System.exit(0);*/
	}
	
	
	/**
	 * Generates the geometry using the current range and stores it in the mother volume. 
	 */
	public void makeVolumes()
	{
		System.out.println("factory: generating geometry with the following parameters");
		
		if( bShift )
		{
			System.out.println("  variation: shifted" );
			if( !(scaleT - 1.0 < 1.0E-3 && scaleR - 1.0 < 1.0E-3) ){ System.out.println("  scale(T,R): "+ scaleT + " " + scaleR ); }
		}
		else
		{
			System.out.println("  variation: ideal");
		}
		System.out.println( "  "+showRange() );
		System.out.println("build passive materials ? "+ BUILDPASSIVES );
		System.out.println("include physical sensors ? "+ BUILDSENSORS );
		if( BUILDSENSORS ) System.out.println("include sensor active and dead zones ? "+ BUILDSENSORZONES );
		
		//double sumZ0Active = 0;
		
		for( int region = regionMin-1; region < regionMax; region++ ) // NREGIONS
		{
			if( VERBOSE ) System.out.println("r "+region);
			
			Geant4Basic regionVol = createRegion( region );
			regionVol.setMother( motherVol );
			regionVol.setName( regionVol.getName() + (region+1) );
			Util.appendChildrenName( regionVol, "_r"+ (region+1) );
			
			double zStartPhysical = SVTConstants.Z0ACTIVE[region] - SVTConstants.DEADZNLEN; // Cu edge of hybrid sensor's physical volume
			regionVol.setPosition( 0, 0, (zStartPhysical - SVTConstants.FIDORIGINZ)*0.1 + regionVol.getParameters()[2]/2 ); // central dowel hole
			
			//regionVol.setRotation("xyz", 0.0, 0.0, -Math.toRadians(90.0) ); // add a shift to test Util.replaceChildrenMother
			//Util.moveChildrenToMother( regionVol );
			
			//sumZ0Active += SVTConstants.Z0ACTIVE[region];
			
			if( bShift )
			{
				for( int sector = sectorMin[region]-1; sector < sectorMax[region]; sector++ )
				{
					
					Geant4Basic sectorVol = regionVol.getChildren().get( sector );
					
					//System.out.println("N "+sectorVol.gemcString() );
					Point3D[] fidPos3Ds = SVTAlignmentFactory.getIdealFiducials( region, sector );
					Triangle3D fidTri3D = new Triangle3D( fidPos3Ds[0], fidPos3Ds[1], fidPos3Ds[2] );
					
					//System.out.println("rs "+ convertRegionSector2SvtIndex( aRegion, sector ));
					double[] shift = SVTConstants.getDataAlignmentSectorShift()[SVTConstants.convertRegionSector2Index( region, sector )].clone();
					
					if( VERBOSE )
					{
						Vector3D vec = new Vector3D( shift[3], shift[4], shift[5] ).asUnit();
						vec.scale( (shift[6] == 0) ? 0 : 10 ); // length in mm, but only if non-zero angle
						//vec.show();
						
						Geant4Basic vecVol = Util.createArrow("rotAxis"+sector, vec, 2.0, 1.0, true, true, false );
						vecVol.setPosition( fidTri3D.center().x()*0.1, fidTri3D.center().y()*0.1, fidTri3D.center().z()*0.1 );
						vecVol.setMother( regionVol );
						//System.out.println( vecVol.gemcString() );
					}
					
					int n = 1; // number of steps to show (n=1 for final step only)
					double d = shift[6]/n;
					for( int i = 1; i < n; i++ )
					{
						//System.out.println( "vol "+ i );
						Geant4Basic stepVol = Util.clone( sectorVol );
						stepVol.setMother(regionVol);
						Util.appendName( stepVol, "_"+i );
						shift[6] = i*d;
						AlignmentFactory.applyShift( stepVol, shift, fidTri3D.center(), scaleT, scaleR );
						//System.out.println("  "+stepVol.gemcString() );
						//for( int j = 0; j < stepVol.getChildren().size(); j++ )
							//System.out.println( stepVol.getChildren().get(j).gemcString() );
					}
					
					AlignmentFactory.applyShift( sectorVol, SVTConstants.getDataAlignmentSectorShift()[SVTConstants.convertRegionSector2Index( region, sector )], fidTri3D.center(), scaleT, scaleR );
					//System.out.println("S "+sectorVol.gemcString() );
				}
			}
		}
		
		//double meanZ0Active = sumZ0Active/(regionMax - regionMin + 1);
		
		motherVol.setParameters( 0, SVTConstants.LAYERRADIUS[regionMax-1][1]*0.1, (SVTConstants.SECTORLEN*1.5)*0.1, 0, 360 );
	}
	
	
	/**
	 * Returns one region, containing the required number of sector modules.
	 * 
	 * @param aRegion an index starting from 0
	 * @return Geant4Basic a dummy volume positioned at the origin
	 * @throws IllegalArgumentException index out of bounds
	 */
	public Geant4Basic createRegion( int aRegion ) throws IllegalArgumentException
	{
		if( aRegion < 0 || aRegion > SVTConstants.NREGIONS-1 ){ throw new IllegalArgumentException("region out of bounds"); }
		
		double rcen = 0.5*(SVTConstants.LAYERRADIUS[aRegion][1] + SVTConstants.LAYERRADIUS[aRegion][0]);
		double rthk = 0.5*(SVTConstants.LAYERRADIUS[aRegion][1] - SVTConstants.LAYERRADIUS[aRegion][0])*3; // scale factor to encompass the entire volume of the sectors
		double rmin = rcen - rthk;
		double rmax = rcen + rthk;
		double zlen = SVTConstants.SECTORLEN; // same length as dummy sector volume
		
		if( VERBOSE )
		{
			System.out.println("\nregion="+ aRegion );
			System.out.println("rcen="+ rcen );
			System.out.println("rthk="+ rthk );
			System.out.println("rmin="+ rmin );
			System.out.println("rmax="+ rmax );
			System.out.println("len="+ zlen );
		}
		
		Geant4Basic regionVol = new Geant4Basic("region", "Tube", rmin*0.1, rmax*0.1, zlen*0.1, 0, 360 );
		
		// create faraday cage here?
		
		for( int sector = sectorMin[aRegion]-1; sector < sectorMax[aRegion]; sector++ ) // NSECTORS[region]
		{
			if( VERBOSE ) System.out.println(" s "+sector);
			Geant4Basic sectorVol = createSector();
			sectorVol.setMother( regionVol );
			
			sectorVol.setName( sectorVol.getName() + (sector+1) ); // append name
			Util.appendChildrenName( sectorVol, "_s"+ (sector+1) ); // append tag to end of name (GdmlFile's material replacement search fails if it's prepended)
			
			double phi = -2.0*Math.PI/SVTConstants.NSECTORS[aRegion]*sector + SVTConstants.PHI0; // module rotation about target / origin
			double psi = phi - SVTConstants.SECTOR0 - Math.PI; // module rotation about centre of geometry, -180 deg to set zero volume rotation for sector 1
			
			Transformation3D rotatePhi = new Transformation3D();
			rotatePhi.rotateZ( phi );
			
			//double heatSinkThk = SVTConstants.MATERIALDIMENSIONS.get("heatSink")[1]; // 2.880 mm
			//double fiducialRadius = SVTConstants.SUPPORTRADIUS[aRegion] + heatSinkThk;
			
			//Point3D pos = new Point3D( fiducialRadius - heatSinkThk + sectorVol.getParameters()[1]/2, 0, zStartPhysical - SVTConstants.FIDORIGINZ + sectorVol.getParameters()[2]/2 );
			Point3D pos = new Point3D( rcen, 0.0, 0.0 );
			rotatePhi.apply( pos );
			sectorVol.setPosition( pos.x()*0.1, pos.y()*0.1, pos.z()*0.1 );
			sectorVol.setRotation("xyz", 0.0, 0.0, -psi ); // change of sign for active/alibi -> passive/alias rotation
			//Util.moveChildrenToMother( sectorVol );
		}
		
		return regionVol;
	}
	
	
	/**
	 * Returns one sector module, containing a pair of sensor modules and backing structure.
	 * 
	 * @return Geant4Basic a dummy volume positioned in the lab frame
	 */
	public Geant4Basic createSector()
	{
		double heatSinkThk = SVTConstants.MATERIALDIMENSIONS.get("heatSink")[1];
		double rohacellThk = SVTConstants.MATERIALDIMENSIONS.get("rohacell")[1];
		double carbonFiberThk = SVTConstants.MATERIALDIMENSIONS.get("carbonFiber")[1];
		double busCableThk = SVTConstants.MATERIALDIMENSIONS.get("busCable")[1];
		//double epoxyThk = SVTConstants.MATERIALDIMENSIONS.get("epoxyMajor")[1];
		double pitchAdaptorThk = SVTConstants.MATERIALDIMENSIONS.get("pitchAdaptor")[1];
		
		double heatSinkLen = SVTConstants.MATERIALDIMENSIONS.get("heatSink")[2];
		double rohacellLen = SVTConstants.MATERIALDIMENSIONS.get("rohacell")[2];
		double heatSinkRidgeLen = SVTConstants.MATERIALDIMENSIONS.get("heatSinkRidge")[2];
		
		double rohacellZStart = 59.730; // CuEnd from fidOriginZ
		double heatSinkCuZStart = 5.60; // CuStart from fidOriginZ
		double carbonFiberZStart = 1.0; // from CuEnd of heatSinkRidge
		double pitchAdaptorZEnd = 61.43; // from fidOriginZ
		double pcBoardZEnd = 57.13; // from fidOriginZ
		
		double wid = SVTConstants.MODULEWID;
		double thk = SVTConstants.LAYERGAPTHK + 2*SVTConstants.SILICONTHK;
		double len = SVTConstants.SECTORLEN;
		
		Geant4Basic sectorVol = new Geant4Basic("sector", "Box", wid*0.1, thk*0.1, len*0.1 );
		
		// position components relative to fiducial layer
		
		// for each side (U,V)
		// 		create sensor module
		// 		create passive materials (carbon fibre, bus cable, epoxy)
		
		if( BUILDPASSIVES )
		{
			//Geant4Basic sectorBall = new Geant4Basic("sectorBall", "Orb", 0.075 ); // cm
			//sectorBall.setMother( sectorVol );
			
			Geant4Basic heatSinkVol = createHeatSink();
			heatSinkVol.setMother( sectorVol );
			heatSinkVol.setPosition( 0.0, -heatSinkThk/2*0.1, -heatSinkCuZStart*0.1 + heatSinkLen/2*0.1 );
			//Util.moveChildrenToMother( heatSinkVol );
			
			Geant4Basic rohacellVol = createNamedBox("rohacell");
			rohacellVol.setMother( sectorVol );
			rohacellVol.setPosition( 0.0, -rohacellThk/2*0.1, rohacellZStart*0.1 + rohacellLen/2*0.1 );
		}
		
		for( int module = moduleMin-1; module < moduleMax; module++ ) // NMODULES
		{
			if( VERBOSE ) System.out.println("  m "+ module );
			Geant4Basic moduleVol = createModule();
			moduleVol.setMother( sectorVol );
			moduleVol.setName( moduleVol.getName() + (module+1) );
			Util.appendChildrenName( moduleVol, "_m"+ (module+1) );
			
			double moduleY = 0.0;
			
			switch( module ) 
			{
			case 0: // U = inner
				moduleY = 0.0 + SVTConstants.PASSIVETHK + SVTConstants.SILICONTHK/2;
				break;
				
			case 1: // V = outer
				moduleY = 0.0 - rohacellThk - SVTConstants.PASSIVETHK - SVTConstants.SILICONTHK/2;
				break;
			}
			
			moduleVol.setPosition( 0.0, moduleY*0.1, (SVTConstants.FIDORIGINZ + SVTConstants.MODULELEN/2)*0.1 );
			//Util.moveChildrenToMother( moduleVol );
			
			if( BUILDPASSIVES )
			{
				Geant4Basic carbonFiberVol = createCarbonFiber();
				carbonFiberVol.setMother( sectorVol );
				carbonFiberVol.setName( carbonFiberVol.getName() + (module+1) );
				Util.appendChildrenName( carbonFiberVol, "_m"+ (module+1) );
				
				Geant4Basic busCableVol = createBusCable();
				busCableVol.setMother( sectorVol );
				busCableVol.setName( busCableVol.getName() + (module+1) );
				Util.appendChildrenName( busCableVol, "_m"+ (module+1) );
				
				Geant4Basic pitchAdaptorVol = createNamedBox("pitchAdaptor");
				pitchAdaptorVol.setMother( sectorVol );
				pitchAdaptorVol.setName( pitchAdaptorVol.getName() + (module+1) );
				Util.appendChildrenName( pitchAdaptorVol, "_m"+ (module+1) );
				
				Geant4Basic pcBoardAndChipsVol = createPcBoardAndChips();
				pcBoardAndChipsVol.setMother( sectorVol );
				pcBoardAndChipsVol.setName( pcBoardAndChipsVol.getName() + (module+1) );
				Util.appendChildrenName( pcBoardAndChipsVol, "_m"+ (module+1) );
				
				double carbonFiberY = 0.0;
				double busCableY = 0.0;
				double pitchAdaptorY = 0.0;
				double pcBoardY = 0.0;
				
				switch( module ) 
				{
				case 0: // U = inner
					carbonFiberY  = 0.0 + carbonFiberThk/2;
					busCableY     = 0.0 + carbonFiberThk + busCableThk/2;
					pitchAdaptorY = 0.0 + SVTConstants.PASSIVETHK + pitchAdaptorThk/2;
					pcBoardY      = 0.0 + SVTConstants.PASSIVETHK + pcBoardAndChipsVol.getParameters()[1]/2*10;
					break;
					
				case 1: // V = outer
					carbonFiberY  = 0.0 - rohacellThk - carbonFiberThk/2;
					busCableY     = 0.0 - rohacellThk - carbonFiberThk - busCableThk/2;
					pitchAdaptorY = 0.0 - rohacellThk - SVTConstants.PASSIVETHK - pitchAdaptorThk/2;
					pcBoardY      = 0.0 - rohacellThk - SVTConstants.PASSIVETHK - pcBoardAndChipsVol.getParameters()[1]/2*10;
					pcBoardAndChipsVol.setRotation("xyz", 0.0, 0.0, -Math.PI );
					break;
				}
			
				carbonFiberVol.setPosition(  0.0, carbonFiberY*0.1,  (0.0 - heatSinkCuZStart + heatSinkRidgeLen + carbonFiberZStart)*0.1 + carbonFiberVol.getParameters()[2]/2 );
				busCableVol.setPosition(     0.0, busCableY*0.1,     (0.0 - heatSinkCuZStart + heatSinkRidgeLen + carbonFiberZStart)*0.1 + busCableVol.getParameters()[2]/2 ); // same Z position as carbonFiber
				pitchAdaptorVol.setPosition( 0.0, pitchAdaptorY*0.1, pitchAdaptorZEnd*0.1 - pitchAdaptorVol.getParameters()[2]/2 );
				pcBoardAndChipsVol.setPosition(      0.0, pcBoardY*0.1,      pcBoardZEnd*0.1 - pcBoardAndChipsVol.getParameters()[2]/2 );
			}
			
			/*for( int kapton = 0; kapton < 1; kapton++ ) // left, right
			{
				
			}*/
		}
		
		for( Geant4Basic child : sectorVol.getChildren() )
			Util.shiftPosition( child, 0.0, rohacellThk/2*0.1, -sectorVol.getParameters()[2]/2 );
	
		return sectorVol;
	}
	
	
	/**
	 * Returns one sensor module, containing 3 physical sensors.
	 * 
	 * @return Geant4Basic a volume positioned at the origin
	 */
	public Geant4Basic createModule()
	{		
		Geant4Basic moduleVol = new Geant4Basic( "module", "Box", SVTConstants.PHYSSENWID*0.1, SVTConstants.SILICONTHK*0.1, SVTConstants.MODULELEN*0.1 );
		
		if( BUILDSENSORS ) // gemc handles the physical sensors itself in the hit process algorithm
		{
			for( int sensor = 0; sensor < SVTConstants.NSENSORS; sensor++ )
			{
				if( VERBOSE ) System.out.println("   sp "+ sensor );
				Geant4Basic sensorVol = createSensorPhysical(); // includes active and dead zones as children
				sensorVol.setMother( moduleVol );
				sensorVol.setName( sensorVol.getName() + (sensor+1) ); // add switch for hybrid, intermediate and far labels?
				Util.appendChildrenName( sensorVol, "_sp"+ (sensor+1) );
				
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
				sensorVol.setPosition( 0.0, 0.0, sensorZ*0.1 );
				//sensorVol.setPosition( 0.0, 0.0, (SVTConstants.DEADZNLEN + sensor*( SVTConstants.ACTIVESENLEN + SVTConstants.DEADZNLEN + SVTConstants.MICROGAPLEN + SVTConstants.DEADZNLEN) - SVTConstants.MODULELEN/2.0 + SVTConstants.ACTIVESENLEN/2.0)*0.1 );
				
				Util.moveChildrenToMother( sensorVol );
			}
		}
		
		return moduleVol;
	}
	
	
	/**
	 * Returns one physical sensor, containing active zone and dead zones.
	 * 
	 * @return Geant4Basic a volume positioned at the origin
	 */
	public Geant4Basic createSensorPhysical()
	{		
		Geant4Basic senPhysicalVol = new Geant4Basic( "sensorPhysical", "Box", SVTConstants.PHYSSENWID*0.1, SVTConstants.SILICONTHK*0.1, SVTConstants.PHYSSENLEN*0.1 );
		
		if( BUILDSENSORZONES ) // gemc handles the active and dead zones itself in the hit process algorithm
		{
			Geant4Basic senActiveVol = createSensorActive();
			senActiveVol.setMother( senPhysicalVol );
			
			// physical sensor length = || DZ |  AL  | DZ || // dead zones either side of active zone
			//
			// + - - - - - - - - + - +
			// |           l1        |
			// | - + - - - - - - + - +
			// | w2| active zone | w3|
			// | - + - - - - - - + - +
			// |           l0        |
			// + - + - - - - - - + - +
			//
			// length, width
			
			List<Geant4Basic> deadZnVols = new ArrayList<>();
			for( int deadZn = 0; deadZn < 4; deadZn++ )
			{
				Geant4Basic deadZnVol = null;
				switch( deadZn )
				{
				case 0:
					deadZnVol = createDeadZone("l");
					deadZnVol.setPosition( -(SVTConstants.ACTIVESENWID + SVTConstants.DEADZNWID + VOLSPACER)/2*0.1, 0.0, 0.0 );
					break;
				case 1:
					deadZnVol = createDeadZone("l");
					deadZnVol.setPosition(  (SVTConstants.ACTIVESENWID + SVTConstants.DEADZNWID + VOLSPACER)/2*0.1, 0.0, 0.0 );
					break;
				case 2:
					deadZnVol = createDeadZone("w");
					deadZnVol.setPosition( 0.0, 0.0, -(SVTConstants.ACTIVESENLEN + SVTConstants.DEADZNLEN + VOLSPACER)/2*0.1 );
					break;
				case 3:
					deadZnVol = createDeadZone("w");
					deadZnVol.setPosition( 0.0, 0.0,  (SVTConstants.ACTIVESENLEN + SVTConstants.DEADZNLEN + VOLSPACER)/2*0.1 );
					break;
				}
				
				deadZnVol.setName( deadZnVol.getName() + (deadZn+1) );
				deadZnVol.setMother(senPhysicalVol);
				deadZnVols.add( deadZnVol );
			}
		}
		
		return senPhysicalVol;
	}
	
	
	/**
	 * Returns one active zone of a sensor.
	 * 
	 * @return Geant4Basic a volume positioned at the origin
	 */
	public Geant4Basic createSensorActive( )
	{	
		return new Geant4Basic( "sensorActive", "Box", SVTConstants.ACTIVESENWID*0.1, SVTConstants.SILICONTHK*0.1, SVTConstants.ACTIVESENLEN*0.1 );
	}
	
	
	/**
	 * Returns one dead zone of a sensor.
	 * 
	 * @param aType one of two types of dead zone, extending along the length "l" or width "w" of the sensor
	 * @return Geant4Basic a volume positioned relative to a physical sensor
	 * @throws IllegalArgumentException unknown type
	 */
	public Geant4Basic createDeadZone( String aType ) throws IllegalArgumentException
	{
		Geant4Basic deadZnVol = null;
		
		switch( aType )
		{
		case "l":
			deadZnVol = new Geant4Basic( "deadZoneLen", "Box", SVTConstants.DEADZNWID*0.1, SVTConstants.SILICONTHK*0.1, (SVTConstants.ACTIVESENLEN + 2*SVTConstants.DEADZNLEN)*0.1 );
			break;
		case "w":
			deadZnVol = new Geant4Basic( "deadZoneWid", "Box", SVTConstants.ACTIVESENWID*0.1, SVTConstants.SILICONTHK*0.1, SVTConstants.DEADZNLEN*0.1 );
			break;
		default:
			throw new IllegalArgumentException("unknown dead zone type: "+ aType );
		}
		return deadZnVol;
	}
	
	
	
	public Geant4Basic createHeatSink()
	{
		Geant4Basic mainVol = createNamedBox("heatSink");
		
		Geant4Basic cuVol = createNamedBox("heatSinkCu");
		cuVol.setMother( mainVol );
		
		Geant4Basic ridgeVol = createNamedBox("heatSinkRidge"); // small protruding ridge
		ridgeVol.setMother( mainVol );
		
		cuVol.setPosition(    0.0, ridgeVol.getParameters()[1]/2, 0.0 );
		ridgeVol.setPosition( 0.0, ridgeVol.getParameters()[1] + cuVol.getParameters()[1]/2, ridgeVol.getParameters()[2]/2 - cuVol.getParameters()[2]/2 );
		
		return mainVol;
	}
	
	
	
	public Geant4Basic createCarbonFiber()
	{
		Geant4Basic mainVol = createNamedBox("carbonFiber");
		
		Geant4Basic cuVol = createNamedBox("carbonFiberCu");
		cuVol.setMother( mainVol );
		
		Geant4Basic pkVol = createNamedBox("carbonFiberPk");
		pkVol.setMother( mainVol );
		
		cuVol.setPosition( 0.0, 0.0, cuVol.getParameters()[2]/2 - mainVol.getParameters()[2]/2 );
		pkVol.setPosition( 0.0, 0.0, pkVol.getParameters()[2]/2 - mainVol.getParameters()[2]/2 + cuVol.getParameters()[2] );
		
		//     + - - +
		// + - +     |
		// |Cu | Pk  |
		// + - +     |
		//     + - - +
		
		return mainVol;
	}

	
	
	public Geant4Basic createBusCable()
	{
		Geant4Basic mainVol = createNamedBox("busCable");
		
		Geant4Basic cuVol = createNamedBox("busCableCu");
		cuVol.setMother( mainVol );
		
		Geant4Basic pkVol = createNamedBox("busCablePk");
		pkVol.setMother( mainVol );
		
		cuVol.setPosition( 0.0, 0.0, cuVol.getParameters()[2]/2 - mainVol.getParameters()[2]/2 );
		pkVol.setPosition( 0.0, 0.0, pkVol.getParameters()[2]/2 - mainVol.getParameters()[2]/2 + cuVol.getParameters()[2] );
		
		return mainVol;
	}
	
	
	
	public Geant4Basic createPcBoardAndChips()
	{
		Geant4Basic mainVol = createNamedBox("pcBoard");
		
		Geant4Basic pcBoardVol = createNamedBox("pcBoard");
		pcBoardVol.setMother( mainVol );
		
		Geant4Basic chipLVol = createNamedBox("chip");
		chipLVol.setMother( mainVol );
		chipLVol.setName("chipL");
		
		Geant4Basic chipRVol = createNamedBox("chip");
		chipRVol.setMother( mainVol );
		chipRVol.setName("chipR");
		
		double[] pars = mainVol.getParameters();
		mainVol.setParameters( pars[0], pars[1] + chipLVol.getParameters()[1], pars[2] );
		mainVol.setName("pcBoardAndChips");
		
		double chipX = 13.74 - 7.50/2;
		double chipZ = pcBoardVol.getParameters()[2]/2*10 - 17.99 + 17.45 - chipLVol.getParameters()[2]/2*10;
				
		pcBoardVol.setPosition( 0.0, -pcBoardVol.getParameters()[1]/2, 0.0 );
		chipLVol.setPosition(  chipX*0.1, chipLVol.getParameters()[1]/2, chipZ*0.1 );
		chipRVol.setPosition( -chipX*0.1, chipRVol.getParameters()[1]/2, chipZ*0.1 );
		
		return mainVol;
	}
	
	
	/**
	 * Returns the mother volume to export the geometry to, for example, a GDML file.
	 * 
	 * @return Geant4Basic the mother volume
	 */
	public Geant4Basic getMotherVolume()
	{
		return motherVol;
	}
	
	
	/**
	 * Returns a value from the Properties HashMap.
	 * 
	 * @param aName name of a key
	 * @return String the value associated with the given key, or "none" if the key does not exist
	 */
	public String getProperty( String aName )
	{
		return properties.containsKey( aName ) ? properties.get( aName ) : "none";
	}
	
	
	/**
	 * Returns a value from the Parameters HashMap.
	 * 
	 * @param aName name of a key
	 * @return String the value associated with the given key, or "none" if the key does not exist.
	 */
	public String getParameter( String aName )
	{
		return parameters.containsKey( aName ) ? parameters.get( aName ) : "none";
	}
	
	
	/**
	 * Returns the "Parameters" HashMap. 
	 * Used by GEMC to interface with CCDB.
	 * 
	 * @return HashMap a HashMap containing named constants and core parameters.
	 */
	public HashMap<String, String> getParameters()
	{
		return parameters;
	}
	
	
	/**
	 * Appends a tag to the current volumes.
	 * Useful to avoid conflicts in a GDML file.
	 * 
	 * @param aTag something to add
	 */
	public void appendName( String aTag )
	{
		Util.appendName( motherVol, aTag );
	}
	
	
	/**
	 * Returns a list of all the volumes in the gemcString() format.
	 * 
	 * @return String multiple lines of text
	 */
	@Override
	public String toString()
	{
		return Util.toString( motherVol );
	}
	
	
	/**
	 * Sets whether alignment shifts from CCDB should be applied to the geometry during generation.
	 * 
	 * @param b true/false
	 */
	public void setApplyAlignmentShifts( boolean b )
	{
		bShift = b;
	}
	
	
	/**
	 * Returns whether alignment shifts are applied.
	 * 
	 * @return boolean true/false
	 */
	public boolean isSetApplyAlignmentShifts()
	{
		return bShift;
	}
	
	
	/**
	 * Sets scale factors to amplify alignment shifts for visualisation purposes.
	 *  
	 * @param aScaleTranslation a scale factor for translation shifts
	 * @param aScaleRotation a scale factor for rotation shifts
	 */
	public void setAlignmentShiftScale( double aScaleTranslation, double aScaleRotation  )
	{
		scaleT = aScaleTranslation;
		scaleR = aScaleRotation;
	}
	
	
	/** 
	 * Sets the range of indices to cycle over when generating the geometry in makeVolumes().
	 * Enter 0 to use the previous/default value.
	 * 
	 * @param aLayerMin an index starting from 1
	 * @param aLayerMax an index starting from 1
	 * @param aSectorMin an index starting from 1
	 * @param aSectorMax an index starting from 1
	 * @throws IllegalArgumentException indices out of bounds
	 */
	public void setRange( int aLayerMin, int aLayerMax, int[] aSectorMin, int[] aSectorMax ) throws IllegalArgumentException
	{
		if( aLayerMin < 0 || aLayerMax > SVTConstants.NLAYERS ){ throw new IllegalArgumentException("layer out of bounds"); }
		if( aSectorMin.length != SVTConstants.NREGIONS || aSectorMax.length != SVTConstants.NREGIONS ){ throw new IllegalArgumentException("invalid sector array"); }
		if( aLayerMin > aLayerMax ){ throw new IllegalArgumentException("invalid layer min/max"); }
		
		for( int i = 0; i < SVTConstants.NREGIONS; i++ )
		{
			if( aSectorMin[i] < 0 || aSectorMax[i] > SVTConstants.NSECTORS[i] )
				throw new IllegalArgumentException("sector out of bounds");
			if( aSectorMin[i] > aSectorMax[i] )
				throw new IllegalArgumentException("invalid sector min/max");
		}
		
		// 0 means use default / previous value
		if( aLayerMin != 0 )
		{
			layerMin = aLayerMin;
			regionMin = SVTConstants.convertLayer2RegionModule( aLayerMin )[0]+1;
			moduleMin = SVTConstants.convertLayer2RegionModule( aLayerMin )[1]+1;
		}
		if( aLayerMax != 0 )
		{
			layerMax = aLayerMax;
			regionMax = SVTConstants.convertLayer2RegionModule( aLayerMax )[0]+1;
			moduleMax = SVTConstants.convertLayer2RegionModule( aLayerMax )[1]+1;
		}
		for( int i = 0; i < SVTConstants.NREGIONS; i++ )
		{
			if( aSectorMin[i] != 0 ){ sectorMin[i] = aSectorMin[i]; }
			if( aSectorMax[i] != 0 ){ sectorMax[i] = aSectorMax[i]; }
		}
	}
	
	
	/** 
	 * Sets the range of indices to cycle over when generating the geometry in makeVolumes().
	 * Enter 0 to use the previous/default value.
	 *  
	 * @param aRegionMin an index starting from 1
	 * @param aRegionMax an index starting from 1
	 * @param aSectorMin an index starting from 1
	 * @param aSectorMax an index starting from 1
	 * @param aModuleMin an index starting from 1
	 * @param aModuleMax an index starting from 1
	 * @throws IllegalArgumentException indices out of bounds
	 */
	public void setRange( int aRegionMin, int aRegionMax, int[] aSectorMin, int[] aSectorMax, int aModuleMin, int aModuleMax ) throws IllegalArgumentException
	{
		if( aRegionMin < 0 || aRegionMax > SVTConstants.NREGIONS ){ throw new IllegalArgumentException("region out of bounds"); }
		if( aSectorMin.length != SVTConstants.NREGIONS || aSectorMax.length != SVTConstants.NREGIONS ){ throw new IllegalArgumentException("invalid sector array"); }
		if( aRegionMin > aRegionMax ){ throw new IllegalArgumentException("invalid region min/max"); }
		
		for( int i = 0; i < SVTConstants.NREGIONS; i++ )
		{
			if( aSectorMin[i] < 0 || aSectorMax[i] > SVTConstants.NSECTORS[i] )
				throw new IllegalArgumentException("sector out of bounds");
			if( aSectorMin[i] > aSectorMax[i] )
				throw new IllegalArgumentException("invalid sector min/max");
		}
		if( aModuleMin < 0 || aModuleMax > SVTConstants.NMODULES )
			throw new IllegalArgumentException("module out of bounds");
		
		// 0 means use default / previous value
		if( aRegionMin != 0 ){ regionMin = aRegionMin; }
		if( aRegionMax != 0 ){ regionMax = aRegionMax; }
		for( int i = 0; i < SVTConstants.NREGIONS; i++ )
		{
			if( aSectorMin[i] != 0 ){ sectorMin[i] = aSectorMin[i]; }
			if( aSectorMax[i] != 0 ){ sectorMax[i] = aSectorMax[i]; }
		}
		if( aModuleMin != 0 ){ moduleMin = aModuleMin; }
		if( aModuleMax != 0 ){ moduleMax = aModuleMax; }
		if( aRegionMin != 0 || aModuleMin != 0 ){ layerMin = SVTConstants.convertRegionModule2Layer( regionMin-1, moduleMin-1 )+1; }
		if( aRegionMax != 0 || aModuleMax != 0 ){ layerMax = SVTConstants.convertRegionModule2Layer( regionMax-1, moduleMax-1 )+1; }
	}
	
	
	/**
	 * Sets the range of indices to cycle over when generating the geometry in makeVolumes().
	 * Enter 0 to use the previous/default value.
	 * 
	 * @param aRegion an index starting from 1
	 * @param aSectorMin an index starting from 1
	 * @param aSectorMax an index starting from 1
	 * @throws IllegalArgumentException indices out of bounds
	 */
	public void setRange( int aRegion, int aSectorMin, int aSectorMax ) throws IllegalArgumentException
	{
		if( aRegion < 0 || aRegion > SVTConstants.NREGIONS ){ throw new IllegalArgumentException("region out of bounds"); }
		if( aSectorMin < 0 || aSectorMin > SVTConstants.NSECTORS[aRegion] ){ throw new IllegalArgumentException("sectorMin out of bounds"); }
		if( aSectorMax < 0 || aSectorMax > SVTConstants.NSECTORS[aRegion] ){ throw new IllegalArgumentException("sectorMax out of bounds"); }
		if( aSectorMin > aSectorMax ){ throw new IllegalArgumentException("invalid sector min/max"); }
		
		// 0 means use default / previous value
		if( aRegion != 0 ){ regionMin = aRegion; regionMax = aRegion; }
		if( aSectorMin != 0 ){ sectorMin[regionMin-1] = aSectorMin; }
		if( aSectorMax != 0 ){ sectorMax[regionMax-1] = aSectorMax; }
		if( aRegion != 0 ){ layerMin = SVTConstants.convertRegionModule2Layer( regionMin-1, moduleMin-1 )+1;
						    layerMax = SVTConstants.convertRegionModule2Layer( regionMax-1, moduleMax-1 )+1; }
	}
	
	
	/**
	 * Returns a string to display the current range of indices.
	 * 
	 * @return String a line of text
	 */
	public String showRange()
	{
		String range = ""; 
		range = range +"layer ["+layerMin+":"+layerMax+"]";
		range = range +" region ["+regionMin+":"+regionMax+"]";
		range = range +" module ["+moduleMin+":"+moduleMax+"]";
		range = range +" sector ";
		for( int i = 0; i < SVTConstants.NREGIONS; i++ )
			range = range +"["+sectorMin[i]+":"+sectorMax[i]+"]";
		return range;
	}
	
	
	/**
	 * Returns the first region to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: 1
	 * 
	 * @return int a lower bound for the region index
	 */
	public int getRegionMin()
	{
		return regionMin;
	}

	
	/**
	 * Returns the last region to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: NREGIONS
	 * 
	 * @return int an upper bound for the region index
	 */
	public int getRegionMax()
	{
		return regionMax;
	}

	
	/**
	 * Returns the first sector in each region to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: 1
	 * 
	 * @return int a lower bound for the sector index in each region
	 */
	public int[] getSectorMin()
	{
		return sectorMin;
	}

	
	/**
	 * Returns the last sector in each region to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: NSECTORS[region]
	 * 
	 * @return int an upper bound for the sector index in each region
	 */
	public int[] getSectorMax()
	{
		return sectorMax;
	}

	
	/**
	 * Returns the first module in a sector to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: 1
	 * 
	 * @return int a lower bound for the module index
	 */
	public int getModuleMin()
	{
		return moduleMin;
	}

	
	/**
	 * Returns the last module in a sector to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: NMODULES
	 * 
	 * @return int an upper bound for the module index
	 */
	public int getModuleMax()
	{
		return moduleMax;
	}

	
	/**
	 * Returns the first layer to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: 1
	 * 
	 * @return int a lower bound for the layer index
	 */
	public int getLayerMin()
	{
		return layerMin;
	}

	
	/**
	 * Returns the last layer to be generated on makeVolumes().
	 * To use in a for loop, do {@code for( int i = min-1; i < max; i++)}
	 * Default: NLAYERS
	 * 
	 * @return int an upper bound for the layer index
	 */
	public int getLayerMax()
	{
		return layerMax;
	}
}
