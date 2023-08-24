package james;

import static james.VoxelData.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.joml.Vector3i;

/**
 * Class for packaging model files into .voxel custom format as while as saving/loading .voxel files.
 */
public class VoxelPackager {

    //comment this out when not packaging to .voxel format
    
    private static final String PATH = "cottage_b.ply";
    public static void main(String[] args){
        long startTime = System.nanoTime();
        PackagePly("models\\" + PATH);
        //PackagePly("models\\" + "tree_cedar.ply");
        System.out.println("Successfully packaged " + PATH + " to .voxel, elapsed time " + String.valueOf(((double)(System.nanoTime() - startTime)) / 1000000000.0) + " seconds.");
    }

    /**
     * Loads a .voxel file to memory.
     * @param path
     *      - the path of the file to be loaded, relative to \\resources, and including the .voxel file extension.
     * @return
     *      - the populated VoxelMeshData object.
     */
    public static VoxelData LoadVoxelMesh(String path) {
        
        String filePath  = new File("").getAbsolutePath() + Main.RESOURCE_PATH + path;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("could not read voxel file " + filePath);
        }
        Error invalidVoxelFileError = new Error("invalid voxel file" + filePath);
        
        // this is actually disgusting to look at but it's naturally pretty encapsulated, so I don't care.
        // out of sight out of mind

        // name
        String[] currentLine = ReadNextLine(reader);
        String name = currentLine[0];

        // $ voxel_count
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        currentLine = ReadNextLine(reader);
        int voxelCount = Integer.parseInt(currentLine[0]);

        // $ vertex_count
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        currentLine = ReadNextLine(reader);
        int vertexCount = Integer.parseInt(currentLine[0]);

        // $ triangle_count
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        currentLine = ReadNextLine(reader);
        int triangleCount = Integer.parseInt(currentLine[0]);

        VoxelData result = new VoxelData(name, voxelCount, vertexCount, triangleCount);

        //face tri offsets
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        currentLine = ReadNextLine(reader);
        for(int i = 0; i < 6; i++) result.faceOffsets[i] = Integer.parseInt(currentLine[i]);

        //face tri counts
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        currentLine = ReadNextLine(reader);
        for(int i = 0; i < 6; i++) result.faceCounts[i] = Integer.parseInt(currentLine[i]);

        //vertices
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        for(int i = 0; i < vertexCount; i++){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
            result.vertices[i][0] = Integer.parseInt(currentLine[0]);
            result.vertices[i][1] = Integer.parseInt(currentLine[1]);
            result.vertices[i][2] = Integer.parseInt(currentLine[2]);
        }
        
        //indices
        currentLine = ReadNextLine(reader);
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        for(int i = 0; i < triangleCount; i++){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
            result.indices[i * 3 + 0] = Integer.parseInt(currentLine[0]);
            result.indices[i * 3 + 1] = Integer.parseInt(currentLine[1]);
            result.indices[i * 3 + 2] = Integer.parseInt(currentLine[2]);
        }

        
        //colors
        currentLine = ReadNextLine(reader);
        while(!currentLine[0].equals("$")) {
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        for(int i = 0; i < vertexCount; i++){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
            result.colors[i] = Integer.parseInt(currentLine[0]);
        }

        //ambient occlusion
        currentLine = ReadNextLine(reader);
        while(!currentLine[0].equals("$")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw invalidVoxelFileError;
        }
        for(int i = 0; i < vertexCount; i++){
            currentLine = ReadNextLine(reader);
            result.ambientOcclusion[i] = Integer.parseInt(currentLine[0]);
        }

        return result;
    }

    /**
     * Reads and packages .ply files in the point-cloud voxel format (point in MagicaVoxel).
     * Saves the packaged file in the .voxel format with the same name as its origin.
     * @param path
     *      - the path of the file to be packaged relative to \\resources, and including the file extension.
     */
    public static void PackagePly(String path) {
        String filePath  = new File("").getAbsolutePath() + Main.RESOURCE_PATH + path;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("could not read voxel file " + filePath);
        }

        String[] currentLine = ReadNextLine(reader);
        while(!currentLine[0].equals("element") || !currentLine[1].equals("vertex")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw new Error("invalid voxel file " + filePath);
        }
        int voxelCount = Integer.valueOf(currentLine[2]);
        while(!currentLine[0].equals("end_header")){
            currentLine = ReadNextLine(reader);
            if(currentLine == null) throw new Error("invalid voxel file " + filePath);
        }
        Vector3i[] voxels = new Vector3i[voxelCount];
        Color[] voxelColors = new Color[voxelCount];
        Vector3i minBd = new Vector3i();
        Vector3i maxBd = new Vector3i();
        for(int i = 0; i < voxelCount; i++) {
            currentLine = ReadNextLine(reader);
            voxels[i] = new Vector3i(Integer.parseInt(currentLine[0]), Integer.parseInt(currentLine[2]), Integer.parseInt(currentLine[1]));
            voxelColors[i] = new Color(Integer.parseInt(currentLine[3]), Integer.parseInt(currentLine[4]), Integer.parseInt(currentLine[5]), 256);

            if(voxels[i].x > maxBd.x) maxBd.x = voxels[i].x;
            if(voxels[i].y > maxBd.y) maxBd.y = voxels[i].y;
            if(voxels[i].z > maxBd.z) maxBd.z = voxels[i].z;
            if(voxels[i].x < minBd.x) minBd.x = voxels[i].x;
            if(voxels[i].y < minBd.y) minBd.y = voxels[i].y;
            if(voxels[i].z < minBd.z) minBd.z = voxels[i].z;
        }
        Color[][][] voxelData = new Color[maxBd.x - minBd.x + 1][maxBd.y - minBd.y + 1][maxBd.z - minBd.z + 1];
        for(int i = 0; i < voxelCount; i++){
            voxelData[voxels[i].x - minBd.x][voxels[i].y - minBd.y][voxels[i].z - minBd.z] = voxelColors[i];
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        VoxelData meshData = BuildVoxelMesh(voxelData);

        String voxelPath = path.replace(".ply", ".voxel");
        SaveVoxelMeshData(voxelPath, meshData);
    }

    
    /**
     * Builds the voxel mesh from the given color data. Packages into Face array format, calculates ambient occlusion and then calls
     * BuildGameMesh() with the analogous data.
     * @param data
     *      - null if no voxel, else the voxel's color. An intermediate form between the model's original file format and the .voxel.
     * @return the mesh data using integer (0,0,0)-indexed vertices and color pallet-indexed colors.
     */
    public static VoxelData BuildVoxelMesh(Color[][][] data){
        int xLen = data.length;
        int yLen = data[0].length;
        int zLen = data[0][0].length;

        int voxelCount = 0;
        Face[][][][] faceMask = new Face[6][xLen][yLen][zLen];
        for(int x = 0; x < xLen; x++){
            for(int y = 0; y < yLen; y++){
                for(int z = 0; z < zLen; z++){
                    if(data[x][y][z] == null) continue;
                    voxelCount++;
                    
                    if(z == 0 || data[x][y][z - 1] == null) 
                        faceMask[BACK][x][y][z] = new Face(data[x][y][z].rgba_8_8_8_8, x, y, z, 1, 1);
                    if(z == zLen - 1 || data[x][y][z + 1] == null) 
                        faceMask[FRONT][x][y][z] = new Face(data[x][y][z].rgba_8_8_8_8, x, y, z, 1, 1);
                    
                    if(x == 0 || data[x - 1][y][z] == null) 
                        faceMask[LEFT][x][y][z] = new Face(data[x][y][z].rgba_8_8_8_8, x, y, z, 1, 1);
                    if(x == xLen - 1 || data[x + 1][y][z] == null) 
                        faceMask[RIGHT][x][y][z] = new Face(data[x][y][z].rgba_8_8_8_8, x, y, z, 1, 1);
                    
                    if(y == 0 || data[x][y - 1][z] == null) 
                        faceMask[BOTTOM][x][y][z] = new Face(data[x][y][z].rgba_8_8_8_8, x, y, z, 1, 1);
                    if(y == yLen - 1 || data[x][y + 1][z] == null) 
                        faceMask[TOP][x][y][z] = new Face(data[x][y][z].rgba_8_8_8_8, x, y, z, 1, 1);
                }
            }
        }
        
        //calculate ambient occlusion values
        for(int k = 0; k < 6; k++){
            for(int x = 0; x < xLen; x++){
                for(int y = 0; y < yLen; y++){
                    for(int z = 0; z < zLen; z++){
                        if(faceMask[k][x][y][z] == null) continue;
                        Face face = faceMask[k][x][y][z];

                        boolean[][] neighbors = new boolean[3][3];
                        if(k == TOP){
                            if(y == yLen - 1) continue;
                            for(int u = -1; u <= 1; u++){
                                for(int v = -1; v <= 1; v++){
                                    if(x + u < 0 || x + u >= xLen || z + v < 0 || z + v >= zLen) continue;
                                    neighbors[u + 1][v + 1] = data[x + u][y + 1][z + v] != null ? true : false;
                                }
                            }
                        }
                        if(k == LEFT){
                            if(x == 0) continue;
                            for(int u = -1; u <= 1; u++){
                                for(int v = -1; v <= 1; v++){
                                    if(y + u < 0 || y + u >= yLen || z + v < 0 || z + v >= zLen) continue;
                                    neighbors[u + 1][v + 1] = data[x - 1][y + u][z + v] != null ? true : false;
                                }
                            }
                            if(y == 0) face.ao[0] = face.ao[3] = 2;
                        }
                        if(k == BACK){
                            if(z == 0) continue;
                            for(int u = -1; u <= 1; u++){
                                for(int v = -1; v <= 1; v++){
                                    if(x + u < 0 || x + u >= xLen || y + v < 0 || y + v >= yLen) continue;
                                    neighbors[u + 1][v + 1] = data[x + u][y + v][z - 1] != null ? true : false;
                                }
                            }
                            if(y == 0) face.ao[0] = face.ao[1] = 2;
                        }
                        face.ao[0] = Math.max(face.ao[0], CalculateAO(neighbors[0][0], neighbors[1][0], neighbors[0][1]));
                        face.ao[1] = Math.max(face.ao[1], CalculateAO(neighbors[2][0], neighbors[1][0], neighbors[2][1]));
                        face.ao[2] = Math.max(face.ao[2], CalculateAO(neighbors[2][2], neighbors[1][2], neighbors[2][1]));
                        face.ao[3] = Math.max(face.ao[3], CalculateAO(neighbors[0][2], neighbors[0][1], neighbors[1][2]));
                    }
                }
            }
        }

        VoxelData result = BuildGameMesh(faceMask);
        result.voxelCount = voxelCount;
        return result;
    }

    /**
     * Builds a Voxel Data object from an array of Faces. Performs greedy meshing and packages it appropriately to the intermediate
     * VoxelData format.
     * @param faceMask
     *      - array of faces, arranged as Face[face index][x][y][z].
     * @return
     *      A populated VoxelData object. The field "voxelCount" is not assigned to here, as some meshes may not be voxel based.
     */
    public static VoxelData BuildGameMesh(Face[][][][] faceMask){
        int xLen = faceMask[0].length;
        int yLen = faceMask[0][0].length;
        int zLen = faceMask[0][0][0].length;

        //greedy meshing
        ArrayList<Face> faces = new ArrayList<>();
        int[] faceOffsets = new int[6];
        int[] faceCounts = new int[6];
        for(int k = 0; k < 6; k++){
            for(int x = 0; x < xLen; x++){
                for(int y = 0; y < yLen; y++){
                    for(int z = 0; z < zLen; z++){
                        if(faceMask[k][x][y][z] == null) continue;
                        int w = 0;
                        int h = 0;
                        do { w++; } while(faceMask[k][x][y][z].equals(GetNeighbor(k, faceMask, x, y, z, w, 0)));
                        do { h++; } while(faceMask[k][x][y][z].equals(GetNeighbor(k, faceMask, x, y, z, 0, h)));

                        int[] sliceHeights = new int[w];
                        for(int u = 0; u < w; u++) {
                            for(int v = 0; v < h; v++) {
                                if(faceMask[k][x][y][z].equals(GetNeighbor(k, faceMask, x, y, z, u, v)) == false){
                                    break;
                                }
                                sliceHeights[u]++;
                            }
                            if(u > 0) sliceHeights[u] = Math.min(sliceHeights[u], sliceHeights[u - 1]);
                        }
                        int width = 1, height = 1;
                        for(int u = 1; u <= w; u++){
                            for(int v = 1; v <= h; v++){
                                if(u * Math.min(sliceHeights[u - 1], v) > width * height){
                                    width = u;
                                    height = Math.min(sliceHeights[u - 1], v);
                                }
                            }
                        }

                        //disables greedy meshing
                        //width = height = 1;

                        faces.add(new Face(k, faceMask[k][x][y][z].color, faceMask[k][x][y][z].ao, x, y, z, width, height));
                        for(int m = k + 1; m < 6; m++) faceOffsets[m]++;
                        faceCounts[k]++;
                        for(int u = 0; u < width; u++){
                            for(int v = 0; v < height; v++){
                                SetNeighbor(k, faceMask, x, y, z, u, v, null);
                            }
                        }
                    }
                }
            }
        }

        int[] faceIndex = new int[6];
        int[][] vertices = new int[faces.size() * 4][3];
        int[] indices = new int[faces.size() * 6];
        int[] colors = new int[faces.size() * 4];
        int[] amientOcclusion = new int[faces.size() * 4];
        for(int i = 0; i < faces.size(); i++){
            Face face = faces.get(i);
            int k = face.k; int x = face.x; int y = face.y; int z = face.z; int du = face.du; int dv = face.dv;
            
            int vertIndex = (faceOffsets[k] + faceIndex[k]) * 4;
            int triIndex = (faceOffsets[k] + faceIndex[k]) * 6;
            if(k == TOP){
                vertices[vertIndex + 0] = new int[]{ x, y + 1, z };
                vertices[vertIndex + 1] = new int[]{ x + du, y + 1, z };
                vertices[vertIndex + 2] = new int[]{ x + du, y + 1, z + dv };
                vertices[vertIndex + 3] = new int[]{ x, y + 1, z + dv};
            }
            if(k == BOTTOM){
                vertices[vertIndex + 0] = new int[]{ x, y, z };
                vertices[vertIndex + 3] = new int[]{ x + du, y, z };
                vertices[vertIndex + 2] = new int[]{ x + du, y, z + dv };
                vertices[vertIndex + 1] = new int[]{ x, y, z + dv};
            }
            if(k == LEFT){
                vertices[vertIndex + 0] = new int[]{ x, y, z };
                vertices[vertIndex + 1] = new int[]{ x, y + du, z };
                vertices[vertIndex + 2] = new int[]{ x, y + du, z + dv };
                vertices[vertIndex + 3] = new int[]{ x, y, z + dv};
            }
            if(k == RIGHT){
                vertices[vertIndex + 0] = new int[]{ x + 1, y, z };
                vertices[vertIndex + 3] = new int[]{ x + 1, y + du, z };
                vertices[vertIndex + 2] = new int[]{ x + 1, y + du, z + dv };
                vertices[vertIndex + 1] = new int[]{ x + 1, y, z + dv};
            }
            if(k == BACK){
                vertices[vertIndex + 0] = new int[]{ x, y, z };
                vertices[vertIndex + 1] = new int[]{ x + du, y, z };
                vertices[vertIndex + 2] = new int[]{ x + du, y + dv, z };
                vertices[vertIndex + 3] = new int[]{ x, y + dv, z };
            }
            if(k == FRONT){
                vertices[vertIndex + 0] = new int[]{ x, y, z + 1 };
                vertices[vertIndex + 3] = new int[]{ x + du, y, z + 1 };
                vertices[vertIndex + 2] = new int[]{ x + du, y + dv, z + 1 };
                vertices[vertIndex + 1] = new int[]{ x, y + dv, z + 1 };
            }

            amientOcclusion[vertIndex + 0] = face.ao[0];
            amientOcclusion[vertIndex + 1] = face.ao[1];
            amientOcclusion[vertIndex + 2] = face.ao[2];
            amientOcclusion[vertIndex + 3] = face.ao[3];

            for(int m = 0; m < 4; m++) {
                colors[vertIndex + m] = face.color;
            }
            
            //flip quad dependent on ambient occlusion to maintain consistent smoothing
            if(face.ao[0] + face.ao[2] > face.ao[1] + face.ao[3]) {
                indices[triIndex + 0] = i * 4 + 1; 
                indices[triIndex + 1] = i * 4 + 2;
                indices[triIndex + 2] = i * 4 + 3;
                indices[triIndex + 3] = i * 4 + 3;
                indices[triIndex + 4] = i * 4 + 0;
                indices[triIndex + 5] = i * 4 + 1;
            }
            else{
                indices[triIndex + 0] = i * 4 + 0; 
                indices[triIndex + 1] = i * 4 + 1;
                indices[triIndex + 2] = i * 4 + 2;
                indices[triIndex + 3] = i * 4 + 2;
                indices[triIndex + 4] = i * 4 + 3;
                indices[triIndex + 5] = i * 4 + 0;
            }
            faceIndex[k]++;
        }
        return new VoxelData(0, vertices, indices, faceOffsets, faceCounts, colors, amientOcclusion);
    }

    private static class Face{
        public int color;
        public int[] ao;
        public int k, x, y, z, du, dv;

        public Face(int color, int x, int y, int z, int du, int dv){
            this.color = color;
            this.x = x; this.y = y; this.z = z;
            this.du = du; this.dv = dv;
            this.ao = new int[4];
        }

        public Face(int k, int color, int[] ao, int x, int y, int z, int du, int dv){
            this.k = k;
            this.color = color;
            this.ao = ao;
            this.x = x;
            this.y = y;
            this.z = z;
            this.du = du;
            this.dv = dv;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null) return false;
            Face cmp = (Face)obj;
            return (color == cmp.color && ao[0] == cmp.ao[0] && ao[1] == cmp.ao[1] && ao[2] == cmp.ao[2] && ao[3] == cmp.ao[3]);
        }
    }

    private static int CalculateAO(boolean corner, boolean s1, boolean s2) {
        if(s1 && s2) {
            return 3;
        }
        return (s1 ? 1 : 0) + (s2 ? 1 : 0) + (corner ? 1 : 0);
    }

    private static Face GetNeighbor(int faceDirection, Face[][][][] data, int x, int y, int z, int du, int dv){
        if(faceDirection == TOP || faceDirection == BOTTOM) {
            if(x + du >= data[faceDirection].length || z + dv >= data[faceDirection][0][0].length) return null;
            return data[faceDirection][x + du][y][z + dv];
        } 
        if(faceDirection == RIGHT || faceDirection == LEFT) {
            if(y + du >= data[faceDirection][0].length || z + dv >= data[faceDirection][0][0].length) return null;
            return data[faceDirection][x][y + du][z + dv];
        }
        if(x + du >= data[faceDirection].length || y + dv >= data[faceDirection][0].length) return null;
        else return data[faceDirection][x + du][y + dv][z];
    }

    private static void SetNeighbor(int faceDirection, Face[][][][] data, int x, int y, int z, int du, int dv, Face value){
        if(faceDirection == TOP || faceDirection == BOTTOM) {
            data[faceDirection][x + du][y][z + dv] = value;
        } 
        else if(faceDirection == RIGHT || faceDirection == LEFT) {
            data[faceDirection][x][y + du][z + dv] = value;
        }
        else data[faceDirection][x + du][y + dv][z] = value;
    }

    /**
     * Saves the voxel mesh data as a .voxel file.
     * @param path
     *      - the path, relative to //resources, including the .voxel file extension.
     * @param data
     *      - the voxel mesh data to be saved.
     */
    public static void SaveVoxelMeshData(String path, VoxelData data){
        String filePath  = new File("").getAbsolutePath() + Main.RESOURCE_PATH + path;
        BufferedWriter writer = null;
        try{
            writer = new BufferedWriter(new FileWriter(filePath));

            //write model name
            String modelName = path.replace(".voxel", "");
            modelName = modelName.substring(modelName.lastIndexOf("\\") + 1, modelName.length());
            writer.write(modelName + "\n");

            //model properties
            writer.write("$ voxel_count\n");
            writer.write(String.valueOf(data.voxelCount) + "\n");
            writer.write("$ vertex_count\n");
            writer.write(String.valueOf(data.vertices.length) + "\n");
            writer.write("$ triangle_count\n");
            writer.write(String.valueOf(data.indices.length / 3) + "\n");
            writer.write("$ face_offsets\n");
            for(int k = 0; k < 5; k++) writer.write(String.valueOf(data.faceOffsets[k]) + " ");
            writer.write(String.valueOf(data.faceOffsets[5]) + "\n");
            writer.write("$ face_counts\n");
            for(int k = 0; k < 5; k++) writer.write(String.valueOf(data.faceCounts[k]) + " ");
            writer.write(String.valueOf(data.faceCounts[5]) + "\n");

            //model data
            writer.write("$ vertices\n");
            for(int i = 0; i < data.vertices.length; i++){
                writer.write(String.valueOf(data.vertices[i][0]) + " " + String.valueOf(data.vertices[i][1]) + " " + String.valueOf(data.vertices[i][2]) + "\n");
            }
            writer.write("$ indices\n");
            for(int i = 0; i < data.indices.length; i += 3){
                writer.write(String.valueOf(data.indices[i]) + " " + String.valueOf(data.indices[i + 1]) + " " + String.valueOf(data.indices[i + 2]) + "\n");
            }
            writer.write("$ colors\n");
            for(int i = 0; i < data.colors.length; i++){
                writer.write(String.valueOf(data.colors[i]) + "\n");
            }
            writer.write("$ ambient_occlusion\n");
            for(int i = 0; i < data.ambientOcclusion.length; i++){
                writer.write(String.valueOf(data.ambientOcclusion[i]) + "\n");
            }
            writer.close();

        } catch (IOException e) {
            System.out.println("could not save file " + filePath + " to the system");
            e.printStackTrace();
        }

    }

    /**
     * @param reader
     * @return the reader's next line's words, delimited by spaces.
     */
    private static String[] ReadNextLine(BufferedReader reader) {
        String line = null;
        try {
            line = reader.readLine();
        } catch (IOException e) {}
        if(line == null) return null;
        
        return line.split(" ");
    }
}