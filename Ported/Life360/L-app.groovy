/**
 *  ---- Original Header ----
 *
 *  Life360 with States - Hubitat Port
 *
 *	BTRIAL DISTANCE AND SLEEP PATCH 29-12-2017
 *	Updated Code to handle distance from, and sleep functionality
 *
 *	TMLEAFS REFRESH PATCH 06-12-2016 V1.1
 *	Updated Code to match Smartthings updates 12-05-2017 V1.2
 *	Added updateMember function that pulls all usefull information Life360 provides for webCoRE use V2.0
 *
 *  Copyright 2014 Jeff's Account
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
 * ---- End of original header ----
 *
 * ---- New Header ----
 *
 *  ****************  L360 with States App  ****************
 *
 *  Design Usage:
 *  Life360 with all States included
 *
 *  Copyright 2019-2020 Bryan Turcotte (@bptworld)
 *
 *  This App is free.  If you like and use this app, please be sure to mention it on the Hubitat forums!  Thanks.
 *
 *  Remember...I am not a programmer, everything I do takes a lot of time and research!
 *  Donations are never necessary but always appreciated.  Donations to support development efforts are accepted via:
 *
 *  Paypal at: https://paypal.me/bptworld
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Special thanks goes out to @cwwilson08 for working on and figuring out the oauth stuff!
 *  This would not be possible without his work.
 *
 *  Changes:
 *
 *  2.0.9 - 10/07/20 - Attempting fix for jumping GPS
 *  2.0.8 - 09/26/20 - Testing Fix by @jpoeppelman1
 *  2.0.7 - 08/25/20 - Added more error catching
 *  2.0.6 - 06/01/20 - Added code to remove devices if app is uninstalled
 *  2.0.5 - 04/27/20 - Cosmetic changes
 *  2.0.4 - 04/15/20 - Code adjustments, container driver no longer used. New devices need to be created.
 *  2.0.3 - 04/01/20 - Added a timeout to get http commands
 *  2.0.2 - 01/21/20 - Adjusted app to work with new driver
 *  2.0.1 - 01/03/20 - Adjusted logging to not show sensitive data
 *  ---
 *  v1.0.0 - 06/30/19 - Initial port of ST app (cwwilson08) (bptworld)
 */

import java.text.SimpleDateFormat

def setVersion(){
    state.name = "Life360 with States"
	state.version = "2.0.9"
}

definition(
    name: "Life360 with States",
    namespace: "BPTWorld",
    author: "Bryan Turcotte",
    description: "Life360 with all States Included",
	category: "",
    iconUrl: "",
    iconX2Url: "",
    oauth: [displayName: "Life360", displayLink: "Life360"],
    singleInstance: true,
    importUrl: "https://raw.githubusercontent.com/bptworld/Hubitat/master/Ported/Life360/L-app.groovy",
) {
	appSetting "clientId"
	appSetting "clientSecret"
}

preferences {
    page(name: "Credentials", title: "Enter Life360 Credentials", content: "getCredentialsPage", nextPage: "testLife360Connection", install: false)
    page(name: "listCirclesPage", title: "Select Life360 Circle", content: "listCircles", install: false)
    page(name: "myPlaces", title: "My Places", content: "myPlaces", install: true)
}

mappings {
	path("/placecallback") {
		action: [
              POST: "placeEventHandler",
              GET: "placeEventHandler"
		]
	}

    path("/receiveToken") {
		action: [
            POST: "receiveToken",
            GET: "receiveToken"
		]
	}
}

def getCredentialsPage() {
    if(logEnable) log.debug "In getCredentialsPage - (${state.version})"
    if(state.life360AccessToken) {
        listCircles()
    } else {
        dynamicPage(name: "Credentials", title: "Enter Life360 Credentials", nextPage: "listCirclesPage", uninstall: true, install:false){
            section(getFormat("header-green", "${getImage("Blank")}"+" Life360 Credentials")) {
    		    input "username", "text", title: "Life360 Username?", multiple: false, required: true
    		    input "password", "password", title: "Life360 Password?", multiple: false, required: true, autoCorrect: false
    	    }
        }
    }
}

def getCredentialsErrorPage(String message) {
    if(logEnable) log.debug "In getCredentialsErrorPage - (${state.version})"
    dynamicPage(name: "Credentials", title: "Enter Life360 Credentials", nextPage: "listCirclesPage", uninstall: uninstallOption, install:false) {
    	section(getFormat("header-green", "${getImage("Blank")}"+" Life360 Credentials")) {
    		input "username", "text", title: "Life360 Username?", multiple: false, required: true
    		input "password", "password", title: "Life360 Password?", multiple: false, required: true, autoCorrect: false
            paragraph "${message}"
    	}
    }
}

def testLife360Connection() {
    if(logEnable) log.debug "In testLife360Connection - (${state.version})"
    if(state.life360AccessToken) {
        if(logEnable) log.debug "In testLife360Connection - Good!"
        true
    } else {
        if(logEnable) log.debug "In testLife360Connection - Bad!"
    	initializeLife360Connection()
    }
}

 def initializeLife360Connection() {
    if(logEnable) log.debug "In initializeLife360Connection - (${state.version})"

    initialize()

    def username = settings.username
    def password = settings.password

    def url = "https://api.life360.com/v3/oauth2/token.json"

    def postBody =  "grant_type=password&" +
    				"username=${username}&"+
                    "password=${password}"

    def result = null

    try {

     		httpPost(uri: url, body: postBody, headers: ["Authorization": "Basic cFJFcXVnYWJSZXRyZTRFc3RldGhlcnVmcmVQdW1hbUV4dWNyRUh1YzptM2ZydXBSZXRSZXN3ZXJFQ2hBUHJFOTZxYWtFZHI0Vg==" ]) {response ->
     		    result = response
    		}
        if (result.data.access_token) {
       		state.life360AccessToken = result.data.access_token
            return true;
       	}
        return ;
    }
    catch (e) {
       log.error "Life360 initializeLife360Connection, error: $e"
       return false;
    }
}

def listCircles() {
    if(logEnable) log.debug "In listCircles - (${state.version})"
    def uninstallOption = false
    if (app.installationState == "COMPLETE") uninstallOption = true
    dynamicPage(name: "listCirclesPage", title: "", install: true, uninstall: true) {
        display()

    	if(testLife360Connection()) {
    	    def urlCircles = "https://api.life360.com/v3/circles.json"
    	    def resultCircles = null

		    httpGet(uri: urlCircles, headers: ["Authorization": "Bearer ${state.life360AccessToken}", timeout: 30 ]) {response ->
    	         resultCircles = response
		    }

    	    def circles = resultCircles.data.circles

            section(getFormat("header-green", "${getImage("Blank")}"+" Select Life360 Circle")) {
        	    input "circle", "enum", multiple: false, required:true, title:"Life360 Circle", options: circles.collectEntries{[it.id, it.name]}, submitOnChange: true
            }

            if(circles) {
                  state.circle = settings.circle
            } else {
    	        getCredentialsErrorPage("Invalid Usernaname or password.")
            }
        }

        if(circle) {
            if(logEnable) log.debug "In listPlaces - (${state.version})"
            if (app.installationState == "COMPLETE") uninstallOption = true

            if (!state?.circle) state.circle = settings.circle

            def url = "https://api.life360.com/v3/circles/${state.circle}/places.json"
            def result = null

            httpGet(uri: url, headers: ["Authorization": "Bearer ${state.life360AccessToken}", timeout: 30 ]) {response ->
     	        result = response
            }

            def places = result.data.places

            state.places = places

            section(getFormat("header-green", "${getImage("Blank")}"+" Select Life360 Place to Match Current Location")) {
                paragraph "Please select the ONE Life360 Place that matches your Hubitat location: ${location.name}"
                thePlaces = places.collectEntries{[it.id, it.name]}
                sortedPlaces = thePlaces.sort { a, b -> a.value <=> b.value }
                input "place", "enum", multiple: false, required:true, title:"Life360 Places: ", options: sortedPlaces, submitOnChange: true
            }
        }

        if(place && circle) {
            if(logEnable) log.debug "In listUsers - (${state.version})"
            if (app.installationState == "COMPLETE") uninstallOption = true
            if (!state?.circle) state.circle = settings.circle

            def url = "https://api.life360.com/v3/circles/${state.circle}/members.json"
            def result = null

            httpGet(uri: url, headers: ["Authorization": "Bearer ${state.life360AccessToken}", timeout: 30 ]) {response ->
     	        result = response
            }

            def members = result.data.members
            state.members = members

            section(getFormat("header-green", "${getImage("Blank")}"+" Select Life360 Members to Import into Hubitat")) {
                theMembers = members.collectEntries{[it.id, it.firstName+" "+it.lastName]}
                sortedMembers = theMembers.sort { a, b -> a.value <=> b.value }
        	    input "users", "enum", multiple: true, required:false, title:"Life360 Members: ", options: sortedMembers, submitOnChange: true
            }

            section(getFormat("header-green", "${getImage("Blank")}"+" Other Options")) {
			    input(name: "logEnable", type: "bool", defaultValue: "false", submitOnChange: "true", title: "Enable Debug Logging", description: "Enable extra logging for debugging.")
    	    }
            display2()
        }
    }
}

def installed() {
    if(logEnable) log.debug "In installed - (${state.version})"
	if(!state?.circle) state.circle = settings.circle

    settings.users.each {memberId->
    	def member = state.members.find{it.id==memberId}
        if(member) {
          // Modified from @Stephack
            def childDevice = childList()
            if(childDevice.find{it.data.vcId == "${member}"}){
                if(logEnable) log.debug "${member.firstName} already exists...skipping"
            } else {
                if(logEnable) log.debug "Creating Life360 Device: " + member
                try{
                    addChildDevice("BPTWorld", "Location Tracker User Driver", "${app.id}.${member.id}", 1234, ["name": "Life360 - ${member.firstName}", isComponent: false])
                }
                catch (e) {
                    log.error "Child device creation failed with error = ${e}"
                }
            }
          // end mod

            if (childDevice)
        	{
        		if(logEnable) log.debug "Child Device Successfully Created"
     			generateInitialEvent (member, childDevice)
       		}
    	}
    }
    createCircleSubscription()
}

def createCircleSubscription() {
    if(logEnable) log.debug "In createCircleSubscription - (${state.version})"
    if(logEnable) log.debug "Remove any existing Life360 Webhooks for this Circle."

    def deleteUrl = "https://api.life360.com/v3/circles/${state.circle}/webhook.json"
    try { // ignore any errors - there many not be any existing webhooks

    	httpDelete (uri: deleteUrl, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response ->
     		result = response}
    		}

    catch (e) {

    	log.debug (e)
    }

    // subscribe to the life360 webhook to get push notifications on place events within this circle

    if(logEnable) log.debug "Create a new Life360 Webhooks for this Circle."
    createAccessToken() // create our own OAUTH access token to use in webhook url
    def hookUrl = "${getApiServerUrl()}/${hubUID}/apps/${app.id}/placecallback?access_token=${state.accessToken}"
    def url = "https://api.life360.com/v3/circles/${state.circle}/webhook.json"
    def postBody =  "url=${hookUrl}"
    def result = null
    try {
     	httpPost(uri: url, body: postBody, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response ->
     	result = response}
    } catch (e) {
        log.debug (e)
    }

    if (result.data?.hookUrl) {
    	    if(logEnable) log.debug "Webhook creation successful."
        log.info "Subscribed to Cirlce Notifications, Confirmation: ${result.data?.hookUrl}"
    	}
    }

def updated() {
    if(logEnable) log.debug "In updated - (${state.version})"
    if (!state?.circle) { state.circle = settings.circle }
	  if(logEnable) log.debug "In updated() method."

    settings.users.each {memberId->
    def externalId = "${app.id}.${memberId}"
		def deviceWrapper = getChildDevice("${externalId}")

    if (!deviceWrapper) { // device isn't there - so we need to create
    		member = state.members.find{it.id==memberId}
         // Modified from @Stephack
            def childDevice = childList()
            if(childDevice.find{it.data.vcId == "${member}"}){
                if(logEnable) log.debug "${member.firstName} already exists...skipping"
            } else {
                if(logEnable) log.debug "Creating Life360 Device: " + member
                try{
                    addChildDevice("BPTWorld", "Location Tracker User Driver", "${app.id}.${member.id}", 1234, ["name": "Life360 - ${member.firstName}", isComponent: false])
                }
                catch (e) {
                    log.error "Child device creation failed with error = ${e}"
                }
            }
        // end mod

        	if (childDevice) {
        		if(logEnable) log.debug "Child Device Successfully Created"
 				generateInitialEvent (member, childDevice)
       		}
    	}
        else {
          	// if(logEnable) log.debug "Find by Member Id = ${memberId}"
    		def member = state.members.find{it.id==memberId}
        	generateInitialEvent (member, deviceWrapper)
        }
    }

    def childDevices = childList()
    if(logEnable) log.debug "Child Devices: ${childDevices}"
    childDevices.each {childDevice->
        def (childAppName, childMemberId) = childDevice.deviceNetworkId.split("\\.")
        if (!settings.users.find{it==childMemberId}) {
            deleteChildDevice(childDevice.deviceNetworkId)
            def member = state.members.find {it.id==memberId}
            if (member) state.members.remove(member)
        }
    }
    // if we updated the device, make sure we reschedule the updateMembers function
    runEvery1Minute(updateMembers)
}

def generateInitialEvent (member, childDevice) {
    if(logEnable) log.debug "In generateInitialEvent - (${state.version})"
    updateMembers() // Perform an update for the first time
    runEvery1Minute(updateMembers) // Schedule to update every minute

}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

def haversine(lat1, lon1, lat2, lon2) {
    def R = 6372.8
    // In kilometers
    def dLat = Math.toRadians(lat2 - lat1)
    def dLon = Math.toRadians(lon2 - lon1)
    lat1 = Math.toRadians(lat1)
    lat2 = Math.toRadians(lat2)

    def a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
    def c = 2 * Math.asin(Math.sqrt(a))
    def d = R * c
    return(d)
}

def placeEventHandler() {
	if(logEnable) log.warn "Life360 placeEventHandler: params= THIS IS THE LINE I'M LOOKING FOR"

/* Avi commented out - let's see how it behaves when a push causes an updatemembers() invocation which will get the right data to the presence event....

    def circleId = params?.circleId
    def placeId = params?.placeId
    def userId = params?.userId
    def direction = params?.direction
    def timestamp = params?.timestamp

    if (placeId == settings.place) {
		def presenceState = (direction=="in")
		def externalId = "${app.id}.${userId}"

		// find the appropriate child device based on my app id and the device network id
		def deviceWrapper = getChildDevice("${externalId}")

		// invoke the generatePresenceEvent method on the child device
		if (deviceWrapper) {
			deviceWrapper.generatePresenceEvent(presenceState, 0)
    		if(logEnable) log.debug "Life360 event raised on child device: ${externalId}"
		}
   		else {
    		log.warn "Life360 couldn't find child device associated with inbound Life360 event."
    	}
    }

*/
// Avi added - all we need in a push event is to force an update of life360 attributes
// Given traveling with other circle members, why not force an update of all members as part of the
// push event instead of just generate a presence event for the actually pushed devices
// of course this may have a performance implication on highly utilized hubs but in most cases
// payload should be nominal.  Especially if debug is turned off for the application and the child devices
updateMembers()
// Avi done
}

def refresh() {
    listCircles()
    updated()
}

def updateMembers(){
    if(logEnable) log.debug "In updateMembers - (${state.version})"
	if (!state?.circle) state.circle = settings.circle

    def url = "https://api.life360.com/v3/circles/${state.circle}/members.json"
    def result = null
    sendCmd(url, result)
}

def sendCmd(url, result){
    def requestParams = [ uri: url, headers: ["Authorization": "Bearer ${state.life360AccessToken}"], timeout: 10 ]
    asynchttpGet("cmdHandler", requestParams)
}

def cmdHandler(resp, data) {
    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        result = resp.getJson()
    	def members = result.members
    	state.members = members

if (logEnable) log.debug result // If in debug then might as well examine the entire payload...

	    settings.users.each {memberId->
    	    def externalId = "${app.id}.${memberId}"
   	        def member = state.members.find{it.id==memberId}

            try {
                // find the appropriate child device based on my app id and the device network id
                def deviceWrapper = getChildDevice("${externalId}")

                // Define all variables required for event and extraInfo
                def address1
                def address2
                def speed
                def speedMetric
                def speedMiles
                def speedKm
                def xplaces
                def lastUpdated
// Avi start changes below
                def battery = Math.round(member.location.battery.toDouble())
                def latitude = member.location.latitude.toFloat()
                def longitude = member.location.longitude.toFloat()
                def place = state.places.find{it.id==settings.place}
                def memberLatitude = new Float (member.location.latitude)
                def memberLongitude = new Float (member.location.longitude)
// no need?     def memberAddress1 = member.location.address1
// no need?     def memberLocationName = member.location.name
                def placeLatitude = new Float (place.latitude)
                def placeLongitude = new Float (place.longitude)
                def placeRadius = new Float (place.radius)
                def distanceAway = haversine(memberLatitude, memberLongitude, placeLatitude, placeLongitude)*1000 // in meters

                // Convert 0 1 to false true
                def charging = member.location.charge == "0" ? "false" : "true"
                def moving = member.location.inTransit == "0" ? "false" : "true"
                def driving = member.location.isDriving == "0" ? "false" : "true"
                def wifi = member.location.wifiState == "0" ? "false" : "true"

                thePlaces = state.places.sort { a, b -> a.name <=> b.name }
                lastUpdated = new Date()

                if (member.avatar != null){
                    avatar = member.avatar
                    avatarHtml =  "<img src= \"${avatar}\">"
                } else {
                    avatar = "not set"
                    avatarHtml = "not set"
                }

                // Check if we are on Life360 free version (address1, address2 = null) and set to "No Data"
                if(member.location.address1 == null || member.location.address1 == "")
                    address1 = "No Data"
                else
                    address1 = member.location.address1

                if(member.location.address2 == null || member.location.address2 == "")
                    address2 = "No Data"
                else
                    address2 = member.location.address2

                // Make sure we are getting location name if returned.  Otherwise use address information
                if(member.location.name != null) {
                    if(member.location.name != member.location.address1) {
                        log.warn "Life360 with States - Caught the issue, changing address1 to place " + member.location.name
                        address1 = member.location.name
                        address2 = "No Data"
                    }
                }

                //Fix Iphone -1 speed
                if(member.location.speed.toFloat() == -1){
                    speed = 0
                    speed = speed.toFloat()}
                else
                    speed = member.location.speed.toFloat()

                if(speed > 0 ) {
                    speedMetric = speed.toDouble().round(2)
                    speedMiles = speedMetric.toFloat() * 2.23694
                    speedMiles = speedMiles.toDouble().round(2)
                    speedKm = speedMetric.toFloat() * 3.6
                    speedKm = speedKm.toDouble().round(2)
                } else {
                    speedMetric = 0
                    speedMiles = 0
                    speedKm = 0
                }

                // Set presence flag (isPresent) based on current distance being within Home radius perimeter
                boolean isPresent = (distanceAway <= placeRadius)

                // Avi: Force address1 to 'Home' if indeed we are within the radius of home to ensure more timely notifications based on push events
                if (isPresent) { address1 = "Home" }

                if(logEnable) log.info "Life360 Update member ($member.firstName), address1: ($address1), location: ($memberLatitude, $memberLongitude), place: ($placeLatitude, $placeLongitude), radius: $placeRadius, dist: $distanceAway, present: $isPresent"

                // Generate Presence Event first
                deviceWrapper.generatePresenceEvent(isPresent, distanceAway)

                // Add supplemental info to device second
                deviceWrapper.extraInfo(address1, address2, battery, charging, distanceAway, member.location.endTimestamp, moving, driving, latitude, longitude, member.location.since, speedMetric, speedMiles, speedKm, wifi, xplaces, avatar, avatarHtml, lastUpdated)

            } catch(e) {
                if(logEnable) log.debug "In cmdHandler - catch - member: ${member}"
                if(logEnable) log.debug e
            }
        }
    }
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def childList() {
	def children = getChildDevices()
    if(logEnable) log.debug "In childList - children: ${children}"
	return children
}

// ********** Normal Stuff **********

def getImage(type) {					// Modified from @Stephack Code
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
    if(type == "checkMarkGreen") return "${loc}checkMarkGreen2.png height=30 width=30>"
    if(type == "optionsGreen") return "${loc}options-green.png height=30 width=30>"
    if(type == "optionsRed") return "${loc}options-red.png height=30 width=30>"
    if(type == "instructions") return "${loc}instructions.png height=30 width=30>"
    if(type == "logo") return "${loc}logo.png height=60>"
}

def getFormat(type, myText="") {			// Modified from @Stephack Code
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def display() {
    setVersion()
    getHeaderAndFooter()
    theName = app.label
    if(theName == null || theName == "") theName = "New Child App"
    section (getFormat("title", "${getImage("logo")}" + " ${state.name} - ${theName}")) {
        paragraph "${state.headerMessage}"
		paragraph getFormat("line")
	}
}

def display2() {
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-size:20px;font-weight:bold'>${state.name} - ${state.version}</div>"
        paragraph "${state.footerMessage}"
	}
}

def getHeaderAndFooter() {
    //if(logEnable) log.debug "In getHeaderAndFooter (${state.version})"
    def params = [
	    uri: "https://raw.githubusercontent.com/bptworld/Hubitat/master/info.json",
		requestContentType: "application/json",
		contentType: "application/json",
		timeout: 30
	]

    try {
        def result = null
        httpGet(params) { resp ->
            state.headerMessage = resp.data.headerMessage
            state.footerMessage = resp.data.footerMessage
        }
        //if(logEnable) log.debug "In getHeaderAndFooter - headerMessage: ${state.headerMessage}"
        //if(logEnable) log.debug "In getHeaderAndFooter - footerMessage: ${state.footerMessage}"
    }
    catch (e) {
        state.headerMessage = "<div style='color:#1A77C9'><a href='https://github.com/bptworld/Hubitat' target='_blank'>BPTWorld Apps and Drivers</a></div>"
        state.footerMessage = "<div style='color:#1A77C9;text-align:center'>BPTWorld<br><a href='https://github.com/bptworld/Hubitat' target='_blank'>Find more apps on my Github, just click here!</a><br><a href='https://paypal.me/bptworld' target='_blank'>Paypal</a></div>"
    }
}
