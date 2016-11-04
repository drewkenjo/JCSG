package org.jlab.geometry.prim;

import java.util.ArrayList;
import java.util.List;

import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Primitive;
import eu.mihosoft.vrl.v3d.PropertyStorage;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Vertex;

public class Tube implements Primitive {

	private final PropertyStorage properties = new PropertyStorage();
	private double pDr1, pDr2, pDz, pPhi, pDphi;
	
	public Tube( double pDr1, double pDr2, double pDz, double pPhi, double pDphi )
	{
		if( pDr1 < 0 || pDr2 <= 0 || pDz <= 0 || pPhi < 0 || pDphi <= 0 ) {
            throw new IllegalArgumentException("Illegal arguments for Tube Primitive!");
        }
		
		this.pDr1 = pDr1;
		this.pDr2 = pDr2;
		this.pDz = pDz;
		this.pPhi = pPhi;
		this.pDphi = pDphi;
	}

	@Override
	public List<Polygon> toPolygons()
	{
		// copied from Box.java for now
		int[][][] facenorm = {
	            // position     // normal
	            {{0, 4, 6, 2}, {-1, 0, 0}},
	            {{1, 3, 7, 5}, {+1, 0, 0}},
	            {{0, 1, 5, 4}, {0, -1, 0}},
	            {{2, 6, 7, 3}, {0, +1, 0}},
	            {{0, 2, 3, 1}, {0, 0, -1}},
	            {{4, 5, 7, 6}, {0, 0, +1}}
	        };

	        List<Polygon> polygons = new ArrayList<>();
	        for (int[][] face : facenorm) {
	            List<Vertex> vertices = new ArrayList<>();
	            for (int ivert : face[0]) {
	                Vector3d vpos = new Vector3d(
	                		pDr1 * (2 * Math.min(1, ivert & 1) - 1),
	                		pDr2 * (2 * Math.min(1, ivert & 2) - 1),
	                        pDz * (2 * Math.min(1, ivert & 4) - 1)
	                );

	                vertices.add(new Vertex(vpos, new Vector3d(
	                        (double) face[1][0],
	                        (double) face[1][1],
	                        (double) face[1][2]
	                )));
	            }
	            polygons.add(new Polygon(vertices, properties));
	        }

	        return polygons;
	}

	@Override
	public PropertyStorage getProperties()
	{
		return properties;
	}
	
	

}
