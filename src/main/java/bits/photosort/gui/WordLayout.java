/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort.gui;

import java.awt.*;

/**
 * <p>
 * This LayoutManager arranges things like words on a page or, more accurately,
 * like printing text to your screen.  WordLayout does not resize components, assuming, instead, 
 * that each component is already the desired size.  It treats each component as a word,
 * laying them out from left to right in the order in which they are added. 
 * If a component will be clipped by the right side of the container,
 * that component will instead be placed on the next row (like FlowLayout).  The height of the row
 * is determined by the maximum height of any component on that row.  
 * </p><p>
 * While performing the actual layout, WordLayout mantains a "caret" that moves from left
 * to right after placing each component.  The position of this caret can be adjusted by
 * adding invisible WordLayoutComponents.
 * </p><p>
 * WordLayout.createNewLine() - Like a carriage return, the caret goes to the next row
 * and all the way to the left.  If there are no components on a given row, the row height
 * will be equal to zero and the createNewLine component will have no effect.
 * createCursorVertical() or createVerticalStrut() can be used to create vertical gaps.
 * </p><p>
 * WordLayout.createCursorHorizontal(int pixels) - Causes WordLayout to move the caret some 
 * horizontal distance.
 * </p><p>
 * WordLayout.createCursorVertical(int pixels) - Causes WordLayout to move the caret some 
 * vertical distance.
 * </p><p>
 * WordLayout.createVerticalStrut(int pixels) - Creates an invisible, zero-width component with
 * height 'pixels'.  This can be used to set the row size.
 * </p><p>
 * WordLayout.createWrapMode() - (Default is on) Turns "WrapMode" setting on, which causes
 * WordLayout to move to the next row when no more components can fit on that row without
 * clipping.
 * </p><p>
 * WordLayout.createNoWrapMode() - Turns "WrapMode" setting off, causing WordLayout to continue
 * to place components on a given row regardless of available space.  New lines can still
 * be generated with createNewLine() or createCursorVertical().
 * </p><p>
 * WordLayout.createSetIndex(int pixels) - Determines the horizontal indentation of any new line.
 * </p><p>
 * WordLayout.createWallHook() - Causes previously placed component to be stretched or shrunk 
 * to occupy all horizontal space.  The caret will then go to the next line as if it encountered
 * a Newline component.  Only functions in noWrapMode.
 * </p><p>
 * 
 * Example:
 * <br><br><pre>
 * public void MyPanel extends JPanel {
 * 	setLayout(new WordLayout());
 * 	add(WordLayout.createNoWrapMode());
 * 	JButton button = new JButton("Expando Button");
 * 	button.setSize(1, 24);
 * 	add(button);
 * 	add(WordLayout.createWallHook());
 * 	button = new JButton("Static Length Button");
 * 	button.setSize(300, 24);
 * 	add(button);
 * 	add(WordLayout.createNewLine());
 * 
 * 	//etc.
 * }
 * </pre></p><p>
 * In this example, the first button will be at the top, left of the panel and will expand with the
 * panel.  THe second button will be placed below the first button, but, because it is followed by
 * a NewLine instead of a WallHook, will not be resized.
 * </p>
 */

public class WordLayout implements LayoutManager{
    
    private int mHGap, mVGap;
    private int mListHeight;
    private int mAvailableWidth;
    
    public WordLayout(int hgap, int vgap){
        this.mHGap = hgap;
        this.mVGap = vgap;
        mListHeight = 0;
        mAvailableWidth = -1;
    }
    
    public WordLayout(){
        mHGap = 0;
        mVGap = 0;
        mListHeight = 0;
        mAvailableWidth = -1;
    }
    
    public synchronized void setVGap(int gap){
        mVGap = gap;
    }
    
    public synchronized void setHGap(int gap){
        mHGap = gap;
    }
    
    public int getVGap(){
        return mVGap;
    }
    
    public int getHGap(){
        return mHGap;
    }
    
    public synchronized void layoutContainer(Container cont){
        Component [] comp = cont.getComponents();
                        
        int availableWidth = (mAvailableWidth < 0)? cont.getWidth(): mAvailableWidth;
        if(availableWidth < 0)
            return;
        
        boolean wrapMode = true;
        int column = 0;
        int x = mHGap;
        int y = mVGap;
        int indent = mHGap;
        int maxHeight = 0;
        final int maxWidth = availableWidth - mHGap;
        WordLayoutComponent command;
        int temp;
        Component lastComp = null;
        
        for(int i = 0; i < comp.length; i++){
            if(comp[i] instanceof WordLayoutComponent){
                command = (WordLayoutComponent)comp[i];
                switch(command.type){
                case WordLayoutComponent.WALL_HOOK:
                    if(lastComp == null || wrapMode)
                        break;
                    lastComp.setSize(Math.max(1, maxWidth - lastComp.getX()), lastComp.getHeight());
                case WordLayoutComponent.NEW_LINE:
                    lastComp = null;
                    if(column > 0){
                        column = 0;
                        y += maxHeight + mVGap;
                        x = indent;
                        maxHeight = 0;
                    }
                    break;
                case WordLayoutComponent.CURSOR_HORIZONTAL:
                    x += command.value;
                    break;
                case WordLayoutComponent.CURSOR_VERTICAL:
                    y += command.value;
                    maxHeight -= command.value;
                    break;
                case WordLayoutComponent.SET_NO_WRAP_MODE:
                    wrapMode = false;
                    break;
                case WordLayoutComponent.SET_WRAP_MODE:
                    wrapMode = true;
                    break;
                case WordLayoutComponent.VERTICAL_STRUT:
                    if(command.value > maxHeight)
                        maxHeight = command.value;
                    break;
                case WordLayoutComponent.SET_INDENT:
                    if(column == 0){
                        x += (command.value - indent);
                    }
                    indent = command.value;
                    break;
                }				
            }else{
                lastComp = comp[i];
                temp = comp[i].getWidth();
                if(temp + x > maxWidth && wrapMode && column > 0){
                    column = 0;
                    y += maxHeight + mVGap;
                    x = indent;
                    maxHeight = 0;
                }
                
                comp[i].setLocation(x, y);
                x += temp + mHGap;
                temp = comp[i].getHeight();
                if(temp > maxHeight)
                    maxHeight = temp;
                column++;
            }
        }
        
        if(column > 0){
            mListHeight = y + maxHeight + mVGap;
        }else{
            mListHeight = y;
        }	
    }
    
    
    public void addLayoutComponent(String name, Component comp){}
    
    public void removeLayoutComponent(Component comp){}
    
    public Dimension minimumLayoutSize(Container cont){return new Dimension(10, 10);}
    
    public Dimension preferredLayoutSize(Container cont){
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    
    
    public int getLastHeight(){
        return mListHeight;
    }
    
    public void setAvailableWidth(int width){
        mAvailableWidth = width;
    }
    
    public static Component createNewLine(){
        return new WordLayoutComponent(WordLayoutComponent.NEW_LINE, -1);
    }
    
    public static Component createCursorHorizontal(int x){
        return new WordLayoutComponent(WordLayoutComponent.CURSOR_HORIZONTAL, x);
    }
    
    public static Component createCursorVertical(int y){
        return new WordLayoutComponent(WordLayoutComponent.CURSOR_VERTICAL, y);
    }
    
    public static Component createWrapMode(){
        return new WordLayoutComponent(WordLayoutComponent.SET_WRAP_MODE, -1);
    }
    
    public static Component createNoWrapMode(){
        return new WordLayoutComponent(WordLayoutComponent.SET_NO_WRAP_MODE, -1);
    }
    
    public static Component createWallHook(){
        return new WordLayoutComponent(WordLayoutComponent.WALL_HOOK, -1);
    }
    
    public static Component createVerticalStrut(int h){
        return new WordLayoutComponent(WordLayoutComponent.VERTICAL_STRUT, h);
    }
    
    public static Component createSetIndent(int w){
        return new WordLayoutComponent(WordLayoutComponent.SET_INDENT, w);
    }
    
    private static class WordLayoutComponent extends Component{
        public final static int NEW_LINE = 0;
        public final static int CURSOR_HORIZONTAL = 1;
        public final static int CURSOR_VERTICAL = 2;
        public final static int SET_NO_WRAP_MODE = 3;
        public final static int SET_WRAP_MODE = 4;
        public final static int WALL_HOOK = 5;
        public final static int VERTICAL_STRUT = 6;
        public final static int SET_INDENT = 7;
        
        public final int type;
        public final int value;
        
        public WordLayoutComponent(int type, int value){
            this.type = type;
            this.value = value;
            setVisible(false);
            setBounds(-1000, -1000, 1, 1);
        }
    }
}
