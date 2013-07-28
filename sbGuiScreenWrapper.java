package sillybits.core.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import sillybits.core.sbMod;
import sillybits.core.gui.sbGui.MouseButton;

public abstract class sbGuiScreenWrapper extends GuiScreen
{
	protected Container		container;
	protected String		windowName;
	protected String		themeName;
	protected boolean		themePreload;
	protected sbGuiTheme	theme;
	protected sbGuiWindow	window;
	protected boolean		initialized = false;


	public sbGuiScreenWrapper( Container container, String windowName, String themeName, boolean themePreload )
	{
		this.container		= container;
		this.windowName		= windowName;
		this.themeName		= themeName;
		this.themePreload	= themePreload;
	}

	public sbGuiScreenWrapper( Container container, String windowName, String themeName )
	{
		this( container, windowName, themeName, false );
	}

	public sbGuiScreenWrapper( Container container, String windowName )
	{
		this( container, windowName, null, false );
	}


	@Override
	public void initGui()
	{
		if ( theme == null && themeName != null )
		{
			theme = new sbGuiTheme( themeName, themePreload );
			if ( theme == null )
			{
				sbMod.logger().severe( "Unable to load theme!" );
				return;
			}
		}

		window = new Window( width*2.f, height*2.f );

		initialized = true;

		super.initGui();
	}

	@Override
	public void drawScreen( int par1, int par2, float par3 )
	{
		if ( !initialized )
			return;

		window.render();

		super.drawScreen( par1, par2, par3 );
	}


	@Override
	public void handleMouseInput()
	{
		int x = Mouse.getEventX() * this.width  / this.mc.displayWidth;
		int y = Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
		y = height - y;

		if ( window != null && Mouse.getEventButton() == -1 )
			window.onMouse( x, y, MouseButton.NONE, false );

		int wheelEvent = Mouse.getEventDWheel();
		if ( wheelEvent != 0 )
			window.onMouseWheel( x, y, wheelEvent );
				
		super.handleMouseInput();
	}

	@Override
	protected void mouseClicked( int par1, int par2, int par3 )
	{
		if ( window != null && par3 != -1 )
		{
			MouseButton button = MouseButton.get( par3 );
			window.onMouseDown( par1, par2, button );
		}
		super.mouseClicked( par1, par2, par3 );
	}

	@Override
	protected void mouseMovedOrUp( int par1, int par2, int par3 )
	{
		if ( window != null && par3 != -1 )
		{
			MouseButton button = MouseButton.get( par3 );
			window.onMouseUp( par1, par2, button );
		}
		super.mouseMovedOrUp( par1, par2, par3 );
	}


	@Override
    public void handleKeyboardInput()
    {
		int ext = Keyboard.getEventKey();
        if ( ext != Keyboard.KEY_NONE )
		{
			char ch = Keyboard.getEventCharacter();
			if ( Keyboard.getEventKeyState() )
				window.onKeyDown( ch, ext );
			else
				window.onKeyUp( ch, ext );
		}

		super.handleKeyboardInput();
	}

	@Override
	protected void keyTyped( char ch, int par2 )
	{
		if ( window != null )
			window.onKey( ch, par2, Keyboard.getEventKeyState() );
		super.keyTyped( ch, par2 );
	}
	

	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}

//	@Override
//	protected void actionPerformed( GuiButton button )
//	{
//		super.actionPerformed( button );
//	}


	protected abstract void updateUI( sbGuiElement element );

	
	public class Window extends sbGuiWindow
	{
		public Window( float x, float y )
		{
			super( x, y, 0, 0, zLevel, theme, windowName );
		}

		@Override
		protected void onClose()
		{
            mc.displayGuiScreen( null );
            mc.setIngameFocus();
		}

		@Override
		protected void setupLayout()
		{
			super.setupLayout();
			updateUI( null );
		}

		@Override
		protected void onSelectionChanged( sbGuiListbox listbox, int oldSelection, int newSelection )
		{
			updateUI( listbox );
		}

		@Override
		protected void onTextChanged( sbGuiElement child, String newText )
		{
			updateUI( child );
		}

	}

}
