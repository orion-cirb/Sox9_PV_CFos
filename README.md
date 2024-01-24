# Sox9_PV_CFos

* **Developed for:** Rachel
* **Team:** Prochiantz
* **Date:** January 2024
* **Software:** Fiji

### Images description

3D images taken with a x25 objective on the Zeiss W1 spinning-disk microscope

4 channels:
  1. *CSU_405:* DAPI
  2. *CSU_488:* Sox9
  3. *CSU_561:* CFos
  4. *CSU_642:* PV

### Plugin description

* Detect Sox9 and PV cells with Stardist
* For each channel, compute background noise as median intensity value of Z projection over min intensity
* For each cell, compute its volume and background-corrected intensity in its respective channel and in the CFos channel
  
### Dependencies

* **3DImageSuite** Fiji plugin
* **Stardist** "StandardFluo.zip" model

### Version history

Version 1 released on January 24, 2024.
