
# Coming Home

Register as a developer with Nest.  Create a new client at:

[https://developer.nest.com/clients]

Fill out a unique client Name.

Leave OAuth Redirect URI empty.  This app uses PIN authentication.

Permissions needed:

* Away - Read
* ETA - Write

Copy app/src/main/java/net/mceoin/cominghome/oauth/Constants.java-template to Constants.java
and update

* CLIENT_ID
* CLIENT_SECRET

### Images and their sources

* gnome_go_home.png -- GNOME Desktop Icons
* my_briefcase.png -- Harwen - Simple

### Installing App Engine

./gradlew backend:appengineUpdate

