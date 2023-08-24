package james.UI;

import james.Color;
import james.Game;
import james.GameObject;

import static james.UI.Library.*;

import java.text.DecimalFormat;

import org.joml.Vector2f;
import org.joml.Vector4f;

public class ProfilerPanel implements GameObject {

    private static final int UPDATES_PER_SECOND = 4;

    private Text fpsLabelText;
    private Text fpsText;

    private long lastTickTime;
    private long lastFrameCount;

    public ProfilerPanel(){
        Game.AddObject(this);

        Panel fpsPanel = new Panel(new Color(255, 255, 255, 200), 
            new Transform(UIManager.instance.canvas, Anchor.BottomLeft, Anchor.BottomLeft, 0,
                new ScaledRelativeConstraint(0.18f, false),
                new ScaledRelativeConstraint(0.075f, true),
                new Vector2f(0.2f, 0.2f), ScalingFactor.Width, new Vector4f(0.0f)));
        fpsPanel.SetCornerRadii(5, 5, 5, 5);
        AddPanel(fpsPanel);
        fpsLabelText = new Text("FPS:\nFrame Time:\nDraw Time:", "bodyFont", 1.4f, new Color(0, 0, 0, 255),
            Alignment.Left, Alignment.Center, WrapMode.Overflow, OverflowMode.Clip, MeshUpdateMode.Static,
                new Transform(
                fpsPanel.transform, Anchor.Center, Anchor.Center, 1,
                new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f),
                new Vector2f(0.0f), ScalingFactor.Width, new Vector4f(0.0f, 0.0f, 0.0f, 0.08f)));
        fpsText = new Text(25, "bodyFont", 1.4f, new Color(0, 0, 0, 255),
            Alignment.Right, Alignment.Center, WrapMode.Overflow, OverflowMode.Clip, MeshUpdateMode.Dynamic,
                new Transform(
                fpsPanel.transform, Anchor.Center, Anchor.Center, 1,
                new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f),
                new Vector2f(0.0f), ScalingFactor.Width, new Vector4f(0.0f, 0.08f, 0.0f, 0.0f)));
        AddText(fpsLabelText);
        AddText(fpsText);
    }

    public void Update() {
        if(System.nanoTime() - lastTickTime > 1000000000 / UPDATES_PER_SECOND) {
            double fps = (double)(Game.frameCount - lastFrameCount) * (double)UPDATES_PER_SECOND;
            double frameTime = 1000.0 / fps;
            double drawTime = 1000.0 * Game.drawDeltaTime;
            DecimalFormat formatter = new DecimalFormat("0.00");
            fpsText.SetText(
                String.valueOf((int)fps) + "\n" +
                formatter.format(frameTime)  + " ms\n" +
                formatter.format(Math.min(frameTime, drawTime)) + " ms");
            
            lastFrameCount = Game.frameCount;
            lastTickTime = System.nanoTime();
        }
    }

    public void FixedUpdate() {
    }

    public void Draw() {
    }
    
}
