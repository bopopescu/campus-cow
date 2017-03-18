# CampusCow
Multiplayer, location-based Android application

# Description
‣ Campus Cow is a location-based, multiplayer, Android application, inspired by the flash game [Find the Invisible Cow](http://findtheinvisiblecow.com/).

‣ Each player can choose to be a cowboy or a cow. There can only be one cow. The first cowboy to find the cow wins the game.

‣ To win, a cowboy must be within 50m of the cow. The color of each player's geomarker tells the player how close they are to the cow.

‣ Red, orange, and yellow correspond to a cowboy being within 200m, 400m, and 600m of the cow's current location. A blue marker means that the cowboy is farther than 600m from the cow.

# Design
‣ Designed a client with Java that sends each device's current location to the server while simultaneously parsing JSON Objects that hold the locations/ID of each player, storing the data into HashMaps

‣ Created a server with Python and Flask that updates and stores all player data in custom dictionaries and returns a JSON Array to the client

‣ Integrated the Google Maps SDK with the application to give the user visual feedback regarding how far they are from the cow

‣ Constructed an algorithm that calculates the distance between each player and the cow and notifies all devices when a cowboy wins

# Screenshots
![alt tag](http://i.imgur.com/95xnUaI.png)
![alt tag](http://i.imgur.com/Ebi4rGr.png)
![alt tag](http://i.imgur.com/BqZfVDF.png)
