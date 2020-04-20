/*
 *
 * Webhook Dimmer Device Handler
 */


preferences {
  input "webhook_url", "text", title: "Webhook URL", required: false
}

metadata {
 definition(name: "Webhook Dimmer", namespace: "novagl", author: "NovaGL") {
  capability "Actuator"
  capability "Switch"
  capability "Switch Level"
  capability "Sensor"
  capability "Refresh"
  capability "Execute"
 }

 // simulator metadata
 simulator {}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
    			attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
		      	attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
		      	attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState: "turningOff"
        	}
        		tileAttribute("device.level", key: "SLIDER_CONTROL") {
            		attributeState "level", action:"switch level.setLevel"
        		}
        		tileAttribute("level", key: "SECONDARY_CONTROL") {
              		attributeState "level", label: 'Light dimmed to ${currentValue}%'
        		}    
		}
      valueTile("lValue", "device.level", inactiveLabel: true, height:2, width:2, decoration: "flat") {  
			state "levelValue", label:'${currentValue}%', unit:"", backgroundColor: "#53a7c0"  
        }  
	}
}


def parse(String description) {
 //log.debug(description)
 log.info "description is $description"
 log.info ("hello")
}

def on() {	    
    sendEvent(name: "triggerswitch", value: "triggeron", isStateChange: true)
	runCmd("on", "switch")
}

def off() {		
    sendEvent(name: "triggerswitch", value: "triggeroff", isStateChange: true)
    runCmd("off", "switch")
}

def setLevel(val){
        
    // make sure we don't drive switches past allowed values (command will hang device waiting for it to
    // execute. Never commes back)    
    
    def currentstate = device.currentState("switch").getValue()
    //log.info device.currentState("level").getValue()
    
    if( val > 100){    	
        sendEvent(name:"level",value:100, isStateChange: true)
        on ()
    }else if (val == 0 || val < 0){ 
    	sendEvent(name:"level",value:0, isStateChange: true)
    	off()
    }
    else
    {
    	//Switch light status
    	if (currentstate == "off") {
        	log.info "Turning Light on"
        	sendEvent(name: "switch", value: "on", isStateChange: true)
        }
        val = (int)val
    	runCmd(val.toString(),"level")
    	sendEvent(name:"level",value:val, isStateChange: true)    	
    }
}

def runCmd(String power, String type) {
   log.info "Set $power"      	
	def params = [
   			uri: "${webhook_url}",
   			body: [type: type, value: power, device: device.name]
  		]    
  	try {
   		httpPostJson(params) {
    		resp ->
     			if (resp.data) {
      				log.info "${resp.data}"
      				if (type == "switch") {
                    	sendEvent(name: "switch", value: power)
                    } else {
                    	sendEvent(name:"setLevel",value:power)
                    }
     			}
   		}
  	} catch (e) {
   		log.debug "something went wrong: $e"
  	}
}
