[![forthebadge cc-by](https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png)](https://creativecommons.org/licenses/by/4.0)
## Hafen-Client
the new Haven client

### Languages Used
- JAVA, json
-------------

### Project Description
Hafen-Client, the new Haven client with many settings and fixes, created by @[EnderWiggin](https://github.com/EnderWiggin) (Andrii Gook), @[dolda2000](https://github.com/dolda2000) (Fredrik Tolf). 

This client is used to connect to the server for the Haven & Hearth online game and play said game.

-------------
### Credits, Contributors, Organizations
#### List of contributors: 
@[EnderWiggin](https://github.com/EnderWiggin) (Andrii Gook), @[dolda2000](https://github.com/dolda2000) (Fredrik Tolf), @[tomventura](https://github.com/tomventura) (Tom Ventura), @[elsid](https://github.com/elsid) (Roman Siromakha), @[k-t](https://github.com/k-t) (Marat Vildanov), @[ghandhikus](https://github.com/ghandhikus) (Daniel Debert), @[Fr-Dae](https://github.com/Fr-Dae) (Kervern Anthony), @[surculus12](https://github.com/surculus12), @[jamesblack](https://github.com/jamesblack) (James Black), @[qbalukom](https://github.com/qbalukom) (Jakub Łukomski), @[stachowski](https://github.com/stachowski), @[ProgrammerDan](https://github.com/ProgrammerDan) (Daniel Boston), @[romovs](https://github.com/romovs) (Roman Ovseitsev)

#### License: License.md

-------------
### Project Goal / Target Audience
Changes so far:

- Option to save minimap
- Option to always show kin names
- Option to hide flavor objects
- Changed title font to more readable one
- Mass transfer for inventories (CTRL+ALT+Click drops all similar items, SHIFT+ALT+Click transfers all similar items)
- Mass transfer for stockpiles (SHIFT+Click or CTRL+Click to put/remove single item, hold ALT to move all)
- Zoom in/out camera with numpad +/-
- Quick access for hand slots near portrait
- Increased chat font size and added timestamps to chat messages
- Improved behavior and fixes related to certain in-game actions like smelting, crafting, and tool tooltips
- Added various radar icons for different in-game elements
- Updated display options for terrain, inventory, and other game elements
- Improved UI elements, including combat info, tooltips, and highlighting broken items
- Tweaked and fixed various crashes, menu behaviors, and UI enhancements
	
-------------
### Installation
In order to compile the source tree into a useful executable, the Apache Ant build system is needed. Running `ant` reads the `build.xml' file in the root directory of the source tree and performs the actions described by it to produce the executable output. 

The main external dependencies of the source tree are having a local Java Development Kit (JDK) installed and (as mentioned above) the Apache Ant build system. On a Debian-based Linux system, these can usually be installed via the `default-jdk' and `ant' packages. For other distributions or operating systems, please use local documentation or your own faculties.

#### Windows
make a new folder "haven" and download the jar ![here](https://enderwiggin.github.io/hafen/launcher-hafen.jar) 

#### Ubuntu
Open a terminal and copy paste this lign.
```properties
sudo apt update && sudo apt install default-jre -y && mkdir -p ~/Games/Haven/ && cd ~/Games/Haven/ && wget https://enderwiggin.github.io/hafen/launcher-hafen.jar -O launcher-hafen.jar && chmod -R 775 ./ && exit
```
**Make a launcher with icon**
```properties
cp ~/Games/Haven/Haven-ender.desktop ~/.local/share/applications/
```
It *should* be in application menu => Games

-------------
### Usage
This client is used to connect to the server for the Haven & Hearth online game and play said game. For details regarding the game itself, please refer to its main website at <http://www.havenandhearth.com/>.

-------------
### update
automatic update nothing to do

-------------
### How to Contribute
If you want me to accept back any changes that you might have made into the public client available from the website, the one main rule that we wish to enforce is that we want you to sign over ownership of the code you wish to contribute to us. More detailed information about contributing to the project is available in the README file.

More detailed information about the technical aspects and contributing to the project is available in the README file. 
