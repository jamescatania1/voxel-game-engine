package james;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.lwjgl.opengl.GL44C.*;

public class Shader {
    public int id;

    /**
     * Constructor for Shader object class.
     * @param vertexFileName
     *      - the file name of the vertex shader, relative to resources//shaders, including its extension.
     * @param fragmentFileName
     *      - the file name of the fragment shader, relative to resources//shaders, including its extension.
     */
    public Shader(String vertexFileName, String fragmentFileName) {
        int vertex, fragment;

        String vShaderCode = readFile(vertexFileName);
        String fShaderCode = readFile(fragmentFileName);

        
        vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, vShaderCode);
        glCompileShader(vertex);
        int[] vsuccess = {0};
        glGetShaderiv(vertex, GL_COMPILE_STATUS, vsuccess);
        if(vsuccess[0] == 0){
            throw new Error("vertex shader compilation failed\n\n" + vertexFileName + "\nshader log:\n" + glGetShaderInfoLog(vertex));
        }

        fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, fShaderCode);
        glCompileShader(fragment);
        int[] fsuccess = { 0 };
        glGetShaderiv(fragment, GL_COMPILE_STATUS, fsuccess);
        if(fsuccess[0] == 0){
            throw new Error("fragment shader compilation failed\n\n" + fragmentFileName + "\nshader log:\n" +  glGetShaderInfoLog(fragment));
        }

        id = glCreateProgram();
        glAttachShader(id, vertex);
        glAttachShader(id, fragment);
        glLinkProgram(id);
        int[] success = { 0 };
        glGetProgramiv(id, GL_LINK_STATUS, success);
        if (success[0] == 0) {
            throw new Error("shader program failed to link\nshader log:\n" + glGetProgramInfoLog(id));
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    /**
     * Constructor for a compute shader.
     * @param computeFileName
     *      - the file name of the compute shader, relative to resources//shaders, including its extension.
     */
    public Shader(String computeFileName){
        int compute;

        String cShaderCode = readFile(computeFileName);

        compute = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(compute, cShaderCode);
        glCompileShader(compute);
        int[] csuccess = {0};
        glGetShaderiv(compute, GL_COMPILE_STATUS, csuccess);
        if(csuccess[0] == 0){
            throw new Error("compute shader compilation failed\n\n" + computeFileName + "\nshader log:\n" + glGetShaderInfoLog(compute));
        }

        id = glCreateProgram();
        glAttachShader(id, compute);
        glLinkProgram(id);
        int[] success = { 0 };
        glGetProgramiv(id, GL_LINK_STATUS, success);
        if (success[0] == 0) {
            throw new Error("compute shader program failed to link\nshader log:\n" + glGetProgramInfoLog(id));
        }

        glDeleteShader(compute);
    }

    public void Use(){
        glUseProgram(id);
    }

    public void SetBool(String name, boolean value){
        Use();
        glUniform1i(glGetUniformLocation(id, name), value ? 1 : 0);
    }
    public void SetInt(String name, int value) {
        Use();
        glUniform1i(glGetUniformLocation(id, name), value);
    }
    public void SetFloat(String name, float value) {
        Use();
        glUniform1f(glGetUniformLocation(id, name), value);
    }
    public void SetFloatArray(String name, float[] value){
        Use();
        glUniform1fv(glGetUniformLocation(id, name), value);
    }
    public void SetVec2(String name, float x, float y) {
        Use();
        float[] value = {x, y};
        glUniform2fv(glGetUniformLocation(id, name), value);
    }
    public void SetMat4x4(String name, float[] value){
        Use();
        glUniformMatrix4fv(glGetUniformLocation(id, name), false, value);
    }
    /*
    public void SetColor(String name, Color color){
        Use();
        glUniform4fv(glGetUniformLocation(id, name), color.getComponents(null));
    }*/

    private String readFile(String file) {
        BufferedReader reader = null;
        StringBuilder sourceBuilder = new StringBuilder();
        try {
            String filePath  = new File("").getAbsolutePath() + Main.RESOURCE_PATH + "shaders\\" + file;
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
                
            String line;
                
            while ((line = reader.readLine()) != null) {
                sourceBuilder.append(line + "\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return sourceBuilder.toString();		
    }
}
