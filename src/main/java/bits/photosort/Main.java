/*
 * Copyright (c) 2015. Philip DeCamp
 * Released under the BSD 2-Clause License
 * http://opensource.org/licenses/BSD-2-Clause
 */

package bits.photosort;

import bits.photosort.gui.PhotoSortPanel;

import javax.swing.*;


/**
 * @author Philip DeCamp
 */
public class Main {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        PhotoSortPanel panel = new PhotoSortPanel( frame );
        frame.setContentPane( panel );
        frame.setSize( 650, 350 );
        frame.setLocationRelativeTo( null );
        frame.setVisible(true);
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }
}
