package sillybits.core.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;
import sillybits.core.io.sbInStream;
import sillybits.core.sbMod;

/**
 * Font implementation which is able to read .fnt files created with AngelFont BMFont tool.
 * 
 * Current limitation:
 * - No support for kerning yet.
 * - Only one page allowed, so increase texture size if tool yields more than 1 page.
 * 
 * @author SillyBits
 */
public class sbGuiBitmapFont extends sbGuiFont
{
	private int							version			= -1;
	private BlockInfo					infoBlock;
	private BlockCommon					commonBlock;
	private List< String >				pages;
	private Map< Character, CharData >	chars;
//	private Map< ?, ? >					kernings;
	private CharData					charDataUnknown;
	private int							texture			= -1;
//	private int							displistStart	= -1;
	private float						shadowOffset;


	protected sbGuiBitmapFont( String fontName, EnumSet<FontSpec> fontSpecs )
	{
		super( fontName, fontSpecs );
	}


	@Override
	public float drawString( Tessellator tess, float x, float y, float z, int color, String str )
	{
		float width = 0;

		final double orgX = tess.xOffset;
		final double orgY = tess.yOffset;
		final double orgZ = tess.zOffset;

		tess.startDrawingQuads();
		tess.setColorOpaque_I( color );

		GL11.glBindTexture( GL11.GL_TEXTURE_2D, texture );

		CharData charData;
		for ( int i=0; i<str.length(); ++i )
		{
			charData = chars.get( Character.valueOf( str.charAt(i) ) );
			if ( charData == null )
				charData = charDataUnknown;
			tess.setTranslation( x+width+charData.ofsX, y+charData.ofsY, z );
			tess.addVertexWithUV( 0         , charData.h, 0, charData.u0, charData.v1 );
			tess.addVertexWithUV( charData.w, charData.h, 0, charData.u1, charData.v1 );
			tess.addVertexWithUV( charData.w, 0         , 0, charData.u1, charData.v0 );
			tess.addVertexWithUV( 0         , 0         , 0, charData.u0, charData.v0 );
			width += charData.d;
		}

		tess.draw();
		tess.setTranslation( orgX, orgY, orgZ );

		return width;
	}

	@Override
	public float drawCenteredString( Tessellator tess, float x, float y, float z, int color, String str )
	{
		float width = getStringWidth( str );
		drawString( tess, x-(width/2.f), y, z, color, str );
		return width;
	}

	@Override
	public float drawShadowedString( Tessellator tess, float x, float y, float z, int color, String str )
	{
		drawString( tess, x+shadowOffset, y+shadowOffset, z-0.0000001f, sbGui.getShadowColor( color ), str );
		return drawString( tess, x, y, z, color, str ) + shadowOffset;
	}

	@Override
	public float drawCenteredShadowedString( Tessellator tess, float x, float y, float z, int color, String str )
	{
		float width = getStringWidth( str ); //-(shadowOffset/2.f);
		drawShadowedString( tess, x-(width/2.f), y, z, color, str );
		return width;
	}
			
//<editor-fold defaultstate="collapsed" desc="drawString2 using a display list">
//	private boolean buildDisplayLists()
//	{
//		displistStart = GL11.glGenLists( 256 );
//		if ( displistStart == 0 )
//			return false;
////		int glError = GL11.glGetError();
////		if ( glError != 0 )
////			sbMod.logger().log( "GL error "+glError+" while generating new lists" );
//
//		CharData charData;
//		for ( int i=0; i<256; ++i )
//		{
//			charData = chars.get( Character.valueOf( (char)i ) );
//			if ( charData == null )
//				charData = charDataUnknown;
//
//			GL11.glNewList( displistStart+i, GL11.GL_COMPILE_AND_EXECUTE );
//				GL11.glTranslatef( charData.ofsX, charData.ofsY, 0 );
//				GL11.glBegin( GL11.GL_QUADS );
//					GL11.glTexCoord2f( charData.u0, charData.v1 );	GL11.glVertex3f( 0         , charData.h, 0 );
//					GL11.glTexCoord2f( charData.u1, charData.v1 );	GL11.glVertex3f( charData.w, charData.h, 0 );
//					GL11.glTexCoord2f( charData.u1, charData.v0 );	GL11.glVertex3f( charData.w, 0         , 0 );
//					GL11.glTexCoord2f( charData.u0, charData.v0 );	GL11.glVertex3f( 0         , 0         , 0 );
//				GL11.glEnd();
//				GL11.glTranslatef( charData.d-charData.ofsX, -charData.ofsY, 0 );
////			glError = GL11.glGetError();
////			if ( glError != 0 )
////				sbMod.logger().log( "GL error "+glError+" while executing glEnd" );
//			GL11.glEndList();
//
//			int glError = GL11.glGetError();
//			if ( glError != 0 )
//				sbMod.logger().log( "GL error "+glError+" while creating list with offset "+i );
//		}
//
//		return true;
//	}
//	
//	public float drawString2( Tessellator tess, float x, float y, float z, int color, String str )
//	{
//		try
//		{
//			int glError;
//			float width = 0;
//
////			ByteBuffer charBuffer = BufferUtils.createByteBuffer( str.length() );
////			charBuffer.put( str.getBytes("UTF-8") );
////			charBuffer.flip();
//			if ( displistStart == -1 )
//				buildDisplayLists();
//			ShortBuffer charBuffer = BufferUtils.createShortBuffer( str.length() );
//			for ( int i=0; i<str.length(); ++i )
//				charBuffer.put( (short)(str.charAt(i)+displistStart) );
//			charBuffer.flip();
//
//			GL11.glMatrixMode( GL11.GL_MODELVIEW );
//			GL11.glPushMatrix();
//			GL11.glTranslatef( x, y, z );
//			glError = GL11.glGetError();
//			if ( glError != 0 )
//				sbMod.logger().log( "GL error "+glError+" while working with/pushing matrix" );
//
//			GL11.glColor4b( (byte)(color&0xFF), (byte)((color>>8)&0xFF), (byte)((color>>16)&0xFF), (byte)((color>>24)&0xFF) );
//			glError = GL11.glGetError();
//			if ( glError != 0 )
//				sbMod.logger().log( "GL error "+glError+" while setting color" );
//			
//			GL11.glBindTexture( GL11.GL_TEXTURE_2D, texture );
//			glError = GL11.glGetError();
//			if ( glError != 0 )
//				sbMod.logger().log( "GL error "+glError+" while binding texture" );
//			
////			GL11.glListBase( displistStart );
//////			glError = GL11.glGetError();
//////			if ( glError != 0 )
//////				sbMod.logger().log( "GL error "+glError+" while setting list base" );
//
////			GL11.glBegin( GL11.GL_QUADS );
////			glError = GL11.glGetError();
////			if ( glError != 0 )
////				sbMod.logger().log( "GL error "+glError+" while executing glBegin" );
//
//			GL11.glCallLists( charBuffer );
////			glError = GL11.glGetError();
////			if ( glError != 0 )
////				sbMod.logger().log( "GL error "+glError+" while executing glCallLists" );
//			
////			GL11.glEnd();
////			glError = GL11.glGetError();
////			if ( glError != 0 )
////				sbMod.logger().log( "GL error "+glError+" while executing glEnd" );
//
//			glError = GL11.glGetError();
//			if ( glError != 0 )
//				sbMod.logger().log( "GL error "+glError+" while executing list" );
//
//			GL11.glMatrixMode( GL11.GL_MODELVIEW );
//			GL11.glPopMatrix();
//
//			glError = GL11.glGetError();
//			if ( glError != 0 )
//				sbMod.logger().log( "GL error "+glError+" while popping matrix" );
//
//			return width;
//		}
//		catch ( Exception e ) { }
//		return 0;
//	}
//</editor-fold>


	@Override
	public float getStringWidth( String str )
	{
		float width = 0;

		CharData charData;
		for ( int i=0; i<str.length(); ++i )
		{
			charData = chars.get( Character.valueOf( str.charAt(i) ) );
			if ( charData == null )
				charData = charDataUnknown;
			width += charData.d;
		}

		return width;
	}

	@Override
	public float getCharWidth( char ch )
	{
		CharData charData = chars.get( ch );
		if ( charData == null )
			charData = charDataUnknown;
		return charData.d;
	}

	@Override public float getHeight() { return commonBlock.lineHeight; }


	@Override
	protected boolean setup( String fontPath )
	{
		String fontSpec = sbGuiFont.FontSpec2String( fontSpecs );
		String fontFile = fontPath + fontName + fontSpec + ".fnt";

		try
		{
			InputStream inStream = getClass().getResourceAsStream( fontFile );
			if ( inStream == null )
			{
				sbMod.logger().severe( "Font '" + fontFile + "' with spec '" + fontSpec + "' not found!" );
				return false;
			}
			sbInStream data = new sbInStream( inStream );

			HeaderReader headerReader = new HeaderReader();
			if ( !headerReader.read( data ) )
			{
				sbMod.logger().severe( "Invalid header detected!" );
				return false;
			}

			BlockReader blockReader = new BlockReader();
			while ( data.available() > 0 )
			{
				if ( !blockReader.read( data ) )
				{
					sbMod.logger().severe( "Failed to read a block!" );
					return false;
				}
			}

			if ( infoBlock == null || commonBlock == null || pages == null || chars == null 
			  || commonBlock.lineHeight == 0 || commonBlock.pages != 1 )
			{
				sbMod.logger().severe( "Font not set up correctly!" );
				return false;
			}

			charDataUnknown	= chars.get( Character.valueOf( UNKNOWN_CHAR ) );
			texture			= Minecraft.getMinecraft().renderEngine.getTexture( fontPath + pages.get(0) );
			shadowOffset	= 0.25f + ( 0.5f / 48.f * Math.abs( infoBlock.fontSize ) );
			if ( isBold() )
				shadowOffset *= 1.05f;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			sbMod.logger().severe( "... while reading resource '" + fontFile +"' with spec '" + fontSpec + "'" );
			return false;
		}
		
		return true;
	}


	/*
	 * Helper classes
	 */

	private interface IReader
	{
		public boolean read( sbInStream data ) throws IOException;
	}


	/*
	 * header
	 * The first three bytes are the file identifier and must always be 66, 77, 70, or "BMF". The fourth byte gives the format version, 
	 * currently it must be 3.
	 */
	private final class HeaderReader implements IReader
	{
		@Override
		public final boolean read( sbInStream data ) throws IOException
		{
			if ( data.readByte() != 'B' )
				return false;
			if ( data.readByte() != 'M' )
				return false;
			if ( data.readByte() != 'F' )
				return false;
			version = data.readByte();
			if ( version != 3 )
			{
				sbMod.logger().severe( "Unsupported version! (version "+version+")" );
				return false;
			}
			return true;
		}
	}

	/*
	 * block
	 * Each block starts with a one byte block type identifier, followed by a 4 byte integer that gives the size of the block, not 
	 * including the block type identifier and the size value:
	 * 
	 * type			byte		Block type
	 * length		int			Length of block following (not incl. type or length itself)
	 */
	private final class BlockReader implements IReader
	{
		@Override
		public final boolean read( sbInStream data ) throws IOException
		{
			int type	= data.readByte();
			int length	= readInt( data );

			IBlockReader reader;
			switch ( type )
			{
				case 1:		reader = new BlockInfo();		break;
				case 2:		reader = new BlockCommon();		break;
				case 3:		reader = new BlockPages();		break;
				case 4:		reader = new BlockChars();		break;
			//	case 5:		reader = new BlockKernings();	break;
				default:	
					sbMod.logger().severe( "Invalid block type detected! ("+data.available()+" bytes left)" );
					return false;
			}

			if ( !reader.validateLength( length ) )
			{
				sbMod.logger().severe( "Length check failed for block of type "+type+"! (length "+length+")" );
				return false;
			}
			return reader.readBlock( length, data );
		}
	}
	
	private interface IBlockReader
	{
		public abstract boolean validateLength( int length );
		public abstract boolean readBlock( int length, sbInStream data ) throws IOException;
	}

	/*
	 * info (type=1)
	 * This tag holds information on how the font was generated:
	 * 
	 * face			This is the name of the true type font.
	 * size			The size of the true type font.
	 * bold			The font is bold.
	 * italic		The font is italic.
	 * charset		The name of the OEM charset used (when not unicode).
	 * unicode		Set to 1 if it is the unicode charset.
	 * stretchH		The font height stretch in percentage. 100% means no stretch.
	 * smooth		Set to 1 if smoothing was turned on.
	 * aa			The supersampling level used. 1 means no supersampling was used.
	 * padding		The padding for each character (up, right, down, left).
	 * spacing		The spacing for each character (horizontal, vertical).
	 * outline		The outline thickness for the characters
	 * 
	 * field			size	type	pos		comment
	 * fontSize			2		int		0		If negative, it represents a pt size
	 * bitField			1		bits	2		bit	0: smooth, bit 1: unicode, bit 2: italic, bit 3: bold, bit 4: fixedHeigth, bits 5-7: reserved
	 * charSet			1		uint	3		
	 * stretchH			2		uint	4		If =100, no stretching applied
	 * aa				1		uint	6	
	 * paddingUp		1		uint	7	
	 * paddingRight		1		uint	8	
	 * paddingDown		1		uint	9	
	 * paddingLeft		1		uint	10	
	 * spacingHoriz		1		uint	11	
	 * spacingVert		1		uint	12	
	 * outline			1		uint	13		added with version 2
	 * fontName			n+1		string	14		null terminated string with length n, with n = Block length - 14, incl. terminating \0
	 * 
	 * This structure gives the layout of the fields. Remember that there should be no padding between members. Allocate the size of 
	 * the block using the blockSize, as following the block comes the font name, including the terminating null char. Most of the time 
	 * this block can simply be ignored.
	 */
	private final class BlockInfo implements IBlockReader
	{
		public short	fontSize;
		public boolean	smooth;
		public boolean	unicode;
		public boolean	italic;
		public boolean	bold;
		public boolean	fixedHeight;
		public int		charSet;
		public short	stretchH;
		public byte		aa;
		public byte		padUp;
		public byte		padRight;
		public byte		padDown;
		public byte		padLeft;
		public byte		spaceH;
		public byte		spaceV;
		public byte		outline;
		public String	fontName;

		@Override public boolean validateLength( int length )	{ return length > 14; }

		@Override
		public boolean readBlock( int length, sbInStream data ) throws IOException
		{
			if ( infoBlock != null )
			{
				sbMod.logger().severe( "Invalid state detected! (BlockInfo)" );
				return false;
			}

			fontSize	= readShort( data );
			byte bits	= data.readByte();
			smooth		= ( bits & 0x80 ) != 0;
			unicode		= ( bits & 0x40 ) != 0;
			italic		= ( bits & 0x20 ) != 0;
			bold		= ( bits & 0x10 ) != 0;
			fixedHeight	= ( bits & 0x08 ) != 0;
			charSet		= data.readByte();
			stretchH	= readShort( data );
			aa			= data.readByte();
			padUp		= data.readByte();
			padRight	= data.readByte();
			padDown		= data.readByte();
			padLeft		= data.readByte();
			spaceH		= data.readByte();
			spaceV		= data.readByte();
			outline		= data.readByte();
			fontName	= data.readString();
			
			infoBlock = this;
			return true;
		}
	}

	/*
	 * common (type=2)
	 * This tag holds information common to all characters:
	 * 
	 * lineHeight	This is the distance in pixels between each line of text.
	 * base			The number of pixels from the absolute top of the line to the base of the characters.
	 * scaleW		The width of the texture, normally used to scale the x pos of the character image.
	 * scaleH		The height of the texture, normally used to scale the y pos of the character image.
	 * pages		The number of texture pages included in the font.
	 * packed		Set to 1 if the monochrome characters have been packed into each of the texture channels. In this case alphaChnl describes what is stored in each channel.
	 * alphaChnl	Set to 0 if the channel holds the glyph data, 1 if it holds the outline, 2 if it holds the glyph and the outline, 3 if its set to zero, and 4 if its set to one.
	 * redChnl		Set to 0 if the channel holds the glyph data, 1 if it holds the outline, 2 if it holds the glyph and the outline, 3 if its set to zero, and 4 if its set to one.
	 * greenChnl	Set to 0 if the channel holds the glyph data, 1 if it holds the outline, 2 if it holds the glyph and the outline, 3 if its set to zero, and 4 if its set to one.
	 * blueChnl		Set to 0 if the channel holds the glyph data, 1 if it holds the outline, 2 if it holds the glyph and the outline, 3 if its set to zero, and 4 if its set to one.
	 * 
	 * field			size	type	pos		comment							Example
	 * lineHeight		2		uint	0										1A 00		26px			Line height
	 * base				2		uint	2										12 00		18px			Base line offset
	 * scaleW			2		uint	4										00 02		512px			Width of page textures
	 * scaleH			2		uint	6										00 02		512px			Height of page textures
	 * pages			2		uint	8										01 00		1 page total
	 * bitField			1		bits	10		bits 0-6: res, bit 7: packed	00			00000000		No packing
	 * alphaChnl		1		uint	11										03			3				Zero alpha (but we need outline here!)
	 * redChnl			1		uint	12										00			0				Red   channel -> Pixel data
	 * greenChnl		1		uint	13										00			0				Green channel -> Pixel data
	 * blueChnl			1		uint	14										00			0				Blue  channel -> Pixel data
	 */
	private final class BlockCommon implements IBlockReader
	{
		public float	lineHeight;
		public float	baseLine;
		public float	scaleW;
		public float	scaleH;
		public int		pages;
		public boolean	packed;
		public byte		alpha;
		public byte		red;
		public byte		green;
		public byte		blue;

		@Override public boolean validateLength( int length )	{ return length == 15; }

		@Override
		public boolean readBlock( int length, sbInStream data ) throws IOException
		{
			if ( infoBlock == null || commonBlock != null )
			{
				sbMod.logger().severe( "Invalid state detected! (BlockCommon)" );
				return false;
			}

			lineHeight	= readShort( data ) / sbGuiTheme.DOWNSCALE;
			baseLine	= readShort( data ) / sbGuiTheme.DOWNSCALE;
			scaleW		= readShort( data );
			scaleH		= readShort( data );
			pages		= readShort( data );
			byte bits	= data.readByte();
			packed		= ( bits & 0x01 ) != 0;
			alpha		= data.readByte();
			red			= data.readByte();
			green		= data.readByte();
			blue		= data.readByte();
			
			commonBlock   = this;

			return true;
		}
	}

	/*
	 * page (type=3)
	 * This tag gives the name of a texture file. There is one for each page in the font:
	 * 
	 * id			The page id.
	 * file			The texture file name.
	 * 
	 * field			size	type	pos		comment
	 * pageNames		p*(n+1)	strings	0		p null terminated strings, each with length n
	 *											Example: 45 62 72 69 6D 61 31 36 62 69 6E 5F 30 2E 70 6E 67 00 -> Ebrima16bin_0.png\0
	 * 
	 * This block gives the name of each texture file with the image data for the characters. The string pageNames holds the names 
	 * separated and terminated by null chars. Each filename has the same length, so once you know the size of the first name, you can 
	 * easily determine the position of each of the names. The id of each page is the zero-based index of the string name.
	 */
	private final class BlockPages implements IBlockReader
	{
		@Override public boolean validateLength( int length )	{ return length > 0; }

		@Override
		public boolean readBlock( int length, sbInStream data ) throws IOException
		{
			if ( infoBlock == null || commonBlock == null || pages != null )
			{
				sbMod.logger().severe( "Invalid state detected! (BlockPages)" );
				return false;
			}

			pages = new ArrayList<String>();

			int remain = length;
			while ( remain > 0 )
			{
				String str = data.readString();
				remain -= str.length()+1;
				pages.add( str );
			}
			if ( pages.size() != commonBlock.pages )
			{
				sbMod.logger().severe( "Unable to load pages! (expected "+commonBlock.pages+", but found "+pages.size()+")" );
				return false;
			}
			
			return true;
		}
	}
	
	/*
	 * char (type=4)
	 * This tag describes on character in the font. There is one for each included character in the font:
	 * 
	 * id			The character id.
	 * x			The left position of the character image in the texture.
	 * y			The top position of the character image in the texture.
	 * width		The width of the character image in the texture.
	 * height		The height of the character image in the texture.
	 * xoffset		How much the current position should be offset when copying the image from the texture to the screen.
	 * yoffset		How much the current position should be offset when copying the image from the texture to the screen.
	 * xadvance		How much the current position should be advanced after drawing the character.
	 * page			The texture page where the character image is found.
	 * chnl			The texture channel where the character image is found (1 = blue, 2 = green, 4 = red, 8 = alpha, 15 = all channels).
	 * 
	 * field			size	type	pos			comment		example
	 * id				4		uint	0+c*20					21 00 00 00		ASCII 33 -> !
	 * x				2		uint	4+c*20					E6 00			Left = 230px
	 * y				2		uint	6+c*20					1B 00			Top = 27px
	 * width			2		uint	8+c*20					03 00			Width = 3px
	 * height			2		uint	10+c*20					1A 00			Height = 26px
	 * xoffset			2		int		12+c*20					01 00			Offset X = 1
	 * yoffset			2		int		14+c*20					00 00			OffsetY = 0
	 * xadvance			2		int		16+c*20					05 00			Advance = 5px
	 * page				1		uint	18+c*20					00				Page 0
	 * chnl				1		uint	19+c*20					0F				15 => All channels
	 * 
	 * These fields are repeated until all characters have been described. The number of characters in the file can be computed by 
	 * taking the size of the block and dividing with the size of the charInfo structure, i.e.: numChars = charsBlock.blockSize/20
	 */
	private final class BlockChars implements IBlockReader
	{
		@Override public boolean validateLength( int length )	{ return length > 0 && ( length % 20 ) == 0; }
		@Override
		public boolean readBlock( int length, sbInStream data ) throws IOException
		{
			if ( infoBlock == null || commonBlock == null || pages == null || chars != null )
			{
				sbMod.logger().severe( "Invalid state detected! (BlockChars)" );
				return false;
			}

			chars = new TreeMap<Character,CharData>();

			int remain = length/20;
			CharData charData;
			Character ch;
			while ( remain > 0 )
			{
				charData = new CharData();
				ch = charData.read( data );
				if ( ch == null || charData.page < 0 || charData.page >= commonBlock.pages )
				{
					if ( ch == null )
						sbMod.logger().severe( "Unable to read a char" );
					else
						sbMod.logger().severe( "Invalid page number detected! (BlockChars)" );
					break;
				}
				chars.put( ch, charData );
				--remain;
			}
			if ( remain != 0 )
				return false;

			return true;
		}
		
	}

	public final class CharData
	{
		public byte		page;
		public byte		channel;
		public float	u0;
		public float	v0;
		public float	u1;
		public float	v1;
		public float	w;
		public float	h;
		public float	d;
		public float	ofsX;
		public float	ofsY;

		public Character read( sbInStream data ) throws IOException
		{
			int chVal = readInt( data );
			if ( chVal < Character.MIN_VALUE || chVal > Character.MAX_VALUE )
			{
				sbMod.logger().severe( "Character value out of range" );
				return null;
			}

			int left	= readShort( data );
			int top		= readShort( data );
			int width	= readShort( data );
			int height	= readShort( data );
			int offsetX	= readShort( data );
			int offsetY	= readShort( data );
			int advance	= readShort( data );
			page		= data.readByte();
			channel		= data.readByte();

			u0		= left / commonBlock.scaleW;
			v0		= top  / commonBlock.scaleH;
			u1		= u0 + ( width  / commonBlock.scaleW );
			v1		= v0 + ( height / commonBlock.scaleH );
			w		= width   / sbGuiTheme.DOWNSCALE;
			h		= height  / sbGuiTheme.DOWNSCALE;
			d		= advance / sbGuiTheme.DOWNSCALE;
			ofsX	= offsetX / sbGuiTheme.DOWNSCALE;
			ofsY	= offsetY / sbGuiTheme.DOWNSCALE;

			return Character.valueOf( (char)( chVal & 0xFFFF ) );
		}
	}

	/*
	 * kerning (type=5) => Not implemented for now
	 * The kerning information is used to adjust the distance between certain characters, e.g. some characters should be placed 
	 * closer to each other than others:
	 * 
	 * first		The first character id.
	 * second		The second character id.
	 * amount		How much the x position should be adjusted when drawing the second character immediately following the first.
	 * 
	 * field			size	type	pos		comment
	 * first			4		uint	0+c*10	These fields are repeated until all kerning pairs have been described
	 * second			4		uint	4+c*10	
	 * amount			2		int		8+c*6	
	 * 
	 * This block is only in the file if there are any kerning pairs with amount differing from 0.
	 * /
	private final class BlockKernings implements IBlockReader
	{
		@Override public boolean validateLength( int length )	{ return length > 14; }

		@Override
		public boolean readBlock( sbBitmapFont parent, int length, sbInStream data ) throws IOException
		{
		}
	}
*/


	private static int readInt( sbInStream data ) throws IOException
	{
		int i0 = data.readByte();
		int i1 = data.readByte();
		int i2 = data.readByte();
		int i3 = data.readByte();
		int i = (i0&0xFF) | ((i1&0xFF)<<8) | ((i2&0xFF)<<16) | ((i3&0xFF)<<24);
		if ( ( i3 & 0x80 ) != 0 )
			i = -i;
		return i;
	}

	private static short readShort( sbInStream data ) throws IOException
	{
		int i0 = data.readByte();
		int i1 = data.readByte();
		int s = (i0&0xFF) | ((i1&0xFF)<<8);
		if ( ( i1 & 0x80 ) != 0 )
			s = -s;
		return (short)s;
	}

}
