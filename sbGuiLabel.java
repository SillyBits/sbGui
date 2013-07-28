package sillybits.core.gui;

import java.util.EnumSet;
import java.util.Map;
import net.minecraft.client.renderer.Tessellator;
import sillybits.core.gui.sbGuiFont.FontSpec;
import sillybits.core.gui.sbGuiLayout.Layout;
import sillybits.core.sbMod;

/**
 * A simple static text label.
 * 
 * @author SillyBits
 */
public class sbGuiLabel extends sbGuiElement
{
	protected final static boolean					DEBUG		= false;

	protected final static EnumSet					FONTSPEC	= EnumSet.of( FontSpec.SIZE_NORMAL, FontSpec.STYLE_BOLD );
	protected final static int						TEXT_COLOR	= 0x00FFFFFF;

	@Layout("text") protected String				text;
	@Layout("state") protected State				state;
	protected sbGuiFont								font;
	@Layout("font") protected EnumSet< FontSpec >	fontSpec	= FONTSPEC;
	@Layout("color") protected int					color		= TEXT_COLOR;
	protected static sbGuiTexture					dummyTexture;


	public enum State
	{
		NONE, DISABLED;
	};


	public sbGuiLabel( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text, boolean disabled )
	{
		super( parent, name, posX, posY, posZ, width, height );
		this.text	= text;
		this.state	= disabled ? State.DISABLED : State.NONE;
	}

	public sbGuiLabel( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text )
	{
		this( parent, name, posX, posY, posZ, width, height, text, false );
	}

	public sbGuiLabel( sbGuiPane parent, String name, float posX, float posY, float posZ, String text, boolean disabled )
	{
		this( parent, name, posX, posY, posZ, 0, 0, text, disabled );
	}

	public sbGuiLabel( sbGuiPane parent, String name, float posX, float posY, float posZ, String text )
	{
		this( parent, name, posX, posY, posZ, 0, 0, text, false );
	}

	private sbGuiLabel( sbGuiPane parent )
	{
		super( parent, null, 0, 0, 0, 0, 0 );
	}


	public void	setText( String text )		{ this.text = text; }
	public void	setFont( sbGuiFont font )	{ this.font = font; }
	public void	setColor( int color )		{ this.color = color; }
	public void setState( State state )		{ this.state = state; }


	@Override
	protected void onThemeChanged()
	{
		super.onThemeChanged();
		font = null;
	}


	@Override
	public void render()
	{
		if ( text == null || text.isEmpty() )
			return;

		if ( font == null )
		{
			font = getTheme().selectFont( fontSpec );
			if ( font == null )
				return;
		}

		if ( DEBUG )
			sbMod.logger().log( "Drawing text '"+name+"' at "+posX+"/"+posY+"/"+posZ+", text='"+text+"'" );

//		if ( width == 0 && height == 0 )
		{
//TODO: Add horizontal and vertical alignments: LEFT, MIDDLE, RIGHT, TOP, CENTER, BOTTOM:
//			final float textWidth = font.getStringWidth( text );
//			final float textX     = posX + ( width / 2.f );
//			final float textY     = posY + ( ( height - font.getHeight() ) / 2.f );
//			font.drawShadowedString( Tessellator.instance, textX, textY, posZ+0.0001f, color, text );
			font.drawShadowedString( Tessellator.instance, posX, posY, posZ, color, text );
		}
//		else
//		{
//			if ( dummyTexture == null )
//			{
//				BufferedImage image = new BufferedImage( 1, 1, BufferedImage.TYPE_4BYTE_ABGR );
//				image.setRGB( 0, 0, 0x00FFFFFF );
//				int glTexNo = Minecraft.getMinecraft().renderEngine.allocateAndSetupTexture( image );
//				dummyTexture = new sbGuiTexture( "~labelClipper", 1, 1, glTexNo );
//			}
//			
//			final float w = ( width  != 0 ) ? width  : font.getStringWidth( text );
//			final float h = ( height != 0 ) ? height : font.getHeight();
//
//			GL11.glPushAttrib( GL11.GL_ALL_ATTRIB_BITS );
//			GL11.glPushClientAttrib( GL11.GL_ALL_CLIENT_ATTRIB_BITS );
//
//	//		GL11.glClear( GL11.GL_DEPTH_BUFFER_BIT );=> Would interfere with MC rendering
//			GL11.glEnable( GL11.GL_STENCIL_TEST );
//
//			GL11.glColorMask( false, false, false, false );
//			GL11.glDepthMask( false );
//			GL11.glStencilFunc( GL11.GL_NEVER, 1, 0xFF );
//			GL11.glStencilOp( GL11.GL_REPLACE,  GL11.GL_KEEP,  GL11.GL_KEEP );  // draw 1s on test fail (always)
//			GL11.glStencilMask( 0xFF );
//			GL11.glClear( GL11.GL_STENCIL_BUFFER_BIT );
//			dummyTexture.select();
//			sbGui.drawTexturedRect( posX, posY, posX+w, posY+h, posZ, 0, 0, 1, 1 );
//
//			GL11.glColorMask( true, true, true, true );
//			GL11.glDepthMask( true );
//			GL11.glStencilMask( 0x00 );
//			GL11.glStencilFunc( GL11.GL_EQUAL, 1, 0xFF );
//			font.drawShadowedString( Tessellator.instance, posX, posY, posZ, color, text );
//
//			GL11.glPopClientAttrib();
//			GL11.glPopAttrib();
//		}
	}


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

	public static sbGuiLabel loadFromLayout( sbGuiPane parent, Map< String, String > params )
	{
		try
		{
			sbGuiLabel label = new sbGuiLabel( parent );
			if ( sbGuiLayout.loadLayout( label, params ) )
				return label;
		}
		catch ( Exception e )
		{ 
			sbMod.logger().severe( "... while creating label" );
		}
		return null; 
	}

}
