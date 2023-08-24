package james;

//import james.Audio.AudioLibrary;
import james.UI.*;
import james.UI.Library.Alignment;
import james.UI.Library.Anchor;
import james.UI.Library.ClipDirection;
import james.UI.Library.ConstraintDimension;
import james.UI.Library.MeshUpdateMode;
import james.UI.Library.OverflowMode;
import james.UI.Library.ScalingFactor;
import james.UI.Library.ToggleMode;
import james.UI.Library.WrapMode;

import static james.UI.Library.*;

import org.joml.*;
import java.util.Random;

//import static org.lwjgl.opengl.GL44C.*;

public class GameScene extends Scene {

    public static enum GameState { Playing, Paused };
    public static GameState state;

    public void LoadScene(){
        InitUI();

        
        //initialize color palette
        new ColorPalette();
        
        new Lighting();
        new Camera();


        new CullingQuadTree();
 
        //initialize voxel mesh loader and load meshes
        new VoxelLoader();

        //initialize the main scene renderer
        new SceneRenderer();

        //initialize voxel renderer
        SceneRenderer.LoadAllMeshes();

        Random random = new Random();
        for(int i = 0; i < 256; i++){
            for(int j = 0; j < 256; j++){
                int rand = random.nextInt(500);
                if(rand < 80){
                    SceneRenderer.AddMeshInstance("tree_cedar", i + 128, j + 128);
                }
                else if(rand < 82){
                    SceneRenderer.AddMeshInstance("cottage_b", i + 128, j + 128);
                }
                else if(rand == 83){
                    SceneRenderer.AddMeshInstance("monu2", i + 128, j + 128);
                }
            }
        }

        //SceneRenderer.AddMeshInstance("cottage_b", 206, 226);
        //SceneRenderer.AddMeshInstance("cottage_b", 212, 228);

        new Terrain();

        state = GameState.Playing;
    }

    public void OnSceneUnload(){

    }

    private void InitUI(){
        Game.AddObject(new UIManager(), 99);

        UIManager.instance.scaleFactor = 1.0;

        Font titleFont = new Font("titleFont", "coolveltica.png", "coolveltica.fnt", GetNextTextureUnit(), 0.35f, 0.25f, 48);
        Font bodyFont = new Font("bodyFont", "opensans.png", "opensans.fnt", GetNextTextureUnit(), 0.46f, 0.2f, 7);
        AddFont(titleFont);
        AddFont(bodyFont);

        new ProfilerPanel();

        Button cornerPauseButton = new Button( 
            new Transform(UIManager.instance.canvas, Anchor.TopRight, Anchor.TopRight, 0,
                new ScaledRelativeConstraint(0.035f, false),
                new ScaledRelativeConstraint(0.035f, true),
                new Vector2f(-0.2f, -0.2f), ScalingFactor.WidthHeight, new Vector4f(0.0f)), ToggleMode.CanToggle, 50f, 50f, 50f);
        cornerPauseButton.AddPanel(
            new Panel(Color.WHITE, new Transform(cornerPauseButton.transform, Anchor.Center, Anchor.Center, 0,
            new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f))), 
            new Color(1.0, 1.0, 1.0, 1.0), new Color(1.0, 1.0, 1.0, 0.9), new Color(1.0, 1.0, 1.0, 1.0), new Color(1.0, 1.0, 1.0, 1.0));
        cornerPauseButton.panels.get(0).SetCornerRadii(4, 4, 4, 4);
        cornerPauseButton.panels.get(0).SetBorder(0, 5, new Color(37, 70, 111, 60));
        cornerPauseButton.AddImage(
            new Image(Color.WHITE, new Transform(cornerPauseButton.transform, Anchor.Center, Anchor.Center, 1,
            new RelativeConstraint(ConstraintDimension.ParentWidth, 0.8f), new RelativeConstraint(ConstraintDimension.ParentHeight, 0.8f)), ATLASIMAGE_SETTINGSBUTTON), 
            new Color(37, 70, 111, 256), new Color(37, 70, 111, 256), new Color(37, 70, 111, 256), new Color(37, 70, 111, 256));
        cornerPauseButton.AddListener(new ButtonAudio("button_01.wav", "", "", "", "", "", 1.0f, 0.95f));
        
        //Panel crosshair = new Panel(new Color(0.0, 1.0, 1.0, 1.0), new Transform(UIManager.instance.canvas, Anchor.Center, Anchor.Center, 0, 
        //new ScaledRelativeConstraint(0.005f, false), new ScaledRelativeConstraint(0.005f, true)));
        //AddPanel(crosshair);
        
        Panel pausePanel = new Panel(new Color(256, 256, 256, 220), 
        new Transform(UIManager.instance.canvas, Anchor.Center, Anchor.Center, 0, 
        new ScaledRelativeConstraint(0.5f, false), new ScaledRelativeConstraint(0.6f, true)));
        cornerPauseButton.AddListener(new PauseButton(cornerPauseButton, pausePanel));
        pausePanel.SetCornerRadii(7, 7, 7, 7);
        pausePanel.SetBorder(0, 11, new Color(37, 70, 111, 60));
        AddPanel(pausePanel);
        Text pausePanelText = new Text("OPTIONS", "titleFont", 3.0f, new Color(37, 70, 111, 256), Alignment.Center, Alignment.Center, WrapMode.Overflow, OverflowMode.Clip,
        MeshUpdateMode.Static, new Transform(
            pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
            new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), 
            new AspectConstraint(0.15f)));
        AddText(pausePanelText);
        Panel pausePanelTopLine = new Panel(new Color(0, 0, 0, 80),
            new Transform(pausePanelText.transform, Anchor.Bottom, Anchor.Top, 1,
            new RelativeConstraint(ConstraintDimension.ParentWidth, 1.2f), 
            new AspectConstraint(0.004f)));
        pausePanelTopLine.AddCullRect(pausePanel.transform, ClipDirection.ClipOutside, 0);
        AddPanel(pausePanelTopLine);
        
        Button pauseExitButton = new Button(new Transform(pausePanel.transform, Anchor.TopRight, Anchor.TopRight, 1,
        new RelativeConstraint(ConstraintDimension.ParentWidth, 0.045f), new AspectConstraint(1.0f), new Vector2f(-0.8f, -0.8f), ScalingFactor.WidthHeight, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        pauseExitButton.AddPanel(new Panel(new Color(256, 256, 256), new Transform(
            pauseExitButton.transform, Anchor.Center, Anchor.Center, 2, new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f),
            new AspectConstraint(1.0f))), new Color(240, 240, 240, 100), new Color(200, 200, 200, 140), new Color(37, 70, 111, 205), new Color(37, 70, 111, 255));
            pauseExitButton.panels.get(0).SetCornerRadii(6, 6, 6, 6);
            pauseExitButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 100));
        pauseExitButton.AddImage(new Image(new Color(0), new Transform(
            pauseExitButton.transform, Anchor.Center, Anchor.Center, 3, new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f),
            new AspectConstraint(1.0f)), ATLASIMAGE_EXITBUTTON), new Color(111, 37, 37, 255), new Color(111, 37, 37, 255), new Color(111, 37, 37, 255), new Color(111, 37, 37, 255));
        pauseExitButton.AddListener(new PauseExitButton(cornerPauseButton, pausePanel));

        float pausePanelButtonsWidth = 0.67f;
        float pausePanelButtonsHeight = 0.124f;

        Button settingsButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, -2.5f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        settingsButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        settingsButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        settingsButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        settingsButton.AddText("SETTINGS", "titleFont", 1.8f, new Color(37, 70, 111, 256), 2);

        Button helpButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, settingsButton.transform.positionOffset.y - 1.5f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        helpButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        helpButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        helpButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        helpButton.AddText("HELP", "titleFont", 1.8f, new Color(37, 70, 111, 256), 2);

        Panel pausePanelMidline = new Panel(new Color(0, 0, 0, 80),
            new Transform(helpButton.transform, Anchor.Bottom, Anchor.Top, 1,
            new RelativeConstraint(ConstraintDimension.ParentWidth, 1.25f), 
            new AspectConstraint(0.004f), new Vector2f(0.0f, -0.5f), ScalingFactor.ParentHeight, new Vector4f(0.0f)));
        AddPanel(pausePanelMidline);

        Button saveGameButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, helpButton.transform.positionOffset.y - 2.05f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        saveGameButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        saveGameButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        saveGameButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        saveGameButton.AddText("SAVE GAME", "titleFont", 1.8f, new Color(37, 70, 111), 2);

        Button saveGameAsButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, saveGameButton.transform.positionOffset.y - 1.5f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        saveGameAsButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        saveGameAsButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        saveGameAsButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        saveGameAsButton.AddText("SAVE AS...", "titleFont", 1.8f, new Color(37, 70, 111, 256), 2);

        Panel pausePanelBottomLine = new Panel(new Color(0, 0, 0, 80),
            new Transform(saveGameAsButton.transform, Anchor.Bottom, Anchor.Top, 1,
            new RelativeConstraint(ConstraintDimension.ParentWidth, 1.25f), 
            new AspectConstraint(0.004f), new Vector2f(0.0f, -0.5f), ScalingFactor.ParentHeight, new Vector4f(0.0f)));
        AddPanel(pausePanelBottomLine);

        Button resumeGameButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, saveGameAsButton.transform.positionOffset.y - 2.05f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        resumeGameButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        resumeGameButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        resumeGameButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        resumeGameButton.AddText("RESUME GAME", "titleFont", 1.8f, new Color(37, 111, 60), 2);
        resumeGameButton.AddListener(new PauseExitButton(cornerPauseButton, pausePanel));

        Button exitToMenuButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, resumeGameButton.transform.positionOffset.y - 1.5f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        exitToMenuButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        exitToMenuButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        exitToMenuButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        exitToMenuButton.AddText("EXIT TO MENU", "titleFont", 1.8f, new Color(37, 70, 111, 256), 2);
        exitToMenuButton.AddListener(new PauseReturnToMenuButton());

        Button exitToDesktopButton = new Button(new Transform(pausePanel.transform, Anchor.Top, Anchor.Top, 1, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, pausePanelButtonsWidth), new AspectConstraint(pausePanelButtonsHeight),
        new Vector2f(0.0f, exitToMenuButton.transform.positionOffset.y - 1.5f), ScalingFactor.Height, new Vector4f(0.0f)),
        ToggleMode.NoToggle, 50f, 50f, 50f);
        exitToDesktopButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        exitToDesktopButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        exitToDesktopButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        exitToDesktopButton.AddText("QUIT TO DESKTOP", "titleFont", 1.8f, new Color(111, 38, 37, 256), 2);
        exitToDesktopButton.AddListener(new PauseQuitToDesktopButton());

        pausePanel.transform.SetAllHidden(true);


        RefreshUIBuffer();
    }

    private class PauseButton implements ButtonListener, GameObject {
        private Button button;
        private Panel pausePanel;

        public PauseButton(Button button, Panel pausePanel){
            Game.AddObject(this);
            this.button = button;
            this.pausePanel = pausePanel;
        }

        public void OnHover() {
        }

        public void OnClick() {
        }

        public void OnPress() {
        }
        
        public void OnHoverExit() {
        }
        
        public void OnToggle() {
        }
        
        public void OnToggleOn() {
            GameScene.state = GameState.Paused;
            button.Disable(true);
            pausePanel.transform.SetAllHidden(false);
            SceneRenderer.EnableBlur(0.2f);
            
            RefreshUIBuffer();
        }
        
        public void OnToggleOff() {
            GameScene.state = GameState.Playing;
            button.Enable(true);
            pausePanel.transform.SetAllHidden(true);
            SceneRenderer.DisableBlur(0.125f);
            RefreshUIBuffer();
        }

        public void Update() {
            if(Input.GetKeyPressed(Input.KEY_ESCAPE)){
                button.Toggle();
            }
        }

        public void FixedUpdate() {
        }

        public void Draw() {
        }
    }
    
    private class PauseExitButton implements ButtonListener {
        private Panel parentPanel;
        private Button pauseButton;
        
        public PauseExitButton(Button pauseButton, Panel parentPanel){
            this.pauseButton = pauseButton;
            this.parentPanel = parentPanel;
        }
        
        public void OnHover() {
        }
        
        public void OnClick() {
            pauseButton.transform.SetAllHidden(false);
            pauseButton.Toggle();
            parentPanel.transform.SetAllHidden(true);
        }

        public void OnPress() {
        }
        
        public void OnHoverExit() {
        }
        
        public void OnToggle() {
        }
        
        public void OnToggleOn() {
        }
        
        public void OnToggleOff() {
        }
    }

    private class PauseReturnToMenuButton implements ButtonListener {        
        public void OnHover() {
        }
        
        public void OnClick() {
            Game.LoadScene(new MenuScene());
        }

        public void OnPress() {
        }
        
        public void OnHoverExit() {
        }
        
        public void OnToggle() {
        }
        
        public void OnToggleOn() {
        }
        
        public void OnToggleOff() {
        }
    }

    private class PauseQuitToDesktopButton implements ButtonListener {        
        public void OnHover() {
        }
        
        public void OnClick() {
            System.exit(0);
        }

        public void OnPress() {
        }
        
        public void OnHoverExit() {
        }
        
        public void OnToggle() {
        }
        
        public void OnToggleOn() {
        }
        
        public void OnToggleOff() {
        }
    }
}
