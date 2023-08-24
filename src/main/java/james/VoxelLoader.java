package james;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

/**
 * Class that loads and indexes all the .voxel files at initialization. The active color palette MUST be
 * initialized before the constructor is called.
 */
public class VoxelLoader {

    /**
     * The VoxelMeshData objects that are loaded in memory, indexed by their name (without the file extension).
     * Colors in the mesh objects are indexed by their color palette indicies, NOT in the form RGBA8888.
     */
    public static HashMap<String, VoxelData> meshObjects;

    public VoxelLoader(){
        meshObjects = LoadMeshObjects();
    }

    private HashMap<String, VoxelData> LoadMeshObjects(){
        String filePath  = new File("").getAbsolutePath() + Main.RESOURCE_PATH + "models";
        File directory = new File(filePath);
        
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".voxel");
            }
        });
        if(files == null) return null;
        
        HashMap<String, VoxelData> result = new HashMap<>();
        for(int i = 0; i < files.length; i++){
            String meshPath = files[i].getAbsolutePath();
            meshPath = meshPath.substring(meshPath.lastIndexOf("resources\\") + 10, meshPath.length());
            VoxelData mesh = VoxelPackager.LoadVoxelMesh(meshPath);

            //set the mesh's colors to be that of the active color palette.
            for(int j = 0; j < mesh.colors.length; j++){
                mesh.colors[j] = ColorPalette.GetColorPaletteIndex(mesh.colors[j]);
            }

            result.put(mesh.name, mesh);
        }

        //update the color palette buffer
        ColorPalette.UpdateColorUBO();

        return result;
    }
}
