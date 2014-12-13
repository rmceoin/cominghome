
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

./gradlew api:appengineUpdate

### Play Store entry

Coming Home is published on the Google Play store at:

https://play.google.com/store/apps/details?id=net.mceoin.cominghome

### Website

http://cominghome.mceoin.net/

