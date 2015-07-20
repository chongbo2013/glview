package com.glview.graphics.shader;

import android.opengl.Matrix;


//纹理三角形
public class Coordinate 
{	       
    //3个元素数组,x,y,z
    public void projectToWindow(float vOutput[], float vInput[],float totalM[], int viewport[]){
    	if(vOutput.length == vInput.length){
    		int count = vOutput.length/3;
    		for(int i = 0; i < count; i ++){
    			
    			gluProjectPoint(vOutput, i*3, vInput, i*3, totalM, viewport);
    			
    		}
    	}
    }
    
    
    public void projectToAndroidWindow(float vOutput[], float vInput[],float totalM[], int viewport[], int width, int height){
    	if(vOutput.length == vInput.length){
    		int count = vOutput.length/3;
    		for(int i = 0; i < count; i ++){
    			
    			gluProjectPoint(vOutput, i*3, vInput, i*3, totalM, viewport);
    			
    			vOutput[i*3 + 1] = height - vOutput[i*3 + 1];
    			
    		}
    	}
    }
    
    
    public	boolean	gluProjectPoint(float vOutput[], float vInput[],float totalM[], int viewport[]){
    	/* matrice de transformation */	
    	/* initilise la matrice et le vecteur a transformer */
    	mOut[0] = vInput[0];
    	mOut[1] = vInput[1];
    	mOut[2] = vInput[2];
    	mOut[3] = 1.0f;
    	
    	Matrix.multiplyMV(mIn, 0, totalM, 0, mOut, 0);
    	
    	
    	/* d’ou le resultat normalise entre -1 et 1 */
    	if (mIn[3] == 0.0){
    		return false;
    	}
    	
    	mIn[0] /= mIn[3];
    	mIn[1] /= mIn[3];
    	mIn[2] /= mIn[3];
    	/* en coordonnees ecran */
    	vOutput[0] = viewport[0] + (1 + mIn[0]) * viewport[2] / 2;
    	vOutput[1] = viewport[1] + (1 + mIn[1]) * viewport[3] / 2;
    	
    	/* entre 0 et 1 suivant z */
    	vOutput[2] = (1 + mIn[2]) / 2;
    	
    	return true;
    }
    
    public	boolean	gluProjectPoint(float vOutput[], int outOffset, float vInput[], int inOffset, float totalM[], int viewport[]){
    	/* matrice de transformation */	
    	/* initilise la matrice et le vecteur a transformer */
    	mOut[0] = vInput[0 + inOffset];
    	mOut[1] = vInput[1 + inOffset];
    	mOut[2] = vInput[2 + inOffset];
    	mOut[3] = 1.0f;
    	
    	Matrix.multiplyMV(mIn, 0, totalM, 0, mOut, 0);
    	
    	
    	/* d’ou le resultat normalise entre -1 et 1 */
    	if (mIn[3] == 0.0){
    		return false;
    	}
    	
    	mIn[0] /= mIn[3];
    	mIn[1] /= mIn[3];
    	mIn[2] /= mIn[3];
    	/* en coordonnees ecran */
    	vOutput[0 + outOffset] = viewport[0] + (1 + mIn[0]) * viewport[2] / 2;
    	vOutput[1 + outOffset] = viewport[1] + (1 + mIn[1]) * viewport[3] / 2;
    	
    	/* entre 0 et 1 suivant z */
    	vOutput[2 + outOffset] = (1 + mIn[2]) / 2;
    	
    	return true;
    }
    
    
    
    public void projectToWindow(float vOutput[], float vInput[],float model[], float pro[], int viewport[]){
    	if(vOutput.length == vInput.length){
    		
    		Matrix.multiplyMM(mMatrix, 0, pro, 0, model, 0);
    		
    		projectToWindow(vOutput, vInput, mMatrix, viewport);
    	}
    }
    
    
	float mIn[]  = new float[4];
	float mOut[] = new float[4];
	float mMatrix[] = new float[16];
	float mMatrixInv[] = new float[16];
	
    //3,x,y,z
    public	boolean	gluProjectPoint(float vOutput[], float vInput[], float model[], float proj[], int viewport[]){
    	/* matrice de transformation */

	
    	/* initilise la matrice et le vecteur a transformer */
    	mIn[0] = vInput[0];
    	mIn[1] = vInput[1];
    	mIn[2] = vInput[2];
    	mIn[3] = 1.0f;
    	
    
    	Matrix.multiplyMV(mOut, 0, model, 0, mIn, 0);
    	Matrix.multiplyMV(mIn, 0, proj, 0, mOut, 0);
    	
    	
    	/* d’ou le resultat normalise entre -1 et 1 */
    	if (mIn[3] == 0.0){
    		return false;
    	}
    	
    	mIn[0] /= mIn[3];
    	mIn[1] /= mIn[3];
    	mIn[2] /= mIn[3];
    	/* en coordonnees ecran */
    	vOutput[0] = viewport[0] + (1 + mIn[0]) * viewport[2] / 2;
    	vOutput[1] = viewport[1] + (1 + mIn[1]) * viewport[3] / 2;
    	
    	/* entre 0 et 1 suivant z */
    	vOutput[2] = (1 + mIn[2]) / 2;
    	
    	return true;
    }
    
    //3,x,y,z
    public	boolean	gluProjectPoint(float vOutput[], int outOffset, float vInput[],  int inOffset, float model[], float proj[], int viewport[]){
    	/* matrice de transformation */

	
    	/* initilise la matrice et le vecteur a transformer */
    	mIn[0] = vInput[0 + inOffset];
    	mIn[1] = vInput[1 + inOffset];
    	mIn[2] = vInput[2 + inOffset];
    	mIn[3] = 1.0f;
    	
    
    	Matrix.multiplyMV(mOut, 0, model, 0, mIn, 0);
    	Matrix.multiplyMV(mIn, 0, proj, 0, mOut, 0);
    	
    	
    	/* d’ou le resultat normalise entre -1 et 1 */
    	if (mIn[3] == 0.0){
    		return false;
    	}
    	
    	mIn[0] /= mIn[3];
    	mIn[1] /= mIn[3];
    	mIn[2] /= mIn[3];
    	/* en coordonnees ecran */
    	vOutput[0 + outOffset] = viewport[0] + (1 + mIn[0]) * viewport[2] / 2;
    	vOutput[1 + outOffset] = viewport[1] + (1 + mIn[1]) * viewport[3] / 2;
    	
    	/* entre 0 et 1 suivant z */
    	vOutput[2 + outOffset] = (1 + mIn[2]) / 2;
    	
    	return true;
    }
    
    
    //winz --- 0 , 1
    boolean glhUnProjectf(float winx, float winy, float winz, float modelview[], float projection[], int viewport[], float objectCoordinate[])
    {
        //Transformation matrices
        //Calculation for inverting a matrix, compute projection x modelview
        //and store in A[16]
        //MultiplyMatrices4by4OpenGL_FLOAT(A, projection, modelview);
        
        Matrix.multiplyMM(mMatrix, 0, projection, 0, modelview, 0);
        
        //Now compute the inverse of matrix A
        if(Matrix.invertM(mMatrixInv, 0, mMatrix, 0)){
           return false;
        }
        //Transformation of normalized coordinates between -1 and 1
        mIn[0] = (winx - (float)viewport[0])/(float)viewport[2]*2.0f - 1.0f;
        mIn[1] = (winy - (float)viewport[1])/(float)viewport[3]*2.0f - 1.0f;
        mIn[2] = 2.0f*winz - 1.0f;
        mIn[3] = 1.0f;
        
        //Objects coordinates
        Matrix.multiplyMV(mOut, 0, mMatrixInv, 0, mIn, 0);
        
        if(mOut[3] == 0.0f){
           return false;
        }
        mOut[3]=1.0f/mOut[3];
        
        objectCoordinate[0] = mOut[0]*mOut[3];
        objectCoordinate[1] = mOut[1]*mOut[3];
        objectCoordinate[2] = mOut[2]*mOut[3];
        
        return true;
    }
}
