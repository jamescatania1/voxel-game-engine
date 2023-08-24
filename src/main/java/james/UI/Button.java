package james.UI;

import james.*;
import james.UI.*;
import static james.UI.Library.*;

import java.util.ArrayList;

@SuppressWarnings("all")
public class Button implements GameObject{
    public Transform transform;
    
    public float hoverTintSpeed;
    public float hoverExitTintSpeed;
    public float clickTintSpeed;
    public ToggleMode toggleMode;
    
    public ArrayList<ButtonListener> listeners;
    public ArrayList<Panel> panels;
    public ArrayList<Image> images;
    public ArrayList<Text> texts;

    public boolean disabled;

    // [][][0] -> base
    // [][][1] -> hover
    // [][][2] -> clicked
    // [][][3] -> toggled
    private ArrayList<Color[]>[] tints;

    private boolean toggled;
    private boolean overOnDown;
    private boolean hovering;

    /**
     * Button constructor. Do not register the button as a GameObject, it is taken care of.
     * @param transform
     *      - the button's transform.
     * @param toggleMode
     *      - the button's toggle capabilities. One of ToggleMode.{ CanToggle, CanToggleOff, NoToggle }. Regardless of the parameter provide, the button may be toggled by code via the Toggle() functions.
     * @param hoverTintSpeed
     *      - speed for hover tint color transitions. A greater value represents a faster transition.
     * @param hoverExitTintSpeed
     *      - speed for hover exit tint color transitions. A greater value represents a faster transition.
     * @param clickTintSpeed
     *      - speed for click/toggle color transitions. A greater value represents a faster transition.
     */
    public Button(Transform transform, ToggleMode toggleMode, float hoverTintSpeed, float hoverExitTintSpeed, float clickTintSpeed){
        Game.AddObject(this);

        this.transform = transform;
        this.toggleMode = toggleMode;
        this.hoverTintSpeed = hoverTintSpeed;
        this.hoverExitTintSpeed = hoverExitTintSpeed;
        this.clickTintSpeed = clickTintSpeed;
        
        listeners = new ArrayList<>();

        tints = new ArrayList[3];
        for(int i = 0; i < 3; i++) tints[i] = new ArrayList<Color[]>();

        panels = new ArrayList<>();
        images = new ArrayList<>();
        texts = new ArrayList<>();
    }

    /**
     * Bind a panel to the button. Do not register the panel with the UIManager separately, it is taken care of.
     * @param panel
     *      - panel that is to be binded. Does not effect the panel's transform parent.
     * @param baseColor
     * @param hoverColor
     * @param clickedColor
     * @param toggledColor
     *      - may be null if the button is not toggleable, or if the hover/click effects are desired for while the button is in the toggled state.
     */
    public void AddPanel (Panel panel, Color baseColor, Color hoverColor, Color clickedColor, Color toggledColor) {
        panel.color = baseColor.Clone();
        panels.add(panel);
        UIManager.AddPanel(panel);
        tints[0].add(new Color[]{ baseColor, hoverColor, clickedColor, toggledColor });
    }

    /**
     * Shorthand bind a panel to the button with a fill-centered transform. Panel's layer is as specified.
     * @param baseColor
     * @param hoverColor
     * @param clickedColor
     */
    public void AddPanel (Color baseColor, Color hoverColor, Color clickedColor, int layer){
        Panel panel = new Panel(new Color(baseColor), new Transform(transform, Anchor.Center, Anchor.Center, layer, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f)));
        AddPanel(panel, baseColor, hoverColor, clickedColor, new Color(clickedColor));
    }

    /**
     * Shorthand bind a panel to the button with a fill-centered transform. Panel's layer is as specified.
     * @param baseColor
     * @param hoverColor
     * @param clickedColor
     * @param toggledColor
     */
    public void AddPanel (Color baseColor, Color hoverColor, Color clickedColor, Color toggledColor, int layer){
        Panel panel = new Panel(new Color(baseColor), new Transform(transform, Anchor.Center, Anchor.Center, layer, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f)));
        AddPanel(panel, baseColor, hoverColor, clickedColor, toggledColor);
    }

    /**
     * Bind an image to the button. Do not register the image with the UIManager separately, it is taken care of.
     * @param image
     *      - image that is to be binded. Does not effect the image's transform parent.
     * @param baseColor
     * @param hoverColor
     * @param clickedColor
     * @param toggledColor
     *      - may be null if the button is not toggleable, or if the hover/click effects are desired for while the button is in the toggled state.
     */
    public void AddImage (Image image, Color baseColor, Color hoverColor, Color clickedColor, Color toggledColor) {
        image.color = baseColor.Clone();
        images.add(image);
        UIManager.AddImage(image);
        tints[1].add(new Color[]{ baseColor, hoverColor, clickedColor, toggledColor });
    } 

    /**
     * Bind a text component to the button. Do not register the text with the UIManager separately, it is taken care of.
     * @param text
     *      - text component that is to be binded. Does not effect the text's transform parent.
     * @param baseColor
     * @param hoverColor
     * @param clickedColor
     * @param toggledColor
     *      - may be null if the button is not toggleable, or if the hover/click effects are desired for while the button is in the toggled state.
     */
    public void AddText (Text text, Color baseColor, Color hoverColor, Color clickedColor, Color toggledColor) {
        text.color = baseColor.Clone();
        texts.add(text);
        UIManager.AddText(text);
        tints[2].add(new Color[]{ baseColor, hoverColor, clickedColor, toggledColor });
    }

    /**
     * Shorthand add and bind a text component to the button with center-alignment and layer equal to the specified layer.
     * @param text
     * @param font
     * @param fontSize
     * @param color
     */
    public void AddText (String text, String font, float fontSize, Color color, int layer){
        Text textObject = new Text(text, font, fontSize, color, Alignment.Center, Alignment.Center,
        WrapMode.Overflow, OverflowMode.Overflow, MeshUpdateMode.Static, new Transform(transform, Anchor.Center, Anchor.Center, layer, 
        new RelativeConstraint(ConstraintDimension.ParentWidth, 1.0f), new RelativeConstraint(ConstraintDimension.ParentHeight, 1.0f)));
        AddText(textObject, color, new Color(color), new Color(color), new Color(color));
    }

    /**
     * Binds the ButtonListener for custom functionality on callbacks.
     * @param listener
     *      - the listener that is to be added.
     */
    public void AddListener(ButtonListener listener){
        listeners.add(listener);
    }

    /**
     * Disables any user input for the button.
     */
    public void Disable(boolean hide){
        if(hide) transform.SetAllHidden(true);
        disabled = true;
    }

    /**
     * Enables user input for the button.
     */
    public void Enable(boolean unhide){
        if(unhide) transform.SetAllHidden(false);
        disabled = false;
    }

    /**
     * Toggles the button to the opposite of its current state.
     */
    public void Toggle(){
        OnToggle();
        if(toggled) ToggleOff();
        else ToggleOn();
    }

    /**
     * Toggles the button on, if it is not already so.
     */
    public void ToggleOn(){
        if(toggled) return;
        toggled = true;
        OnToggleOn();
    }

    /**
     * Toggles the button off, if it is not already so.
     */
    public void ToggleOff(){
        if(!toggled) return;
        toggled = false;
        OnToggleOff();
    }

    public void Update() {

        //do button input
        ProcessButtonInput();

        //color interpolation
        for(int i = 0; i < panels.size(); i++){
            if(toggled && tints[0].get(i)[3] != null){
                panels.get(i).color.Interpolate(tints[0].get(i)[3], clickTintSpeed * (float)Game.deltaTime);
            }
            else if(overOnDown){
                panels.get(i).color.Interpolate(tints[0].get(i)[2], clickTintSpeed * (float)Game.deltaTime);
            }
            else if(hovering){
                panels.get(i).color.Interpolate(tints[0].get(i)[1], hoverTintSpeed * (float)Game.deltaTime);
            }
            else{
                panels.get(i).color.Interpolate(tints[0].get(i)[0], hoverExitTintSpeed * (float)Game.deltaTime);
            }
        }
        for(int i = 0; i < images.size(); i++){
            if(toggled && tints[1].get(i)[3] != null){
                images.get(i).color.Interpolate(tints[1].get(i)[3], clickTintSpeed * (float)Game.deltaTime);
            }
            else if(overOnDown){
                images.get(i).color.Interpolate(tints[1].get(i)[2], clickTintSpeed * (float)Game.deltaTime);
            }
            else if(hovering){
                images.get(i).color.Interpolate(tints[1].get(i)[1], hoverTintSpeed * (float)Game.deltaTime);
            }
            else{
                images.get(i).color.Interpolate(tints[1].get(i)[0], hoverExitTintSpeed * (float)Game.deltaTime);
            }
        }
        for(int i = 0; i < texts.size(); i++){
            if(toggled && tints[2].get(i)[3] != null){
                texts.get(i).color.Interpolate(tints[2].get(i)[3], clickTintSpeed * (float)Game.deltaTime);
            }
            else if(overOnDown){
                texts.get(i).color.Interpolate(tints[2].get(i)[2], clickTintSpeed * (float)Game.deltaTime);
            }
            else if(hovering){
                texts.get(i).color.Interpolate(tints[2].get(i)[1], hoverTintSpeed * (float)Game.deltaTime);
            }
            else{
                texts.get(i).color.Interpolate(tints[2].get(i)[0], hoverExitTintSpeed * (float)Game.deltaTime);
            }
        }
    }

    private void ProcessButtonInput() {
        if(disabled || transform.hidden){
            hovering = false;
            overOnDown = false;
            return;
        }

        boolean mouseOver = MouseOver();

        if(Input.leftButton.pressed && mouseOver && !(toggled && toggleMode == ToggleMode.CanToggle)){
            overOnDown = true;
            OnPress();
        }

        if(Input.leftButton.released) {
            if(overOnDown && mouseOver) {
                OnClick();
                if(toggleMode == ToggleMode.CanToggle){
                    toggled = true;
                    OnToggle();
                    hovering = false;
                }
                else if(toggleMode == ToggleMode.CanToggleOff){
                    toggled = !toggled;
                    OnToggle();
                }
            }
            overOnDown = false;
        }

        if(!Input.leftButton.down && mouseOver && !(toggled && toggleMode == ToggleMode.CanToggle)){
            if(!hovering){
                OnHover();
            }
            hovering = true;
        }

        if(hovering && !mouseOver){
            hovering = false;
            if(!Input.leftButton.down){
                OnHoverExit();
            }
        }
    }

    public void FixedUpdate() {
    }

    public void Draw() {
    }

    private boolean MouseOver(){
        return !(Input.mouseX < transform.left || Input.mouseX > transform.right 
        || Input.mouseY < transform.bottom || Input.mouseY > transform.top);
    }


    private void OnPress(){
        for(ButtonListener listener : listeners) listener.OnPress();
    }
    
    private void OnClick(){
        for(ButtonListener listener : listeners) listener.OnClick();
    }

    private void OnToggle(){
        for(ButtonListener listener : listeners) listener.OnToggle();
        if(toggled) OnToggleOn();
        else OnToggleOff();
    }

    private void OnToggleOn(){
        for(ButtonListener listener : listeners) listener.OnToggleOn();
    }

    private void OnToggleOff(){
        for(ButtonListener listener : listeners) listener.OnToggleOff();
    }
    
    private void OnHover(){
        for(ButtonListener listener : listeners) listener.OnHover();
    }
    
    private void OnHoverExit(){
        for(ButtonListener listener : listeners) listener.OnHoverExit();
    }
}
