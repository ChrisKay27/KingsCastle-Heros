package com.kingscastle.gameElements.livingThings;

import android.graphics.Rect;


import com.kingscastle.framework.Image;


class AreaLoader
{

	
    public Rect getAreaFromImage(  Image image ,  Rect intoThis )
	{
		if ( image == null )
		{
			throw new IllegalArgumentException( " image == null "); 
		}
		if ( intoThis == null )
		{
			throw new IllegalArgumentException( " intoThis == null "); 
		}
		return intoThis;
	}

}
