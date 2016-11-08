package org.jlab.detector.geant4.v2.SVT;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.geant4.v2.Misc.Util;
import org.jlab.detector.volume.Geant4Basic;
import org.jlab.geometry.exporter.IGdmlExporter;
import org.jlab.geometry.exporter.VolumeExporterFactory;
import org.jlab.geometry.prim.Line3d;

public class main {

	public static void main(String[] args) {
		
		SVTConstants.VERBOSE = true;
		DatabaseConstantProvider cp = SVTConstants.connect( true );
		
		int regionSelector = 1, sectorSelector = 6;
		double fidBallRadius = 0.1, // cm
				fidArrowCapRadius = 2.0, // mm
				fidArrowPointerRadius = 1.0, // mm
				stripArrowCapRadius = 0.5, // mm
				stripArrowPointerRadius = 0.25,
				cornerBallRadius = 0.075; // cm
		
		SVTVolumeFactory svtIdealVolumeFactory = new SVTVolumeFactory( cp, false );
		
		//svtIdealVolumeFactory.setRange( regionSelector, sectorSelector, sectorSelector );
		svtIdealVolumeFactory.setRange( regionSelector, 0, 0 );
		//svtIdealVolumeFactory.setRange( 1, 1, new int[]{1,1,1,1}, new int[]{1,1,1,1}, 1, 1 ); // test one module
		//svtIdealVolumeFactory.setRange( 1, 1, new int[]{1,1,1,1}, new int[]{1,1,1,1}, 0, 0 );
		
		//svtIdealVolumeFactory.VERBOSE = true;
		svtIdealVolumeFactory.BUILDSENSORZONES = false;
		svtIdealVolumeFactory.BUILDSENSORS = false;
		//svtIdealVolumeFactory.BUILDPASSIVES = false;
		//svtIdealVolumeFactory.VOLSPACER = 1.0;
		
		//svtIdealVolumeFactory.makeVolumes();
		Geant4Basic sectorVol = svtIdealVolumeFactory.createSector(); sectorVol.setMother( svtIdealVolumeFactory.getMotherVolume() );
		//Geant4Basic pcBoardVol = svtIdealVolumeFactory.createPcBoard(); pcBoardVol.setMother( svtIdealVolumeFactory.getMotherVolume() );
		
		SVTStripFactory svtIdealStripFactory = new SVTStripFactory( cp, false );
		
		//String fileNameIdealFiducials = "factory_fiducials_ideal.dat";
		//Writer fileIdealFiducials = Util.openOutputDataFile( fileNameIdealFiducials );
		
		for( int region = svtIdealVolumeFactory.getRegionMin()-1; region < svtIdealVolumeFactory.getRegionMax(); region++ )
			for( int sector = svtIdealVolumeFactory.getSectorMin()[region]-1; sector < svtIdealVolumeFactory.getSectorMax()[region]; sector++ )
			{
				for( int module = svtIdealVolumeFactory.getModuleMin()-1; module < svtIdealVolumeFactory.getModuleMax(); module++ )
				{
					for( int strip = 0; strip < SVTConstants.NSTRIPS; strip+=32 ) // SVTConstants.NSTRIPS
					{
						Line3d stripLine = svtIdealStripFactory.getStrip( region, sector, module, strip );
						//System.out.printf("r%ds%dm%ds%d ", region, sector, module, strip ); stripLine.show();
						Geant4Basic stripVol = Util.createArrow("strip"+strip+"_m"+module+"_s"+sector+"_r"+region, stripLine.end().minus(stripLine.origin()), stripArrowCapRadius, stripArrowPointerRadius, false, true, false ); // mm
						stripVol.setPosition( stripLine.origin().times( 0.1 ) ); // mm->cm
						//stripVol.setMother( svtIdealVolumeFactory.getMotherVolume() );
						//System.out.println( stripVol.gemcString() );
						//for( int c = 0; c < stripVol.getChildren().size(); c++ )
							//System.out.println( stripVol.getChildren().get(c).gemcString() );
					}
					
					/*Point3D[] layerCorners = svtIdealStripFactory.getLayerCorners( region, sector, module );
					for( int i = 0; i < layerCorners.length; i++ )
					{
						Geant4Basic cornerBall = new Geant4Basic("cornerBall"+i+"_m"+module+"_s"+sector+"_r"+region, "Orb", cornerBallRadius ); // cm
						cornerBall.setPosition( layerCorners[i].x()*0.1, layerCorners[i].y()*0.1, layerCorners[i].z()*0.1 ); // mm -> cm
						cornerBall.setMother( svtIdealVolumeFactory.getMotherVolume() );
					}*/
				}
				
				/*Point3D fidPos3Ds[] = SVTAlignmentFactory.getIdealFiducials( region, sector );
				
				for( int fid = 0; fid < SVTConstants.NFIDUCIALS; fid++ )
				{
					Util.writeLine( fileIdealFiducials, String.format("R%dS%02dF%d % 8.3f % 8.3f % 8.3f\n", region+1, sector+1, fid+1, fidPos3Ds[fid].x(), fidPos3Ds[fid].y(), fidPos3Ds[fid].z() ) );
					
					Geant4Basic fidBall = new Geant4Basic("fiducialBall"+fid+"_s"+sector+"_r"+region, "Orb", 0.1 ); // cm
					fidBall.setPosition( fidPos3Ds[fid].x()*0.1, fidPos3Ds[fid].y()*0.1, fidPos3Ds[fid].z()*0.1 ); // mm->cm
					fidBall.setMother( svtIdealVolumeFactory.getMotherVolume() );
				}
				
				Triangle3D fidTri3D = new Triangle3D( fidPos3Ds[0], fidPos3Ds[1], fidPos3Ds[2] );
				Vector3D fidVec3D = fidTri3D.normal().asUnit();
				fidVec3D.scale( 10 ); // length of arrow in mm
				Geant4Basic fidCen = Util.createArrow( "fiducialCenter_s"+sector+"_r"+region, fidVec3D, fidArrowCapRadius, fidArrowPointerRadius, true, true, false ); // mm
				fidCen.setPosition( fidTri3D.center().x()*0.1, fidTri3D.center().y()*0.1, fidTri3D.center().z()*0.1 ); // mm->cm
				fidCen.setMother( svtIdealVolumeFactory.getMotherVolume() );*/
			}
		
		//Util.closeOutputDataFile( fileNameIdealFiducials, fileIdealFiducials );
		
		System.out.println( svtIdealVolumeFactory.toString() );
		
		IGdmlExporter gdmlFile = VolumeExporterFactory.createGdmlFactory();
		//gdmlFile.setVerbose( true ); // not useful for large numbers of volumes
		gdmlFile.setPositionLoc("local");
		gdmlFile.setRotationLoc("local");
		gdmlFile.addTopVolume( svtIdealVolumeFactory.getMotherVolume() );
		
		gdmlFile.addMaterialPreset("mat_hide", "mat_vacuum"); // use to hide dummy/container volumes
		gdmlFile.addMaterialPreset("mat_half", "mat_vacuum"); // use for debugging
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_heatSink", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_heatSinkCu", "materialref", "ref", "mat_vacuum");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_heatSinkRidge", "materialref", "ref", "mat_vacuum");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_carbonFiber", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_carbonFiberCu", "materialref", "ref", "mat_vacuum");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_carbonFiberPk", "materialref", "ref", "mat_vacuum");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_busCable", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_busCableCu", "materialref", "ref", "mat_vacuum");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_busCablePk", "materialref", "ref", "mat_vacuum");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_pcBoardAndChips", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_epoxyAndRailAndPads", "materialref", "ref", "mat_hide");
		
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_sector", "materialref", "ref", "mat_half");
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_region", "materialref", "ref", "mat_half");
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_svt", "materialref", "ref", "mat_half");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_sector", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_region", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_svt", "materialref", "ref", "mat_hide");
		
		gdmlFile.replaceAttribute( "structure", "volume", "name", "arrow0", "materialref", "ref", "mat_hide");
		
		gdmlFile.writeFile("SVTFactory_ideal");

	}

}
