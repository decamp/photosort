/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.progress;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Component;

public class LayoutAdapter implements LayoutManager {

    public final static Dimension MIN_DIMENSION = new Dimension(1, 1);
    public final static Dimension MAX_DIMENSION = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    
    
    public void layoutContainer(Container cont) {}

    public Dimension minimumLayoutSize(Container cont) {
	return MIN_DIMENSION;
    }

    public Dimension preferredLayoutSize(Container cont) {
	return MAX_DIMENSION;
    }

    public void addLayoutComponent(String name, Component comp) {}
    
    public void removeLayoutComponent(Component comp) {}
    
}
