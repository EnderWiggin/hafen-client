# Haven & Hearth Client -- Copying Rights

This file documents the copying rights of the Haven & Hearth client source tree. 
The source tree is partitioned into a few sections, each with their own copying restrictions. 
These partitions and their respective licensing details are as follows:

### `src/haven` Directory
The files in the `src/haven' directory and its subdirectories are the main source code of the client. 
It is subject to the GNU Lesser General Public License, version 3, as published by the Free Software Foundation. 
A copy of this license can be found in the file `doc/LGPL-3' in this source tree. 
The copyright to all these files is owned by Fredrik Tolf and Björn Johannessen.

### `lib` Directory
- **lib/ext/jogl/:** These files are part of JOGL, the Java OpenGL implementation. 
They are co-owned by Sun Microsystems and the JogAmp community, licensed under a BSD license. 
Please see its homepage, [JogAmp](http://jogamp.org/), for further and current details.
- **lib/jglob.jar:** This is a simple annotation processor written by Fredrik Tolf, considered to be in the public domain. 
Source code can be found [here](http://www.dolda2000.com/gitweb/?p=jglob.git).
- **lib/ext/builtin-res.jar, lib/ext/hafen-res.jar:** These files contain various data files with game content. 
Their contents are owned by Fredrik Tolf and Björn Johannessen. 

### `src/com/jcraft` Directory
These files are part of JCraft's open source implementation of the OGG and Vorbis multimedia formats, licensed under the LPGL, version 2.1, and owned by JCraft, Inc. 
Visit [JCraft](http://www.jcraft.com/jorbis/) for details.

### `src/dolda/xiphutil` Directory
Files constituting a simple library for using JCraft's Jogg/Jorbis libraries, written by Fredrik Tolf, considered to be in the public domain.

### Other Files
- `etc/icon.png': Icon used for the main client window, considered our trademark.
- Majority of other files, including `build.xml', are considered to be in the public domain.

### Build Script
The `build/hafen.jar` file consists of files compiled from the `src` directory, some resources from the `etc` directory, and the extracted JCraft files. 
It is primarily LPGL and a little bit public domain.

### Additional Notes
The Git repository may contain copies of game resources in historical versions (before `hafen-res.jar`). 
They are subject to terms similar to `lib/ext/hafen-res.jar`. 
If you decide to publish a historical version containing these resources, you must add a notice that they are owned by Fredrik Tolf and Björn Johannessen.

If you make changes to incorporate new files or licenses, update this file accordingly. 
For incorporating changes into the mainline client, copyright ownership transfer is required (see README for details).

---

*Authored by Fredrik Tolf. Last updated on 2021-08-29.*
