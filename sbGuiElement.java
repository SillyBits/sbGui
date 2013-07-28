package sillybits.core.gui;

import java.util.EnumSet;
import java.util.Map;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import sillybits.core.sbMod;
import sillybits.core.gui.sbGui.ExtKeyboard;
import sillybits.core.gui.sbGui.MouseButton;
import sillybits.core.gui.sbGuiFont.FontSpec;
import sillybits.core.gui.sbGuiLayout.Layout;

/**
 * The base for drawable elements, all other elements in this framework derive from this class.
 * 
 * @author SillyBits
 */
public abstract class sbGuiElement
{
	protected final static boolean		DEBUG			= false;

	protected static sbGuiElement		focusedElement	= null;
	protected static int				extKeyState		= 0;

	protected sbGuiPane					parent;
	@Layout("name")	protected String	name;
	@Layout("x") protected float		posX;
	@Layout("y") protected float		posY;
	@Layout("z") protected float		posZ;
	@Layout("w") protected float		width;
	@Layout("h") protected float		height;


	public sbGuiElement( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height )
	{
		this.parent	= parent;
		this.name	= name;
		this.posX	= posX   / sbGuiTheme.DOWNSCALE;
		this.posY	= posY   / sbGuiTheme.DOWNSCALE;
		this.posZ	= posZ   / sbGuiTheme.DOWNSCALE;
		this.width	= width  / sbGuiTheme.DOWNSCALE;
		this.height	= height / sbGuiTheme.DOWNSCALE;
	}


	/*
	 * Getter/setter
	 */

	public String getName()				{ return name; }

	public void   setX( float x )		{ posX = x; }
	public float  getX()				{ return posX; }

	public void   setY( float y )		{ posY = y; }
	public float  getY()				{ return posY; }

	public void   setZ( float z )		{ posZ = z; }
	public float  getZ()				{ return posZ; }

	public void   setWidth( float w )	{ width = w; }
	public float  getWidth()			{ return width; }

	public void   setHeight( float h )	{ height = h; }
	public float  getHeight()			{ return height; }

	protected sbGuiPane getTopmostParent() { return parent.getTopmostParent(); }


	/*
	 * Theme support
	 */

	public sbGuiTheme getTheme()		{ return parent.getTheme(); }
	protected void onThemeChanged()		{ tooltipTexture = null; tooltipFont = null; }


	/*
	 * Rendering
	 */

	protected abstract void render();


	/*
	 * Tooltip support
	 */

	private final static String								TOOLTIP				= "tooltip";
	private final static EnumSet							TOOLTIP_FONTSPEC	= EnumSet.of( sbGuiFont.FontSpec.SIZE_SMALL );
	private final static int								TOOLTIP_TEXT_COLOR	= 0;

	@Layout("tooltip") protected String						tooltipText;
	protected sbGuiFont										tooltipFont;
	@Layout("tooltip_font") protected EnumSet< FontSpec >	tooltipFontSpec		= TOOLTIP_FONTSPEC;
	@Layout("tooltip_color") protected int					tooltipColor		= TOOLTIP_TEXT_COLOR;
	protected static sbGuiTexture							tooltipTexture;
	protected static float									tooltipSlice;
	protected static float									tooltipHeight;
	protected static float									tooltipTextureLeft;
	protected static float									tooltipTextureRight;
	protected static float									tooltipFontY;
	protected static sbGuiElement							tooltipElement;
	protected static float									tooltipX;
	protected static float									tooltipY;

	protected void		setTooltip( String tooltip )	{ this.tooltipText = tooltip; }
	protected String	getTooltip()					{ return tooltipText; }

	protected void		setTooltipColor( int color )	{ tooltipColor = color; }
	protected int		getTooltipColor()				{ return tooltipColor; }

	protected boolean hasTooltip()
	{
		final String currTooltipText = this.getTooltip();
		return ( currTooltipText != null && !currTooltipText.isEmpty() );
	}

	protected void checkTooltip()
	{
		if ( isMouseWithin( tooltipX, tooltipY ) )
		{
			if ( tooltipElement == null && hasTooltip() )
			{
				if ( DEBUG )
					sbMod.logger().log( "Element '"+getName()+"' is active tooltip" );
				tooltipElement = this;
			}
		}
		else
			if ( tooltipElement == this )
			{
				if ( DEBUG )
					sbMod.logger().log( "Element '"+getName()+"' isn't active tooltip anymore" );
				tooltipElement = null;
			}
	}

	protected void renderTooltip()
	{
		final String currTooltipText = getTooltip();
		if ( currTooltipText == null || currTooltipText.isEmpty() )
			return;

		if ( tooltipTexture == null )
		{
			tooltipTexture      = getTheme().getTexture( TOOLTIP );
			final int slice     = tooltipTexture.width / 3;
			tooltipSlice        = slice / sbGuiTheme.DOWNSCALE;
			tooltipHeight       = tooltipTexture.height / sbGuiTheme.DOWNSCALE;
			tooltipTextureLeft  = tooltipTexture.getU( slice );
			tooltipTextureRight = tooltipTexture.getU( tooltipTexture.width - slice );
		}
		if ( tooltipFont == null )
		{
			tooltipFont = getTheme().selectFont( tooltipFontSpec );
			if ( tooltipFont == null )
				return;
			tooltipFontY = ( tooltipHeight - tooltipFont.getHeight() ) / 2.f;
		}

		final float tooltipWidth = tooltipFont.getStringWidth( currTooltipText );

		/*  _______________
		 * /               \
		 * |    Tooltip    |
		 * \_______________/
		 * x0  xL     xR  x1
		 */
		final float x0 = 0;
		final float xL = x0 + tooltipSlice;
		final float xR = xL + tooltipWidth;
		final float x1 = xR + tooltipSlice;

		tooltipTexture.select();

		GL11.glTranslatef( tooltipX, tooltipY, posZ+0.0001f );

		GL11.glBegin( GL11.GL_QUADS );
		sbGui.injectTexturedRect( x0, 0, xL, tooltipHeight, 0, 0                  , 0, tooltipTextureLeft , 1 );
		sbGui.injectTexturedRect( xL, 0, xR, tooltipHeight, 0, tooltipTextureLeft , 0, tooltipTextureRight, 1 );
		sbGui.injectTexturedRect( xR, 0, x1, tooltipHeight, 0, tooltipTextureRight, 0, 1                  , 1 );
		GL11.glEnd();

		tooltipFont.drawCenteredString( Tessellator.instance, xL+(tooltipWidth/2.f), tooltipFontY, posZ+0.0002f, tooltipColor, currTooltipText );

		GL11.glTranslatef( -tooltipX, -tooltipY, -(posZ+0.0001f) );
	}


	/*
	 * Mouse handling
	 */

	public void onMouse( float mouseX, float mouseY, MouseButton button, boolean down )
	{
		tooltipX = mouseX;
		tooltipY = mouseY;
		checkTooltip();
	}

	public void onMouseWheel( float mouseX, float mouseY, int clicks )
	{
	}

	public void onMouseDown( float mouseX, float mouseY, MouseButton button )
	{
		if ( canGainFocus( mouseX, mouseY ) )
			gainFocus( mouseX, mouseY );
		else
			looseFocus( mouseX, mouseY );
	}

	public void onMouseUp( float mouseX, float mouseY, MouseButton button )
	{
	}

	protected boolean canGainFocus( float mouseX, float mouseY )
	{
		return false;
	}

	protected void gainFocus( float mouseX, float mouseY )
	{
		if ( focusedElement != this )
		{
			if ( DEBUG )
				sbMod.logger().log( "Element '"+getName()+"' gains focus" );
			focusedElement = this;
		}
	}

	protected void looseFocus( float mouseX, float mouseY )
	{
		if ( focusedElement == this )
		{
			if ( DEBUG )
				sbMod.logger().log( "Element '"+getName()+"' looses focus" );
			focusedElement = null;
		}
	}

	protected boolean isMouseWithin( float mouseX, float mouseY )
	{
		return posX <= mouseX && mouseX <= posX+width && posY <= mouseY && mouseY <= posY+height;
	}


	/*
	 * Keyboard handling
	 */

	public void onKey( char key, int extended, boolean down )
	{
	}

	public void onKeyDown( char key, int extended )
	{
		final int bit = ExtKeyboard.get( extended );
		if ( bit != 0 )
			extKeyState |= bit;
	}

	public void onKeyUp( char key, int extended )
	{
		final int bit = ExtKeyboard.get( extended );
		if ( bit != 0 )
			extKeyState = extKeyState & ~bit;
	}

	public boolean isControlKeyPressed()
	{
		return ( extKeyState & ( sbGui.ExtKeyboard.LEFT_CONTROL.id()|sbGui.ExtKeyboard.RIGHT_CONTROL.id() ) ) != 0;
	}

	public boolean isShiftKeyPressed()
	{
		return ( extKeyState & ( sbGui.ExtKeyboard.LEFT_SHIFT.id()|sbGui.ExtKeyboard.RIGHT_SHIFT.id() ) ) != 0;
	}

	public boolean isAltKeyPressed()
	{
		return ( extKeyState & ( sbGui.ExtKeyboard.LEFT_ALT.id()|sbGui.ExtKeyboard.RIGHT_ALT.id() ) ) != 0;
	}


	/*
	 * Layout support
	 */

	public void onParameterSet( String id )
	{
		if ( id.equals("x") )
			posX /= 4.f;
		else if ( id.equals("y") )
			posY /= 4.f;
		else if ( id.equals("w") )
			width /= 4.f;
		else if ( id.equals("h") )
			height /= 4.f;
	}

	public boolean loadParameter( String id, String value )
	{
		if ( id.equals( "tooltip_font" ) )
		{
			tooltipFontSpec = sbGuiFont.String2FontSpec( value );
			return true;
		}
		sbMod.logger().log( "Can't handle parameter '"+id+"' in sbGuiElement" );
		return false;
	}

}
