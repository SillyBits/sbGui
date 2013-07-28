package sillybits.core.gui;

import java.util.EnumSet;
import java.util.Iterator;
import net.minecraft.client.renderer.Tessellator;
import sillybits.core.sbMod;

/**
 * Basic font support, real fonts will derive from this.
 * 
 * @author SillyBits
 */
public abstract class sbGuiFont
{
	protected final static boolean	DEBUG			= false;
	protected final static char		UNKNOWN_CHAR	= '?';

	protected String				fontName;
	protected EnumSet< FontSpec >	fontSpecs;


	public enum FontSpec
	{
		SIZE,
		SIZE_SMALL("16",SIZE), SIZE_16("16",SIZE), 
		SIZE_NORMAL("24",SIZE), SIZE_24("24",SIZE), 
		SIZE_BIG("32",SIZE), SIZE_32("32",SIZE),
		SIZE_HUGE("48",SIZE), SIZE_48("48",SIZE),

		STYLE,
		STYLE_NORMAL("",STYLE), STYLE_BOLD("bold",STYLE), STYLE_ITALIC("italic",STYLE),

		TYPE,
		TYPE_TRUETYPE("truetype",TYPE), TYPE_BITMAP("bitmap",TYPE);

		private FontSpec()								{ this.id = null; this.type = null; }
		private FontSpec( String id, FontSpec type )	{ this.id = id;   this.type = type; }

		private String		id;
		private FontSpec	type;
		public String	id()	{ return id; }
		public FontSpec type()	{ return type; }

		public static FontSpec size( EnumSet< FontSpec > fontSpecs )
		{
			EnumSet result = matching( fontSpecs, SIZE );
			return result.size() > 0 ? (FontSpec)result.toArray()[0] : null; 
		}
		public static EnumSet< FontSpec > styles( EnumSet< FontSpec > fontSpecs )
		{ 
			return matching( fontSpecs, STYLE );
		}
		public static FontSpec type( EnumSet< FontSpec > fontSpecs )
		{
			EnumSet result = matching( fontSpecs, TYPE );
			return result.size() > 0 ? (FontSpec)result.toArray()[0] : null; 
		}
		private static EnumSet< FontSpec > matching( EnumSet< FontSpec > fontSpecs, FontSpec type )
		{
			EnumSet< FontSpec > matching = EnumSet.noneOf( FontSpec.class );
			for ( FontSpec fontSpec : fontSpecs )
				if ( fontSpec.type() == type )//if ( fontSpec.type().ordinal() == type.ordinal() )
					matching.add( fontSpec );
			return matching;
		}
	};


	protected sbGuiFont( String fontName, EnumSet< FontSpec > fontSpecs )
	{
		this.fontName	= fontName;
		this.fontSpecs	= fontSpecs;
	}


	/*
	 * Getter/setter
	 */

	public String				getName()	{ return fontName; }
	public FontSpec				getSize()	{ return FontSpec.size  ( fontSpecs ); }
	public EnumSet< FontSpec >	getStyles()	{ return FontSpec.styles( fontSpecs ); }
	public FontSpec				getType()	{ return FontSpec.type  ( fontSpecs ); }
	public EnumSet< FontSpec >	getSpec()	{ return fontSpecs; }

	public boolean				isBold()	{ return fontSpecs.contains(FontSpec.STYLE_BOLD); }
	public boolean				isItalic()	{ return fontSpecs.contains(FontSpec.STYLE_ITALIC); }
	public boolean				isTruetype(){ return fontSpecs.contains(FontSpec.TYPE_TRUETYPE); }
	public boolean				isBitmap()	{ return fontSpecs.contains(FontSpec.TYPE_BITMAP); }


	/*
	 * Drawing strings
	 */

	public abstract float		drawString( Tessellator tess, float x, float y, float z, int color, String str );
	public abstract float		drawCenteredString( Tessellator tess, float x, float y, float z, int color, String str );
//	public abstract float		drawRightString( Tessellator tess, float x, float y, float z, int color, String str );
	public abstract float		drawShadowedString( Tessellator tess, float x, float y, float z, int color, String str );
	public abstract float		drawCenteredShadowedString( Tessellator tess, float x, float y, float z, int color, String str );
//	public abstract float		drawRightShadowedString( Tessellator tess, float x, float y, float z, int color, String str );

	public abstract float		getStringWidth( String str );
	public abstract float		getCharWidth( char ch );
	public abstract float		getHeight();

	public float getStringHeight( String str )
	{
		if ( str == null || str.isEmpty() )
			return 0;
		int count = 1;
		final int length = str.length();
		int lastPos = 0;
		int pos;
		while ( lastPos < length )
		{
			pos = str.indexOf( "\\n", lastPos );
			if ( pos == -1 )
				break;
			count ++;
			lastPos = pos+1;
		}
		return count * getHeight();
	}


	protected abstract boolean setup( String fontPath );

	public static sbGuiFont load( String fontPath, String fontName, EnumSet< FontSpec > fontSpecs )
	{
		sbGuiFont font = null;
		if ( fontSpecs.contains( FontSpec.TYPE_BITMAP ) )
			font = new sbGuiBitmapFont( fontName, fontSpecs );
//		else if ( fontSpecs.contains( FontSpec.TYPE_TRUETYPE ) )
//			font = new sbGuiTruetypeFont( fontName, fontSpecs );
		if ( font != null )
			if ( !font.setup( fontPath ) )
				font = null;
		return font;
	}


	/*
	 * Helpers
	 */

	public static String FontSpec2String( EnumSet< FontSpec > fontSpecs )
	{
		String str = FontSpec.size( fontSpecs ).id();
		if ( fontSpecs.contains( FontSpec.STYLE_BOLD ) )
			str += FontSpec.STYLE_BOLD.id();
		if ( fontSpecs.contains( FontSpec.STYLE_ITALIC ) )
			str += FontSpec.STYLE_ITALIC.id();
//		str += "." + FontSpec.type( fontSpecs ).id();
		return str;
	}

	public static EnumSet< FontSpec > String2FontSpec( String str )
	{
		if ( DEBUG )
			sbMod.logger().log( "String2FontSpec: Trying to convert spec '"+str+"'" );
		FontSpec[] fontSpecArray = FontSpec.values();
		EnumSet< FontSpec > fontSpecs = EnumSet.noneOf( FontSpec.class );
		String[] strArray = str.split("\\,");
		if ( strArray.length > 0 )
		{
			int i, j;
			String id;
			for ( i=0; i<strArray.length; ++i )
			{
				for ( j=0; j<fontSpecArray.length; ++j )
				{
					id = fontSpecArray[j].id();
					if ( id != null && id.equals( strArray[i] ) )
					{
						fontSpecs.add( fontSpecArray[j] );
						break;
					}
				}
				if ( j == fontSpecArray.length )
					sbMod.logger().log( "String2FontSpec: '" + str + "' contains an invalid value '" + strArray[i] + "'" );
			}
		}
		else if ( DEBUG )
			sbMod.logger().log( "String2FontSpec: '" + str + "' resulted in an empty array" );
		return fontSpecs;
	}

}
