package org.jlab.detector.geant4.v2.Misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

import org.jlab.detector.volume.Geant4Basic;
import eu.mihosoft.vrl.v3d.Vector3d;

/**
 * <h1> Geometry Utility </h1>
 * 
 * Universal class for a variety of useful methods.
 * 
 * <ul>
 * <li> Vector manipulation: visualisation as volume, convert between Vector3d, Matrix and double[], convert difference to axis-angle rotation </li>
 * <li> Volume manipulation: toString for GEMC, clone, append name, promote children, shift position, scale position </li>
 * <li> Interfacing with Matrix to use appropriate rotation conversions for standard vectors and Geant </li>
 * <li> File I/O </li>
 * </ul>
 * 
 * @author pdavies
 * @version 1.0.0
 */
public class Util
{
	/**
	 * Calculates the distance with uncertainty between two measured points.
	 * 
	 * @param p0 first point
	 * @param p1 second point
	 * @param u0 uncertainty in the measurement of the coordinates of the first point
	 * @param u1 uncertainty in the measurement of the coordinates of the second point
	 * @return double[] an array containing a {value,uncertainty} pair
	 */
	public static double[] calcDistance( Vector3d p0, Vector3d p1, double u0, double u1 )
	{
		double distance = p0.distance( p1 );
		double partial = 0;
		double sum = 0;
		double[] c0 = Util.toDoubleArray( p0 ); // point 0
		double[] c1 = Util.toDoubleArray( p1 ); // point 1
		double[] u = new double[]{u0,u1}; // uncertainties
		
		for( int i = 0; i < 3; i++ ) // coordinates (x,y,z)
		{
			partial += (c1[i] - c0[i])/distance;
			for( int j = 0; j < 2; j++ ) // points (0,1)
				sum += Math.pow(Math.pow(-1,j)*partial*u[j],2);
		}
		return new double[]{ distance, Math.sqrt(sum) };
	}
	
	
	/**
	 * Replaces mother of children with mother of given volume, while preserving positions and rotations, and removes given volume from mother.
	 * Effectively promotes a given volume's children to the same rank as that volume in the tree hierarchy.
	 * 
	 * @param aVol volume whose children are to be moved
	 */
	/*public static void moveChildrenToMother( Geant4Basic aVol )
	{	
		// Mother - aVol - Children
		// Mother - Children
		
		if( aVol.getChildren().size() > 0 )
		{		
			Geant4Basic mother = aVol.getMother();
			double[] posV = aVol.getPosition();
			Vector3d vecVolInMother = Util.toVector3d( posV );
			double[] rotV = aVol.getRotation();
			Matrix rotVolInMother = Matrix.convertRotationFromEulerInXYZ_ExZYX( -rotV[0], -rotV[1], -rotV[2] );
			
			boolean verbose = false;
			
			if( verbose ) System.out.println("replaceChildrenMother");
			if( verbose ) System.out.println("mother: "+ mother.gemcString() );
			if( verbose ) System.out.println("volume: "+ aVol.gemcString() );
			
			for( int i = 0; i < aVol.getChildren().size(); i++ )
			{
				Geant4Basic child = aVol.getChildren().get(i);
				
				double[] rotC = child.getRotation(); 
				Matrix rotChildInVol = Matrix.convertRotationFromEulerInXYZ_ExZYX( -rotC[0], -rotC[1], -rotC[2] );
				Matrix rotChildInMother = Matrix.matMul( rotVolInMother, rotChildInVol ); // transpose by passing args backwards = rotate like normal in volume's frame, then append rotation in mother's frame  
				double[] rotNew = Matrix.convertRotationToEulerInXYZ_ExZYX( rotChildInMother );
				
				Vector3d vecChildInVol = Util.toVector3d( child.getPosition() ); // volume's frame
				vecChildInVol = Util.toVector3d(Matrix.matMul( rotVolInMother, Util.toMatrix(vecChildInVol) )); // convert to mother's frame
				Vector3d vecChildInMother = vecChildInVol.add( vecVolInMother ); // mother's frame
				double[] posNew = Util.toDoubleArray( vecChildInMother );
				
				if( verbose ) System.out.printf("child %d: %s\n", i, child.gemcString() );
	
				child.setPosition( posNew[0], posNew[1], posNew[2] );
				child.setRotation("xyz", -rotNew[0], -rotNew[1], -rotNew[2] );
				child.setMother( mother );
				//child.setName( child.getName()+"-" );
				
				if( verbose ) System.out.printf("child %d: %s\n", i, child.gemcString() );
			}
			mother.getChildren().remove( aVol );
		}
	}*/
	
	
	/**
	 * Recursively appends the given tag to the name of the given volume and its descendents.
	 * 
	 * @param aVol volume
	 * @param aTag string to append
	 */
	/*public static void appendName( Geant4Basic aVol, String aTag )
	{
		aVol.setName( aVol.getName() + aTag );
		appendChildrenName( aVol, aTag );
	}*/
	
	
	/**
	 * Recursively appends the given tag to the name of the descendents of the given volume.
	 * 
	 * @param aVol volume
	 * @param aTag string to append
	 */
	public static void appendChildrenName( Geant4Basic aVol, String aTag )
	{
		List<Geant4Basic> children = aVol.getChildren();
		for( int i = 0; i < children.size(); i++ )
		{
			children.get(i).setName( children.get(i).getName() + aTag );
			appendChildrenName( children.get(i), aTag ); // tail recursive
		}
	}
	
	
	/**
	 * Returns a string, in a format suitable for GEMC, of the given volume, including all descendents.
	 * 
	 * @param aVol volume
	 * @return String multiple lines of text
	 */
	public static String toString( Geant4Basic aVol )
	{
		StringBuilder str = new StringBuilder();
		_toString( aVol, str );
		return str.toString();
	}
	
	
	/**
	 * Appends a list of gemcStrings of the given volume and its children to the given StringBuilder
	 * 
	 * @param aVol a volume
	 * @param aStr a StringBuilder
	 */
	private static void _toString( Geant4Basic aVol, StringBuilder aStr )
	{
		aStr.append( aVol.gemcString() );
		aStr.append(System.getProperty("line.separator"));
		for( Geant4Basic childVol : aVol.getChildren() )
			_toString( childVol, aStr ); // recursive
	}
	
	
	/**
	 * Clones a volume, including children.
	 * 
	 * @param aVol volume
	 * @return Geant4Basic clone
	 */
	/*public static Geant4Basic clone( Geant4Basic aVol )
	{
		Geant4Basic b = cloneNoChildren( aVol );
		for( int i = 0; i < aVol.getChildren().size(); i++ )
		{
			b.getChildren().add( clone( aVol.getChildren().get(i) ) ); // recursive
		}
		return b;
	}*/
	
	
	/**
	 * Clones a volume, but not its children.
	 * 
	 * @param aVol volume 
	 * @return Geant4Basic clone
	 */
	/*public static Geant4Basic cloneNoChildren( Geant4Basic aVol )
	{
		Geant4Basic b = new Geant4Basic( aVol.getName(), aVol.getType(), aVol.getParameters() );
		b.setPosition( aVol.getPosition()[0], aVol.getPosition()[1], aVol.getPosition()[2]);
		b.setRotation( aVol.getRotationOrder(), aVol.getRotation()[0], aVol.getRotation()[1], aVol.getRotation()[2] );
		return b;
	}*/
	
	
	/**
	 * Converts the given vector to a column-based Matrix.
	 * 
	 * @param v vector
	 * @return Matrix column-based Matrix
	 */
	public static Matrix toMatrix( Vector3d v )
	{
		return new Matrix( 3, 1, new double[]{ v.x, v.y, v.z } ); // column vector
	}
	
	
	/**
	 * Converts the given column-based Matrix to a vector.  
	 * 
	 * @param m matrix
	 * @return Vector3d vector
	 * @throws IllegalArgumentException matrix wrong size
	 */
	public static Vector3d toVector3d( Matrix m ) throws IllegalArgumentException
	{
		if( !(m.nRows == 3 && m.nCols == 1 ) ) throw new IllegalArgumentException("Matrix wrong size");
		return new Vector3d( m.getData()[0], m.getData()[1], m.getData()[2] );
	}
	
	
	/**
	 * Converts the given vector to a double[] array.
	 * 
	 * @param v vector
	 * @return double[] array
	 */
	public static double[] toDoubleArray( Vector3d v )
	{
		return new double[]{ v.x, v.y, v.z };
	}
	
	
	/**
	 * Converts the given double[] array to a vector.
	 * 
	 * @param a array
	 * @return Vector3d vector
	 * @throws IllegalArgumentException array wrong size
	 */
	public static Vector3d toVector3d( double[] a ) throws IllegalArgumentException
	{
		if( a.length == 3 )
			return new Vector3d( a[0], a[1], a[2] );
		else
			throw new IllegalArgumentException("array wrong size");
	}
	
	
	/**
	 * Creates a cylinder, capped with two spheres, pointing in the direction of the given vector.
	 * Length of cylinder is equal to magnitude of vector.
	 * 
	 * @param aName subvolumes will have _arrow# appended to aName
	 * @param aVec direction to point
	 * @param aCapRadius radius of spherical caps
	 * @param aPointerRadius radius of cylindrical shaft
	 * @param aDisplayCapStart switch to show start cap
	 * @param aDisplayPointer switch to show shaft
	 * @param aDisplayCapEnd switch to show end cap
	 * @return Geant4Basic pseudo volume containing arrow components
	 */
	/*public static Geant4Basic createArrow( String aName, Vector3d aVec,
			double aCapRadius, double aPointerRadius, boolean aDisplayCapStart, boolean aDisplayPointer, boolean aDisplayCapEnd )
	{
		Geant4Basic arrowVol = new Geant4Basic(aName+"_arrow0", "Box", 0 ); // container
		// put cap at base of vector, with arrow pointing in direction of vector, and optional second cap at end of arrow
		
		//System.out.printf("arrow vector x=% 8.3f y=% 8.3f z=% 8.3f mag=% 8.3f\n", aVec.x(), aVec.y(), aVec.z(), aVec.mag() );
		
		if( aDisplayCapStart )
		{
			Geant4Basic capStartVol = new Geant4Basic( aName+"_arrow1", "orb", aCapRadius*0.1 );
			capStartVol.setMother( arrowVol ); // origin of a Line3D
		}
		if( aDisplayPointer )
		{
			Geant4Basic pointerVol = new Geant4Basic( aName+"_arrow2", "eltube", aPointerRadius*0.1, aPointerRadius*0.1, aVec.mag()/2*0.1 );
			pointerVol.setMother( arrowVol );
			
			double[] eulerAngles = Util.convertRotationVectorToGeant( aVec.theta(), aVec.phi() );
			pointerVol.setRotation("xyz", -eulerAngles[0], -eulerAngles[1], -eulerAngles[2] );
			
			// shift centre of geometry of arrow to put first end at start ball
			pointerVol.setPosition( aVec.divide(2).x()*0.1, aVec.divide(2).y()*0.1, aVec.divide(2).z()*0.1 );
		}
		if( aDisplayCapEnd )
		{
			Geant4Basic capEndVol = new Geant4Basic( aName+"_arrow3", "orb", aCapRadius*0.1 );
			capEndVol.setPosition( aVec.x()*0.1, aVec.y()*0.1, aVec.z()*0.1 );
			capEndVol.setMother( arrowVol );
		}
		
		return arrowVol;
	}*/
	
	
	/**
	 * Converts the given spherical angles to a rotation matrix.
	 * Tait-Bryan/Euler Intrinsic ZYX/Extrinsic XYZ.
	 * 
	 * @param theta elevation angle in radians
	 * @param phi azimuth angle in radians
	 * @return Matrix a column-based rotation matrix
	 */
	public static Matrix convertRotationVectorToMatrix( double theta, double phi )
	{
		// standard Vector rotation formalism is identical to Euler_InZYX_ExXYZ( 0.0, theta, phi )
		// first, rotate about the X-axis by zero angle (for all vectors), 
		// then rotate about the Y-axis by angle theta, 
		// and finally rotate about the Z-axis by angle phi
		//return Matrix.matMul( Matrix.rotateZ( phi ), Matrix.rotateY( theta ) ); // col-based matrices multiply right->left
		return Matrix.convertRotationFromEulerInZYX_ExXYZ( 0.0, theta, phi );
	}
	
	
	/**
	 * Converts the given spherical angles to Tait-Bryan angles suitable for Geant4Basic rotations.
	 * Tait-Bryan/Euler Intrinsic XYZ/Extrinsic ZYX.
	 * N.B. Geant = passive/alias, Java = active/alibi, so please negate the angles before passing to Geant4Basic.
	 * 
	 * @param theta elevation angle in radians
	 * @param phi azimuth angle in radians
	 * @return double[] xyz angles in radians
	 */
	public static double[] convertRotationVectorToGeant( double theta, double phi )
	{
		return Matrix.convertRotationToEulerInXYZ_ExZYX( convertRotationVectorToMatrix( theta, phi ) );
	}
	
	
	/**
	 * Calculates the difference between the two given direction vectors as an axis-angle rotation.
	 * http://math.stackexchange.com/questions/293116/rotating-one-3d-vector-to-another
	 * 
	 * @param a vector
	 * @param b vector
	 * @return double[] axis-angle rotation, format: { rx, ry, rz, ra }, angle in radians
	 */
	public static double[] convertVectorDiffToAxisAngle( Vector3d a, Vector3d b )
	{
		// http://math.stackexchange.com/questions/293116/rotating-one-3d-vector-to-another
		Vector3d eulerAxis = null;
		double eulerAngle = a.angle(b);
		
		double e = 1E-3;
		
		if( Math.abs(eulerAngle) < e )
		{
			return new double[]{ 0.0, 0.0, 0.0, 0.0 };
		}
		else if( Math.abs(Math.PI - eulerAngle) < e )
		{
			double[] c = new double[]{ a.x, a.y, a.z };
			int minLoc = 0; double minVal = c[minLoc];
			for( int i = 1; i < c.length; i++ ) if( c[i] < minVal ) { minLoc = i; minVal = c[i]; }
			
			Vector3d d = new Vector3d( 0.0, 0.0, 0.0 );
			switch( minLoc )
			{
			case 0:
				d.x = 1.0;
				break;
			case 1:
				d.y = 1.0;
				break;
			case 2:
				d.z = 1.0;
				break;
			}
			eulerAxis = a.cross(d).normalized();
		}
		else
		{
			eulerAxis = a.cross(b).normalized();
		}
		
		return new double[]{ eulerAxis.x, eulerAxis.y, eulerAxis.z, eulerAngle };
	}
	

	/**
	 * Recursively multiplies each position coordinate of the given volume and its children by the given scale factor.
	 * 
	 * @param aVol volume
	 * @param aFactor scale factor
	 */
	/*public static void scalePosition( Geant4Basic aVol, double aFactor )
	{
		double[] p = aVol.getPosition();
		aVol.setPosition( p[0]*aFactor, p[1]*aFactor, p[2]*aFactor );
		
		List<Geant4Basic> children = aVol.getChildren();
		for( int i = 0; i < children.size(); i++ )
		{
			scalePosition( children.get(i), aFactor ); // tail recursive
		}
	}
	
	
	public static void scaleDimensions( Geant4Basic aVol, double aFactor )
	{
		double[] d = aVol.getParameters();
		
		String type = aVol.getType().toLowerCase();
		
		switch( type )
		{
		case "box": // cube or cuboid
		case "eltube": // cylinder along Z axis
		case "orb": // sphere
			
			for( int i = 0; i < d.length; i++ )
				d[i] *= aFactor;
			break;
			
		case "tube": // hollow tube segment
			
			for( int i = 0; i < 3; i++ ) // rmax, rmin, z, deltaphi, startphi
				d[i] *= aFactor;
			break;
			
		default:
			throw new IllegalArgumentException("unknown type: \""+ type +"\"");
		}
		
		aVol.setParameters( d );
		
		List<Geant4Basic> children = aVol.getChildren();
		for( int i = 0; i < children.size(); i++ )
		{
			scaleDimensions( children.get(i), aFactor ); // tail recursive
		}
	}*/
	
	
	/**
	 * Translates the given volume's position by the given shifts. 
	 * 
	 * @param aVol volume
	 * @param aShiftX shift
	 * @param aShiftY shift
	 * @param aShiftZ shift
	 */
	/*public static void shiftPosition( Geant4Basic aVol, double aShiftX, double aShiftY, double aShiftZ )
	{
		double[] p = aVol.getPosition();
		aVol.setPosition( p[0] + aShiftX, p[1] + aShiftY, p[2] + aShiftZ );
	}*/
	
	
	/**
	 * Sums elements of aArray from 0 to (aIndex-1)
	 * 
	 * @param aArray an array of ints
	 * @param aIndex a terminating index
	 * @return int cumulative sum of elements
	 */
	public static int subArraySum( int[] aArray, int aIndex )
	{
		// sums elements of aArray from 0 to aIndex-1
		if( aIndex > 0 && aIndex < aArray.length+1 )
		{
			int[] subArray = new int[aIndex];
			for( int i = 0; i < aIndex; i++ )
				subArray[i] = aArray[i];
			return IntStream.of(subArray).sum();
		}
		return 0;
	}
	
	
	/**
	 * Reads data from a file of the format: String, double[].
	 * 
	 * @param aFilename name of file
	 * @param recLen number of fields to read per record
	 * @return double[][] array indexed as [record][field]
	 * @throws IllegalArgumentException null or empty filename
	 * @throws IOException incorrect record length
	 */
	public static double[][] inputTaggedData( String aFilename, int recLen ) throws IllegalArgumentException, IOException
	{
		if( aFilename == null )
			throw new IllegalArgumentException("null aFilename");
		if( aFilename.isEmpty() )
			throw new IllegalArgumentException("empty aFilename");
		
		//System.out.println("_inputData()");
		//System.out.println("aFilename=\""+ aFilename +"\"");
		
		double[][] dataResult = null;
		boolean bVerbose = false;
		Scanner scanner = null;
		
		try
		{
			File file = new File( aFilename );
			scanner = new Scanner( file );
			
			//ArrayList<String> tagList = new ArrayList<String>();
			ArrayList<double[]> dataList = new ArrayList<double[]>();
			
			//System.out.println("dataList.size()="+ dataList.size() );
			
			int i = 0;
			while( scanner.hasNext() )
			{
				if( bVerbose ) System.out.print("i="+ i );
				String tag = scanner.next();
				if( bVerbose ) System.out.print(" tag=\""+ tag +"\"");
				//tagList.add( tag );
				double[] data = new double[recLen];
				if( bVerbose ) System.out.print(" data=");
				for( int j = 0; j < recLen; j++ )
				{
					try{ data[j] = scanner.nextDouble(); }
					catch( Exception e )
					{ 
						//System.err.println("error reading \""+aFilename+"\" with recLen "+recLen+": line "+(i+1)+" data "+(j+1) ); 
						throw new IOException("error reading \""+aFilename+"\" with recLen "+recLen+": line "+(i+1)+" data "+(j+1) );
						//System.exit(-1);
					}
					if( bVerbose ) System.out.printf(" % 8.3f", data[j] );
				}
				dataList.add( data );
				if( bVerbose ) System.out.println();
				//System.out.println("dataList.size()="+ dataList.size() );
				i++;
			}
			
			//int tagLen = tagList.size();
			int dataLen = dataList.size();
			//System.out.println("tagLen="+ tagLen +" dataLen="+ dataLen );
			
			dataResult = new double[dataLen][3]; // like an RGB image
			
			for( int k = 0; k < dataLen; k++ )
				dataResult[k] = dataList.get(k);
			
			System.out.println("read "+ dataLen +" lines from \""+ aFilename +"\"");
			if( dataLen == 0 ){ throw new IllegalArgumentException("no data in file"); }
			
		}
		catch( FileNotFoundException e ) { e.printStackTrace(); System.exit(-1); }
		finally{ if(scanner != null){ scanner.close(); } }
		
		return dataResult;
	}
	
	
	/**
	 * Opens a file ready for writing.
	 * Terminates program if IOException occurs.
	 * 
	 * @param aName a file name
	 * @return Writer a file handle
	 * @throws IllegalArgumentException null or empty filename
	 */
	public static Writer openOutputDataFile( String aName ) throws IllegalArgumentException
	{
		if( aName == null ){ throw new IllegalArgumentException("filename is null"); }
		if( aName.isEmpty() ){ throw new IllegalArgumentException("filename is empty"); }
		
		Writer file = null;
		try
		{
			file = new BufferedWriter( new FileWriter( aName ) );
			System.out.println("opened \""+ aName +"\"");
		}
		catch( IOException e )
		{
			e.printStackTrace();
			System.exit(-1);
		}
		return file;
	}
	
	
	/**
	 * Appends the given String to the given file.
	 * Terminates program if IOException occurs.
	 *  
	 * @param aWriter file handle
	 * @param aLine string to write
	 */
	public static void writeLine( Writer aWriter, String aLine )
	{		
		try
		{
			if( aWriter != null )
				aWriter.write( aLine );
		}
		catch( IOException e )
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	/**
	 * Closes the given file.
	 * Terminates program if IOException occurs.
	 * 
	 * @param aName file name
	 * @param aFile file handle
	 */
	public static void closeOutputDataFile( String aName, Writer aFile )
	{
		if( aFile != null )
		{
			try
			{
				aFile.close();
				System.out.println("closed \""+ aName +"\"");
			}
			catch( IOException e )
			{
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else
		{
			System.out.println("\""+ aName +"\" is already closed");
		}
	}	
}
