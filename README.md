# NeuN_CFos

* **Developed for:** Rachel
* **Team:** Prochiantz
* **Date:** September 2023
* **Software:** Fiji

### Images description

3D images taken with a x20 objective

2 channels:
  1. *CSU_488:* NeuN cells
  2. *CSU_642:* CFos cells

### Plugin description

* Detect NeuN and CFos cells with Stardist
* Colocalize NeuN and CFos cells
* For each channel, compute background noise as median intensity value of Z projection over min intensity
* For each cell, compute its volume and intensity/background-corrected intensity in each channel in which it appears

### Dependencies

* **3DImageSuite** Fiji plugin
* **Stardist** "StandardFluo.zip" model

### Version history

Version 1 released on September 13, 2023.
