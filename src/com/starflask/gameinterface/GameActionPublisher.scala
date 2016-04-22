package com.starflask.gameinterface

 
  
import com.starflask.util._; 

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.Serializable;


//http://jim-mcbeath.blogspot.com/2009/10/simple-publishsubscribe-example-in.html

//For subscribers of things that turn on and off
class GameActionPublisher extends Publisher[GameActionPublisher.CustomGameAction] 

// use "import AbledPublisher._" to pick up these definitions
object GameActionPublisher {

       
    
     sealed class CustomGameAction() {
       def CustomGameAction(){}
     }
     
     
    @Serializable case class NoAction(params: Map[String,Any]) extends CustomGameAction  {  
      def this() = this(Map())
      }
   
    @Serializable case class MoveAction(tid:Int, uid:Int, newPos:Vector3f, newFac: Vector3f ) extends CustomGameAction {  
      def this() = this(0,-1,new Vector3f(0,0,0),new Vector3f(0,0,0)) 
        
       var tickId = tid
       var unitId = uid
       var position = newPos
       var facing = newFac
            
      }
    
     @Serializable case class SpawnUnitAction(tid:Int, uid:Int, pid:Int, pos:Vector3f, fac:Vector3f ) extends CustomGameAction { 
      
       var tickId = tid
       var unitId = uid
       var position = pos
       var facing = fac
       var ownerId = pid
       
      }
     
     @Serializable case class UnitStatChangeAction(tid:Int, uid:Int, newStats: Map[String,Int] ) extends CustomGameAction { 
      
       var tickId = tid
       var unitId = uid
       var stats = newStats  //make held weapon a stat 
       
      }
   
    @Serializable case class FireAction( tid:Int, uid:Int, pid:Int, pos:Vector3f, fac:Vector3f  ) extends CustomGameAction { 
      
      var tickId = tid
       var unitId = uid
       var position = pos
       var facing = fac
       var ownerId = pid
       
      }
   
    @Serializable  case class JoinServerAction(name: String) extends CustomGameAction {  
        def this() = this( "New Player" )  
        
        var playerName = name
        
      }

       
     //Registration error: no-argument constructor not found on:class com.starflask.gameinterface.GameActionPublisher$JoinServerAct
     
     
     
    object CustomGameAction { 
        
       def apply(b:Boolean) = if (b) MoveAction else FireAction
       
       }
    
    
    
    
    
    
}

 