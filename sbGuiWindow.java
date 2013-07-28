package sillybits.core.gui;

import java.util.EnumSet;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import sillybits.core.sbMod;
import sillybits.core.gui.sbGui.MouseButton;
import sillybits.core.gui.sbGuiFont.FontSpec;

/**
 * Windows are panes with both a caption and a close button.
 * A window is also aware of a theme to use.
 * 
 * @author SillyBits
 */
public abstract class sbGuiWindow extends sbGuiPane
{
	protected final static boolean	DEBUG				= false;

	protected final static EnumSet	CAPTION_FONTSPEC	= EnumSet.of( FontSpec.SIZE_BIG, FontSpec.STYLE_BOLD );
	protected final static int		CAPTION_TEXT_COLOR	= 0x00FFFFFF;
	protected final static String	BACKGROUND_TEXTURE	= "background";
	protected final static String	OVERLAY_TEXTURE		= "overlay";
	protected final static String	CLOSE_BUTTON		= "close";
	protected final static String	CAPTION				= "caption";
	protected final static String	CAPTION_FONT		= "caption_font";
	protected final static String	CAPTION_COLOR		= "caption_color";
	protected final static String	CAPTION_OFFSET		= "caption_offset";

	private String					caption;
	private sbGuiFont				captionFont;
	private EnumSet< FontSpec >		captionFontSpec		= CAPTION_FONTSPEC;
	private int						captionColor		= CAPTION_TEXT_COLOR;
	private float					captionOffset		= 0.f;
	private sbGuiTheme				theme;
	private String					layoutName;
	private sbGuiLayout				layout;
	private sbGuiTexture			backgroundTexture;
	private sbGuiTexture			overlayTexture;
	private sbGuiButton				closeButton;


	public sbGuiWindow( float posX, float posY, float posZ, float width, float height, String caption, sbGuiTheme theme, String layoutName )
	{
		super( null, null, posX, posY, posZ, width, height );
		this.caption	= caption;
		this.theme		= theme;
		this.layoutName	= layoutName;
	}

	public sbGuiWindow( float posX, float posY, float posZ, float width, float height, String caption, sbGuiTheme theme )
	{
		this( posX, posY, posZ, width, height, caption, theme, null );
	}

	public sbGuiWindow( float posX, float posY, float posZ, float width, float height, sbGuiTheme theme, String layoutName )
	{
		this( posX, posY, posZ, width, height, null, theme, layoutName );
	}


	/*
	 * Getter/setter
	 */

	public void setTheme( sbGuiTheme theme )	{ this.theme = theme; onThemeChanged(); }
	@Override public sbGuiTheme getTheme()		{ return theme; }

	public void setCaption( String caption )	{ this.caption = caption; }
	public void setCaptionFont( sbGuiFont font ){ captionFont = font; }
	public void setCaptionColor( int color )	{ captionColor = color; }

	@Override protected sbGuiPane getTopmostParent() { return this; }


	@Override
	protected void onThemeChanged()
	{
		super.onThemeChanged();
		backgroundTexture = null;
	}


	protected abstract void onClose();
	protected abstract void onSelectionChanged( sbGuiListbox listbox, int oldSelection, int newSelection );
	protected abstract void onTextChanged( sbGuiElement child, String newText );

	protected void onButtonClicked( sbGuiElement button )
	{
		if ( closeButton != null && closeButton.getName().equals( button.getName() ) )
			onClose();
	}


	@Override
	public void render()
	{
		if ( layoutName != null && layout == null )
			setupLayout();
		if ( captionFont == null )
			captionFont = theme.selectFont( captionFontSpec );

		if ( backgroundTexture != null )
		{
			backgroundTexture.select();
			GL11.glColor4f( 1.f, 1.f, 1.f, 1.f );
			sbGui.drawTexturedRect( posX, posY, posX+width, posY+height, posZ-0.1f, 0, 0, 1, 1 );
		}
		else if ( DEBUG )
			sbMod.logger().log( "No background to render" );

		super.render();

		if ( overlayTexture != null )
		{
			overlayTexture.select();
			GL11.glColor4f( 1.f, 1.f, 1.f, 1.f );
			sbGui.drawTexturedRect( posX, posY, posX+width, posY+height, posZ, 0, 0, 1, 1 );
		}
		else if ( DEBUG )
			sbMod.logger().log( "No overlay to render" );

		if( caption != null && captionFont != null )
			captionFont.drawCenteredShadowedString( Tessellator.instance, posX+(width/2.f), posY+captionOffset, posZ+0.0001f, captionColor, caption );
		else if ( DEBUG )
			sbMod.logger().log( "No caption to render" );
	}


	protected void setupLayout()
	{
		layout = sbGuiLayout.load( theme, layoutName );
		if ( layout != null )
		{
			if ( backgroundTexture == null && layout.properties.containsKey( BACKGROUND_TEXTURE ) )
				backgroundTexture = theme.getTexture( layout.properties.get( BACKGROUND_TEXTURE ) );
			if ( backgroundTexture == null )
				sbMod.kill( "Window '"+name+"' is missing its background texture, THIS is a bug you should report!" );

			if ( layout.properties.containsKey( OVERLAY_TEXTURE ) )
				overlayTexture = theme.getTexture( layout.properties.get( OVERLAY_TEXTURE ) );

			if ( ( caption == null || caption.isEmpty() ) && layout.properties.containsKey( CAPTION ) )
			{
				caption = layout.properties.get( CAPTION );
				if ( layout.properties.containsKey( CAPTION_FONT ) )
					captionFontSpec = sbGuiFont.String2FontSpec( layout.properties.get( CAPTION_FONT ) );
				if ( layout.properties.containsKey( CAPTION_COLOR ) )
					captionColor = (Integer)sbGui.convertValue( Integer.class, layout.properties.get( CAPTION_COLOR ) );
				if ( layout.properties.containsKey( CAPTION_OFFSET ) )
					captionOffset = ( (Float)sbGui.convertValue( Float.class, layout.properties.get( CAPTION_OFFSET ) ) ) / sbGuiTheme.DOWNSCALE;
			}

			if ( width == 0 )
			{
				width  = backgroundTexture.width  / sbGuiTheme.DOWNSCALE;
				height = backgroundTexture.height / sbGuiTheme.DOWNSCALE;
				posX  -= width  / 2.f;
				posY  -= height / 2.f;
			}

			if ( !layout.createChilds( this ) )
				sbMod.kill( "Error creating one or more childs, THIS is a bug you should report!" );
			String closeButtonName = layout.properties.containsKey( CLOSE_BUTTON ) ? layout.properties.get( CLOSE_BUTTON ) : CLOSE_BUTTON;
			closeButton = (sbGuiButton)getChild( closeButtonName );
			if ( closeButton == null )
				sbMod.logger().severe( "Mandatory 'close' button not found, window won't be able to close on button press" );
//				sbMod.kill( "Mandatory 'close' button not found, THIS is a bug you should report!" );
		}
	}

}
