package com.starflask

import com.starflask.events.CoreEventQueue
import com.starflask.events.CoreEventPublisher.CoreEvent
import com.starflask.events.CoreEventPublisher._
import com.starflask.states.HeadlessGameState
import com.starflask.states._


object UltraBlackBloodDeath {
  
  def main(args: Array[String]): Unit = {
    var app = new UltraBlackBloodDeath();
    		
		//AppSettings settings = new AppSettings(false);
		//settings.setAudioRenderer(AppSettings.JOAL);
		//app.setSettings(settings);  
		
	 //app.start( JmeContext.Type.Headless );
    app.start();
  }
}

class UltraBlackBloodDeath extends MonkeyApplication {
  
  
 // static WindowBuilder windowBuilder;
	
 
	 
	 
	 var coreEventQueue = new CoreEventQueue();

	override def simpleInitApp() {
		 
	        
	     //   this.getStateManager().attach( new  GameState() ) ;
	       var terminalState = this.getStateManager.getState(classOf[TerminalState]); 
	     terminalState.terminalMenu.coreEventPublisher.subscribe { ev => responseToCoreEvent(ev) }
	}
	
	override def simpleUpdate(tpf: Float) {
	  coreEventQueue.feedEvents( (ev) => responseToCoreEvent(ev) )
	}
	
	
	  def responseToCoreEvent(event: CoreEvent) = event match {
	  case h: HostServerEvent => hostGame(h); 
	  case g: JoinServerEvent => joinGame(g); 
	  
	  case _ => println("invalid event")
	    
	}
	
	
	def hostGame(ev: HostServerEvent)
	{
	   this.getStateManager().attach( new  HeadlessGameState() ) ;
	   //collapse the terminal
	   
	}
	
	def joinGame(ev: JoinServerEvent)
	{
	 println("joining");
	   this.getStateManager().attach( new  GameState() ) ;
	}
	
}