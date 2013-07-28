package sillybits.core.gui;

import cpw.mods.fml.client.FMLClientHandler;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import sillybits.core.sbMod;
import sillybits.core.util.sbUtil;
import sillybits.core.gui.sbGui.FileReader;
import sillybits.core.gui.sbGui.SimpleFileParser;
import sillybits.core.gui.sbGuiFont.FontSpec;

/**
 * Themes, the glue for handling textures and fonts.
 * 
 * @author SillyBits
 */
public class sbGuiTheme
{
	private final static boolean						DEBUG_LOAD		= false;

	public final static String							DEFAULT_THEME	= "default";
	public final static String							DEFAULT_PATH	= "/mods/themes/";
	public final static float							DOWNSCALE		= 4.f;
	private	final static String							TEXTURE_ID		= "texture";
	private final static String							FONT_ID			= "font";

	private final static Minecraft						client			= FMLClientHandler.instance().getClient();

	private String										themeName		= null;
	private String										themePath		= null;
	private Map< String, sbGuiTexture >					textures		= new HashMap();
	private Map< String, Map< String, sbGuiFont > >		fonts			= new HashMap();


	public sbGuiTheme( String themeName, String themePath, boolean preload )
	{
		this.themeName	= themeName;
		this.themePath	= themePath;
		if ( !this.themePath.endsWith("/") )
			this.themePath += "/";
		if ( preload )
			preload();
	}

	public sbGuiTheme( String themeName, String themePath )
	{
		this( themeName, themePath, false );
	}

	public sbGuiTheme( String themeName, boolean preload )
	{
		this( themeName, DEFAULT_PATH, preload );
	}

	public sbGuiTheme( String themeName )
	{
		this( themeName, DEFAULT_PATH, false );
	}


	public String getPath()
	{
		return themePath + themeName + "/";
	}


	public boolean hasTexture( String textureName )
	{
		return textures.containsKey( textureName );
	}

	public sbGuiTexture getTexture( String textureName )
	{
		if ( DEBUG_LOAD )
			sbMod.logger().log( "Trying to get texture '"+textureName+"'" );
		sbGuiTexture texture = textures.get( textureName );
		if ( texture == null )
		{
			try
			{
				String tex = getPath() + "textures/" + textureName + ".png";
				if ( DEBUG_LOAD )
					sbMod.logger().log( "Texture '"+textureName+"' not yet loaded, trying to load from '"+tex+"'" );
				InputStream inStream = getClass().getResourceAsStream( tex );
				if ( inStream != null )
				{
					BufferedImage image = ImageIO.read( inStream );
					if ( image != null )
					{
						int glTexNo = client.renderEngine.allocateAndSetupTexture( image );//getTexture( tex );
						texture = new sbGuiTexture( textureName, image.getWidth(), image.getHeight(), glTexNo );
						textures.put( textureName, texture );
					}
					else
						sbMod.logger().log( "Error loading texture '"+tex+"'" );
				}
				else
					sbMod.logger().log( "Texture '"+tex+"' not found" );
			}
			catch ( Exception e )
			{
				if ( !(e instanceof FileNotFoundException) ) 
				{
					e.printStackTrace();
					sbMod.logger().log( "... while loading texture '"+textureName+"'" );
				}
			}
		}
		return texture;
	}


	public sbGuiFont getFont( String fontName, EnumSet<FontSpec> fontSpecs )
	{
		if ( DEBUG_LOAD )
			sbMod.logger().log( "Trying to get font '"+fontName+"' (spec="+fontSpecs.toString()+")" );
		Map< String, sbGuiFont > alternatives = fonts.get( fontName );
		if ( alternatives == null )
		{
			alternatives = new HashMap();
			fonts.put( fontName, alternatives );
		}
		String fontSpec = sbGuiFont.FontSpec2String( fontSpecs );
		sbGuiFont font = alternatives.get( fontSpec );
		if ( font == null )
		{
			if ( DEBUG_LOAD )
				sbMod.logger().log( "Font '"+fontName+"' not yet loaded, trying to load now (spec="+fontSpecs.toString()+")" );
			font = sbGuiFont.load( getPath() + "fonts/", fontName, fontSpecs );
			if ( font != null )
				alternatives.put( fontSpec, font );
		}
		return font;
	}

	public sbGuiFont selectFont( EnumSet<FontSpec> fontSpecs )
	{
		sbGuiFont font = null;
		String fontSpec = sbGuiFont.FontSpec2String( fontSpecs );
		for ( String fontName : fonts.keySet() )
		{
			Map< String, sbGuiFont > alternatives = fonts.get( fontName );
			font = alternatives.get( fontSpec );
			if ( font == null )
			{
				font = sbGuiFont.load( getPath() + "fonts/", fontName, fontSpecs );
				if ( font != null )
					alternatives.put( fontSpec, font );
			}
		}
		if ( font == null )
			sbMod.logger().log( "Unable to find a font alternative for "+fontSpecs.toString() );
		return font;
	}


	private void preload()
	{
		final String indexFile = getPath() + "index.thm";
		FileReader reader = new FileReader( indexFile, new ThemeFileParser() );
		reader.read();
	}

	private final class ThemeFileParser extends SimpleFileParser
	{
		@Override
		public boolean parseLine( String line )
		{
			String[] strArray = line.split( ":" );
			if ( strArray.length >= 2 )
			{
				if ( strArray[0].equals( TEXTURE_ID ) && strArray.length == 2 )
					return getTexture( strArray[1] ) != null;
				if ( strArray[0].equals( FONT_ID ) && strArray.length == 3 )
				{
					EnumSet<FontSpec> fontSpecs = sbGuiFont.String2FontSpec( strArray[2] );
					if ( fontSpecs != null && fontSpecs.size() > 0 )
					{
						if ( getFont( strArray[1], fontSpecs ) != null )
							return true;
						sbMod.logger().log( "Theme index contains an invalid entry '" + line + "', failed to load font. "+sbUtil.array2String(strArray) );
					}
					else
						sbMod.logger().log( "Theme index contains an invalid entry '" + line + "', can't create font specs. "+sbUtil.array2String(strArray) );
				}
				else
					sbMod.logger().log( "Theme index contains an invalid entry '" + line + "', no match on id. "+sbUtil.array2String(strArray) );
			}
			else
				sbMod.logger().log( "Theme index contains an invalid entry '" + line + "', not enough parameters. "+sbUtil.array2String(strArray) );
			return false;
		}
	}

}
