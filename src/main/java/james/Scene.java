package james;

import java.util.ArrayList;

public abstract class Scene {
    public ArrayList<GameObject> objects;
    public int[] updateOrderIndices;

    public ArrayList<WindowResizeListener> windowResizeListeners;
    private int textureUnitCount = 0;

    public Scene(){
        objects = new ArrayList<GameObject>();
        updateOrderIndices = new int[100];
        for(int i = 0; i < 100; i++) updateOrderIndices[i] = 0;
        
        windowResizeListeners = new ArrayList<>();
    }

    public int GetNextTextureUnit(){
        textureUnitCount++;
        if(textureUnitCount > 16) throw new Error("too many active texture units. will have to rebind some unit(s) at runtime instead");
        return textureUnitCount - 1;
    }

    public abstract void LoadScene();
    public abstract void OnSceneUnload();
}