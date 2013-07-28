package sillybits.core.gui;

import java.util.EnumSet;
import java.util.Map;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import sillybits.core.sbMod;
import sillybits.core.gui.sbGui.MouseButton;
import sillybits.core.gui.sbGuiFont.FontSpec;
import sillybits.core.gui.sbGuiLayout.Layout;

/**
 * A simple button, other button types (like @see sbGuiBitmapButton) will derive from this.
 * 
 * @author SillyBits
 */
public class sbGuiButton extends sbGuiElement
{
	protected final static boolean					DEBUG				= false;
	
	protected final static String					BUTTON				= "button";
	protected final static EnumSet					BUTTON_FONTSPEC		= EnumSet.of( FontSpec.SIZE_NORMAL );
	protected final static int						BUTTON_TEXT_COLOR	= 0x00FFFFFF;

	@Layout("text") protected String				text;
	@Layout("tristate") protected boolean			tristate;
	@Layout("state") protected State				state;
	protected sbGuiTexture[]						textures;
	protected sbGuiFont								font;
	@Layout("font") protected EnumSet< FontSpec >	fontSpec			= BUTTON_FONTSPEC;
	@Layout("color") protected int					color				= BUTTON_TEXT_COLOR;
	protected float									buttonSliceX;
	protected float									buttonSliceY;
	protected float									buttonTextureLeft;
	protected float									buttonTextureRight;
	protected float									buttonTextureTop;
	protected float									buttonTextureBottom;
	protected float									textWidth			= -1;


	public enum State
	{
		NONE, DISABLED, HIGHLIGHT, SELECTED;
	};


	public sbGuiButton( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text, boolean tristate, boolean disabled )
	{
		super( parent, name, posX, posY, posZ, width, height );
		this.text		= text;
		this.tristate	= tristate;
		this.state		= disabled ? State.DISABLED : State.NONE;
	}

	public sbGuiButton( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text, boolean tristate )
	{
		this( parent, name, posX, posY, posZ, width, height, text, tristate, false );
	}

	public sbGuiButton( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text )
	{
		this( parent, name, posX, posY, posZ, width, height, text, false, false );
	}

	public sbGuiButton( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height )
	{
		this( parent, name, posX, posY, posZ, width, height, "", false, false );
	}

	private sbGuiButton( sbGuiPane parent )
	{
		super( parent, null, 0, 0, 0, 0, 0 );
	}


	public void	setText( String text )		{ this.text = text; textWidth = -1; }
	public String getText()					{ return text; }

	public void	setFont( sbGuiFont font )	{ this.font = font; fontSpec = font.getSpec(); }
	public sbGuiFont getFont()				{ return font; }

	public void	setColor( int color )		{ this.color = color; }
	public int getColor()					{ return color; }

	public void setState( State state )		{ this.state = state; }
	public State getState()					{ return state; }


	@Override
	protected void onThemeChanged()
	{
		super.onThemeChanged();
		textures = null;
		font = null;
//		textWidth = -1;
	}


	@Override
	public void render()
	{
		if ( textures == null )
		{
			setupTextures();
			if ( textures[0] == null )
				sbMod.kill( "Button '"+name+"' is missing its textures, THIS is a bug you should report!" );
			sbGuiTexture texture = textures[0];
			int sliceX           = texture.width / 3;
			int sliceY           = texture.width / 3;
			buttonSliceX         = sliceX / sbGuiTheme.DOWNSCALE;
			buttonSliceY         = sliceY / sbGuiTheme.DOWNSCALE;
			buttonTextureLeft    = texture.getU( sliceX );
			buttonTextureRight   = texture.getU( texture.width  - sliceX );
			buttonTextureTop     = texture.getV( sliceY );
			buttonTextureBottom  = texture.getV( texture.height - sliceY );
		}
		if ( font == null )
		{
			font = getTheme().selectFont( fontSpec );
			if ( font == null )
				return;
			textWidth = -1;
		}
		if ( textWidth == -1 )
			textWidth = font.getStringWidth( text );

		if ( DEBUG )
			sbMod.logger().log( "Drawing text button '"+name+"' at "+posX+"/"+posY+"/"+posZ );

		/*  ______________  y0
		 * /              \ yT
		 * |    Button    |
		 * \______________/ yB
		 * x0  xL    xR  x1 y1
		 */
		final float x0 = posX;
		final float xL = x0 + buttonSliceX;
		final float xR = xL + textWidth;
		final float x1 = xR + buttonSliceX;
		final float y0 = posY;
		final float yT = y0 + buttonSliceY;
		final float yB = yT + font.getHeight();
		final float y1 = yB + buttonSliceY;

		getTexture().select();

		GL11.glBegin( GL11.GL_QUADS );

		sbGui.injectTexturedRect( x0, y0, xL, yT, posZ, 0                 , 0                  , buttonTextureLeft , buttonTextureTop );
		sbGui.injectTexturedRect( xL, y0, xR, yT, posZ, buttonTextureLeft , 0                  , buttonTextureRight, buttonTextureTop );
		sbGui.injectTexturedRect( xR, y0, x1, yT, posZ, buttonTextureRight, 0                  , 1                 , buttonTextureTop );

		sbGui.injectTexturedRect( x0, yT, xL, yB, posZ, 0                 , buttonTextureTop   , buttonTextureLeft , buttonTextureBottom );
		sbGui.injectTexturedRect( xL, yT, xR, yB, posZ, buttonTextureLeft , buttonTextureTop   , buttonTextureRight, buttonTextureBottom );
		sbGui.injectTexturedRect( xR, yT, x1, yB, posZ, buttonTextureRight, buttonTextureTop   , 1                 , buttonTextureBottom );

		sbGui.injectTexturedRect( x0, yB, xL, y1, posZ, 0                 , buttonTextureBottom, buttonTextureLeft , 1 );
		sbGui.injectTexturedRect( xL, yB, xR, y1, posZ, buttonTextureLeft , buttonTextureBottom, buttonTextureRight, 1 );
		sbGui.injectTexturedRect( xR, yB, x1, y1, posZ, buttonTextureRight, buttonTextureBottom, 1                 , 1 );

		GL11.glEnd();

		float textX = posX + ( width / 2.f );
		float textY = y0 + ( ( height - font.getHeight() ) / 2.f );
		if ( state == State.SELECTED )
		{
			textX += 1.f;
			textY += 1.f;
		}
		font.drawCenteredShadowedString( Tessellator.instance, textX, textY, posZ+0.0001f, color, text );
		
		if ( tooltipElement == this )
			renderTooltip();
	}


	@Override
	public void onMouse( float mouseX, float mouseY, MouseButton button, boolean down )
	{
		if ( state != State.DISABLED )
		{
			if ( isMouseWithin( mouseX, mouseY ) )
				state = ( down && button == MouseButton.LEFT ) ? State.SELECTED : State.HIGHLIGHT;
			else
				state = State.NONE;
		}
		super.onMouse( mouseX, mouseY, button, down );
	}

	@Override public void onMouseDown( float mouseX, float mouseY, MouseButton button )
	{
		if ( state != State.DISABLED )
		{
			if ( isMouseWithin( mouseX, mouseY ) )
				state = State.SELECTED;
		}
		super.onMouseDown( mouseX, mouseY, button );
	}

	@Override public void onMouseUp  ( float mouseX, float mouseY, MouseButton button )
	{
		if ( state != State.DISABLED )
		{
			if ( isMouseWithin( mouseX, mouseY ) )
			{
				if ( state == State.SELECTED )
				{
					sbGuiWindow topmost = (sbGuiWindow)getTopmostParent();
					if ( topmost != null )
						topmost.onButtonClicked( this );
					else
						sbMod.logger().severe( "NO topmost parent found, THIS is a bug you should report!" );
				}
				state = State.HIGHLIGHT;
			}
		}
		super.onMouseUp( mouseX, mouseY, button );
	}


	/*
	 * Helpers
	 */

	protected void setupTextures()
	{
		sbGuiTheme theme = getTheme();
		State[] states = State.values();
		textures = new sbGuiTexture[ states.length ];
		for ( int i=0; i<states.length; ++i )
		{
			String textureName = BUTTON + "-" + name;
			if ( states[i] != State.NONE )
				textureName += "-" + states[i].name().toLowerCase();
			if ( theme.hasTexture( textureName ) )
				textures[i] = theme.getTexture( textureName );
		}
	}

	protected sbGuiTexture getTexture()
	{
		return textures[ state.ordinal() ];
	}


	/*
	 * ILayoutable
	 */

	@Override
	public boolean loadParameter( String id, String value )
	{
		if ( id.equals( "font" ) )
		{
			fontSpec = sbGuiFont.String2FontSpec( value );
			return true;
		}
		if ( id.equals( "state" ) )
		{
			state = value.equals( "disabled" ) ? State.DISABLED : State.NONE;
			return true;
		}
		return false;
	}

	public static sbGuiButton loadFromLayout( sbGuiPane parent, Map< String, String > params )
	{
		try
		{
			sbGuiButton button = null;
			String type = params.get( "type" );
			if ( type != null )
				params.remove( "type" );
			if ( type == null || type.isEmpty() )
				button = new sbGuiButton( parent );
			else if ( type.equals( "bitmap" ) )
				button = new sbGuiBitmapButton( parent );
			else
				sbMod.logger().severe( "Unknown button type '"+type+"'" );
			if ( button != null && sbGuiLayout.loadLayout( button, params ) )
				return button;
		}
		catch ( Exception e )
		{ 
			sbMod.logger().severe( "... while creating button" );
		}
		return null; 
	}

}
