package sillybits.core.gui;

import org.lwjgl.opengl.GL11;

/**
 * Texture storage, just to store texture dimensions.
 * 
 * @author SillyBits
 */
public class sbGuiTexture
{
	protected String	name;
	protected int		width;
	protected int		height;
	protected int		glTexNo;


	public sbGuiTexture( String name, int width, int height, int glTexNo )
	{
		this.name		= name;
		this.width		= width;
		this.height		= height;
		this.glTexNo	= glTexNo;
	}


	public int		getWidth()			{ return width; }
	public int		getHeight()			{ return height; }

	public void		select()			{ GL11.glBindTexture( GL11.GL_TEXTURE_2D, glTexNo ); }

	public float	getU( int posX )	{ return ((float)posX) / ((float)width); }
	public float	getV( int posY )	{ return ((float)posY) / ((float)height); }

}
