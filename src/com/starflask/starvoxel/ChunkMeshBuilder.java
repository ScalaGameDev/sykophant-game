package com.starflask.starvoxel;
 

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.starflask.util.Vector3Int;
 
public class ChunkMeshBuilder  {

	public final static Vector3f normalUp = new Vector3f(0, 1, 0);
	public final static Vector3f normalDown = new Vector3f(0, -1, 0);
	public final static Vector3f normalRight = new Vector3f(1, 0, 0);
	public final static Vector3f normalLeft = new Vector3f(-1, 0, 0);
	public final static Vector3f normalFront = new Vector3f(0, 0, 1);
	public final static Vector3f normalBack = new Vector3f(0, 0, -1);
	
	  
	 
	
	/*
	 * Dont draw chunks that are not near the current view
	 * 
	 * 
	 */
	
	 private static boolean chunkStillNearView(Chunk chunk,Vector3f cameraPosition) {
		 
		return chunk.getChunkLOD( cameraPosition ) != ChunkLOD.HIDDEN ;
	}
  
	 
	
	
	 public static class RenderDataBuilder implements Callable<Chunk>  {
		     private final Chunk chunk;
		     private final Vector3f cameraPosition;

		     public RenderDataBuilder(Chunk chunk,Vector3f cameraPosition) {
		         this.chunk = chunk;
		         this.cameraPosition = cameraPosition;
		     }

		     	public Chunk call() {
		    	 
		     	if( chunkStillNearView(chunk,cameraPosition) )
		     	{
		     		buildRenderData(  chunk  );
		     	}
		    	 
				return chunk;
		         // use ipToPing
		     }
		}
		 
	
	
	public static void buildRenderData(Chunk chunk  ) {
		 MeshConstructionBuffers buffers = null;
		
		 if(chunk.isGreedy())
		 {
			 buffers =  buildGreedyRenderData(chunk );  //only good for nontextured since it connects adjacent faces
		 }else
		 {
			 buffers =  buildStandardRenderData(chunk );
		 }
		 
		 
		 	if(buffers != null)
		 	{
			
			Mesh mesh = generateNewMesh( buffers );
			
			 if(mesh!=null)
			 {
				 mesh.updateBound();
				 chunk.setMesh(mesh);
				 //chunk.threadedBuildFinished = true;
			 }
		 	}
		 
	}



	
	public static  Mesh generateNewMesh(MeshConstructionBuffers buffers) {
		return generateNewMesh(buffers.vertexBuffer, buffers.normalBuffer, buffers.colorBuffer, buffers.indexBuffer);
	}
	
	public static  Mesh generateNewMesh(FloatBuffer vertexBuffer, FloatBuffer normalBuffer, FloatBuffer colorBuffer, IntBuffer indexBuffer) {
		Mesh newMesh = new Mesh();
		
		 
			//make color a single int and use shader?
		 newMesh.setBuffer(Type.Color, 4, VertexBuffer.Format.Float,	colorBuffer);
		  
		 newMesh.setBuffer(Type.Position, 3,   vertexBuffer );
		 
		 newMesh.setBuffer(Type.Index, 3, 	indexBuffer);	 
		 
		 newMesh.setBuffer(Type.Normal, 3,VertexBuffer.Format.Float,  normalBuffer );
		 
		 return newMesh;
		
	}

	public static MeshConstructionBuffers buildGreedyRenderData(Chunk chunk ) {
		 
			MeshConstructionArrays arrays = new MeshConstructionArrays();
		 
			
			/*
			 * These are just working variables for the algorithm � almost all taken
			 * directly from Mikola Lysenko�s javascript implementation.
			 */
			int i, j, k, l, w, h, u, v, n;

			CUBEFACE side = CUBEFACE.RIGHT_FACE;

			final int[] x = new int[] { 0, 0, 0 };
			final int[] q = new int[] { 0, 0, 0 };
			final int[] du = new int[] { 0, 0, 0 };
			final int[] dv = new int[] { 0, 0, 0 };

			int CHUNK_HEIGHT = chunk.getSize().y;
			int CHUNK_WIDTH = chunk.getSize().x;
			//int lodSize= 1;
			
			/*
			 * We create a mask � this will contain the groups of matching voxel
			 * faces as we proceed through the chunk in 6 directions � once for each
			 * face.
			 */
			final VoxelFace[] mask = new VoxelFace[CHUNK_WIDTH * CHUNK_HEIGHT];

			/*
			 * These are just working variables to hold two faces during comparison.
			 */
			VoxelFace voxelFace, voxelFace1;
			
			

			/**
			 * We start with the lesser-spotted boolean for-loop (also known as the
			 * old flippy floppy).
			 * 
			 * The variable backFace will be TRUE on the first iteration and FALSE
			 * on the second � this allows us to track which direction the indices
			 * should run during creation of the quad.
			 * 
			 * This loop runs twice, and the inner loop 3 times � totally 6
			 * iterations � one for each voxel face.
			 */
			for (boolean backFace = true, b = false; b != backFace; backFace = backFace && b, b = !b) {

				/*
				 * We sweep over the 3 dimensions � most of what follows is well
				 * described by Mikola Lysenko in his post � and is ported from his
				 * Javascript implementation. Where this implementation diverges,
				 * I�ve added commentary.
				 */
				for (int d = 0; d < 3; d++) {

					u = (d + 1) % 3;
					v = (d + 2) % 3;

					x[0] = 0;
					x[1] = 0;
					x[2] = 0;

					q[0] = 0;
					q[1] = 0;
					q[2] = 0;
					q[d] = 1;

				//	int mostCommonBlockId = getMostCommonBlockId();

					/*
					 * Here we�re keeping track of the side that we�re meshing.
					 */
					if (d == 0) {
						side = backFace ? CUBEFACE.RIGHT_FACE : CUBEFACE.LEFT_FACE;
					} else if (d == 1) {
						side = backFace ? CUBEFACE.BOTTOM_FACE : CUBEFACE.TOP_FACE;
					} else if (d == 2) {
						side = backFace ? CUBEFACE.BACK_FACE : CUBEFACE.FRONT_FACE;
					}

					/*
					 * We move through the dimension from front to back
					 */
					for (x[d] = -1; x[d] < CHUNK_WIDTH;) {

						/*
						 * ����������������������- We compute the mask
						 * ����������������������-
						 */
						n = 0;

						for (x[v] = 0; x[v] < CHUNK_HEIGHT; x[v]++) {

							for (x[u] = 0; x[u] < CHUNK_WIDTH; x[u]++) {

								/*
								 * Here we retrieve two voxel faces for comparison.
								 */

								 
									voxelFace = (x[d] >= 0) ? getVoxelFace(chunk,x[0], x[1], x[2], side) : null;
									voxelFace1 = (x[d] < CHUNK_WIDTH - 1) ? getVoxelFace(chunk,x[0] + q[0], x[1] + q[1], x[2]
											+ q[2], side) : null;
 
								/*
								 * Note that we�re using the equals function in the
								 * voxel face class here, which lets the faces be
								 * compared based on any number of attributes.
								 * 
								 * Also, we choose the face to add to the mask
								 * depending on whether we�re moving through on a
								 * backface or not.
								 */
								mask[n++] = ((voxelFace != null && voxelFace1 != null && (voxelFace.equals(voxelFace1)))) ? null
										: backFace ? voxelFace1 : voxelFace;
							}
						}

						x[d]++;

						/*
						 * Now we generate the mesh for the mask
						 */
						n = 0;

						for (j = 0; j < CHUNK_HEIGHT; j++) {

							for (i = 0; i < CHUNK_WIDTH;) {

								if (mask[n] != null) {

									/*
									 * We compute the width
									 */
									for (w = 1; i + w < CHUNK_WIDTH && mask[n + w] != null && mask[n + w].equals(mask[n]); w++) {
									}

									/*
									 * Then we compute height
									 */
									boolean done = false;

									for (h = 1; j + h < CHUNK_HEIGHT; h++) {

										for (k = 0; k < w; k++) {

											if (mask[n + k + h * CHUNK_WIDTH] == null
													|| !mask[n + k + h * CHUNK_WIDTH].equals(mask[n])) {
												done = true;
												break;
											}
										}

										if (done) {
											break;
										}
									}

									/*
									 * Here we check the "transparent" attribute in
									 * the VoxelFace class to ensure that we don�t
									 * mesh any culled faces.
									 * 
									 * Is this working?
									 */
									if (!mask[n].transparent) {
										/*
										 * Add quad
										 */
										x[u] = i;
										x[v] = j;

										du[0] = 0;
										du[1] = 0;
										du[2] = 0;
										du[u] = w;

										dv[0] = 0;
										dv[1] = 0;
										dv[2] = 0;
										dv[v] = h;

										/*
										 * And here we call the quad function in
										 * order to render a merged quad in the
										 * scene.
										 * 
										 * We pass mask[n] to the function, which is
										 * an instance of the VoxelFace class
										 * containing all the attributes of the face
										 * � which allows for variables to be passed
										 * to shaders � for example lighting values
										 * used to create ambient occlusion.
										 */
										//boolean hasQuad = true;
										
										 quad(chunk, arrays,  new Vector3f(x[0], x[1], x[2]), new Vector3f(x[0] + du[0], x[1] + du[1], x[2]
												+ du[2]), new Vector3f(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2]
												+ du[2] + dv[2]), new Vector3f(x[0] + dv[0], x[1] + dv[1], x[2] + dv[2]),
												w, h, mask[n], backFace);
									}

									/*
									 * We zero out the mask
									 */
									for (l = 0; l < h; ++l) {

										for (k = 0; k < w; ++k) {
											mask[n + k + l * CHUNK_WIDTH] = null;
										}
									}

									/*
									 * And then finally increment the counters and
									 * continue
									 */
									i += w;
									n += w;

								} else {

									i++;
									n++;
								}
							}
						}
					}
				}
			}
			
		 return arrays.toMeshBuffers(chunk.drawTextures);
	}
	
	/**
	 * This function renders a single quad in the scene. This quad may represent
	 * many adjacent voxel faces � so in order to create the illusion of many
	 * faces, you might consider using a tiling function in your voxel shader.
	 * For this reason I�ve included the quad width and height as parameters.
	 * 
	 * For example, if your texture coordinates for a single voxel face were 0 �
	 * 1 on a given axis, they should now be 0 � width or 0 � height. Then you
	 * can calculate the correct texture coordinate in your fragement shader
	 * using coord.xy = fract(coord.xy).
	 * @param arrays 
	 * 
	 * 
	 * @param bottomLeft
	 * @param topLeft
	 * @param topRight
	 * @param bottomRight
	 * @param width
	 * @param height
	 * @param voxel
	 * @param backFace
	 */

	//boolean defQuad = false;

	public static void quad(Chunk chunk, MeshConstructionArrays arrays, final Vector3f bottomLeft, final Vector3f topLeft, final Vector3f topRight, final Vector3f bottomRight,
			final int width, final int height, final VoxelFace voxel, final boolean backFace) {

		int blockId = voxel.type;
		
	 
		Vector3f cubeSize = chunk.getCubeSize();

		/*CubeType cubetype = gamedata.cubetypes[blockId];

		if (cubetype == null) {
			// dont add any data to the mesh
			return;
		}*/

		final Vector3f[] vertices = new Vector3f[4];  
		vertices[0] = bottomLeft.mult(cubeSize);
		vertices[1] = bottomRight.mult(cubeSize);
		vertices[2] = topLeft.mult(cubeSize);
		vertices[3] = topRight.mult(cubeSize);

		for (Vector3f v : vertices) {
			arrays.getVertices().add(new float[]{v.x,v.y,v.z});
		}

		int verticesSize = arrays.getVertices().size();
		if (!backFace) {

			arrays.getIndexes().add(new int[]{
				verticesSize + 2,
				verticesSize + 3,
				verticesSize + 1,
				verticesSize + 1,
				verticesSize + 0,
				verticesSize + 2,
			});
			 

		} else {
			 
			arrays.getIndexes().add(new int[]{
					verticesSize + 2,
					verticesSize + 0,
					verticesSize + 1,
					verticesSize + 1,
					verticesSize + 3,
					verticesSize + 2,
				});
		}

		arrays.getNormals().add(new float[]{
				voxel.getNormal().x,voxel.getNormal().y,voxel.getNormal().z,
				voxel.getNormal().x,voxel.getNormal().y,voxel.getNormal().z,
				voxel.getNormal().x,voxel.getNormal().y,voxel.getNormal().z,
				voxel.getNormal().x,voxel.getNormal().y,voxel.getNormal().z
				});
		
		/*if (isTextured()) { 

			addTexCoords(cubetype.getTexId(voxel.side), width, height);
		}

		ColorRGBA cubetypeColor = MainApp.getGameState().getSunlightColor().clone();
		ColorRGBA cubetypeSpecular = new ColorRGBA(0.1f, 0.7f, 1f, 1f);

		if (cubetype.color != null) {
			cubetypeColor = cubetype.color.clone();
			cubetypeSpecular = cubetype.specular.clone();
		}

		if (cubetype.topcolor != null && voxel.side == CUBEFACE.TOP_FACE) {
			cubetypeColor = cubetype.topcolor.clone();
		}

		if (cubetype.bottomcolor != null && voxel.side == CUBEFACE.BOTTOM_FACE) {
			cubetypeColor = cubetype.bottomcolor.clone();
		}

		Vector4f topRightColor = terrain.getLightAverage(cubetypeColor, cubetypeSpecular, 1f, absX + 1, absY + 1,
				absZ + 1);*/
		
		Vector4f topRightColor = chunk.getColor(blockId);
		 
		arrays.getColors().add(new float[]{
				topRightColor.x,topRightColor.y,topRightColor.z,topRightColor.w,
				topRightColor.x,topRightColor.y,topRightColor.z,topRightColor.w,
				topRightColor.x,topRightColor.y,topRightColor.z,topRightColor.w,
				topRightColor.x,topRightColor.y,topRightColor.z,topRightColor.w
				});
		 
 
	}

	
	/**
	 * This function returns an instance of VoxelFace containing the attributes
	 * for one side of a voxel. In this simple demo we just return a value from
	 * the sample data array. However, in an actual voxel engine, this function
	 * would check if the voxel face should be culled, and set per-face and
	 * per-vertex values as well as voxel values in the returned instance.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param face
	 * @return
	 */
	public static VoxelFace getVoxelFace(Chunk chunk, final int x, final int y, final int z, final CUBEFACE side) {

		VoxelFace voxelFace = new VoxelFace(chunk.getBlockTypeFromLocalPosition(x, y, z));

		voxelFace.side = side;

		return voxelFace;
	}
	
	/**
	 * This class is used to encapsulate all information about a single voxel
	 * face. Any number of attributes can be included � and the equals function
	 * will be called in order to compare faces. This is important because it
	 * allows different faces of the same voxel to be merged based on varying
	 * attributes.
	 * 
	 * Each face can contain vertex data � for example, int[] sunlight, in order
	 * to compare vertex attributes.
	 * 
	 * Since it�s optimal to combine greedy meshing with face culling, I have
	 * included a "transparent" attribute here and the mesher skips transparent
	 * voxel faces. The getVoxelData function below � or whatever it�s
	 * equivalent might be when this algorithm is used in a real engine � could
	 * set the transparent attribute on faces based on whether they should be
	 * visible or not.
	 */
	static class VoxelFace {

		public VoxelFace(int type) {
			this.type = type;

			if (type <= 0  ) {
				this.transparent = true;
			}
		}

		public Vector3f getNormal() {

			switch (side) {
			case FRONT_FACE:
				return normalFront;
			case BACK_FACE:
				return normalBack;
			case LEFT_FACE:
				return normalLeft;
			case RIGHT_FACE:
				return normalRight;
			case TOP_FACE:
				return normalUp;
			case BOTTOM_FACE:
				return normalDown;

			}

			return normalUp;
		}

		public boolean transparent;
		public int type;
		public CUBEFACE side;

		public boolean equals(final VoxelFace face) {
			return face.transparent == this.transparent && face.type == this.type;
		}
	}
	
	public enum CUBEFACE {
		RIGHT_FACE, LEFT_FACE, TOP_FACE, BOTTOM_FACE, FRONT_FACE, BACK_FACE;
	}

	 
	public static MeshConstructionBuffers buildStandardRenderData(Chunk chunk ) {
		// Generate vertex data
		/*List<float[]> vertexArrays = new ArrayList<float[]>();
		List<int[]> indexArrays = new ArrayList<int[]>();
		List<float[]> normalArrays = new ArrayList<float[]>();
		List<float[]> colorArrays = new ArrayList<float[]>();
		List<float[]> texCoordArrays = new ArrayList<float[]>();*/
		
		MeshConstructionArrays arrays = new MeshConstructionArrays();
		
		
		Vector3Int size = chunk.getSize();
		Vector3Int position = chunk.getPosition();
		//int[][][] cubes = chunk.getCubes();
		Vector3f cubeSize = chunk.getCubeSize();
		Vector3Int worldSize = chunk.getWorldSize();
		
		int verticesSize = 0;
		
		// Generate data for each cube
		for(int x = 0; x < size.x; x++) {
			for(int y = 0; y < size.y; y++) {
				for(int z = 0; z < size.z; z++) {
					//int type = cubes[position.x + x][position.y + y][position.z + z];
					int type = chunk.getBlockTypeFromLocalPosition(x, y, z);
					
					// Skip this cube if the type is EMPTY
					if(type == CubeType.EMPTY)
						continue;
					
					// Get the color and texture coords for this cube type
					Vector4f color = chunk.getColor(type);
					 Rectf textureCoordinates =  chunk.getTextureCoordinates(type);
					
					Vector3f pos1 = new Vector3f(x * cubeSize.x, y * cubeSize.y, z * cubeSize.z);
					Vector3f pos2 = pos1.clone().add(  cubeSize);
					
					verticesSize = arrays.vertexArrays.size()*4;
				 	
					int indexes[] = { 2,0,1, 1,3,2 }; //original
					// int indexes[] = {2,1,0, 0,3,2};
					
					//bottom and top is z
					//left and right is x
					
					// Top
					if((position.y + y == worldSize.y - 1) || ( chunk.getBlockTypeFromLocalPosition(position.x + x,position.y + y + 1,position.z + z) == 0)) {
						// Vertex data
						arrays.vertexArrays.add(new float[] { pos1.x, pos2.y, pos1.z,// BOTTOM LEFT
													pos1.x, pos2.y, pos2.z,// BOTTOM RIGHT
													pos2.x, pos2.y, pos1.z,//TOP LEFT
													pos2.x, pos2.y, pos2.z });//TOP RIGHT
						
						System.out.println("rendering top ");
						
						//Indices
						 
						arrays.indexArrays.add(new int[] {
								verticesSize + indexes[0],
								verticesSize + indexes[1],
								verticesSize + indexes[2],
								verticesSize + indexes[3],
								verticesSize + indexes[4],
								verticesSize + indexes[5],
						});
						
						// Normals
						arrays.normalArrays.add(new float[] { 0.0f, 1.0f, 0.0f,
													0.0f, 1.0f, 0.0f,
													0.0f, 1.0f, 0.0f,
													0.0f, 1.0f, 0.0f});
						
						// Colors
						arrays.colorArrays.add(new float[] { color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w});
						
						// Texture coordinates
						arrays.texCoordArrays.add(new float[] { textureCoordinates.right, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.bottom,
														textureCoordinates.right, textureCoordinates.bottom});
					}
					
					verticesSize = arrays.vertexArrays.size()*4;
					
					// Bottom
					if((position.y + y == 0) || ( chunk.getBlockTypeFromLocalPosition(position.x + x,position.y + y - 1,position.z + z) == 0)) {
						System.out.println("rendering bottom ");
						
						// Vertex data
						arrays.vertexArrays.add(new float[] {pos1.x, pos1.y, pos1.z,// BOTTOM LEFT
													pos2.x, pos1.y, pos1.z,// BOTTOM RIGHT
													pos1.x, pos1.y, pos2.z,//TOP LEFT
													pos2.x, pos1.y, pos2.z });//TOP RIGHT
						
						
						

						
						
						//Indices
						 
						arrays.indexArrays.add(new int[] {
								verticesSize + indexes[0],
								verticesSize + indexes[1],
								verticesSize + indexes[2],
								verticesSize + indexes[3],
								verticesSize + indexes[4],
								verticesSize + indexes[5],
						});
						
						// Normals
						arrays.normalArrays.add(new float[] { 0.0f, -1.0f, 0.0f,
													0.0f, -1.0f, 0.0f,
													0.0f, -1.0f, 0.0f,
													0.0f, -1.0f, 0.0f});
						
						// Colors
						arrays.colorArrays.add(new float[] { color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w});
						
						// Texture coordinates
						arrays.texCoordArrays.add(new float[] { textureCoordinates.right, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.bottom,
														textureCoordinates.right, textureCoordinates.bottom});
					}
					
					verticesSize = arrays.vertexArrays.size()*4;
					
					// Front
					
					if((position.z + z == worldSize.z - 1) || (chunk.getBlockTypeFromLocalPosition(position.x + x,position.y + y ,position.z + z + 1) == 0)) {
						System.out.println("rendering front ");
						
						// Vertex data
						arrays.vertexArrays.add(new float[] { pos1.x, pos1.y, pos2.z, // BOTTOM LEFT
													pos2.x, pos1.y, pos2.z,// BOTTOM RIGHT
													pos1.x, pos2.y, pos2.z,//TOP LEFT
													pos2.x, pos2.y, pos2.z });//TOP RIGHT
						
						 
						
						//Indices
						
						arrays.indexArrays.add(new int[] {
								verticesSize + indexes[0],
								verticesSize + indexes[1],
								verticesSize + indexes[2],
								verticesSize + indexes[3],
								verticesSize + indexes[4],
								verticesSize + indexes[5],
						});
						
						// Normals
						arrays.normalArrays.add(new float[] { 0.0f, 0.0f, 1.0f,
													0.0f, 0.0f, 1.0f,
													0.0f, 0.0f, 1.0f,
													0.0f, 0.0f, 1.0f});
						
						// Colors
						arrays.colorArrays.add(new float[] { color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w});
						
						// Texture coordinates
						arrays.texCoordArrays.add(new float[] { textureCoordinates.right, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.bottom,
														textureCoordinates.right, textureCoordinates.bottom});
					}
					
					verticesSize = arrays.vertexArrays.size()*4;
					
					// Back
					if((position.z + z == 0) || (chunk.getBlockTypeFromLocalPosition(position.x + x,position.y + y ,position.z + z - 1) == 0)) {
						System.out.println("rendering back ");
						
						// Vertex data
						arrays.vertexArrays.add(new float[] {  pos1.x, pos1.y, pos1.z, // BOTTOM LEFT
													pos1.x, pos2.y, pos1.z,// BOTTOM RIGHT
													pos2.x, pos1.y, pos1.z,//TOP LEFT
													pos2.x, pos2.y, pos1.z });//TOP RIGHT
						
						//Indices
						 
						arrays.indexArrays.add(new int[] {
								verticesSize + indexes[0],
								verticesSize + indexes[1],
								verticesSize + indexes[2],
								verticesSize + indexes[3],
								verticesSize + indexes[4],
								verticesSize + indexes[5],
						});
						
						// Normals
						arrays.normalArrays.add(new float[] { 0.0f, 0.0f, -1.0f,
													0.0f, 0.0f, -1.0f,
													0.0f, 0.0f, -1.0f,
													0.0f, 0.0f, -1.0f});
						
						// Colors
						arrays.colorArrays.add(new float[] { color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w});
						
						// Texture coordinates
						arrays.texCoordArrays.add(new float[] { textureCoordinates.right, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.bottom,
														textureCoordinates.right, textureCoordinates.bottom});
					}
					
					verticesSize = arrays.vertexArrays.size()*4;
					
					// Right
					if((position.x + x == worldSize.x - 1) || (chunk.getBlockTypeFromLocalPosition(position.x + x + 1,position.y + y ,position.z + z ) == 0)) {
						// Vertex data
						arrays.vertexArrays.add(new float[] { pos2.x, pos1.y, pos1.z, // BOTTOM LEFT
													pos2.x, pos2.y, pos1.z, // BOTTOM RIGHT
													pos2.x, pos1.y, pos2.z, //TOP LEFT
													pos2.x, pos2.y, pos2.z }); //TOP RIGHT
						
		 
						
						//Indices
					 
						arrays.indexArrays.add(new int[] {
								verticesSize + indexes[0],
								verticesSize + indexes[1],
								verticesSize + indexes[2],
								verticesSize + indexes[3],
								verticesSize + indexes[4],
								verticesSize + indexes[5],
						});
						
						// Normals
						arrays.normalArrays.add(new float[] { 1.0f, 0.0f, 0.0f,
													1.0f, 0.0f, 0.0f,
													1.0f, 0.0f, 0.0f,
													1.0f, 0.0f, 0.0f});
						
						// Colors
						arrays.colorArrays.add(new float[] { color.x, color.y, color.z, color.w, //w is alpha
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w});
						
						// Texture coordinates
						arrays.texCoordArrays.add(new float[] { textureCoordinates.right, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.bottom,
														textureCoordinates.right, textureCoordinates.bottom});
					}
					 
					 verticesSize = arrays.vertexArrays.size()*4;
					  
					// Left
					if((position.x + x == 0) || (chunk.getBlockTypeFromLocalPosition(position.x + x - 1,position.y + y ,position.z + z ) == 0)) {
						// Vertex data
						arrays.vertexArrays.add(new float[] {pos1.x, pos1.y, pos1.z, // BOTTOM LEFT
													pos1.x, pos1.y, pos2.z, // BOTTOM RIGHT
													pos1.x, pos2.y, pos1.z, //TOP LEFT
													pos1.x, pos2.y, pos2.z }); //TOP RIGHT
						
						//Indices
					 
						arrays.indexArrays.add(new int[] {
								verticesSize + indexes[0],
								verticesSize + indexes[1],
								verticesSize + indexes[2],
								verticesSize + indexes[3],
								verticesSize + indexes[4],
								verticesSize + indexes[5],
						});
						
						// Normals
						arrays.normalArrays.add(new float[] { -1.0f, 0.0f, 0.0f,
													-1.0f, 0.0f, 0.0f,
													-1.0f, 0.0f, 0.0f,
													-1.0f, 0.0f, 0.0f});
						
						// Colors
						arrays.colorArrays.add(new float[] { color.x, color.y, color.z, color.w, //w is alpha
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w,
													color.x, color.y, color.z, color.w});
						
						// Texture coordinates
						arrays.texCoordArrays.add(new float[] { textureCoordinates.right, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.top,
														textureCoordinates.left, textureCoordinates.bottom,
														textureCoordinates.right, textureCoordinates.bottom});
					}  
				}
			}
			
			
		}
		
		
		 return arrays.toMeshBuffers(chunk.drawTextures);

		 /*
		// Create the float vertex buffer
		int numFloats = 0;
		
		for(float[] array : arrays.vertexArrays) {
			numFloats += array.length;
		}
		
		buffers.vertexBuffer = BufferUtils.createFloatBuffer(numFloats);
		
		System.out.println("vertices:"+numFloats);
		for(float[] array : arrays.vertexArrays) {
			for(float f : array)
			{
				System.out.println("vertex " + f);
			}
			buffers.vertexBuffer.put(array);
		}
		
		buffers.vertexBuffer.flip();
		
		//create index buffer
		  numFloats = 0;
		
		for(int[] array : arrays.indexArrays) {
			numFloats += array.length;
		}
		buffers.indexBuffer = BufferUtils.createIntBuffer(numFloats);
		
		System.out.println("indices:"+numFloats);
		for(int[] array : arrays.indexArrays) {	
			
			for(int i : array)
			{
				System.out.println("index " + i);
			}
			
			buffers.indexBuffer.put(array);
		}
		
		buffers.indexBuffer.flip();
		
		// Create the normal buffer
		numFloats = 0;
		
		for(float[] array : arrays.normalArrays) {
			numFloats += array.length;
		}
		
		buffers.normalBuffer = BufferUtils.createFloatBuffer(numFloats);
		
		for(float[] array : arrays.normalArrays) {
			buffers.normalBuffer.put(array);
		}
		
		buffers.normalBuffer.flip();
		
		// Create the color buffer
		numFloats = 0;
		
		for(float[] array : arrays.colorArrays) {
			numFloats += array.length;
		}
		
		buffers.colorBuffer = BufferUtils.createFloatBuffer(numFloats);
		
		for(float[] array : arrays.colorArrays) {
			buffers.colorBuffer.put(array);
		}
		
		buffers.colorBuffer.flip();
		
		// Create the tex coord buffer
		numFloats = 0;
		
	 	for(float[] array : arrays.texCoordArrays) {
			numFloats += array.length;
		}
		
	 	if(chunk.drawTextures)
	 	{
	 		buffers.texCoordBuffer = BufferUtils.createFloatBuffer(numFloats);
		
		for(float[] array : arrays.texCoordArrays) {
			buffers.texCoordBuffer.put(array);
		}
		
		buffers.texCoordBuffer.flip(); 
	 	}
	 	*/
	 	
	 	
	 	 
	 	
		/*
		// Delete old list (it will be recreated in the render method)
		if(glListIndex != 0) {
			GL11.glDeleteLists(glListIndex, 1);
			glListIndex = 0;
		}*/
	}
	
	
}
