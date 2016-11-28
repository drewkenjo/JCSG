package org.jlab.detector.geant4.v2.SVT;

import java.io.Writer;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.volume.G4Box;
import org.jlab.detector.volume.G4Tubs;
import org.jlab.detector.volume.Geant4Basic;
import org.jlab.geometry.exporter.GdmlExporter;
import org.jlab.geometry.exporter.VolumeExporterFactory;
import org.jlab.geometry.prim.Line3d;
import org.jlab.geometry.prim.Triangle3d;

import eu.mihosoft.vrl.v3d.Vector3d;

public class main {

	public static void main(String[] args) {
		
		//double[][] nominalData = new double[][]{ new double[]{ 1,0,-1 }, new double[]{ -1,0,-1 }, new double[]{ 0,0,2 } };
		//double[][] measuredData = new double[][]{ new double[]{ 1,0,1 }, new double[]{ 1,0,-1 }, new double[]{ -2,0,0 } };
		
		/*double[][] nominalData = new double[][]{ new double[] { -17.350, -68.283, -286.541 }, new double[]{ 17.350, -68.283, -286.541 }, new double[]{ 3.500, -68.283, 122.333 } };
		double[][] measuredData = new double[][]{ new double[]{ -17.295, -68.028, -286.365 }, new double[]{ 17.385, -67.934, -286.308 }, new double[]{ 3.942, -68.334, 122.473 } };
		
		for( int k = 0; k < nominalData.length/3; k+=3 )
		{
			System.out.println();
			for( int j = 0; j < 3; j++ )
			{
				System.out.printf("NP%d % 8.3f % 8.3f % 8.3f", j, nominalData[k+j][0], nominalData[k+j][1], nominalData[k+j][2] );
				//System.out.printf("    UP%d % 8.3f % 8.3f % 8.3f", j, measuredData[k+j][0], measuredData[k+j][1], measuredData[k+j][2] );
				//for( int i = 0; i < 3; i++ ) measuredData[k+j][i] = measuredData[k+j][i] + uncertainData[k+j][i];
				System.out.printf("    MP%d % 8.3f % 8.3f % 8.3f", j, measuredData[k+j][0], measuredData[k+j][1], measuredData[k+j][2] );
				System.out.println();
			}
		}
		
		double[][] nominalDistances = new double[nominalData.length/3][3];
		double[][] centerData = new double[nominalData.length/3][3];
		
		for( int k = 0; k < nominalData.length/3; k+=3 ) // triangle
		{
			Vector3d[] nominalPos3Ds = new Vector3d[3];
			
			System.out.printf("\n%3s", " ");
			for( int j = 0; j < 3+1; j++ ) // point, with offset
			{
				if( j < 3 )	nominalPos3Ds[j] =  new Vector3d(nominalData[k+j][0], nominalData[k+j][1], nominalData[k+j][2]);
				if( j > 0 ) // offset to wait for first point to be defined
				{
					if( k == 0) System.out.printf("%4s(%d %d)", " ", j-1, j%3 );
					nominalDistances[k][j-1] = nominalPos3Ds[j-1].distance( nominalPos3Ds[j%3] );
				}
			}
			
			Triangle3d centerTri = new Triangle3d( nominalPos3Ds[0], nominalPos3Ds[1], nominalPos3Ds[2] );
			centerData[k] = Util.toDoubleArray( centerTri.center() );
			
			System.out.printf("\nND%d % 8.3f % 8.3f % 8.3f", k, nominalDistances[k][0], nominalDistances[k][1], nominalDistances[k][2] );
			System.out.printf("    NC%d % 8.3f % 8.3f % 8.3f", k, centerData[k][0], centerData[k][1], centerData[k][2] );
			System.out.println();
		}
		
		AlignmentFactory.VERBOSE = true;
		double[][] shiftData = AlignmentFactory.calcShifts( 1, nominalData, measuredData );
		//double[][] shiftData = AlignmentFactory.calcShifts( 1, nominalData, measuredData, uncertainty );
		
		double[][] shiftedData = AlignmentFactory.applyShift( nominalData, shiftData, centerData, 1, 1 );
		
		double[][] deltasData = AlignmentFactory.calcDeltas( 3, 3, measuredData, shiftedData );
		
		System.exit(0);*/
		
		SVTConstants.VERBOSE = true;
		DatabaseConstantProvider cp = SVTConstants.connect( true );
		
		SVTAlignmentFactory.setup( cp, "survey_ideals_reformat.dat", "survey_measured_reformat.dat" );
		double[][] dataFactoryIdeal = SVTAlignmentFactory.getFactoryIdealFiducialData();
		
		AlignmentFactory.VERBOSE = true;
		SVTAlignmentFactory.calcShifts( dataFactoryIdeal, SVTAlignmentFactory.getDataSurveyMeasured(), "shifts_survey_measured_from_factory_ideal.dat" );
		//SVTAlignmentFactory.calcDeltas( dataFactoryIdeal, SVTAlignmentFactory.getDataSurveyMeasured(), "deltas_survey_measured_from_factory_ideal.dat" );
		AlignmentFactory.VERBOSE = false;
		
		SVTAlignmentFactory.calcDeltas( dataFactoryIdeal, SVTAlignmentFactory.getDataSurveyIdeal(), "deltas_survey_ideal_from_factory_ideal.dat");
		//SVTAlignmentFactory.calcDeltas( SVTAlignmentFactory.getDataSurveyIdeal(), SVTAlignmentFactory.getDataSurveyMeasured(), "deltas_survey_measured_from_survey_ideals.dat");
		
		double[][][] dataSideIdeals = SVTAlignmentFactory.calcTriangleSides( dataFactoryIdeal, 0, "sides_factory_ideal.dat");
		double[][][] dataSideMeasureds = SVTAlignmentFactory.calcTriangleSides( SVTAlignmentFactory.getDataSurveyMeasured(), 0.020, "sides_survey_measured.dat");
		SVTAlignmentFactory.calcDistanceDeltas( dataSideIdeals, dataSideMeasureds, "sides_survey_measured_from_factory_ideal.dat");
		
		//System.exit(0);
		
		int regionSelector = 1, sectorSelector = 1;
		double fidBallRadius = 0.1, // cm
				fidArrowCapRadius = 2.0, // mm
				fidArrowPointerRadius = 1.0, // mm
				stripArrowCapRadius = 0.5, // mm
				stripArrowPointerRadius = 0.25,
				cornerBallRadius = 0.075; // cm
		
		
		SVTVolumeFactory svtIdealVolumeFactory = new SVTVolumeFactory( cp, false );
		
		svtIdealVolumeFactory.setRange( regionSelector, sectorSelector, sectorSelector );
		//svtIdealVolumeFactory.setRange( regionSelector, 0, 0 );
		//svtIdealVolumeFactory.setRange( regionSelector, 1, 2 );
		
		svtIdealVolumeFactory.VERBOSE = true;
		svtIdealVolumeFactory.BUILDSENSORS = true;
		svtIdealVolumeFactory.BUILDSENSORZONES = true;
		svtIdealVolumeFactory.BUILDPASSIVES = false;
		
		svtIdealVolumeFactory.makeVolumes();
		//Geant4Basic sectorVol = svtIdealVolumeFactory.createSector(); sectorVol.setMother( svtIdealVolumeFactory.getMotherVolume() );
		//Geant4Basic pcBoardVol = svtIdealVolumeFactory.createPcBoard(); pcBoardVol.setMother( svtIdealVolumeFactory.getMotherVolume() );
		
		SVTStripFactory svtIdealStripFactory = new SVTStripFactory( cp, false );
		
		String fileNameIdealFiducials = "factory_fiducials_ideal.dat";
		Writer fileIdealFiducials = Util.openOutputDataFile( fileNameIdealFiducials );
		
		for( int region = svtIdealVolumeFactory.getRegionMin()-1; region < svtIdealVolumeFactory.getRegionMax(); region++ )
			for( int sector = svtIdealVolumeFactory.getSectorMin()[region]-1; sector < svtIdealVolumeFactory.getSectorMax()[region]; sector++ )
			{
				for( int module = svtIdealVolumeFactory.getModuleMin()-1; module < svtIdealVolumeFactory.getModuleMax(); module++ )
				{
					for( int strip = 0; strip < SVTConstants.NSTRIPS; strip+=16 ) // SVTConstants.NSTRIPS
					{
						Line3d stripLine = svtIdealStripFactory.getStrip( region, sector, module, strip );
						//System.out.printf("\nr%ds%dm%ds%d %s\n", region, sector, module, strip, stripLine.toString() );
						Geant4Basic stripVol = Util.createArrow("strip"+strip+"_m"+module+"_s"+sector+"_r"+region, stripLine, stripArrowCapRadius, stripArrowPointerRadius, false, true, false ); // mm
						stripVol.setMother( svtIdealVolumeFactory.getMotherVolume() );
						//System.out.println( stripVol.gemcString() );
						//for( int c = 0; c < stripVol.getChildren().size(); c++ )
							//System.out.println( stripVol.getChildren().get(c).gemcString() );
					}
					
					Vector3d[] layerCorners = svtIdealStripFactory.getLayerCorners( region, sector, module );
					for( int i = 0; i < layerCorners.length; i++ )
					{
						Geant4Basic cornerDisc = new G4Tubs("cornerBall"+i+"_m"+module+"_s"+sector+"_r"+region, 0, cornerBallRadius, cornerBallRadius, 0, 360 ); // cm
						cornerDisc.setPosition( layerCorners[i].times(0.1) ); // mm -> cm
						//cornerDisc.setMother( svtIdealVolumeFactory.getMotherVolume() );
					}
				}
				
				Vector3d fidPos3Ds[] = SVTAlignmentFactory.getIdealFiducials( region, sector );
				
				for( int fid = 0; fid < SVTConstants.NFIDUCIALS; fid++ )
				{
					Util.writeLine( fileIdealFiducials, String.format("R%dS%02dF%d % 8.3f % 8.3f % 8.3f\n", region+1, sector+1, fid+1, fidPos3Ds[fid].x, fidPos3Ds[fid].y, fidPos3Ds[fid].z ) );
					
					Geant4Basic fidBall = new G4Box("fiducialBall"+fid+"_s"+sector+"_r"+region, 0.1, 0.1, 0.1 ); // cm
					fidBall.setPosition( fidPos3Ds[fid].times(0.1) ); // mm->cm
					//fidBall.setMother( svtIdealVolumeFactory.getMotherVolume() );
				}
				
				/*Triangle3d fidTri3D = new Triangle3d( fidPos3Ds[0], fidPos3Ds[1], fidPos3Ds[2] );
				Vector3d fidVec3D = fidTri3D.normal.normalized();
				fidVec3D.times( 10 ); // length of arrow in mm
				Geant4Basic fidCen = Util.createArrow( "fiducialCenter_s"+sector+"_r"+region, fidVec3D, fidArrowCapRadius, fidArrowPointerRadius, true, true, false ); // mm
				fidCen.setPosition( fidTri3D.center().times(0.1) ); // mm->cm
				fidCen.setMother( svtIdealVolumeFactory.getMotherVolume() );*/
			}
		
		Util.closeOutputDataFile( fileNameIdealFiducials, fileIdealFiducials );
		
		System.out.println( svtIdealVolumeFactory.toString() );
		
		GdmlExporter gdmlFile = VolumeExporterFactory.createGdmlFactory();
		//gdmlFile.setVerbose( true ); // not useful for large numbers of volumes
		gdmlFile.setPositionLoc("local");
		gdmlFile.setRotationLoc("local");
		gdmlFile.addTopVolume( svtIdealVolumeFactory.getMotherVolume() );
		
		gdmlFile.addMaterialPreset("mat_hide", "mat_vacuum"); // use to hide dummy/container volumes
		gdmlFile.addMaterialPreset("mat_half", "mat_vacuum"); // use for debugging
		
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_heatSink", "materialref", "ref", "mat_hide");
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_heatSink", "materialref", "ref", "mat_half");
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
		
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_module", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_sensorPhysical", "materialref", "ref", "mat_hide");
		
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_sector", "materialref", "ref", "mat_half");
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_region", "materialref", "ref", "mat_half");
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_svt", "materialref", "ref", "mat_half");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_sector", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_region", "materialref", "ref", "mat_hide");
		gdmlFile.replaceAttribute( "structure", "volume", "name", "vol_svt", "materialref", "ref", "mat_hide");
		
		gdmlFile.replaceAttribute( "structure", "volume", "name", "arrow0", "materialref", "ref", "mat_hide");
		//gdmlFile.replaceAttribute( "structure", "volume", "name", "arrow0", "materialref", "ref", "mat_half");
		
		gdmlFile.writeFile("SVTFactory_ideal");

	}

}
