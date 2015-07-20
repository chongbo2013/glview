package com.glview.graphics.shader;

public enum TileMode {
        /**
         * replicate the edge color if the shader draws outside of its
         * original bounds
         */
        CLAMP   (),
        /**
         * repeat the shader's image horizontally and vertically
         */
        REPEAT  (),
        /**
         * repeat the shader's image horizontally and vertically, alternating
         * mirror images so that adjacent images always seam
         */
        MIRROR  ();
    
        TileMode() {
        }
    }