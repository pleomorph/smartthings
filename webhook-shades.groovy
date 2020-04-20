/**
 *  Copyright 2018, 2019 SmartThings
 *
 *  Provides a simulated window shade.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.json.JsonOutput

metadata {
	definition (name: "Webhook Window Shade", namespace: "novagl", author: "NovaGL", runLocally: false, mnmn: "SmartThings", vid: "generic-window-shade") {
		capability "Actuator"
		capability "Window Shade"				

		// Commands to use in the simulator		
		command "opened"
		command "closed"		
	}

	preferences {
		section("External Access") {
  			input "webhook_url", "text", title: "Webhook URL", required: true
 		}
		section {
			input("actionDelay", "number",
				title: "Action Delay\n\nAn emulation for how long it takes the window shade to perform the requested action.",
				description: "In seconds (1-120; default if empty: 5 sec)",
				range: "1..120", displayDuringSetup: false)
		}
		section {
			input("supportedCommands", "enum",
				title: "Supported Commands\n\nThis controls the value for supportedWindowShadeCommands.",
				description: "open, close, pause", defaultValue: "2", multiple: false,
				options: [
					"1": "open, close",
					"2": "open, close, pause",
					"3": "open",
					"4": "close",
					"5": "pause",
					"6": "open, pause",
					"7": "close, pause",
					"8": "<empty list>",
					// For testing OCF/mobile client bugs
					"9": "open, closed, pause",
					"10": "open, closed, close, pause"
				]
			)
		}
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"windowShade", type: "generic", width: 6, height: 4){
			tileAttribute ("device.windowShade", key: "PRIMARY_CONTROL") {
				attributeState "open", label:'${name}', action:"close", icon:"st.shades.shade-open", backgroundColor:"#79b821", nextState:"closing"
				attributeState "closed", label:'${name}', action:"open", icon:"st.shades.shade-closed", backgroundColor:"#ffffff", nextState:"opening"
				attributeState "partially open", label:'Open', action:"close", icon:"st.shades.shade-open", backgroundColor:"#79b821", nextState:"closing"
				attributeState "opening", label:'${name}', action:"pause", icon:"st.shades.shade-opening", backgroundColor:"#79b821", nextState:"partially open"
				attributeState "closing", label:'${name}', action:"pause", icon:"st.shades.shade-closing", backgroundColor:"#ffffff", nextState:"partially open"
				attributeState "unknown", label:'${name}', action:"open", icon:"st.shades.shade-closing", backgroundColor:"#ffffff", nextState:"opening"
			}
			/*tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"setLevel"
			}*/
		}

		valueTile("blank", "device.blank", width: 2, height: 2, decoration: "flat") {
			state "default", label: ""	
		}
		valueTile("commandsLabel", "device.commands", width: 6, height: 1, decoration: "flat") {
			state "default", label: "Commands:"	
		}

		standardTile("windowShadeOpen", "device.windowShade", width: 2, height: 2, decoration: "flat") {
			state "default", label: "open", action:"open", icon:"st.Home.home2"
		}
		standardTile("windowShadeClose", "device.windowShade", width: 2, height: 2, decoration: "flat") {
			state "default", label: "close", action:"close", icon:"st.Home.home2"
		}
		standardTile("windowShadePause", "device.windowShade", width: 2, height: 2, decoration: "flat") {
			state "default", label: "pause", action:"pause", icon:"st.Home.home2"
		}
		valueTile("statesLabel", "device.states", width: 6, height: 1, decoration: "flat") {
			state "default", label: "State Events:"	
		}
	main(["windowShade"])
		details(["windowShade",
				 "commandsLabel",
				 "windowShadeOpen", "windowShadeClose", "windowShadePause", "windowShadePreset", "blank", "blank",
				 "statesLabel",
				 "windowShadePartiallyOpen", "windowShadeOpening", "windowShadeClosing", "windowShadeOpened", "windowShadeClosed", "windowShadeUnknown"])

	}
}

private getSupportedCommandsMap() {
	[
		"1": ["open", "close"],
		"2": ["open", "close", "pause"],
		"3": ["open"],
		"4": ["close"],
		"5": ["pause"],
		"6": ["open", "pause"],
		"7": ["close", "pause"],
		"8": [],
		// For testing OCF/mobile client bugs
		"9": ["open", "closed", "pause"],
		"10": ["open", "closed", "close", "pause"]
	]
}

private getShadeActionDelay() {
	(settings.actionDelay != null) ? settings.actionDelay : 5
}

def installed() {
	log.debug "installed()"

	updated()
	opened()
}

def updated() {
	log.debug "updated()"

	def commands = (settings.supportedCommands != null) ? settings.supportedCommands : "2"

	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(supportedCommandsMap[commands]))
}

def parse(String description) {
	log.debug "parse(): $description"
}

// Capability commands

def open() {	
    //sendEvent(name: "windowShade", value: "opening", isStateChange: true)
	runIn(shadeActionDelay, "opened")
    runCmd("open", "window")
}

def close() {
    //sendEvent(name: "windowShade", value: "closing", isStateChange: true)    
	runIn(shadeActionDelay, "closed")
    runCmd("close", "window")
}

def pause() {	
	runCmd("pause", "window")
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
                    switch(type) {            
					 case "open": 
						log.debug "windowShade: open"
						sendEvent(name: "windowShade", value: "open")                        
						break; 
					 case "close": 
							log.debug "windowShade: closed"
							sendEvent(name: "windowShade", value: "closed")
						break; 
					 case "pause": 
						log.debug "windowShade: partially open"
						sendEvent(name: "windowShade", value: "partially open")
						break; 					 
				  }
     			}
   		}
  	} catch (e) {
   		log.debug "something went wrong: $e"
  	}
}
