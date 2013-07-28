package sillybits.core.gui;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import sillybits.core.gui.sbGui.MouseButton;
import sillybits.core.gui.sbGuiFont.FontSpec;
import sillybits.core.gui.sbGuiLayout.Layout;
import sillybits.core.sbMod;

/**
 * A simple static text label.
 * 
 * @author SillyBits
 */
public class sbGuiEdit extends sbGuiElement
{
	protected final static boolean					DEBUG			= true;

	protected final static EnumSet					FONTSPEC		= EnumSet.of( FontSpec.SIZE_NORMAL, FontSpec.STYLE_BOLD );
	protected final static int						TEXT_COLOR		= 0x00FFFFFF;
	protected final static int						COLOR_SEL_RECT	= 0xFF0000FF;
	protected final static int						COLOR_SEL_TEXT	= 0x00FFFFFF;
	protected final static float					OFFSET_X		= 10.f / sbGuiTheme.DOWNSCALE;
	protected final static float					OFFSET_Y		=  3.f / sbGuiTheme.DOWNSCALE;
	protected final static char						CURSOR_CHAR		= '|';
	protected final static int						CURSOR_BLINK	= 20;// 1s one, 1s off

	@Layout("text") protected String				text;
	@Layout("state") protected State				state;
	protected sbGuiFont								font;
	@Layout("font") protected EnumSet< FontSpec >	fontSpec		= FONTSPEC;
	@Layout("color") protected int					color			= TEXT_COLOR;
	@Layout("color_sel_rect") protected int			colorSelRect	= COLOR_SEL_RECT;
	@Layout("color_sel_text") protected int			colorSelText	= COLOR_SEL_TEXT;
	protected static sbGuiTexture					texture;
	protected int									cursorPos		= -1;
	protected int									selectionStart	= -1;
	protected int									selectionEnd	= -1;
	protected float									cursorPosX;
	protected float									cursorWidth;
	protected float									selectionStartX;
	protected float									selectionEndX;
	protected boolean								insertMode		= true;
	protected static int							cursorTick		= 0;


	public enum State
	{
		NONE, READONLY;
	};


	public sbGuiEdit( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text, boolean readonly )
	{
		super( parent, name, posX, posY, posZ, width, height );
		this.text	= text;
		this.state	= readonly ? State.READONLY : State.NONE;
	}

	public sbGuiEdit( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, String text )
	{
		this( parent, name, posX, posY, posZ, width, height, text, false );
	}

	public sbGuiEdit( sbGuiPane parent, String name, float posX, float posY, float posZ, String text, boolean disabled )
	{
		this( parent, name, posX, posY, posZ, 0, 0, text, disabled );
	}

	public sbGuiEdit( sbGuiPane parent, String name, float posX, float posY, float posZ, String text )
	{
		this( parent, name, posX, posY, posZ, 0, 0, text, false );
	}

	private sbGuiEdit( sbGuiPane parent )
	{
		super( parent, null, 0, 0, 0, 0, 0 );
	}


	public void	setText( String text )		{ this.text = text; cursorPos = selectionStart = selectionEnd = -1; }
	public String getText()					{ return text; }

	public void	setFont( sbGuiFont font )	{ this.font = font; cursorWidth = font.getCharWidth( CURSOR_CHAR ); }
	public sbGuiFont getFont()				{ return font; }

	public void	setColor( int color )		{ this.color = color; }
	public int getColor()					{ return color; }

	public void setState( State state )		{ this.state = state; if ( state == State.READONLY ) cursorPos = selectionStart = selectionEnd = -1; }
	public State getState()					{ return state; }


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
			cursorWidth = font.getCharWidth( CURSOR_CHAR );
		}
		if ( height <= 0 )
			height = font.getHeight();

		if ( DEBUG )
			sbMod.logger().log( "Drawing editable text '"+name+"' at "+posX+"/"+posY+"/"+posZ
								+", text='"+text+"', cursor="+cursorPos+", sel="+selectionStart+"/"+selectionEnd 
								);

		GL11.glTranslatef( posX, posY, posZ );

//TODO: Find out why clipping isn't working correctly as it did work with listbox
//		if ( width == 0 && height == 0 )
//		{
//TODO: Add horizontal and vertical alignments: LEFT, MIDDLE, RIGHT, TOP, CENTER, BOTTOM:
//			final float textWidth = font.getStringWidth( text );
//			final float textX     = posX + ( width / 2.f );
//			final float textY     = posY + ( ( height - font.getHeight() ) / 2.f );
//			font.drawShadowedString( Tessellator.instance, textX, textY, posZ+0.0001f, color, text );
//			font.drawShadowedString( Tessellator.instance, posX, posY, posZ, color, text );
			drawText();
//		}
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
////			font.drawShadowedString( Tessellator.instance, posX, posY, posZ, color, text );
//			drawText();
//
//			GL11.glPopClientAttrib();
//			GL11.glPopAttrib();
//		}

		GL11.glTranslatef( -posX, -posY, -posZ );
	}


	@Override
	public void onMouseDown( float mouseX, float mouseY, MouseButton button )
	{
		super.onMouseDown( mouseX, mouseY, button );
		if ( focusedElement == this )
		{
			if ( cursorPos == -1 )
			{
				cursorPos = getMaxCursorPos();
				cursorPosX = getCursorX( cursorPos );
			}
//TODO: Find cursor position from mousedown position
//TODO: Select text with moving cursor while mouse is down
		}
	}

	@Override
	protected boolean canGainFocus( float mouseX, float mouseY )
	{
		return isMouseWithin( mouseX, mouseY );
	}


	@Override
	public void onKeyDown( char key, int extended )
	{
		super.onKeyDown( key, extended );
		if ( focusedElement == this )
		{
			if( extended == Keyboard.KEY_LEFT )
			{
				if ( cursorPos > 0 )
				{
					--cursorPos;
					cursorPosX = getCursorX( cursorPos );

					if ( isShiftKeyPressed() )
					{
						selectionStart = cursorPos;
						selectionStartX = getCursorX( selectionStart );
						if ( selectionEnd == -1 )
						{
							selectionEnd = cursorPos+1;
							selectionEndX = getCursorX( selectionEnd );
						}
					}
					else
						selectionStart = selectionEnd = -1;
				}
			}
			else if ( extended == Keyboard.KEY_RIGHT )
			{
				if ( cursorPos < getMaxCursorPos() )
				{
					++cursorPos;
					cursorPosX = getCursorX( cursorPos );

					if ( isShiftKeyPressed() )
					{
						selectionEnd = cursorPos;
						selectionEndX = getCursorX( selectionEnd );
						if ( selectionStart == -1 )
						{
							selectionStart = cursorPos-1;
							selectionStartX = getCursorX( selectionStart );
						}
					}
					else
						selectionStart = selectionEnd = -1;
				}
			}
			else if ( extended == Keyboard.KEY_HOME )
			{
				int prevCursorPos = cursorPos;

				cursorPos = 0;
				cursorPosX = getCursorX( cursorPos );

				if ( isShiftKeyPressed() )
				{
					selectionStart = cursorPos;
					selectionStartX = getCursorX( selectionStart );
					if ( selectionEnd == -1 )
					{
						selectionEnd = prevCursorPos;
						selectionEndX = getCursorX( selectionEnd );
					}
				}
				else
					selectionStart = selectionEnd = -1;
			}
			else if ( extended == Keyboard.KEY_END )
			{
				int prevCursorPos = cursorPos;

				cursorPos = getMaxCursorPos();
				cursorPosX = getCursorX( cursorPos );

				if ( isShiftKeyPressed() )
				{
					selectionEnd = cursorPos;
					selectionEndX = getCursorX( selectionEnd );
					if ( selectionStart == -1 )
					{
						selectionStart = prevCursorPos;
						selectionStartX = getCursorX( selectionStart );
					}
				}
				else
					selectionStart = selectionEnd = -1;
			}
			else if ( extended == Keyboard.KEY_INSERT )
			{
				insertMode = !insertMode;
			}
			else if ( extended == Keyboard.KEY_DELETE )
			{
				//TODO: Remove char following cursor, if any avail, resp. delete whole selection
				if ( selectionStart != -1 && selectionEnd != -1 )
				{
					if ( selectionStart == 0 )
					{
						if ( selectionEnd < getMaxCursorPos() )
							text = text.substring( selectionEnd );
						else
							text = "";
					}
					else
						text = text.substring( 0, selectionStart ) + text.substring( selectionEnd );
					cursorPos = selectionStart;
					cursorPosX = getCursorX( cursorPos );
					selectionStart = selectionEnd = -1;
				}
				else if ( cursorPos < getMaxCursorPos() )
				{
					if ( cursorPos == 0 )
						text = text.substring( 1 );
					else
						text = text.substring( 0, cursorPos ) + text.substring( cursorPos+1 );
				}
			}
			else if ( extended == Keyboard.KEY_BACK )
			{
				//TODO: Remove char before cursor, if any avail, resp. delete whole selection
				if ( selectionStart != -1 && selectionEnd != -1 )
				{
					if ( selectionStart == 0 )
					{
						if ( selectionEnd < getMaxCursorPos() )
							text = text.substring( selectionEnd );
						else
							text = "";
					}
					else
						text = text.substring( 0, selectionStart ) + text.substring( selectionEnd );
					cursorPos = selectionStart;
					cursorPosX = getCursorX( cursorPos );
					selectionStart = selectionEnd = -1;
				}
				else if ( cursorPos > 0 )
				{
					if ( cursorPos < getMaxCursorPos() )
						text = text.substring( 0, cursorPos-1 ) + text.substring( cursorPos );
					else
						text = text.substring( 0, cursorPos-1 );
					--cursorPos;
					cursorPosX = getCursorX( cursorPos );
				}
			}
			else if ( isControlKeyPressed() )
			{
				key = Character.toLowerCase(key);
				if ( key == 'x' || key == 'c' )// Cut -or- copy selection
				{
					if ( selectionStart != -1 && selectionEnd != -1 )
					{
						String selection = text.substring( selectionStart, selectionEnd );
						sbGui.setClipboardContents( selection );
					}
				}
				else if ( key == 'v' )// Paste
				{
					String clipboard = sbGui.getClipboardContents();
					if ( selectionStart != -1 && selectionEnd != -1 )
					{
						if ( selectionStart == 0 )
						{
							if ( selectionEnd < getMaxCursorPos() )
								text = clipboard + text.substring( selectionEnd );
							else
								text = clipboard;
						}
						else
							text = text.substring( 0, selectionStart ) + clipboard + text.substring( selectionEnd );
						cursorPos = selectionStart;
						cursorPosX = getCursorX( cursorPos );
						selectionStart = selectionEnd = -1;
					}
					else 
					{
						if ( cursorPos == 0 )
							text = clipboard + text;
						else if ( cursorPos < getMaxCursorPos() )
							text = text.substring( 0, cursorPos ) + clipboard + text.substring( cursorPos );
						else
							text = text.substring( 0, cursorPos ) + clipboard;
					}
				}
				if ( key == 'x' )// Cut selection
				{
					if ( selectionStart != -1 && selectionEnd != -1 )
					{
						if ( selectionStart == 0 )
						{
							if ( selectionEnd < getMaxCursorPos() )
								text = text.substring( selectionEnd );
							else
								text = "";
						}
						else
							text = text.substring( 0, selectionStart ) + text.substring( selectionEnd );
						cursorPos = selectionStart;
						cursorPosX = getCursorX( cursorPos );
						selectionStart = selectionEnd = -1;
					}
				}
			}
			else
			{
				// Normal key press, just insert at cursor pos or replace selecion. Advancing cursor accordingly
				if ( selectionStart != -1 && selectionEnd != -1 )
				{
					if ( selectionStart == 0 )
					{
						if ( selectionEnd < getMaxCursorPos() )
							text = key + text.substring( selectionEnd );
						else
							text = key + "";
					}
					else
						text = text.substring( 0, selectionStart ) + key + text.substring( selectionEnd );
					cursorPos = selectionStart;
					cursorPosX = getCursorX( cursorPos );
					selectionStart = selectionEnd = -1;
				}
				else if ( cursorPos < getMaxCursorPos() )
				{
					if ( cursorPos == 0 )
						text = key + text;
					else
						text = text.substring( 0, cursorPos ) + key + text.substring( cursorPos );
				}
			}
		}
	}


	protected int getMaxCursorPos()
	{
		return ( text != null && !text.isEmpty() ) ? text.length() : 0;
	}

	protected float getCursorX( int pos )
	{
		float x = OFFSET_X;
		if ( pos > 0 && pos <= getMaxCursorPos() )
			x += font.getStringWidth( text.substring( 0, pos ) );
		return x;
	}

	protected void drawText()
	{
		cursorTick = ( cursorTick+1 ) % (CURSOR_BLINK*2);

		if ( cursorPos == -1 && selectionStart == -1 )
		{
			font.drawString( Tessellator.instance, OFFSET_X, OFFSET_Y, 0, color, text );
		}
		else
		{
			if ( texture == null )
			{
				BufferedImage image = new BufferedImage( 1, 1, BufferedImage.TYPE_4BYTE_ABGR );
				image.setRGB( 0, 0, colorSelRect );
				int glTexNo = Minecraft.getMinecraft().renderEngine.allocateAndSetupTexture( image );
				texture = new sbGuiTexture( "~edit", 1, 1, glTexNo );
			}

			float xB = selectionStartX;
			float xE = selectionEndX;
			if ( selectionStart != -1 )
			{
				if ( cursorPos == selectionStart )
				{
					xB += cursorWidth;
					xE += cursorWidth;
				}
				texture.select();
				sbGui.drawRect( xB, OFFSET_Y, xE, font.getHeight(), 0 );
			}

			if ( insertMode )
			{
				String str;
				if ( cursorPos == 0 )
					str = CURSOR_CHAR + text;
				else if ( cursorPos == getMaxCursorPos() )
					str = text + CURSOR_CHAR;
				else
					str = text.substring( 0, cursorPos ) + CURSOR_CHAR + text.substring( cursorPos );
				font.drawString( Tessellator.instance, OFFSET_X, OFFSET_Y, 0, color, str );
				if ( cursorTick >= CURSOR_BLINK )
					font.drawString( Tessellator.instance, cursorPosX, OFFSET_Y, 0, sbGui.invertColor(color), CURSOR_CHAR+"" );
			}
			else
			{
				if ( cursorTick >= CURSOR_BLINK )
				{
					char ch = text.charAt( cursorPos );
					texture.select();
					sbGui.drawRect( cursorPosX, OFFSET_Y, cursorPosX+font.getCharWidth( ch ), font.getHeight(), 0 );
					sbGui.setColor( color );
					font.drawString( Tessellator.instance, OFFSET_X, OFFSET_Y, 0, color, text );
					font.drawString( Tessellator.instance, cursorPosX, OFFSET_Y, 0, sbGui.invertColor(color), ch+"" );
				}
				else
					font.drawString( Tessellator.instance, OFFSET_X, OFFSET_Y, 0, color, text );
			}

			if ( selectionStart != -1 )
				font.drawString( Tessellator.instance, xB, OFFSET_Y, 0, colorSelText, text.substring( selectionStart, selectionEnd ) );
		}
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
			state = value.equals( "disabled" ) ? State.READONLY : State.NONE;
			return true;
		}
		return false;
	}

	public static sbGuiEdit loadFromLayout( sbGuiPane parent, Map< String, String > params )
	{
		try
		{
			sbGuiEdit edit = new sbGuiEdit( parent );
			if ( sbGuiLayout.loadLayout( edit, params ) )
				return edit;
		}
		catch ( Exception e )
		{ 
			sbMod.logger().severe( "... while creating edit" );
		}
		return null; 
	}

}
