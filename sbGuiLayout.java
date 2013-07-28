package sillybits.core.gui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sillybits.core.sbMod;
import sillybits.core.util.sbUtil;
import sillybits.core.gui.sbGui.FileReader;
import sillybits.core.gui.sbGui.SimpleFileParser;

//TODO: sbGuiEdit, sbGuiCheckbox, sbGuiListbox, ....

/**
 * A layout container, used to define windows.
 * 
 * @author SillyBits
 */
public class sbGuiLayout
{
	private final static boolean							DEBUG_LOAD		= false;
	private final static boolean							DEBUG_CREATE	= true;

	public Map< Content, List< Map< String, String > > >	controls		= new HashMap();
	public Map< String, String >							properties		= new HashMap();


	public enum Content
	{
		CONTROL,
		LABEL   ( CONTROL, sbGuiLabel.class ), 
		BUTTON  ( CONTROL, sbGuiButton.class ), 
		LISTBOX ( CONTROL, sbGuiListbox.class),
		EDIT    ( CONTROL, sbGuiEdit.class ), 
//		CHECKBOX( CONTROL, sbGuiCheckbox.class ), 

		PROPERTY;

		private Content()							{ this.type = null; this.clazz = null; }
		private Content( Content type, Class clazz ){ this.type = type; this.clazz = clazz; }

		private Content	type;
		private Class	clazz;
		public Content	type()	{ return type; }
		public Class	clazz()	{ return clazz; }

		public static Content getType( String id )
		{
			for ( int i=0; i<content.length; ++i )
				if ( id.equals(content[i].name()) )
					return content[i];
			return null;
		}
		private final static Content[] content = values();
	};


	private sbGuiLayout() { }


	public static sbGuiLayout load( sbGuiTheme theme, String name )
	{
		final String layoutFile	= theme.getPath() + "layouts/" + name + ".lay";
		sbGuiLayout layout = new sbGuiLayout();

		FileReader reader = new sbGui.FileReader( layoutFile, new LayoutFileParser( layout ) );
		if ( !reader.read() )
			layout = null;

		return layout;
	}


	public boolean createChilds( sbGuiPane parent )
	{
		if ( DEBUG_CREATE )
			sbMod.logger().log( "Creating all childs for parent '"+parent.getName()+"'" );
		
		Content[] content = Content.values();
		for ( int i=0; i<content.length; ++i )
		{
			Content type = content[i].type();
			if ( type == null || type != Content.CONTROL )
			{
				if ( DEBUG_CREATE )
					sbMod.logger().log( "Skipping type '"+content[i].name()+"'" );
				continue;
			}
			if ( DEBUG_CREATE )
				sbMod.logger().log( "Processing type '"+content[i].name()+"'" );

			List< Map< String, String > > childs = controls.get( content[i] );
			if ( childs == null || childs.isEmpty() )
			{
				if ( DEBUG_CREATE )
					sbMod.logger().log( "No entries, skipping type '"+content[i].name()+"'" );
				continue;
			}
			if ( DEBUG_CREATE )
				sbMod.logger().log( "Creating "+childs.size()+" childs of type '"+content[i].name()+"'" );

			try
			{
				Method loadFromLayoutMethod = content[i].clazz().getDeclaredMethod( "loadFromLayout", sbGuiPane.class, Map.class );
				if ( loadFromLayoutMethod == null )
				{
					sbMod.logger().log( "Creating accessor for type '"+content[i].name()+"' failed!" );
					return false;
				}
				loadFromLayoutMethod.setAccessible( true );

				for ( Map< String, String > childData : childs )
				{
					String childLabel = childData.get("name");
					if ( DEBUG_CREATE )
						sbMod.logger().log( "Creating '"+childLabel+"'" );

					sbGuiElement child = (sbGuiElement)loadFromLayoutMethod.invoke( null, parent, childData );
					if ( child != null )
						parent.addChild( child );
					else
					{
						sbMod.logger().log( "Creating '"+childLabel+"' failed!" );
						return false;
					}
				}
			}
			catch ( Exception e )
			{
				if ( DEBUG_CREATE )
				{
					e.printStackTrace();
					sbMod.logger().log( "... while processing type '"+content[i].name()+"' failed!" );
				}
				else
					sbMod.logger().log( "Error creating instance of type '"+content[i].name()+"'!" );
				return false;
			}
		}

		if ( DEBUG_CREATE )
			sbMod.logger().log( "Done creating all childs for parent '"+parent.getName()+"'" );
		return true;
	}


	private final static class LayoutFileParser extends SimpleFileParser
	{
		private sbGuiLayout layout;

		public LayoutFileParser( sbGuiLayout layout )
		{
			this.layout = layout;
		}

		@Override
		public boolean parseLine( String line )
		{
			String[] strArray = split( line, ":", 2 );
			if ( DEBUG_LOAD )
				sbMod.logger().log( "Parsing '"+line+"' resulted in: "+sbUtil.array2String( strArray ) );
			if ( strArray != null && strArray.length == 2 )
			{
				Content type = Content.getType( strArray[0].toUpperCase() );
				if ( type != null && type.type() == Content.CONTROL )
					return parseControl( type, strArray[1] );
				if ( type == Content.PROPERTY )
					return parseProperty( strArray[1] );
				sbMod.logger().log( "Layout file contains an invalid entry '" + line + "', no match on id. "+sbUtil.array2String( strArray ) );
			}
			else
				sbMod.logger().log( "Layout file contains an invalid entry '" + line + "', not enough parameters. "+sbUtil.array2String( strArray ) );
			return false;
		}

		private boolean parseControl( Content type, String param )
		{
			String[] paramsArray = param.split( ";" );
			Map< String, String > params = new HashMap();
			String[] array;
			for ( int i=0; i<paramsArray.length; ++i )
			{
				array = paramsArray[i].split( "\\=" );
				if ( array[1].startsWith( "\"" ) && array[1].endsWith( "\"" ) )
					array[1] = array[1].substring( 1, array[1].length()-1 );
				params.put( array[0], array[1] );
			}
			List list = layout.controls.get( type );
			if ( list == null )
			{
				list = new ArrayList< Map >();
				layout.controls.put( type, list );
			}
			list.add( params );
			return true;
		}

		private boolean parseProperty( String param )
		{
			String[] array = param.split( "\\=" );
			if ( array[1].startsWith( "\"" ) && array[1].endsWith( "\"" ) )
				array[1] = array[1].substring( 1, array[1].length()-2 );
			layout.properties.put( array[0], array[1] );
			return true;
		}

	}


	public static boolean loadLayout( sbGuiElement element, Map< String, String > params )
	{
		try
		{
			return loadLayoutRecurs( element, element.getClass(), params );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			sbMod.logger().severe( "... while loading layout parameters" );
			return false;
		}
	}

	private static boolean loadLayoutRecurs( sbGuiElement element, Class clazz, Map< String, String > params ) throws Exception
	{
		if ( DEBUG_LOAD )
			sbMod.logger().log( "Checking class '"+clazz.getName()+"'" );

		Field[] fields = clazz.getDeclaredFields();
		for ( int i=0; i<fields.length; ++i )
		{
			Field field = fields[i];
			if ( DEBUG_LOAD )
				sbMod.logger().log( "Checking field '"+field.getName()+"'" );
			Layout layout = field.getAnnotation( Layout.class );
			if ( layout != null )
			{
				String id    = layout.value();
				String value = params.get( id );
				if ( value != null )
				{
					Object convertedValue = sbGui.convertValue( field.getType(), value );
					if ( convertedValue != null )
					{
						if ( DEBUG_LOAD )
							sbMod.logger().log( "Setting field '"+field.getName()+"' with id '"+id+"' to value '"+convertedValue+"'" );
						if ( !field.isAccessible() )
							field.setAccessible( true );
						try
						{ 
							field.set( element, convertedValue ); 
							element.onParameterSet( id );
						}
						catch ( IllegalArgumentException e )
						{
							// Ok, so simple set failed, try a more complex setup
							if ( !element.loadParameter( id, value ) )
							{
								sbMod.logger().severe( "Unable to set field '"+field.getName()+"' with id '"+id+"' to '"+value );
								continue;
							}
						}
					}
					else
					{
						// Ok, can't use simple set here, try a more complex setup
						if ( !element.loadParameter( id, value ) )
						{
							sbMod.logger().severe( "Unable to set field '"+field.getName()+"' with id '"+id+"' to '"+value );
							continue;
						}
					}
					// Eat this processed parameter
					params.remove( id );
					// Early exit if all params processed
					if ( params.isEmpty() )
						break;
				}
				else if ( DEBUG_LOAD )
					sbMod.logger().log( "No value specified for field '"+id+"', skipping" );
			}// if layout
		}// for fields
	
		// Only ok if all params were eaten up
		if ( !params.isEmpty() )
		{
			if ( DEBUG_LOAD )
				sbMod.logger().log( "Still a "+params.size()+" elements left while loading layout" );
			Class superclazz = clazz.getSuperclass();
			if ( superclazz != null )
			{
				if ( DEBUG_LOAD )
					sbMod.logger().log( "Will try superclass '"+superclazz.getName()+"' now" );
				if ( !loadLayoutRecurs( element, superclazz, params ) )
					return false;
			}
		}
		return params.isEmpty();
	}


	@Retention( RetentionPolicy.RUNTIME )
	@Target(ElementType.FIELD)
	public @interface Layout
	{
//TODO: We could make the "id" optional, defaulting to field name itself if no id given
		String value();
//TODO: Generate an error if a required parameter is missing its setup
//		boolean required() default false;
	}

}
