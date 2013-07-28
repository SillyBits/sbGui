package sillybits.core.gui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import sillybits.core.sbMod;
import sillybits.core.gui.sbGui.MouseButton;
import sillybits.core.gui.sbGuiFont.FontSpec;
import sillybits.core.gui.sbGuiLayout.Layout;

public class sbGuiListbox extends sbGuiElement
{
	protected final static boolean					DEBUG			= false;
	
	protected final static String					SCROLLBAR		= "scrollbar";
	protected final static EnumSet< FontSpec >		FONTSPEC		= EnumSet.of( FontSpec.SIZE_NORMAL, FontSpec.STYLE_BOLD );
	protected final static int						COLOR			= 0x00FFFFFF;
	protected final static float					OFFSET_X		= 10.f / sbGuiTheme.DOWNSCALE;
	protected final static float					OFFSET_Y		=  3.f / sbGuiTheme.DOWNSCALE;
	protected final static int						COLOR_SEL_RECT	= 0xFF0000FF;
	protected final static int						COLOR_SEL_TEXT	= 0x00FFFFFF;

	protected List									elements;
	@Layout("scrollbar") protected sbGuiTexture		scrollbar;
	@Layout("state") protected State				state;
	protected sbGuiFont								font;
	@Layout("font") protected EnumSet< FontSpec >	fontSpec			= FONTSPEC;
	@Layout("color") protected int					color				= COLOR;
	@Layout("color_sel_rect") protected int			colorSelRect		= COLOR_SEL_RECT;
	@Layout("color_sel_text") protected int			colorSelText		= COLOR_SEL_TEXT;
	protected float									listHeight			= -1;
	protected float									listWidth;
	protected float									drawOffset			= 0;
	protected float									maxElementOffset	= 0;
	protected float									scrollbarY			= -1;
	protected float									scrollbarSliceY;
	protected float									scrollbarWidth;
	protected float									scrollbarHeight;
	protected int									elementOffset		= 0;
	protected int									selection			= -1;
	protected IElementRenderer						elementRenderer;
	protected static sbGuiTexture					selectionTexture;


	public enum State
	{
		NONE, DISABLED;
	};


	public sbGuiListbox( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height, List elements )
	{
		super( parent, name, posX, posY, posZ, width, height );
		this.elements = elements;
	}

	public sbGuiListbox( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height )
	{
		this( parent, name, posX, posY, posZ, width, height, null );
	}

	private sbGuiListbox( sbGuiPane parent )
	{
		this( parent, null, 0, 0, 0, 0, 0 );
	}


	public void	setFont( sbGuiFont font )					{ this.font = font; }
	public void	setColor( int color )						{ this.color = color; }
	public void setState( State state )						{ this.state = state; }

	public void setElements( List elements )				{ this.elements = elements; listHeight = -1; selection = -1; }
	public List getElements()								{ return elements; }

	public int	getSelection()								{ return selection; }

	public void setRenderer( IElementRenderer renderer )	{ elementRenderer = renderer; }


	@Override
	protected void onThemeChanged()
	{
		super.onThemeChanged();
		scrollbar = null;
		font = null;
//		listHeight = -1;
	}


	protected void onSelectionChanged( int newSelection )
	{
		if ( newSelection != selection )
		{
			final int oldSelection = selection;
			selection = newSelection;
			sbGuiPane topmostParent = getTopmostParent();
			if ( topmostParent != null )
				((sbGuiWindow)topmostParent).onSelectionChanged( this, oldSelection, newSelection );
			else
				sbMod.logger().severe( "NO topmost parent found, THIS is a bug you should report!" );
		}
	}


	@Override
	protected String getTooltip()
	{
		String tooltip = null;
		if ( tooltipX-posX < listWidth )
			tooltip = elementRenderer.getTooltip( tooltipY-posY );
		return tooltip;
	}


	@Override
	public void render()
	{
		if ( scrollbar == null )
		{
			setupTextures();
			listHeight = -1;
		}
		if ( selectionTexture == null )
		{
			BufferedImage image = new BufferedImage( 1, 1, BufferedImage.TYPE_4BYTE_ABGR );
			image.setRGB( 0, 0, colorSelRect );
			int glTexNo = Minecraft.getMinecraft().renderEngine.allocateAndSetupTexture( image );
			selectionTexture = new sbGuiTexture( "~listboxSelection", 1, 1, glTexNo );
		}
		if ( font == null )
		{
			font = getTheme().selectFont( fontSpec );
			listHeight = -1;
		}
		if ( elementRenderer == null )
			elementRenderer = new StringRenderer( this );
		if ( listHeight == -1 )
			setupList();
		if ( listHeight <= 0 )
			return;

		if ( DEBUG )
			sbMod.logger().log( "Drawing listbox '"+name+"' at "+posX+"/"+posY+"/"+posZ
								+", elementOffset="+elementOffset
								+", selection="+selection
								+", listHeight="+(listHeight*sbGuiTheme.DOWNSCALE)
								+", drawOffset="+(drawOffset*sbGuiTheme.DOWNSCALE)
								+", scrollbarY="+(scrollbarY*sbGuiTheme.DOWNSCALE)
								+", elements="+elements.size()
								);

		GL11.glTranslatef( posX, posY, posZ-0.01f );

		GL11.glPushAttrib( GL11.GL_ALL_ATTRIB_BITS );
		GL11.glPushClientAttrib( GL11.GL_ALL_CLIENT_ATTRIB_BITS );

//		GL11.glClear( GL11.GL_DEPTH_BUFFER_BIT );=> Would interfere with MC rendering
		GL11.glEnable( GL11.GL_STENCIL_TEST );

		GL11.glColorMask( false, false, false, false );
		GL11.glDepthMask( false );
		GL11.glStencilFunc( GL11.GL_NEVER, 1, 0xFF );
		GL11.glStencilOp( GL11.GL_REPLACE,  GL11.GL_KEEP,  GL11.GL_KEEP );  // draw 1s on test fail (always)
		GL11.glStencilMask( 0xFF );
		GL11.glClear( GL11.GL_STENCIL_BUFFER_BIT );
		selectionTexture.select();
		sbGui.drawRect( 0, 0, width, height, 0 );

		GL11.glColorMask( true, true, true, true );
		GL11.glDepthMask( true );
		GL11.glStencilMask( 0x00 );
		GL11.glStencilFunc( GL11.GL_EQUAL, 1, 0xFF );
		float y = 0;
		for ( int index=elementOffset; index<elements.size(); ++index )
		{
			y += elementRenderer.drawElement( index, y );
			if ( y > height )
				break;
		}

		GL11.glPopClientAttrib();
		GL11.glPopAttrib();

		if ( maxElementOffset > 0 )
		{
			scrollbar.select();

//			scrollbarY = ( height - scrollbarHeight ) * ( elementOffset / maxElementOffset );
//			sbGui.drawTexturedRect( listWidth, scrollbarY, listWidth+scrollbarWidth, scrollbarY+scrollbarHeight, 0, 0, 0, 1, 1 );
			
			final float x1 = listWidth + scrollbarWidth;
			final float sH = ( height - (2*scrollbarSliceY) ) * ( 1.f - ( maxElementOffset / elements.size() ) );
			scrollbarY = ( height - sH - (2*scrollbarSliceY) ) * ( elementOffset / maxElementOffset );
			if ( scrollbarY + sH + (2*scrollbarSliceY) > height )
				scrollbarY = height - sH - (2*scrollbarSliceY);
			final float s0 = scrollbarY;
			final float sT = s0 + scrollbarSliceY;
			final float sB = sT + sH;
			final float s1 = sB + scrollbarSliceY;
			sbGui.drawTexturedRect( listWidth, s0, x1, sT, 0, 0, 0.000f, 1, 0.333f );
			sbGui.drawTexturedRect( listWidth, sT, x1, sB, 0, 0, 0.333f, 1, 0.666f );
			sbGui.drawTexturedRect( listWidth, sB, x1, s1, 0, 0, 0.666f, 1, 1.000f );
//TODO: Render scrollbar in 3 steps to give feedback on amount avail
		}

		GL11.glTranslatef( -posX, -posY, -(posZ-0.01f) );
		
		if ( tooltipElement == this )
			renderTooltip();
	}


	@Override
	public void onMouse( float posX, float posY, MouseButton button, boolean down )
	{
		//TODO: Scrollbar dragging
		super.onMouse( posX, posY, button, down );
	}

	@Override
	public void onMouseWheel( float mouseX, float mouseY, int clicks )
	{
		if ( maxElementOffset > 0 && isMouseWithin( mouseX, mouseY ) )
		{
			if ( clicks < 0 )
			{
				if ( elementOffset < maxElementOffset )
				{
					drawOffset += elementRenderer.getElementHeight( elementOffset );
					++elementOffset;
				}
			}
			else if ( clicks > 0 )
			{
				if ( elementOffset > 0 )
				{
					--elementOffset;
					drawOffset -= elementRenderer.getElementHeight( elementOffset );
				}
			}
		}
		super.onMouseWheel( mouseX, mouseY, clicks );
	}

	@Override
	public void onMouseDown( float mouseX, float mouseY, MouseButton button )
	{
		if ( button == MouseButton.LEFT && isMouseWithin( mouseX, mouseY ) )
		{
			mouseX -= posX;
			mouseY -= posY;
			if ( mouseX < listWidth )
			{
				int item = elementRenderer.getItem( mouseY );
				if ( item != -1 )
					onSelectionChanged( item );
			}
			else
			{
				//TODO: Scrollbar dragging
			}
		}
		super.onMouseDown( mouseX, mouseY, button );
	}

//	@Override
//	public void onMouseUp( float posX, float posY, int button )
//	{
//		throw new UnsupportedOperationException("Not supported yet.");
//	}

//	// sbIGuiKeyboardHandler
//	@Override
//	public void onKey( float posX, float posY, char key, boolean down, int extended )
//	{
//		throw new UnsupportedOperationException("Not supported yet.");
//	}
//
//	@Override
//	public void onKeyDown( float posX, float posY, char key, int extended )
//	{
//		throw new UnsupportedOperationException("Not supported yet.");
//	}
//
//	@Override
//	public void onKeyUp( float posX, float posY, char key, int extended )
//	{
//		throw new UnsupportedOperationException("Not supported yet.");
//	}


	private void setupTextures()
	{
		scrollbar = getTheme().getTexture( SCROLLBAR );
		scrollbarWidth  = scrollbar.getWidth()  / sbGuiTheme.DOWNSCALE;
		scrollbarHeight = scrollbar.getHeight() / sbGuiTheme.DOWNSCALE;
		scrollbarSliceY = scrollbarHeight / 3.f;
	}


	private void setupList()
	{
		elementOffset = 0;
		drawOffset = 0;
		scrollbarY = -1;

		listWidth = width;
		listHeight = 0;
		if ( elements != null )
		{
			elementRenderer.updateCache();

			for ( int index=0; index<elements.size(); ++index )
				listHeight += elementRenderer.getElementHeight( index );

			maxElementOffset = 0;
			if ( listHeight+(2*OFFSET_Y) >= height )
			{
				listWidth -= scrollbarWidth;
				elementRenderer.updateCache();

				float h = listHeight - height;
				for ( int index=0; index<elements.size(); ++index )
				{
					h -= elementRenderer.getElementHeight( index );
					if ( h < 0 )
					{
						maxElementOffset = index+1;
						break;
					}
				}
			}
		}
	}


	public interface IElementRenderer
	{
		public void		updateCache();
		public float	getElementHeight( int index );
		public float	drawElement( int index, float y );
		public String	getTooltip( float y );
		public int		getItem( float y );
	}

	private final static class StringRenderer implements IElementRenderer
	{
		private sbGuiListbox			listbox;
		private List< CachedElement >	cache;
		private float					lineHeight;
		private float					lineOffset;

		public StringRenderer( sbGuiListbox listbox )
		{
			this.listbox = listbox;
			lineHeight   = listbox.font.getHeight() * 0.8f;
			lineOffset   = 0;//-listbox.font.getHeight() * 0.1f;
		}

		@Override
		public void updateCache()
		{
			if ( listbox.elements == null || listbox.elements.isEmpty() )
				return;

			cache = new ArrayList();
			final float maxWidth = listbox.listWidth - (2*OFFSET_X);// - (2*OFS);
			for ( int i=0; i<listbox.elements.size(); ++i )
			{
				CachedElement element = new CachedElement();
				Object elem = listbox.elements.get( i );
				element.text   = elem != null ? elem.toString() : "?";
				element.height = lineHeight;//listbox.font.getStringHeight( element.text );
				if ( listbox.font.getStringWidth( element.text ) > maxWidth )
				{
					element.stripped = element.text;
					boolean stripped = false;
					while ( listbox.font.getStringWidth( element.stripped ) > maxWidth )
					{
						element.stripped = element.stripped.substring( 0, element.stripped.length() - ( stripped ? 4 : 1 ) ) + "...";
						stripped = true;
					}
				}
				cache.add( element );
			}
		}

		@Override
		public float getElementHeight( int index )
		{
//			return lineHeight;
			float h = 0;
			if ( cache != null && !cache.isEmpty() )
				h = cache.get( index ).height;
			return h;
		}

		@Override
		public float drawElement( int index, float y )
		{
			if ( cache != null && 0 <= index && index < cache.size() )
			{
				CachedElement element = cache.get( index );
				String str = element.stripped != null ? element.stripped : element.text;
				if ( index == listbox.selection )
				{
					sbGuiListbox.selectionTexture.select();
					sbGui.drawRect( 0/*-OFS*/, y+lineOffset+OFFSET_Y, listbox.listWidth/*+(2*OFS)*/, y+lineOffset+element.height+OFFSET_Y, 0 );
					listbox.font.drawString( Tessellator.instance, OFFSET_X, y+lineOffset, 0, listbox.colorSelText, str );
				}
				else
					listbox.font.drawString( Tessellator.instance, OFFSET_X, y+lineOffset, 0, listbox.color, str );
				return element.height;
			}
			return 0;
		}

		@Override
		public String getTooltip( float y )
		{
			int index = getItem( y );
			if ( index != -1 )
			{
				CachedElement element = cache.get( index );
				if ( element.stripped != null )
					return element.text;
			}
			return null;
		}

		@Override
		public int getItem( float y )
		{
			if ( cache != null && !cache.isEmpty() )
			{
				float h = 0;
				for ( int index=listbox.elementOffset; index<cache.size(); ++index )
				{
					CachedElement element = cache.get( index );
					if ( y <= h+element.height )
					{
						if ( h <= y )
							return index;
						break;
					}
					h += element.height;
				}
			}
			return -1;
		}

		private final class CachedElement
		{
			public String	text;
			public String	stripped;
			public float	height;
		}
	}


	@Override
	public boolean loadParameter( String id, String value )
	{
		if ( id.equals( "font" ) )
		{
			font = getTheme().selectFont( sbGuiFont.String2FontSpec( value ) );
			if ( font != null )
				return true;
		}
		if ( id.equals( "state" ) )
		{
			state = value.equals( "disabled" ) ? State.DISABLED : State.NONE;
			return true;
		}
		return false;
	}

	public static sbGuiListbox loadFromLayout( sbGuiPane parent, Map< String, String > params )
	{
		try
		{
			sbGuiListbox listbox = new sbGuiListbox( parent );
			if ( sbGuiLayout.loadLayout( listbox, params ) )
				return listbox;
		}
		catch ( Exception e )
		{ 
			sbMod.logger().severe( "... while creating listbox" );
		}
		return null; 
	}

}
