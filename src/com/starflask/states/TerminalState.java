package com.starflask.states;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.starflask.MonkeyApplication;
import com.starflask.peripherals.InputActionComponent;
import com.starflask.peripherals.InputActionExecutor;
import com.starflask.peripherals.InputActionType;
import com.starflask.renderable.GuiNodeComponent;
import com.starflask.terminal.*;
import com.starflask.util.EntityAppState; 

public class TerminalState extends EntityAppState implements InputActionExecutor{

	
	TerminalRenderer terminalRenderer;
	TerminalConsoleInterface consoleInterface;
	
	
	TerminalMenu terminalMenu;
	
	
	 @Override
	    public void initialize(AppStateManager stateManager, Application app) {
	      super.initialize(stateManager, app); 
	       
	      this.addComponent(new GuiNodeComponent() );
	      this.addComponent(new InputActionComponent( this ));
	      this.getComponent(InputActionComponent.class).getRawStringInput().setActive(true);
	      
	      consoleInterface = new TerminalConsoleInterface();
	      consoleInterface.start();
	      
	      terminalRenderer = new TerminalRenderer( getAssetLibrary() );
	      this.getComponent(GuiNodeComponent.class).attachChild( terminalRenderer.getComponent( GuiNodeComponent.class  )  );
	      
	      getRootGUINode().attachChild( this.getComponent(GuiNodeComponent.class) );
	      
	      terminalRenderer.build();
	   }
	 
	 
	 @Override
		public void stateAttached(AppStateManager stateManager) {
		 
						
		}
		 
		 @Override
		public void stateDetached(AppStateManager stateManager) {
				
		}
	 


	 	@Override
	    public void setEnabled(boolean enabled) {
	      // Pause and unpause
	      super.setEnabled(enabled);
	      if(enabled){
	        // init stuff that is in use while this state is RUNNING
	    	  this.getComponent(GuiNodeComponent.class).attachChild( terminalRenderer.getComponent( GuiNodeComponent.class  )  );
	 	     
	         
	      } else {
	        // take away everything not needed while this state is PAUSED
	    	  this.getComponent(GuiNodeComponent.class).detachChild( terminalRenderer.getComponent( GuiNodeComponent.class  )  );
	 	     
	         
	      }
	    }
	 
	 
	 
	@Override
	public void update(float tpf)
	{
		super.update(tpf);
		
		
		
	}


	public void log(String string) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void executeInputAction(InputActionType inputAction, boolean pressed) {
		// Register actions here..
		
		
		
	}





	public void toggle() {
		 setEnabled(!this.isEnabled());
		 
		 System.out.println( "toggling terminal statee " + this.isEnabled());
		 System.out.println( "meep " + this.isEnabled());
		
	}


	
}
