package james;
import org.joml.Vector3f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import james.Audio.AudioLibrary;
import james.Audio.AudioManager;
import james.Audio.AudioSource;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL44C.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class Game {
    public static final int FPS_LIMIT = 0;
    public static final int FIXED_UPDATES_PER_SECOND = 120;
        
    public static Scene currentScene;

    /**
     * The total time spent between Update() calls, in seconds.
     */
	public static double deltaTime;

    /**
     * The total time spent between fixed updates, in seconds.
     */
	public static double fixedDeltaTime;

    /**
     * The total time spent rendering the previous frame, in seconds
     */
    public static double drawDeltaTime;

    /**
     * The total number of frames rendered, since game begin.
     */
    public static long frameCount;

    /**
     * The total time since program begin, in seconds. Identical to glfwGetTime() but updated once per update cycle.
     */
    public static double time;

    /**
     * The total time spent in the Draw function(), in seconds. 
     * Commonly used for profiling purposes.
     */
    public static double totalDrawTime;

    public static boolean wireframe = false;

    public Game(){
        Initialize();
        double lastDrawTime = 0.0;
        double lastFixedUpdateTime = 0.0;
        double lastUpdateTime = 0.0;

        while ( !glfwWindowShouldClose(Main.window) ) {
            time = glfwGetTime();

            //update input
			Input.Update();

            //update loop
            Update();
            deltaTime = glfwGetTime() - lastUpdateTime;
            lastUpdateTime = glfwGetTime();

            //fixed update
            fixedDeltaTime = glfwGetTime() - lastFixedUpdateTime;
            if (glfwGetTime() - lastFixedUpdateTime > (1.0 / (double)FIXED_UPDATES_PER_SECOND)) {
                lastFixedUpdateTime = glfwGetTime();
                
                //update input (fixed time)
                Input.UpdateFixedTime();

                FixedUpdate();
            }

            //rendering            
            if (FPS_LIMIT == 0 || glfwGetTime() - lastDrawTime > (1.0 / (double)FPS_LIMIT)) {
                lastDrawTime = glfwGetTime();

                //clear background
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                //draw everything
                Draw();

                //check events and swap buffers
                glfwPollEvents();
                glfwSwapBuffers(Main.window);

                frameCount++;
                drawDeltaTime = glfwGetTime() - lastDrawTime;
                totalDrawTime += drawDeltaTime;
            }
		}

        Free();
    }

    private void Initialize(){
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glDisable(GL_SAMPLE_SHADING);
        glDepthRange(-1.0f, 1.0f);

        //initialize input manager
        new Input();

        //initialize audio manager
        new AudioManager();
        AudioLibrary.SetListenerData(new Vector3f(0.0f), new Vector3f(0.0f));

        LoadScene(new MenuScene());
    }

    private void Update(){
        for(int i = 0; i < currentScene.objects.size(); i++){
            currentScene.objects.get(i).Update();

        }
        if(Input.GetKeyPressed(Input.KEY_K)){
            wireframe = !wireframe;
        }
        if(wireframe){
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }
        else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }
    
    private void FixedUpdate(){
        for(int i = 0; i < currentScene.objects.size(); i++){
            currentScene.objects.get(i).FixedUpdate();
        }
    }
    
    private void Draw(){
        for(int i = 0; i < currentScene.objects.size(); i++){
            currentScene.objects.get(i).Draw();
        }
    }

    public static void OnWindowResize(){
        if(currentScene == null) return;
        for(int i = 0; i < currentScene.windowResizeListeners.size(); i++){
            currentScene.windowResizeListeners.get(i).OnWindowResize();
        }
    }

    public static void LoadScene(Scene scene){
        if(currentScene != null) currentScene.OnSceneUnload();
        currentScene = scene;
        currentScene.LoadScene();
    }

    /**
     * Adds an object to the scene. The added object will be updated before all previously added objects.
     * @param object
     *      - game object to be added.
     */
    public static void AddObject(GameObject object){
        AddObject(object, 0);
    }
    /**
     * Adds an object to the scene with a given update order (lower => updated first)
     * @param object
     *      - the object to be added.
     * @param updateOrder
     *      - the order level at which the object is updated. In range [0, 99].
     */
    public static void AddObject(GameObject object, int updateOrder){
        if(currentScene == null) throw new Error("current Scene is NULL");  
        currentScene.objects.add(currentScene.updateOrderIndices[updateOrder], object);
        for(int i = updateOrder + 1; i < 100; i++){
            currentScene.updateOrderIndices[i]++;
        }
    } 

    public static void RemoveObject(GameObject object){
        if(currentScene == null) throw new Error("current Scene is NULL");
        int objIndex = currentScene.objects.indexOf(object);
        int objOrderLevel = 0;
        for(int i = 0; i < 100; i++){
            if(objIndex >= currentScene.updateOrderIndices[i]) objOrderLevel = i;
            else break;
        }
        for(int i = objOrderLevel; i < 100; i++){
            currentScene.updateOrderIndices[i]--;
        }
        currentScene.objects.remove(objIndex);
    }

    /**
     * Adds a window resize listener to the scene.
     * @param listener
     *      - the object whose OnWindowResize() function is to be called on a window resize event.
     */
    public static void AddWindowResizeListener(WindowResizeListener listener){
        if(currentScene == null) throw new Error("current scene is NULL");
        currentScene.windowResizeListeners.add(listener);
    }

    public static void RemoveWindowResizeListener(WindowResizeListener listener){
        if(currentScene == null) throw new Error("current scene is NULL");
        currentScene.windowResizeListeners.remove(listener);
    }

    private void Free(){
        AudioLibrary.Free();
    }
}
