package james;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL44C.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
	public static final String RESOURCE_PATH = "\\src\\main\\resources\\";

	public static final boolean MSAA_ENABLED = true;
	private static final int MSAA_SAMPLES = 4;
	
	public static final int DEFAULT_WINDOW_WIDTH = 1024;
	public static final int DEFAULT_WINDOW_HEIGHT = 768;

	// The window handle
	public static long window;
	public static int windowX = 1024;
	public static int windowY = 768;
	public static int multiSampleSamples;
	public static int maxMultisampleSamples;
	public static int shadowmapSize = 2048;

	public void run() {
		//System.out.println("LWJGL version " + Version.getVersion());
		
		init();
		
		//create and run game
		new Game();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.as
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		//glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

		//create a hidden window in order to query the maximum hardware-supported multisample levels
		long multisampleQueryWindow = glfwCreateWindow(windowX, windowY, "Engine", NULL, NULL);
		if ( multisampleQueryWindow == NULL )
		throw new RuntimeException("Failed to create the GLFW window");

		glfwMakeContextCurrent(multisampleQueryWindow);
		GL.createCapabilities();

		//get the maximum number of hardware-supported multisample levels and destroy the query window
		int[] maxMultiSampleSamplesBuffer = new int[1];
		glGetIntegerv(GL_MAX_SAMPLES, maxMultiSampleSamplesBuffer);
		maxMultisampleSamples = maxMultiSampleSamplesBuffer[0];
		glfwDestroyWindow(multisampleQueryWindow);

		if(MSAA_ENABLED) glfwWindowHint(GLFW_SAMPLES, Math.min(maxMultisampleSamples, MSAA_SAMPLES));
		multiSampleSamples = MSAA_ENABLED ? Math.min(maxMultisampleSamples, MSAA_SAMPLES) : 1;

		// Create the window
		window = glfwCreateWindow(windowX, windowY, "Engine", NULL, NULL);
		if ( window == NULL )
		throw new RuntimeException("Failed to create the GLFW window");
		
		glfwSetWindowSizeCallback(window, (window, width, height) -> windowResizeCallback(width, height));
		
		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
				window,
				(vidmode.width() - pWidth.get(0)) / 2,
				(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Disable v-sync
		glfwSwapInterval(0);

		// Make the window visible
		glfwShowWindow(window);
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();
		

		//opengl context settings:

		//clear color
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		//blend settings
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_BLEND);

		//msaa
		if(MSAA_ENABLED) glEnable(GL_MULTISAMPLE);
		else glDisable(GL_MULTISAMPLE);
	}

	private void windowResizeCallback(int width, int height){
		glViewport(0, 0, width, height);
		windowX = width;
		windowY = height;
		Game.OnWindowResize();
	}

	public static void main(String[] args) {
		new Main().run();
	}

}