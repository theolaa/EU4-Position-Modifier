# EU4 Position Modifier
This tool helps you alter your mod's map dimensions by quickly and easily adjusting all the positions in your mod.
### Usage
1. Start by selecting your mod from the dropdown
2. Select your save method. See [Save methods](#save-methods)
3. Choose any combination of files you need updated (Positions, Trade Nodes, and Ambient Objects)
4. Configure how your existing positions will be modified. See [Configuration Options](#configuration-options)
5. Click Start
### Save Methods
There are three saving methods available
* Overwrite: Directly overwrites the selected files
* Overwrite with backup: Directly overwrites the selected files, but also leaves a copy of the old file prepended with "~" as a backup.
* Separate output folder: Outputs to a separate folder of your choosing (defaults to a folder on your Desktop) so you can non-destructively verify the output. This is the default option.
### Configuration Options
There are a handful of ways you can configure how the tool modifies your positions.
#### Offset Values
EU4 uses a "Y up" coordinate system for its map. That means that X is your horizontal map axis, Z is your vertical map axis, and Y is the axis that runs perpendicular to the map's surface. Y is basically only used for ambient objects, so you'll mostly be using X and Z.
#### Modification Method
There are two methods of modification. **Shift** moves the points by the specified offset values, while **Scale** multiplies the points by the specified offset values from the origin in the bottom left of the map.
#### Point-wrapping
While in **Shift** mode, you can enable point-wrapping which will allow points that go out-of-bounds to wrap back inside the map's bound (think Asteroids or Pac-Man).
### EU4 from Epic Games
To use the tool with the Epic Games version of EU4, you'll need to create a batch file and add `-egs` as the first launch option.
