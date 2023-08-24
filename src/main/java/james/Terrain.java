package james;

//import static org.lwjgl.opengl.GL44C.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import org.joml.Vector2i;

public class Terrain {
    
    public Terrain(){

        SceneRenderer.AddMeshInstance("world_base", 0, 0);
        //SceneRenderer.instance.instanceRenderers.get("world_base").shadowpassRenderFrontFace = true;
        //System.out.println(new Color(51, 149, 61, 255).rgba_8_8_8_8);
        //System.out.println(new Color(54, 45, 33, 255).rgba_8_8_8_8);
        /*
        //set the mesh's colors to be that of the active color palette.
        for(int j = 0; j < terrainMesh.colors.length; j++){
            terrainMesh.colors[j] = ColorPalette.GetColorPaletteIndex(terrainMesh.colors[j]);
        }
        ColorPalette.UpdateColorUBO();

        terrainMesh.name = "terrain";
        VoxelInstanceRenderer.AddMesh(terrainMesh);
        VoxelInstanceRenderer.AddInstance(terrainMesh.name, 0, 0);*/
    }
}
