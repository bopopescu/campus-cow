# Campus Cow
Multiplayer, location-based Android application inspired by the flash game [Find the Invisible Cow](http://findtheinvisiblecow.com/)

# Description
‣ Each player can choose to be a cowboy or a cow. There can only be one cow. The first cowboy to find the cow wins the game.

‣ To win, a cowboy must be within 50m of the cow. If multiple cowboys are within 50m of the cow, the closest cowboy will win.

‣ A cow can see all players on the map and their respective marker colors. A cowboy can only see all other cowboy markers; however, the position of the cow will be revealed every 15 seconds to motivate the cow to move instead of hide in one place.

‣ Red, orange, and yellow markers correspond to a cowboy being within 200m, 400m, and 600m of the cow's current location. A blue marker means that the cowboy is farther than 600m from the cow.

# Design
‣ Designed a client with Java that sends each device's current location to the server while simultaneously parsing JSON Objects that hold the locations/ID of each player, storing the data into HashMaps

‣ Created a server with Python and Flask that updates and stores all player data in custom dictionaries and returns a JSON Array to the client

‣ Integrated the Google Maps SDK with the application to give the user visual feedback regarding how far they are from the cow

‣ Constructed an algorithm that calculates the distance between each player and the cow and notifies all devices when a cowboy wins

# Screenshots
![alt tag](http://i.imgur.com/95xnUaI.png)
![alt tag](http://i.imgur.com/Ebi4rGr.png)
![alt tag](http://i.imgur.com/BqZfVDF.png)
