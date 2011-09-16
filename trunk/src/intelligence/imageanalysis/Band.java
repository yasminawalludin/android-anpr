/*
------------------------------------------------------------------------
JavaANPR - Automatic Number Plate Recognition System for Java
------------------------------------------------------------------------

This file is a part of the JavaANPR, licensed under the terms of the
Educational Community License

Copyright (c) 2006-2007 Ondrej Martinsky. All rights reserved

This Original Work, including software, source code, documents, or
other related items, is being provided by the copyright holder(s)
subject to the terms of the Educational Community License. By
obtaining, using and/or copying this Original Work, you agree that you
have read, understand, and will comply with the following terms and
conditions of the Educational Community License:

Permission to use, copy, modify, merge, publish, distribute, and
sublicense this Original Work and its documentation, with or without
modification, for any purpose, and without fee or royalty to the
copyright holder(s) is hereby granted, provided that you include the
following on ALL copies of the Original Work or portions thereof,
including modifications or derivatives, that you make:

# The full text of the Educational Community License in a location
viewable to users of the redistributed or derivative work.

# Any pre-existing intellectual property disclaimers, notices, or terms
and conditions.

# Notice of any changes or modifications to the Original Work,
including the date the changes were made.

# Any modifications of the Original Work must be distributed in such a
manner as to avoid any confusion with the Original Work of the
copyright holders.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

The name and trademarks of copyright holder(s) may NOT be used in
advertising or publicity pertaining to the Original or Derivative Works
without specific, written prior permission. Title to copyright in the
Original Work and any associated documentation will at all times remain
with the copyright holders. 

If you want to alter upon this work, you MUST attribute it in 
a) all source files
b) on every place, where is the copyright of derivated work
exactly by the following label :

---- label begin ----
This work is a derivate of the JavaANPR. JavaANPR is a intellectual 
property of Ondrej Martinsky. Please visit http://javaanpr.sourceforge.net 
for more info about JavaANPR. 
----  label end  ----

------------------------------------------------------------------------
                                         http://javaanpr.sourceforge.net
------------------------------------------------------------------------
*/


package intelligence.imageanalysis;

//import java.io.IOException;
import java.util.Vector;

import com.intelligence.NativeGraphics;

import jjil.android.RgbImageAndroid;
import jjil.core.RgbImage;

import android.graphics.Bitmap;
import android.util.Log;

import intelligence.intelligence.Intelligence;
//import javaanpr.configurator.Configurator;

public class Band extends Photo {
    static public Graph.ProbabilityDistributor distributor = new Graph.ProbabilityDistributor(0,0,25,25);
    static private int numberOfCandidates = Intelligence.configurator.getIntProperty("intelligence_numberOfPlates");
            
    private BandGraph graphHandle = null;
    
    /** Creates a new instance of Band */
    public Band() {
        image = null;
    }
    
    public Band(Bitmap bi) {
        super(bi);
    }
    
    public Bitmap renderGraph() {
        this.computeGraph();
        return graphHandle.renderHorizontally(this.getWidth() - 50, 100);
    }
    
    private Vector<Graph.Peak> computeGraph() {
    	if (graphHandle != null) return graphHandle.peaks; // graf uz bol vypocitany
        Bitmap imageCopy = duplicateImage(this.image);
        fullEdgeDetector(imageCopy);
        
        graphHandle = histogram(imageCopy);
        graphHandle.rankFilter(image.getHeight());
        graphHandle.applyProbabilityDistributor(distributor);
        graphHandle.findPeaks(numberOfCandidates);
        Intelligence.console.consoleBitmap(graphHandle.renderHorizontally(this.getWidth() - 50, 100));
        imageCopy.recycle();
        return graphHandle.peaks;
    }
    
    public Vector<Plate> getPlates() {
        Vector<Plate> out = new Vector<Plate>();
        
        Vector<Graph.Peak> peaks = computeGraph();
        //Intelligence.canvas.drawBitmap(renderGraph(), 0, 200, Intelligence.paint);
        for (int i=0; i<peaks.size(); i++) {
            // vyseknut z povodneho! obrazka znacky, a ulozit do vektora. POZOR !!!!!! Vysekavame z povodneho, takze
            // na suradnice vypocitane z imageCopy musime uplatnit inverznu transformaciu
            Graph.Peak p = peaks.elementAt(i);
            Bitmap bi = Bitmap.createBitmap(image, p.getLeft(), 0, p.getDiff(), image.getHeight());
            out.add(new Plate(bi));
        }
        return out;
    }
    
    public BandGraph histogram(Bitmap bi) {
        BandGraph graph = new BandGraph(this);
        /**
         * Graph at horizontal position
         */
        float[] peaks = new float[bi.getWidth()];
        NativeGraphics.getHSVBrightnessHorizontally(bi, peaks);
        graph.addPeaks(peaks);
        return graph;
    }
    
   /**
    * TODO ��������� ���������� �� �������� ���
    * @param source
    */
   public void fullEdgeDetector(Bitmap source) {
        int[] verticalMatrix= { -1, 0, 1, 
								  -2, 0, 2,
								  -1, 0, 1};	
	    Bitmap i1 = NativeGraphics.nativeSobel(source, verticalMatrix);
   		//Intelligence.console.console("fullEdgeDetector");
    	//Intelligence.console.consoleBitmap(i1);
    	int horizontalMatrix[] = {
    			-1,-2,-1,
                0, 0, 0,
                1, 2, 1
        };
    	Bitmap i2 = NativeGraphics.nativeSobel(source, horizontalMatrix);
    	//Intelligence.console.console("fullEdgeDetector 2");
    	//Intelligence.console.consoleBitmap(i2);
        int w = source.getWidth();
        int h = source.getHeight();
        
        for (int x=0; x < w; x++)
            for (int y=0; y < h; y++) {
	            float sum = 0.0f;
	            sum += Photo.getBrightness(i1, x, y);
	            sum += Photo.getBrightness(i2, x, y);
	            Photo.setBrightness(source, x, y, Math.min(1.0f, sum));
            }
        i1.recycle();
        i2.recycle();
    }    
    
}