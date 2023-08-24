package james;

//import james.Audio.AudioLibrary;
import james.UI.*;
import james.UI.Library.Alignment;
import james.UI.Library.Anchor;
import james.UI.Library.ConstraintDimension;
import james.UI.Library.MeshUpdateMode;
import james.UI.Library.OverflowMode;
import james.UI.Library.ScalingFactor;
import james.UI.Library.ToggleMode;
import james.UI.Library.WrapMode;

import static james.UI.Library.*;

import org.joml.*;

//import static org.lwjgl.opengl.GL44C.*;

public class MenuScene extends Scene {

    public void LoadScene(){
        InitUI();

        /*
        source = new AudioSource(1.0f, 2.0f, 1.0f);

        source.SetLooping(true);
        source.SetPosition(40.0f, 0.0f, 0.0f);
        source.Play("bounce.wav");
        for(int i = 0; i < 2; i++){
            AudioLibrary.PlayInterfaceSound("bounce.wav", 1.0f, 1.0f);
        }*/
    }

    public void OnSceneUnload(){

    }

    private void InitUI(){
        Game.AddObject(new UIManager());

        UIManager.instance.scaleFactor = 1.0;

        Font titleFont = new Font("titleFont", "coolveltica.png", "coolveltica.fnt", 1, 0.35f, 0.25f, 48);
        Font bodyFont = new Font("bodyFont", "opensans.png", "opensans.fnt", 2, 0.46f, 0.2f, 7);
        AddFont(titleFont);
        AddFont(bodyFont);
        
        Panel screenBackgroundPanel = new Panel(new Color(212, 191, 129, 256),
            new Transform(UIManager.instance.canvas, Anchor.Center, Anchor.Center, 0,
                new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f),
                new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f)));
        AddPanel(screenBackgroundPanel);

        Panel backgroundPanel = new Panel(new Color(0, 0, 0, 0),
            new Transform(UIManager.instance.canvas, Anchor.Center, Anchor.Center, 0,
                new ScaledRelativeConstraint(0.4f, false),
                new ScaledRelativeConstraint(1.0f, true)));
        AddPanel(backgroundPanel);

        //Panel titlePanel = new Panel(new Color(120, 110, 91, 110), 
        Panel titlePanel = new Panel(new Color(256, 256, 256, 220), 
            new Transform(backgroundPanel.transform, Anchor.Center, Anchor.Bottom, 1,
                new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), 
                new AspectConstraint(0.2125f),
                new Vector2f(0.0f, 0.2f), ScalingFactor.ParentHeight, new Vector4f(0.0f)));
        titlePanel.SetCornerRadii(7, 7, 7, 7);
        titlePanel.SetBorder(0, 11, new Color(37, 70, 111, 60));
        AddPanel(titlePanel);
        
        Text titleText = new Text("GAME ENGINE DEMO", 
        "titleFont", 3.0f, new Color(37, 70, 111, 256), Alignment.Center, Alignment.Center, WrapMode.Overflow, OverflowMode.Overflow, MeshUpdateMode.Static,
        new Transform(titlePanel.transform, Anchor.Center, Anchor.Center, 2,
        new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), 
        new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f)));
        AddText(titleText);
        
        Panel buttonsPanel = new Panel(new Color(256, 256, 256, 220),
        new Transform(backgroundPanel.transform, Anchor.Center, Anchor.Top, 1,
        new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f),
        new AspectConstraint(1.2f),
        new Vector2f(0.0f, 0.15f), ScalingFactor.ParentHeight, new Vector4f(0.0f)));
        buttonsPanel.SetCornerRadii(7, 7, 7, 7);
        buttonsPanel.SetBorder(0, 11, new Color(37, 70, 111, 60));
        AddPanel(buttonsPanel);

        Button newGameButton = new Button( 
            new Transform(buttonsPanel.transform, Anchor.Top, Anchor.Top, 2,
                new RelativeConstraint(ConstraintDimension.ParentWidth, 0.67f),
                new AspectConstraint(0.175f),
                new Vector2f(0.0f, -0.52f), ScalingFactor.Height, new Vector4f(0.0f)), ToggleMode.NoToggle, 30f, 30f, 30f);
        newGameButton.AddPanel(new Color(Color.WHITE), new Color(220, 220, 220, 160), new Color(37, 70, 111, 256), 1);
        newGameButton.panels.get(0).SetCornerRadii(5, 5, 5, 5);
        newGameButton.panels.get(0).SetBorder(0, 4, new Color(0, 0, 0, 80));
        newGameButton.AddText("NEW GAME", "titleFont", 2.4f, new Color(37, 70, 111, 256), 2);
        newGameButton.AddListener(new ButtonAudio("button_01.wav", "", "", "", "", "", 1.0f, 0.85f));
        newGameButton.AddListener(new NewGameButton());

        RefreshUIBuffer();
    }

    private class NewGameButton implements ButtonListener {

        public void OnHover() {
        }

        public void OnClick() {
            Game.LoadScene(new GameScene());
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
