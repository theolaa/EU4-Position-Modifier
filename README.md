# EU4 Position Modifier
This tool helps you alter your mod's map dimensions by quickly and easily adjusting all the positions in your mod.
### Usage
1. Start by selecting your mod from the dropdown
2. Select your save method. See [Save methods](#save-methods)
3. Choose any combination of files that you need updated (Positions, Trade Nodes, and Ambient Objects)
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
X is your horizontal map axis, and Y is your vertical map axis. Height is basically only used for ambient objects, and represents the distance from the surface of the map. Note that while most image editors have their origin in the top left of an image, EU4's map has its origin in the bottom left. This is slightly more intuitive in my opinion, but can cause confusion when comparing edits to coordinates of your map image files, so it's something to be mindful of. To put things in directional terms, when using the **Shift** method -X is West, +X is East, -Y is South, and +Y is North.
#### Modification Method
There are two methods of modification. **Shift** moves the points by the specified offset values, while **Scale** multiplies the points by the specified offset values from the origin in the bottom left of the map.
#### Point-wrapping
While in **Shift** mode, you can enable point-wrapping which will allow points that go out-of-bounds to wrap back inside the map's bound (think Asteroids or Pac-Man).
### EU4 from Epic Games
To use the tool with the Epic Games version of EU4, you'll need run it via a batch file or the command line with `-egs` as the first launch option.
### File Encoding
This tool only supports loading and saving of UTF-8 or Windows 1252 files. Take it from me, supporting more than that is a hellish prospect that I wouldn't wish on my worst enemy.
