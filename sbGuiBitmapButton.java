package sillybits.core.gui;

import sillybits.core.sbMod;

/**
 * A simple bitmapped button.
 * 
 * @author SillyBits
 */
public class sbGuiBitmapButton extends sbGuiButton
{
	protected final static boolean DEBUG = false;


	public sbGuiBitmapButton( sbGuiPane parent, String name, float posX, float posY, float posZ, boolean tristate, boolean disabled )
	{
		super( parent, name, posX, posY, posZ, 0, 0, null, tristate, disabled );
	}

	protected sbGuiBitmapButton( sbGuiPane parent )
	{
		super( parent, null, 0, 0, 0, 0, 0 );
	}


	@Override
	protected void onThemeChanged()
	{
		super.onThemeChanged();
		textures = null;
	}


	@Override
	public void render()
	{
		if ( textures == null )
			setupTextures();

		if ( DEBUG )
			sbMod.logger().log( "Drawing bitmap button '"+name+"' at "+posX+"/"+posY+"/"+posZ );

		getTexture().select();
		sbGui.drawTexturedRect( posX, posY, posX+width, posY+height, posZ, 0, 0, 1, 1 );
		
		if ( tooltipElement == this )
			renderTooltip();
	}


	@Override
	protected void setupTextures()
	{
		super.setupTextures();

		// Find a texture to get our dimensions from
		for ( int i=0; i<textures.length; ++i )
			if ( textures[i] != null )
			{
				width  = textures[i].width  / sbGuiTheme.DOWNSCALE;
				height = textures[i].height / sbGuiTheme.DOWNSCALE;
				return;
			}
		sbMod.kill( "Bitmap button '"+name+"' is missing its textures, THIS is a bug you should report!" );
	}

}
