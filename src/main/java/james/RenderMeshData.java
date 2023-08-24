package james;

/**
 * Packaged format for world meshes that may be uploaded to glsl buffers.
 */
public class RenderMeshData {
    
    // [0] = BOTTOM, [1] = TOP, [2] = LEFT, [3] = RIGHT, [4] = BACK, [5] = FRONT
    private static final boolean[] FACE_CULL = { true, false, false, true, false, true };
    //private static final boolean[] FACE_CULL = { false, false, false, false, false, false};

    public int vertexCount, culledIndexCount, completeIndexCount;
    public int width, depth;
    public float[] vertices;
    public int[] indices;
    
    /**
     * Takes a VoxelData object and packages it into the loaded world mesh format.
     * @param mesh
     *      - the mesh that is to be packaged.
     */
    public RenderMeshData(VoxelData mesh){
        int[] faceOrderIndex = new int[6];
        int curIndex = 0;
        for(int k = 0; k < 6; k++) {
            if(!FACE_CULL[k]){
                faceOrderIndex[curIndex] = k;
                curIndex++;
            }
        }
        for(int k = 0; k < 6; k++) {
            if(FACE_CULL[k]){
                faceOrderIndex[curIndex] = k;
                curIndex++;
            }
        }

        int[] faceCounts = new int[6];
        int[] faceoffsets = new int[6];
        vertexCount = culledIndexCount = completeIndexCount = 0;
        for(int k = 0; k < 6; k++){
            curIndex = faceOrderIndex[k];

            faceCounts[curIndex] = mesh.faceCounts[curIndex];
            for(int m = k + 1; m < 6; m++){
                faceoffsets[faceOrderIndex[m]] += faceCounts[curIndex];
            }
            
            vertexCount += 4 * faceCounts[curIndex];
            completeIndexCount += 6 * faceCounts[curIndex];
            if(!FACE_CULL[curIndex]) culledIndexCount += 6 * faceCounts[curIndex];
        }

        vertices = new float[4 * vertexCount];
        indices = new int[completeIndexCount];
        
        //copy indices
        for(int k = 0; k < 6; k++){
            curIndex = faceOrderIndex[k];

            for(int i = 0; i < faceCounts[curIndex]; i++){
                for(int m = 0; m < 6; m++){
                    indices[(i + faceoffsets[curIndex]) * 6 + m] = mesh.indices[(i + mesh.faceOffsets[curIndex]) * 6 + m] - mesh.faceOffsets[curIndex] * 4 + faceoffsets[curIndex] * 4;
                }
            }
        }

        //copy & package vertices
        int vertIndex = 0;
        for(int k = 0; k < 6; k++){
            curIndex = faceOrderIndex[k];

            for(int i = mesh.faceOffsets[curIndex] * 4; i < (mesh.faceOffsets[curIndex] + mesh.faceCounts[curIndex]) * 4; i++){
                vertices[vertIndex + 0] = (float)mesh.vertices[i][0] / 16.0f;
                vertices[vertIndex + 1] = (float)mesh.vertices[i][1] / 16.0f;
                vertices[vertIndex + 2] = (float)mesh.vertices[i][2] / 16.0f;
                vertices[vertIndex + 3] = Float.intBitsToFloat(
                    (mesh.colors[i] & 0xFF) << 24 | (curIndex & 0xFF) << 21 | (mesh.ambientOcclusion[i] & 0x3) << 19
                );

                if((int)vertices[vertIndex + 0] > width) width = (int)vertices[vertIndex + 0];
                if((int)vertices[vertIndex + 2] > depth) depth = (int)vertices[vertIndex + 2];
                
                vertIndex += 4;
            }
        }
    }
}