package sillybits.core.gui;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import sillybits.core.gui.sbGui.MouseButton;

/**
 * A pane is a container which is able to hold some children. 
 * All rendering, mouse and keyboard events are just routed thru.
 * 
 * @author SillyBits
 */
public class sbGuiPane extends sbGuiElement
{
	private List< sbGuiElement > childs;


	public sbGuiPane( sbGuiPane parent, String name, float posX, float posY, float posZ, float width, float height )
	{
		super( parent, name, posX, posY, posZ, width, height );
	}


	public void addChild( sbGuiElement child )
	{
		if ( childs == null )
			childs = new ArrayList();
		childs.add( child );
		child.posZ = posZ+0.0001f;
	}

	public void removeChild( sbGuiElement child )
	{
		if ( childs != null )
			childs.remove( child );
	}

	public sbGuiElement getChild( String name )
	{
		if ( childs != null )
			for ( sbGuiElement child : childs )
				if ( name.equals( child.name ) )
					return child;
		return null;
	}


	public void setText( String name, String text )
	{
		if ( childs != null )
			for ( sbGuiElement child : childs )
				if ( child.name.equals( name ) )
				{
					if ( child instanceof sbGuiLabel )
						((sbGuiLabel)child).setText( text );
					else if ( child instanceof sbGuiButton )
						((sbGuiButton)child).setText( text );
					else if ( child instanceof sbGuiEdit )
						((sbGuiEdit)child).setText( text );
				}
	}


	@Override
	protected void onThemeChanged()
	{
		super.onThemeChanged();
		if ( childs != null )
			for ( sbGuiElement child : childs )
				child.onThemeChanged();
	}


	@Override
	public void render()
	{
		if ( childs != null )
		{
			GL11.glTranslatef( posX, posY, posZ+0.0001f );
			for ( sbGuiElement child : childs )
				child.render();
			GL11.glTranslatef( -posX, -posY, -(posZ+0.0001f) );
		}
	}


	@Override protected String	getTooltip()	{ return null; }
	@Override protected void	checkTooltip()	{ }


	@Override
	public void onMouse( float mouseX, float mouseY, MouseButton button, boolean down )
	{
		if ( childs != null )
		{
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				child.onMouse( mouseX, mouseY, button, down );
		}
	}

	@Override
	public void onMouseWheel( float mouseX, float mouseY, int clicks )
	{
		if ( childs != null )
		{
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				child.onMouseWheel( mouseX, mouseY, clicks );
		}
	}

	@Override
	public void onMouseDown( float mouseX, float mouseY, MouseButton button )
	{
		if ( childs != null && button != MouseButton.NONE )
		{
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				child.onMouseDown( mouseX, mouseY, button );
		}
	}

	@Override
	public void onMouseUp( float mouseX, float mouseY, MouseButton button )
	{
		if ( childs != null && button != MouseButton.NONE )
		{
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				child.onMouseUp( mouseX, mouseY, button );
		}
	}

	@Override
	protected boolean canGainFocus( float mouseX, float mouseY )
	{
		if ( childs != null )
		{
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				if ( child.canGainFocus( mouseX, mouseY ) )
					return true;
		}
		return false;
	}

	@Override
	protected void gainFocus( float mouseX, float mouseY )
	{
		if ( childs != null )
		{
			looseFocus( mouseX, mouseY );
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				if ( child.canGainFocus( mouseX, mouseY ) )
					child.gainFocus( mouseX, mouseY );
		}
	}

	@Override
	protected void looseFocus( float mouseX, float mouseY )
	{
		if ( childs != null && focusedElement != null )
		{
			mouseX -= posX;
			mouseY -= posY;
			for ( sbGuiElement child : childs )
				child.looseFocus( mouseX, mouseY );
		}
	}


	@Override
	public void onKey( char key, int extended, boolean down )
	{
		if ( childs != null )
			for ( sbGuiElement child : childs )
				child.onKey( key, extended, down );
	}

	@Override
	public void onKeyDown( char key, int extended )
	{
		if ( childs != null )
			for ( sbGuiElement child : childs )
				child.onKeyDown( key, extended );
	}

	@Override
	public void onKeyUp( char key, int extended )
	{
		if ( childs != null )
			for ( sbGuiElement child : childs )
				child.onKeyUp( key, extended );
	}


	@Override
	public boolean loadParameter( String id, String value )
	{
		return false;
	}

}
