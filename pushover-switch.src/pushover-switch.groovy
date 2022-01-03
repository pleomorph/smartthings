/*
 *
 * Pushover Switch Device Handler
 */

metadata {
    definition(name: "Pushover Switch", namespace: "pleomorph", author: "Pleomorph") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
        capability "Contact Sensor"
    }

    preferences {
        section("External Access") {
            //   input "webhook_url", "text", title: "Webhook URL", required: false
            input "webhook_userkey", "text", title: "Pushover User Key", required: false
            input "webhook_apptoken", "text", title: "Pushover Application Token", required: false
            input "webhook_message", "text", title: "Custom Message", required: false
        }
    }



    // simulator metadata
    simulator {
        status "open": "contact:open"
        status "closed": "contact:closed"
    }

    // UI tile definitions
    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        }
        main "switch"
        details(["switch"])
    }
    

}

def parse(String description) {
 log.debug(description)
}

def on() {	
    sendEvent(name: "triggerswitch", value: "triggeron", isStateChange: true)
	runCmd("on", "switch")
    off()
}

def off() {	
    sendEvent(name: "triggerswitch", value: "triggeroff", isStateChange: true)
    //runCmd("off", "switch")
}

def runCmd(String power, String type) {
	def params = [
   		uri: "https://api.pushover.net/1/messages.json",
   		body: [
            token: webhook_apptoken,
            user: webhook_userkey,
            title: "SmartThings Alert",
            message: webhook_message, 
        ]
  	]
  	try {
   		httpPostJson(params) {
    		resp ->
     			if (resp.data) {
      				log.info "${resp.data}"
      				sendEvent(name: "switch", value: power)
     			}
   		}
  	} catch (e) {
   		log.debug "something went wrong: $e"
  	}
}
