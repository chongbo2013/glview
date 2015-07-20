package com.glview.graphics;

class RRect {
	
	RectF fRect;
	
	Type fType =  Type.kUnknown_Type;

	public RRect() {
	}
	
	/**
     * Enum to capture the various possible subtypes of RR. Accessed
     * by type(). The subtypes become progressively less restrictive.
     */
    enum Type {
        // !< Internal indicator that the sub type must be computed.
        kUnknown_Type,

        // !< The RR is empty
        kEmpty_Type,

        //!< The RR is actually a (non-empty) rect (i.e., at least one radius
        //!< at each corner is zero)
        kRect_Type,

        //!< The RR is actually a (non-empty) oval (i.e., all x radii are equal
        //!< and >= width/2 and all the y radii are equal and >= height/2
        kOval_Type,

        //!< The RR is non-empty and all the x radii are equal & all y radii
        //!< are equal but it is not an oval (i.e., there are lines between
        //!< the curves) nor a rect (i.e., both radii are non-zero)
        kSimple_Type,

        //!< The RR is non-empty and the two left x radii are equal, the two top
        //!< y radii are equal, and the same for the right and bottom but it is
        //!< neither an rect, oval, nor a simple RR. It is called "nine patch"
        //!< because the centers of the corner ellipses form an axis aligned
        //!< rect with edges that divide the RR into an 9 rectangular patches:
        //!< an interior patch, four edge patches, and four corner patches.
        kNinePatch_Type,

        //!< A fully general (non-empty) RR. Some of the x and/or y radii are
        //!< different from the others and there must be one corner where
        //!< both radii are non-zero.
        kComplex_Type,
    };
	
	boolean isEmpty() {
		return fRect == null || fRect.isEmpty();
	}

}
