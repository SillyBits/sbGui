package sillybits.core.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import sillybits.core.sbMod;

/**
 * Commonly used stuff like global enums and rendering helpers which are too small for their own file.
 * 
 * @author SillyBits
 */
public class sbGui
{

	/*
	 * Mouse buttons
	 */

	public enum MouseButton
	{
		NONE(-1), LEFT(0), RIGHT(1), MIDDLE(2);
		
		MouseButton( int id ) { this.id = id; }

		private int id;
		public int id() { return id; }

		public static MouseButton get( int mcId )
		{
			for ( int i=0; i<values.length; ++i )
				if ( values[i].id() == mcId )
					return values[i];
			return NONE;
		}
		private final static MouseButton[] values = values();
	}


	/*
	 * Extended keyboard keys
	 */

	public enum ExtKeyboard
	{
		NONE(0), 
		LEFT_SHIFT(Keyboard.KEY_LSHIFT), RIGHT_SHIFT(Keyboard.KEY_RSHIFT),
		LEFT_CONTROL(Keyboard.KEY_LCONTROL), RIGHT_CONTROL(Keyboard.KEY_RCONTROL),
		LEFT_ALT(Keyboard.KEY_LMETA), RIGHT_ALT(Keyboard.KEY_RMETA),
		LEFT_MENU(Keyboard.KEY_LMENU), RIGHT_MENU(Keyboard.KEY_RMENU);
		
		ExtKeyboard( int id ) { this.id = id; }

		private int id;
		public int id() { return id; }

		public static int get( int mcId )
		{
			for ( int i=0; i<values.length; ++i )
				if ( values[i].id() == mcId )
					return 1<<i;
			return 0;
		}
		private final static ExtKeyboard[] values = values();
	}


	/*
	 * Clipboard handling
	 */
    public static String getClipboardContents()
    {
        try
        {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents( null );
            if ( transferable != null && transferable.isDataFlavorSupported( DataFlavor.stringFlavor ) )
                return (String) transferable.getTransferData( DataFlavor.stringFlavor );
        }
        catch ( Exception e ) { }
        return "";
    }

    public static void setClipboardContents( String str )
    {
        try
        {
            StringSelection selection = new StringSelection( str );
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents( selection, null );
        }
        catch ( Exception e ) { }
    }


	/*
	 * Rendering
	 */

	public static void drawRect( float left, float top, float right, float bottom, float z )
	{
		GL11.glBegin( GL11.GL_QUADS );
		injectRect( left, top, right, bottom, z );
		GL11.glEnd();
	}

	public static void drawTexturedRect( float left, float top, float right, float bottom, float z, float leftU, float topV, float rightU, float bottomV )
	{
		GL11.glBegin( GL11.GL_QUADS );
		injectTexturedRect( left, top, right, bottom, z, leftU, topV, rightU, bottomV );
		GL11.glEnd();
	}

//	public static void drawTexture( sbGuiTexture texture, float z )
//	{
//		final float w = texture.width  / sbGuiTheme.DOWNSCALE;
//		final float h = texture.height / sbGuiTheme.DOWNSCALE;
//		texture.select();
//		GL11.glBegin( GL11.GL_QUADS );
//		injectTexturedRect( 0, 0, w, h, z, 0, 0, 1, 1 );
//		GL11.glEnd();
//	}


	public static void injectRect( float left, float top, float right, float bottom, float z )
	{
		GL11.glVertex3f( left , bottom, z );
		GL11.glVertex3f( right, bottom, z );
		GL11.glVertex3f( right, top   , z );
		GL11.glVertex3f( left , top   , z );
	}

	public static void injectTexturedRect( float left, float top, float right, float bottom, float z, float leftU, float topV, float rightU, float bottomV )
	{
		GL11.glTexCoord2f( leftU , bottomV );	GL11.glVertex3f( left , bottom, z );
		GL11.glTexCoord2f( rightU, bottomV );	GL11.glVertex3f( right, bottom, z );
		GL11.glTexCoord2f( rightU, topV );		GL11.glVertex3f( right, top   , z );
		GL11.glTexCoord2f( leftU , topV );		GL11.glVertex3f( left , top   , z );
	}


	public static void setColor( int color )
	{
		final byte alpha = (byte)( ( color >> 24 ) & 0xFF );
		final byte red   = (byte)( ( color >> 16 ) & 0xFF );
		final byte green = (byte)( ( color >>  8 ) & 0xFF );
		final byte blue  = (byte)( ( color       ) & 0xFF );
		GL11.glColor4ub( red, green, blue, alpha );
	}

	public static int invertColor( int color )
	{ 
		return ( ~color & 0x00FFFFFF ) | ( color & 0xFF000000 ); 
	}

	public static int getShadowColor( int color )
	{ 
		return ( ( color & 0xFCFCFC ) >> 2 ) | ( color & 0xFF000000 ); 
	}


	/*
	 * File parser
	 */

	public static class FileReader
	{
		private String		fileName;
		private ILineParser	parser;

		public FileReader( String fileName, ILineParser parser )
		{
			this.fileName	= fileName;
			this.parser		= parser;
		}

		public boolean read()
		{
			try
			{
				InputStream inStream = sbGuiLayout.class.getResourceAsStream( fileName );
				if ( inStream != null )
				{
					BufferedReader reader = new BufferedReader( new InputStreamReader( inStream ) );
					if ( parser.begin( fileName ) )
					{
						while ( reader.ready() )
						{
							String line = reader.readLine();
							if ( canSkip( line ) )
								continue;
							if ( !parser.parseLine( line ) )
								if ( !parser.onError( line ) )
									return false;
						}
						if ( parser.end() )
							return true;
						sbMod.logger().log( "Error ending parsing file '" + fileName + "'" );
					}
					else
						sbMod.logger().log( "Error starting parsing file '" + fileName + "'" );
				}
				else
					sbMod.logger().log( "File '" + fileName + "' not found" );
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				sbMod.logger().severe( "... while reading file '" + fileName + "'" );
			}
			return false;
		}

		public boolean canSkip( String line )
		{
			return line.startsWith( "#" ) || line.isEmpty();
		}

	}

	public interface ILineParser
	{
		public boolean begin( String fileName );
		public boolean parseLine( String line );
		public boolean end();
		public boolean onError( String line );
	}

	public abstract static class SimpleFileParser implements ILineParser
	{
		protected String fileName;

		@Override
		public boolean begin( String fileName )
		{ 
			sbMod.logger().log( "Starting to parse file '" + fileName + "'" );
			this.fileName = fileName;
			return true; 
		}

		@Override
		public boolean end()
		{ 
			sbMod.logger().log( "Ended parsing file '" + fileName + "'" );
			return true; 
		}

		@Override
		public boolean onError( String line )
		{ 
			sbMod.logger().log( "Error parsing file '" + fileName + "', line '" + line + "'" );
			return false; 
		}

		public String[] split( String str, String splitter, int expectedAmount )
		{
			String[] array = str.split( splitter );
			if ( array.length > expectedAmount )//&& array[2].startsWith( "\"" ) )
			{
				// Glue them back together
				for ( int i=2; i<array.length; ++i )
					array[1] += splitter + array[i];
				array = new String[]{ array[0], array[1] };
			}
			return array;
		}
	}


	/*
	 * Type conversion
	 */

	public static Object convertValue( Class type, String value )
	{
		if ( type == String.class )
			return value;
		if ( type == Character.class )
			return Character.valueOf( (char)((int)Integer.valueOf( value )) );

		if ( type == byte.class || type == Byte.class )
			return value.toLowerCase().startsWith("0x") ? Byte.valueOf( value.substring(2), 16 ) : Byte.valueOf( value );
		if ( type == short.class || type == Short.class )
			return value.toLowerCase().startsWith("0x") ? Short.valueOf( value.substring(2), 16 ) : Short.valueOf( value );
		if ( type == int.class || type == Integer.class )
			return value.toLowerCase().startsWith("0x") ? Integer.valueOf( value.substring(2), 16 ) : Integer.valueOf( value );
		if ( type == long.class || type == Long.class )
			return value.toLowerCase().startsWith("0x") ? Long.valueOf( value.substring(2), 16 ) : Long.valueOf( value );

		if ( type == float.class || type == Float.class )
			return Float.valueOf( value );
		if ( type == double.class || type == Double.class )
			return Double.valueOf( value );

		if ( type == boolean.class || type == Boolean.class )
		{
			if ( value.length() == 1 )
				return Boolean.valueOf( Integer.valueOf( value )==1 );
			return Boolean.valueOf( value );
		}

		return null;
	}

}
