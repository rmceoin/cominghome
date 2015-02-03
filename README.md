
# Coming Home

Register as a developer with Nest.  Create a new client at:

[https://developer.nest.com/clients]

Fill out a unique client Name.

Leave OAuth Redirect URI empty.  This app uses PIN authentication.

Permissions needed:

* Away - Read/Write
* ETA - Write

Copy app/src/main/java/net/mceoin/cominghome/oauth/Constants.java-template to Constants.java
and update

* CLIENT_ID
* CLIENT_SECRET

### Images and their sources

* gnome_go_home.png -- GNOME Desktop Icons
* my_briefcase.png -- Harwen - Simple
* home.png -- Author: Deziner Folio, http://www.dezinerfolio.com/

### Installing Cloud Endpoints

1. Go to [https://console.developers.google.com/] and Create Project
2. Any Project Name can be used.  Take note of the Project ID.
3. Update application in api/src/main/webapp/WEB-INF/appengine-web.xml to match the Project ID.
4. In APIs, enable:
..* Google Cloud Datastore API
..* Google Cloud Messaging for Android
..* Google Cloud SQL
..* Google Maps Android API v2
5. Go to API's and Auth -> Credentials -> Public API Access -> Create new key
6. Create a new key -> Server Key -> In accept request field: 0.0.0.0/0 -> Create
5. Using Android Studio -> Build -> Deploy Module to App Engine


### Play Store entry

Coming Home is published on the Google Play store at:

https://play.google.com/store/apps/details?id=net.mceoin.cominghome

### Website

http://cominghome.mceoin.net/

