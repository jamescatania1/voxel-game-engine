package james;

/**
 * Stores voxel mesh data, in the form it is stored in the .voxel files.
 */
public class VoxelData {
    public static int BOTTOM = 0, TOP = 1, LEFT = 2, RIGHT = 3, BACK = 4, FRONT = 5;

    /**
     * The mesh's name.
     */
    public String name;

    /**
     * The total number of voxels in the mesh.
     */
    public int voxelCount;

    /**
     * The zero-indexed vertices, in which for any vertex i,
     * vertices[i] = {x, y, z}.
     */
    public int[][] vertices;

    /**
     * The mesh's triangles.
     */
    public int[] indices;

    /**
     * The vertex color of each vertex, in either RGBA_8888 format, or indexed by the ColorPallet, depending
     * on the loading state of the mesh.
     */
    public int[] colors;

    /**
     * The point of the first index in indices of the given VOXELFACE_*.
     */
    public int[] faceOffsets;

    /**
     * The number of triangles for the given VOXELFACE_*.
     */
    public int[] faceCounts;

    /**
     * The ambient occlusion value for each vertex.
     */
    public int[] ambientOcclusion;

    /**
     * Creates a voxel mesh data object with the presumably initialized data.3
     * @param voxelCount
     * @param vertices
     * @param indices
     * @param faceOffsets
     * @param faceCounts
     * @param colors
     * @param ambientOcclusion
     */
    public VoxelData(int voxelCount, int[][] vertices, int[] indices, int[] faceOffsets, int[] faceCounts, int[] colors, int[] ambientOcclusion){
        this.name = "";
        this.voxelCount = voxelCount;
        this.vertices = vertices;
        this.indices = indices;
        this.faceOffsets = faceOffsets;
        this.faceCounts = faceCounts;
        this.colors = colors;
        this.ambientOcclusion = ambientOcclusion;
    }
    
    /**
     * Creates a voxel mesh data and allocates arrays to a proper size.
     * @param name
     * @param voxelCount
     * @param vertexCount
     * @param triangleCount
     */
    public VoxelData(String name, int voxelCount, int vertexCount, int triangleCount){
        this.name = name;
        this.voxelCount = voxelCount;
        this.vertices = new int[vertexCount][3];
        this.indices = new int[3 * triangleCount];
        this.faceOffsets = new int[6];
        this.faceCounts = new int[6];
        this.colors = new int[vertexCount];
        this.ambientOcclusion = new int[vertexCount];
    }
}